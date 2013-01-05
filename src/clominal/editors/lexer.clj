(ns clominal.editors.lexer
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
           (javax.swing.text StyleConstants Utilities DefaultEditorKit DefaultHighlighter$DefaultHighlightPainter)
           (javax.swing.undo UndoManager)
           (java.io File FileInputStream FileWriter FileNotFoundException))
  (:require [clominal.keys :as keys])
  ;(:require [clominal.dialog :as dialog])
  (:use [clominal.utils]))

; (defn make-lexer
;   [text]
;   (let [lexer (JavaLexer. (StringBuilder. text))]
;     (loop [token (. lexer getNextToken)]
;       (if (= nil token)
;           nil
;           (do
;             ; Color the part of the document
;             ; to which the token refers here.
;             )))))


; (def token-parameters
;   [["keyword0" Color/RED]
;    ["keyword1" Color/BLUE]
;    ;...
;    ;...
;    ])

; (defn make-style
;   [color]
;   (let [style (SimpleAttributeSet.)]
;     (StyleConstants/setForeground style color)
;     style))

; (def styles (HashMap.))

; (doseq [parameter token-parameters]
;   (. styles put (parameter 0) (make-style (parameter 1))))

; (defn set-attributes
;   [document t]
;   (. document setCharactorAttributes
;      (. t getCharBegin)
;      (- (. t getCharEnd) (. t getCharBegin))
;      (getStyle (. t getDescription))
;      true))


; namespace clojure.lang;

; LineNumberingPushbackReader pushbackReader = new LineNumberingPushbackReader(rdr);

; for(Object r = LispReader.read(pushbackReader, false, EOF, false);
;     r != EOF;
;     r = LispReader.read(pushbackReader, false, EOF, false))
; {
;     // Maybe, Implement the logic getting the color of target tokens.
;     LINE_AFTER.set(pushbackReader.getLineNumber());
;     COLUMN_AFTER.set(pushbackReader.getColumnNumber());
;     ret = eval(r,false);
;     LINE_BEFORE.set(pushbackReader.getLineNumber());
;     COLUMN_BEFORE.set(pushbackReader.getColumnNumber());
; }


; (defn tokenize
;   [document]
;   (let [reader     
;         pushback-reader (LineNumberingPushbackReader. reader)]
;     ))

; for(Object r = LispReader.read(pushbackReader, false, EOF, false);
;     r != EOF;
;     r = LispReader.read(pushbackReader, false, EOF, false))
; {
;     // Maybe, Implement the logic getting the color of target tokens.
;     LINE_AFTER.set(pushbackReader.getLineNumber());
;     COLUMN_AFTER.set(pushbackReader.getColumnNumber());
;     ret = eval(r,false);
;     LINE_BEFORE.set(pushbackReader.getLineNumber());
;     COLUMN_BEFORE.set(pushbackReader.getColumnNumber());
; }

(defn remove-highlites
  [text-pane]
  (let [hiliter (. text-pane getHighlighter)
        hilites (. hiliter getHighlights)]
    (doseq [hilite hilites]
      (if (instance? DefaultHighlighter$DefaultHighlightPainter (. hilite getPainter))
          (. hiliter removeHighlight hilite)))))

(defn add-highlight
  [text-pane start end color]
  (let [highlighter (. text-pane getHighlighter)
        painter     (DefaultHighlighter$DefaultHighlightPainter. color)]
    (. highlighter addHighlight start end painter)))
    

(def parentheses-infos {"(" {:src "(" :dst ")" :dir 1}
                        "[" {:src "[" :dst "]" :dir 1}
                        "{" {:src "{" :dst "}" :dir 1}
                        ;")" {:src ")" :dst "(" :dir -1}
                        ; "]" {:src "]" :dst "[" :dir -1}
                        ; "}" {:src "}" :dst "{" :dir -1}
                        })

(def right-parentheses-infos {")" {:src ")" :dst "(" :dir -1}
                              "]" {:src "]" :dst "[" :dir -1}
                              "}" {:src "}" :dst "{" :dir -1}
                              })

(defn get-comment-end
  [text-pane base-pos]
  (loop [pos base-pos]
    (let [c (. text-pane getText pos 1)]
      (cond (or (= c "\r") (= c "\n")) pos
            :else                      (recur (+ pos 1))))))
  
(defn get-string-end
  [text-pane base-pos]
  (loop [pos (+ base-pos 1)]
    (let [c (. text-pane getText pos 1)]
      (cond (= c "\\") (recur (+ pos 2))
            (= c "\"") pos
            :else      (recur (+ pos 1))))))

(defn get-char-end
  [text-pane pos]
  (let [next-pos (+ pos 1)]
    (cond (= "newline" (. text-pane getText next-pos 7))
            (+ next-pos 7)
          (= "space" (. text-pane getText next-pos 5))
            (+ next-pos 5)
          (= "tab" (. text-pane getText next-pos 3))
            (+ next-pos 3)
          :else
            (+ next-pos 1))))
  
(defn get-parentheses-current
  [text-pane base-pos]
  (let [info (parentheses-infos (. text-pane getText base-pos 1))]
    (if (nil? info)
        nil
        (assoc info :src-pos base-pos))))

(defn get-parentheses-pair
  [text-pane src-info max-length]
  (if (nil? src-info)
      nil
      (let [dst     (src-info :dst)
            src-pos (src-info :src-pos)
            dir     (src-info :dir)]
        (loop [pos (+ src-pos dir)]
          (if (< max-length pos)
              nil
              (let [c (. text-pane getText pos 1)]
                (cond (= c dst)
                        pos
                      (= c ";")
                        (recur (get-comment-end text-pane pos))
                      (= c "\"")
                        (recur (+ (get-string-end text-pane pos) 1))
                      (= c "\\")
                        (recur (get-char-end text-pane pos))
                      ; (contains? right-parentheses-infos c)
                      ;   (* -1 pos)
                      (contains? parentheses-infos c)
                        (let [next-src-info (get-parentheses-current text-pane pos)
                              next-src-pos  (get-parentheses-pair text-pane next-src-info max-length)]
                          (if (nil? next-src-pos)
                              pos
                              (recur (+ next-src-pos 1))))
                      :else
                        (recur (+ pos 1)))))))))

(defn set-color-parentheses
  [text-pane pos]
  (remove-highlites text-pane)
  (let [max-length (.. text-pane getDocument getLength)
        src-info (get-parentheses-current text-pane pos)
        dst-info (if (nil? src-info) nil (get-parentheses-pair text-pane src-info max-length))
        color    (cond (nil? src-info) nil
                       (nil? dst-info) Color/PINK
                       (< dst-info 0)  Color/PINK
                       :else           Color/CYAN)]
    (if (not (nil? src-info))
        (let [start (src-info :src-pos)
              end   (+ start 1)]
          (add-highlight text-pane start end color)))

    (if (not (nil? dst-info))
        (let [dst-pos (Math/abs dst-info)
              start dst-pos
              end   (+ start 1)]
          (add-highlight text-pane start end color)))))
