(ns plugins.mode.css_mode
  (:import (clojure.lang LineNumberingPushbackReader LispReader)
           (java.awt Font Color Graphics GraphicsEnvironment GridBagLayout Point)
           (java.awt.event ActionEvent)
           (java.awt.im InputMethodRequests)
           (java.beans PropertyChangeListener)
           (java.util HashMap)
           (javax.swing JList InputMap ActionMap JComponent JTextPane JScrollPane Action
                        JLabel JTextField JPanel JOptionPane SwingConstants JFileChooser
                        SwingUtilities AbstractAction)
           (javax.swing.border LineBorder MatteBorder EmptyBorder CompoundBorder)
           (javax.swing.event CaretListener DocumentListener)
           (javax.swing.text StyleConstants Utilities DefaultEditorKit DefaultHighlighter$DefaultHighlightPainter SimpleAttributeSet
                             DefaultStyledDocument StyleContext Segment)

           (javax.swing.undo UndoManager)
           (java.io File FileInputStream FileWriter FileNotFoundException)
           (org.fife.ui.rsyntaxtextarea SyntaxConstants TokenTypes RSyntaxUtilities Token))
  (:require [clominal.keys :as keys])
  (:require [clominal.editors.editor :as editor])
  (:use [clominal.utils]))

(def mode-name "css-mode")


;; ------------------------------
;; Proper Token
;; ------------------------------

(defn is-ignore-token?
  [token]
  (assert token)
  (or (. token isWhitespace)
      (. token isComment)))

;; ------------------------------
;; Parenthesis
;; ------------------------------

(def bracket-pairs {\{ [\} true], \( [\) true], \[ [\] true], \} [\{ false], \) [\( false], \] [\[ false]})

(defn is-*-parenthesis?
  [token is-left?]
  (if (not (= TokenTypes/SEPARATOR (. token type)))
      false
      (let [ch (aget ^chars (. token text) (. token textOffset))]
        (if-let [pair (bracket-pairs ch)]
          (= (pair 1) is-left?)
          false))))
    
(defn is-left-parenthesis?
  [token]
  (is-*-parenthesis? token true))

(defn is-right-parenthesis?
  [token]
  (is-*-parenthesis? token false))

(defn get-index-from-token-list
  [text-pane
   goForward
   iStart
   is-end?
   nest
   step
   bracket
   bracketMatch
   curLine
   doc
   start
   segment]
  (loop [i     iStart
         token nil]
    (if (is-end? i segment)
        nil
        (let [ch (aget ^chars (. segment array) i)]
          (cond (not (or (= ch bracket) (= ch bracketMatch)))
                  (recur (+ i step) token)
                (nil? token)
                  (recur i (. doc getTokenListForLine curLine)) ; !!CAUTION!! There is a defference of original logic.
                :else
                  (let [offset   (+ start (- i (. segment offset)))
                        tmpToken (RSyntaxUtilities/getTokenAtOffset token offset)]
                    (if (not (= (. tmpToken type) Token/SEPARATOR))
                        (if goForward
                            (recur (+ i step) tmpToken)
                            (recur (+ i step) token))
                        (cond (= ch bracket)
                                (do
                                  (reset! nest (+ @nest 1))
                                  (recur (+ i step) token))
                              (not (= ch bracketMatch))
                                (recur (+ i step) token)
                              (not (= @nest 0))
                                (do
                                  (reset! nest (- @nest 1))
                                  (recur (+ i step) token))
                              :else
                                offset))))))))

(defn get-matching-bracket-position
  [text-pane caretPosition]
  (if (<= caretPosition -1)
      -1
      (let [doc          (. text-pane getDocument)
            bracket      (. doc charAt caretPosition)
            pair         (bracket-pairs bracket)
            bracketMatch (if (nil? pair) nil (pair 0))
            goForward    (if (nil? pair) nil (pair 1))]
        (if (nil? pair)
            -1
            (let [map               (. doc getDefaultRootElement)
                  init-line-index   (. map getElementIndex caretPosition)
                  init-line-element (. map getElement init-line-index)
                  init-start        (. init-line-element getStartOffset)
                  init-end          (. init-line-element getEndOffset)
                  token-list        (. doc getTokenListForLine init-line-index)
                  token             (RSyntaxUtilities/getTokenAtOffset token-list caretPosition)]
              (if (not (= (. token type) Token/SEPARATOR))
                  -1
                  (let [limit       (if goForward (. map getElementCount) -1)
                        base-start  (if goForward (+ caretPosition 1) init-start)
                        base-end    (if goForward init-end caretPosition)
                        nest        (atom 0)
                        segment     (Segment.)
                        is-end?     (if goForward
                                        (fn [index segment]
                                          (<= (+ (. segment offset) (. segment count)) index))
                                        (fn [index segment]
                                          (< index (. segment offset))))
                        step        (if goForward 1 -1)]
                    (loop [this-line-index init-line-index
                           this-line       init-line-element
                           this-start      base-start
                           this-end        base-end
                          ]
                      (. doc getText this-start (- this-end this-start) segment)
                      (let [segOffset (. segment offset)
                            iStart    (if goForward segOffset (+ segOffset (. segment count) -1))
                            result    (get-index-from-token-list text-pane goForward iStart is-end? nest step
                                        bracket bracketMatch this-line-index doc this-start segment)]
                        (cond (not (nil? result))
                                result
                              (= (+ this-line-index step) limit)
                                -1
                              :else
                                (let [next-line-index   (+ this-line-index step)
                                      next-line-element (. map getElement next-line-index)
                                      next-start        (. next-line-element getStartOffset)
                                      next-end          (. next-line-element getEndOffset)]
                                  (recur next-line-index next-line-element next-start next-end))))))))))))


