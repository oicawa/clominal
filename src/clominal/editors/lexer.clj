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
  [document start end value]
  (let [len (count value)
        pos (+ start len -1)]
    (cond (< end pos)
            nil
          (= value (. document getText start len))
            pos
          :else
            nil)))

(defn print-token
  [document caption start end]
  (let [len   (+ (- end start) 1)
        token (. document getText start len)]
    (println (format "range:%d～%d, [%s] '%s'" start end caption token))))

(defn skip-whitespaces
  [document start end]
  (loop [pos start]
    (if (< end pos)
        -1
        (let [char (.. document (getText pos 1) (charAt 0))]
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
  [document start end]
  (let [pos start]
    (let [start-pos (skip-whitespaces document pos end)]
      (if (< start-pos 0)
          -1
          (let [c (. document getText start-pos 1)]
            (or (parse-char document start-pos end c)
                (parse-string document start-pos end c)
                (parse-comment document start-pos end c)
                (parse-list document start-pos end c)
                (parse-atom document start-pos end c)
                start-pos))))))

(defn parse-document
  [document]
  (let [max-length (. document getLength)]
    (loop [pos 0]
      (if (and (<= 0 pos) (< pos max-length))
          (let [res (parse-token document pos max-length)]
            (if (= res -1)
                nil
                (do
                  ; top-levelの属性を付与。
                  (recur (+ res 1))
                  )))
          nil))))

(defn parse-char
  [document start end c]
  (if (not (= c "\\"))
      nil
      (let [pos      (+ start 1)
            char-end (or (get-end document pos end "newline")
                         (get-end document pos end "space")
                         (get-end document pos end "tab")
                         (if (< end pos) end pos))]
        (add-attribute document "char" start char-end (Color. 204 102 0))
        ;(print-token document "char   " start char-end)
        char-end)))

(defn parse-comment
  [document start end c]
  (if (not (= c ";"))
      nil
      (loop [pos (+ start 1)]
        (if (< end pos)
            end
            (let [comment-end (or (get-end document pos end "\r\n")
                                  (get-end document pos end "\r")
                                  (get-end document pos end "\n"))]
              (if (nil? comment-end)
                  (recur (+ pos 1))
                  (do
                    (add-attribute document "comment" start comment-end (Color. 0 102 0))
                    ;(print-token document "comment" start comment-end)
                    comment-end)))))))

(defn parse-string
  [document start end c]
  (if (not (= c "\""))
      nil
      (let [string-end (loop [pos (+ start 1)]
                         (if (< end pos)
                             -1
                             (let [char (. document getText pos 1)]
                               (cond (= char "\\")
                                       (recur (+ pos 2))
                                     (= char "\"")
                                       pos
                                     :else
                                       (recur (+ pos 1))))))]
        (if (< 0 string-end)
            (add-attribute document "string" start string-end Color/RED))
        ;(print-token document "string " start string-end)
        string-end)))

(defn parse-list
  [document start end c]
  (let [pair (left-parentheses-infos c)]
    (if (nil? pair)
        nil
        (let [list-end (loop [pos (+ start 1)]
                         (if (< end pos)
                             -1
                             (let [start-pos (skip-whitespaces document pos end)]
                               (if (= pair (. document getText start-pos 1))
                                   start-pos
                                   (let [next (parse-token document start-pos end)]
                                     (if (= -1 next)
                                         -1
                                         (recur (+ next 1))))))))]
          (add-attribute document "left-parenthesis" start start Color/BLUE)
          (if (< 0 list-end)
              (add-attribute document "right-parenthesis" list-end list-end Color/BLUE))
          list-end))))

(defn parse-atom
  [document start end c]
  (let [atom-end (loop [pos (+ start 1)]
                   (if (< end pos)
                       end
                       (let [val  (. document getText pos 1)
                             char (. val charAt 0)]
                         (if (or (Character/isWhitespace char)
                                 (contains? right-parentheses-infos val))
                             (- pos 1)
                             (recur (+ pos 1))))))]
    (add-attribute document "atom" start atom-end Color/BLACK)
    ;(print-token document "atom   " start atom-end)
    atom-end))

(defn get-offset-backward-s-expression
  [document offset]
  (loop [fix nil
         pos offset
         cnt 0]
    (let [element (. document getCharacterElement pos)
          start   (. element getStartOffset)
          name    (.. element getAttributes (getAttribute "name"))]
      ;(println "fix:" fix ", pos:" pos ", start:" start ", name:" name ", cnt:" cnt)
      (cond (nil? name)
              (recur pos (- start 1) cnt)
            (= name "left-parenthesis")
              (cond (= cnt 0)
                      (if (nil? fix)
                          (recur pos (- pos 1) cnt)
                          fix)
                    (= cnt 1)
                      pos
                    :else
                      (recur pos (- pos 1) (- cnt 1)))
            (= name "right-parenthesis")
              (recur pos (- pos 1) (if (nil? fix) 0 (+ cnt 1)))
            (= pos start)
              (recur pos (- pos 1) cnt)
            :else
              (if (= cnt 0)
                  start
                  (recur start (- start 1) cnt))))))

(defn get-offset-forward-s-expression
  [document offset]
  (loop [fix nil
         pos offset
         cnt 0]
    (let [element (. document getCharacterElement pos)
          end     (. element getEndOffset)
          name    (.. element getAttributes (getAttribute "name"))]
      ;(println "fix:" fix ", pos:" pos ", end:" end ", name:" name ", cnt:" cnt)
      (cond (nil? name)
              (recur pos end cnt)
            (= name "right-parenthesis")
              (cond (= cnt 0)
                      (if (nil? fix)
                          pos
                          fix)
                    (= cnt 1)
                      (+ pos 1)
                    :else
                      (recur (+ pos 1) (+ pos 1) (- cnt 1)))
            (= name "left-parenthesis")
              (recur pos (+ pos 1) (+ cnt 1))
            (= pos end)
              (recur pos (+ pos 1) cnt)
            :else
              (if (= cnt 0)
                  end
                  (recur end end cnt))))))

(defn get-outer-s-expression-start
  [document offset]
  (loop [pos (- offset 1)
         cnt 0]
    (let [element (. document getCharacterElement pos)
          name    (.. element getAttributes (getAttribute "name"))]
      (cond (= name "left-parenthesis")
              (if (= cnt 0)
                  pos
                  (recur (- pos 1) (- cnt 1)))
            (= name "right-parenthesis")
              (recur (- pos 1) (+ cnt 1))
            :else
              (recur (- pos (. element getStartOffset)) cnt)))))
              
; (defn parse-at
;   [document offset length]
;   (let [caret      (+ offset length)
;         prev       (. document getCharacterElement offset)
;         prev-start (. prev getStartOffset)
;         prev-end   (. prev getEndOffset)
;         next-start (skip-whitespaces document prev-end (. document getLength))
;         next       (. document getCharacterElement next-start)
;         next-end   (. next getEndOffset)]
;     (if (= caret prev-end)
;         (parse-token document prev-start next-end))
;     ))

(defn parse-at
  [document offset length]
  (parse-document document))



(defaction show-attribute
  [text-pane]
  (let [pos   (. text-pane getCaretPosition)
        attr  (. text-pane getCharacterAttributes)]
    (println "----------")
    (println "pos:" pos)
    (println "attr:" attr)
    (println "attr-name:" (. attr getAttribute "name"))))
