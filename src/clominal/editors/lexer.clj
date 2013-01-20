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
           (javax.swing.text StyleConstants Utilities DefaultEditorKit DefaultHighlighter$DefaultHighlightPainter SimpleAttributeSet)
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

;; START FROM HERE...

(def parentheses-NG-highlite (DefaultHighlighter$DefaultHighlightPainter. Color/PINK))
(def parentheses-OK-highlite (DefaultHighlighter$DefaultHighlightPainter. Color/CYAN))


(defn remove-highlight
  [text-pane highlight-painter]
  (let [highlighter (. text-pane getHighlighter)
        highlights  (. highlighter getHighlights)]
    (doseq [h highlights]
      (if (= (. h getPainter) highlight-painter)
          (. highlighter removeHighlight h)))))

(defn add-highlight
  [text-pane start end highlight]
  (let [highlighter (. text-pane getHighlighter)]
    (. highlighter addHighlight start end highlight)))
    

(def parentheses-infos {"(" {:src "(" :dst ")" :dir 1}
                        "[" {:src "[" :dst "]" :dir 1}
                        "{" {:src "{" :dst "}" :dir 1}
                        })

(def left-parentheses-infos {"(" ")"
                             "[" "]"
                             "{" "}"
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
  (remove-highlight text-pane parentheses-NG-highlite)
  (remove-highlight text-pane parentheses-OK-highlite)
  (let [max-length (.. text-pane getDocument getLength)
        src-info   (get-parentheses-current text-pane pos)
        dst-info   (if (nil? src-info) nil (get-parentheses-pair text-pane src-info max-length))
        highlight  (cond (nil? src-info) nil
                         (nil? dst-info) parentheses-NG-highlite
                         (< dst-info 0)  parentheses-NG-highlite
                         :else           parentheses-OK-highlite)]
    (if (not (nil? src-info))
        (let [start (src-info :src-pos)
              end   (+ start 1)]
          (add-highlight text-pane start end highlight)))

    (if (not (nil? dst-info))
        (let [dst-pos (Math/abs dst-info)
              start dst-pos
              end   (+ start 1)]
          (add-highlight text-pane start end highlight)))))


(defn add-attribute
  [doc attr-name start end color]
  (let [attr (SimpleAttributeSet.)
        len  (+ (- end start) 1)]
    (StyleConstants/setForeground attr color)
    (. attr addAttribute "name" attr-name)
    (. doc setCharacterAttributes start len attr false)))

(defn get-end
  [text-pane max-length start value]
  (let [len (count value)
        pos (+ start len -1)]
    (cond (< max-length pos)
            nil
          (= value (. text-pane getText start len))
            pos
          :else
            nil)))

(defn print-token
  [text-pane caption start end]
  (let [len   (+ (- end start) 1)
        token (. text-pane getText start len)]
    (println (format "range:%d～%d, [%s] '%s'" start end caption token))))

(defn skip-whitespaces
  [text-pane max-length start]
  (loop [pos start]
    (if (< max-length pos)
        -1
        (let [char (.. text-pane (getText pos 1) (charAt 0))]
          (if (Character/isWhitespace char)
              (recur (+ pos 1))
              pos)))))

(declare
  parse-char
  parse-comment
  parse-string
  parse-list
  parse-atom)

(defn parse-token
  [text-pane max-length start]
  (let [pos start]
    (let [start-pos (skip-whitespaces text-pane max-length pos)]
      (if (< start-pos 0)
          -1
          (let [c   (. text-pane getText start-pos 1)]
            (or (parse-char text-pane max-length start-pos c)
                (parse-string text-pane max-length start-pos c)
                (parse-comment text-pane max-length start-pos c)
                (parse-list text-pane max-length start-pos c)
                (parse-atom text-pane max-length start-pos c)
                start-pos))))))

(defn parse-document
  [text-pane]
  (let [max-length (.. text-pane getDocument getLength)]
    (loop [pos 0]
      (if (and (<= 0 pos) (< pos max-length))
          (let [res (parse-token text-pane max-length pos)]
            (if (= res -1)
                nil
                (recur (+ res 1))))
          nil))))

(defn parse-char
  [text-pane max-length start c]
  (if (not (= c "\\"))
      nil
      (let [pos (+ start 1)
            end (or (get-end text-pane max-length pos "newline")
                    (get-end text-pane max-length pos "space")
                    (get-end text-pane max-length pos "tab")
                    (if (< max-length pos) max-length pos))]
        (add-attribute (. text-pane getDocument) "char" start end (Color. 204 102 0))
        ;(print-token text-pane "char   " start end)
        end)))

(defn parse-comment
  [text-pane max-length start c]
  (if (not (= c ";"))
      nil
      (loop [pos (+ start 1)]
        (if (< max-length pos)
            max-length
            (let [end (or (get-end text-pane max-length pos "\r\n")
                          (get-end text-pane max-length pos "\r")
                          (get-end text-pane max-length pos "\n"))]
              (if (nil? end)
                  (recur (+ pos 1))
                  (do
                    (add-attribute (. text-pane getDocument) "comment" start end (Color. 0 102 0))
                    ;(print-token text-pane "comment" start end)
                    end)))))))

(defn parse-string
  [text-pane max-length start c]
  (if (not (= c "\""))
      nil
      (let [end (loop [pos (+ start 1)]
                  (if (< max-length pos)
                      -1
                      (let [c (. text-pane getText pos 1)]
                        (cond (= c "\\")
                                (recur (+ pos 2))
                              (= c "\"")
                                pos
                              :else
                                (recur (+ pos 1))))))]
        (if (< 0 end)
            (add-attribute (. text-pane getDocument) "string" start end Color/RED))
        ;(print-token text-pane "string " start end)
        end)))

(defn parse-list
  [text-pane max-length start c]
  (let [pair (left-parentheses-infos c)]
    (if (nil? pair)
        nil
        (let [end (loop [pos (+ start 1)]
                    (if (< max-length pos)
                        -1
                        (if (= pair (. text-pane getText pos 1))
                            pos
                            (let [next (parse-token text-pane max-length pos)]
                              (if (= -1 next)
                                  -1
                                  (recur (+ next 1)))))))]
          (add-attribute (. text-pane getDocument) "left-parenthes" start start Color/BLUE)
          (if (< 0 end)
              (add-attribute (. text-pane getDocument) "right-parenthese" end end Color/BLUE))
          end))))

(defn parse-atom
  [text-pane max-length start c]
  (let [end (loop [pos (+ start 1)]
              (if (< max-length pos)
                  max-length
                  (let [val  (. text-pane getText pos 1)
                        char (. val charAt 0)]
                    (if (or (Character/isWhitespace char)
                            (contains? right-parentheses-infos val))
                        (- pos 1)
                        (recur (+ pos 1))))))]
    (add-attribute (. text-pane getDocument) "atom" start end Color/BLACK)
    ;(print-token text-pane "atom   " start end)
    end))

(defn parse-at
  [document offset length]
  (let [element  (. document getCharacterElement offset)
        attr     (. element getAttributes)]
    (println "---")
    (println "offset:" offset ", length:" length ", element:" element ", attr:" attr)))