;; ------------------------------
;; S expression
;; ------------------------------

(defn get-*ward-s-expression
  [text-pane
   base-offset
   limit?
   step
   get-next-offset
   start-curly?
   end-curly?
   terminate?]
  (assert text-pane)
  (loop [offset            base-offset
         last-proper-token nil
         is-return-token?  false]
    (cond (limit? offset)
            nil
          is-return-token?
            last-proper-token
          :else
            (let [token (editor/get-token text-pane offset)]
              (cond (or (nil? token)
                        (= (. token offset) -1))
                      (recur (+ offset step) last-proper-token false)
                    (is-ignore-token? token)
                      (recur (get-next-offset token) last-proper-token false)
                    (start-curly? token)
                      (let [ret (get-matching-bracket-position text-pane (. token offset))]
                        (if (or (nil? ret) (= -1 ret))
                            nil
                            (recur ret (editor/get-token text-pane ret) true)))
                    (end-curly? token)
                      (recur (. token offset) (if terminate? last-proper-token token) true)
                    :else
                      (recur (. token offset) token true))))))

(defn get-backward-s-expression
  ([text-pane base-offset]
   (get-backward-s-expression text-pane base-offset true))
  ([text-pane base-offset terminate?]
   (get-*ward-s-expression
     text-pane
     (- base-offset 1)
     (fn [offset] (< offset 0))
     -1
     (fn [token] (- (. token offset) 1))
     (fn [token] (is-right-parenthesis? token))
     (fn [token] (is-left-parenthesis? token))
     terminate?)))
 
(defn get-upward-s-expression
  [text-pane base-offset]
  (loop [offset base-offset]
    (let [token (get-backward-s-expression text-pane offset)]
      (if token
          (recur (. token offset))
          (get-backward-s-expression text-pane offset false)))))
        
(defn get-forward-s-expression
  [text-pane base-offset]
  (get-*ward-s-expression
    text-pane
    base-offset
    (fn [offset] (<= (.. text-pane getDocument getLength) offset))
    1
    (fn [token] (+ (. token offset) (. token textCount)))
    (fn [token] (is-left-parenthesis? token))
    (fn [token] (is-right-parenthesis? token))
    true))

(defn get-downward-s-expression
  [text-pane base-offset]
  (loop [offset base-offset]
    (let [token (editor/get-token text-pane offset)]
      (cond (nil? token)
              (recur (+ offset 1))
            (is-ignore-token? token)
              (recur (+ offset 1))
            (is-left-parenthesis? token)
              (get-forward-s-expression text-pane (+ (. token offset) (. token textCount)))
            :else
              nil))))

(defaction backward-s-expression [text-pane]
  (let [offset (. text-pane getCaretPosition)
        pos    (if-let [token (get-backward-s-expression text-pane offset)]
                 (. token offset)
                 offset)]
    (. text-pane setCaretPosition pos)))


(defaction upward-s-expression [text-pane]
  (let [offset (. text-pane getCaretPosition)
        pos    (if-let [token (get-upward-s-expression text-pane offset)]
                 (. token offset)
                 offset)]
    (. text-pane setCaretPosition pos)))


(defaction forward-s-expression [text-pane]
  (let [offset (. text-pane getCaretPosition)
        pos    (if-let [token (get-forward-s-expression text-pane offset)]
                 (+ (. token offset) (. token textCount))
                 offset)]
    (. text-pane setCaretPosition pos)))

(defaction downward-s-expression [text-pane]
  (let [offset (. text-pane getCaretPosition)
        pos    (if-let [token (get-downward-s-expression text-pane offset)]
                 (. token offset)
                 offset)]
    (. text-pane setCaretPosition pos)))

(defaction print-current-token-info [text-pane]
  (let [offset (. text-pane getCaretPosition)
        token  (editor/get-token text-pane offset)]
    (println "[current-token]:" token)))



;; ------------------------------
;; Initialize Mode
;; ------------------------------

(defn init-mode
  [text-pane]
  (println "[css-mode] Loading ...")
  ; keybinds
  (let [settings {
                   '(Ctrl Alt h) backward-s-expression
                   '(Ctrl Alt j) downward-s-expression
                   '(Ctrl Alt k) upward-s-expression
                   '(Ctrl Alt l) forward-s-expression
                   '(Ctrl Alt i) print-current-token-info
                 }]
    (doseq [setting settings]
      (let [keybind (setting 0)
            action  (setting 1)]
        (keys/define-keybind editor/multi-line-maps keybind action))))
  (doto text-pane
    (.setSyntaxEditingStyle SyntaxConstants/SYNTAX_STYLE_CSS)
    (.setEncoding "UTF-8"))
  (println "[css-mode] Loading completed."))




