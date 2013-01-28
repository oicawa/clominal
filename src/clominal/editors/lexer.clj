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

(def right-parentheses-infos #{")" "]" "}"})

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

(defn print-token
  [document caption start end]
  (let [len   (- end start)
        token (. document getText start len)]
    (println (format "range:%d～%d, [%s] '%s'" start end caption token))))

(defn add-attribute
  [doc attr-name start end color]
  (let [attr (SimpleAttributeSet.)
        len  (- end start)]
    (StyleConstants/setForeground attr color)
    (. attr addAttribute "name" attr-name)
    (print-token doc "re-coloring" start end)
    (. doc setCharacterAttributes start len attr false)))

(defn remove-attributes
  [doc start end]
  (let [attr (SimpleAttributeSet.)
        len  (+ (- end start) 1)]
    (. doc setCharacterAttributes start len attr true)))

(defn get-end
  [document start end value]
  (let [len (count value)
        pos (+ start len)]
    (cond (< end pos)
            nil
          (= value (. document getText start len))
            pos
          :else
            nil)))

(defn skip-whitespaces
  [document start end]
  (loop [pos start]
    (if (< end pos)
        -1
        (let [char (.. document (getText pos 1) (charAt 0))]
          (if (Character/isWhitespace char)
              (recur (+ pos 1))
              pos)))))

(defn parse-char
  [document start end c]
  (if (not (= c "\\"))
      nil
      (let [pos      (+ start 1)
            char-end (or (get-end document pos end "newline")
                         (get-end document pos end "space")
                         (get-end document pos end "tab")
                         (if (< end (+ pos 1)) end (+ pos 1)))]
        (add-attribute document "char" start char-end (Color. 204 102 0))
        (print-token document "char   " start char-end)
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
                                       (+ pos 1)
                                     :else
                                       (recur (+ pos 1))))))]
        (if (< 0 string-end)
            (add-attribute document "string" start string-end Color/RED))
        ;(print-token document "string " start string-end)
        string-end)))

; (defn parse-list-start
;   [document start end c]
;   (let [pair (left-parentheses-infos c)]
;     (if (nil? pair)
;         nil
;         (let [list-end (loop [pos (+ start 1)]
;                          (if (< end pos)
;                              -1
;                              (let [start-pos (skip-whitespaces document pos end)]
;                                (if (< start-pos 0)
;                                    -1
;                                    (if (= pair (. document getText start-pos 1))
;                                        start-pos
;                                        (let [next (parse-token document start-pos end)]
;                                          (if (= -1 next)
;                                              -1
;                                             (recur (+ next 1)))))))))]
;           (add-attribute document "left-parenthesis" start start Color/BLUE)
;           (if (< 0 list-end)
;               (add-attribute document "right-parenthesis" list-end list-end Color/BLUE))
;           list-end))))

; (defn parse-list-end
;   [document start end c]
;   (let [pair (right-parentheses-infos c)]
;     (if (nil? pair)
;         nil
;         (do
;           (add-attribute document "right-parenthesis" start start Color/BLUE)
;           (+ start 1)))))

(defn parse-left-parenthesis
  [document start end c]
  (let [pair (left-parentheses-infos c)]
    (if (nil? pair)
        nil
        (do
          (add-attribute document "left-parenthesis" start (+ start 1) Color/BLUE)
          (+ start 1)))))

(defn parse-right-parenthesis
  [document start end c]
  (let [pair (right-parentheses-infos c)]
    (if (nil? pair)
        nil
        (do
          (add-attribute document "right-parenthesis" start (+ start 1) Color/BLUE)
          (print-token document "right  " start (+ start 1))
          (+ start 1)))))

(defn parse-atom
  [document start end c]
  (let [atom-end (loop [pos start]
                   (if (< end pos)
                       end
                       (let [val  (. document getText pos 1)
                             char (. val charAt 0)]
                         (if (or (Character/isWhitespace char)
                                 (contains? right-parentheses-infos val))
                             pos
                             (recur (+ pos 1))))))]
    (add-attribute document "atom" start atom-end Color/BLACK)
    ;(print-token document "atom   " start atom-end)
    atom-end))

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
                ; (parse-list-start document start-pos end c)
                ; (parse-list-end document start-pos end c)
                (parse-left-parenthesis document start-pos end c)
                (parse-right-parenthesis document start-pos end c)
                (parse-atom document start-pos end c)
                start-pos))))))

(defn parse-document
  [document start end]
  (loop [pos start]
    (if (and (<= 0 pos) (< pos end))
        (let [res (parse-token document pos end)]
          (if (= res -1)
              nil
              (do
                ; add top-level attribute?
                ;(recur (+ res 1))
                (recur res)
                )))
        nil)))

(defn get-offset-which-s-expression
  [document
   offset
   in-parenthesis
   out-parenthesis
   point-offset
   direction
   get-next-pos
   init-out-paren
   next-in-paren]
  (loop [fix nil
         pos offset
         cnt 0]
    (cond (< pos 0)
            fix
          (< (. document getLength) pos)
            fix
          :else
            (let [element  (. document getCharacterElement pos)
                  next-pos (get-next-pos element)
                  attrs    (. element getAttributes)
                  name     (. attrs getAttribute "name")]
              (cond (nil? name)
                      (recur pos (+ next-pos point-offset) cnt)
                    (= name out-parenthesis)
                      (cond (= cnt 0)
                              (if (nil? fix)
                                  (recur pos (init-out-paren pos) cnt) 
                                  fix)
                            (= cnt 1)
                              (+ (+ pos 1) point-offset)
                            :else
                              (recur (+ (+ pos 1) point-offset)
                                     (+ pos direction)
                                     (- cnt 1)))
                    (= name in-parenthesis)
                      (recur pos
                             (+ pos direction)
                             (next-in-paren fix cnt))
                    (= pos next-pos)
                      (recur pos (+ pos direction) cnt)
                    :else
                      (if (= cnt 0)
                          next-pos
                          (recur next-pos (+ next-pos point-offset) cnt)))))))
         
(defn get-offset-backward-s-expression
  [document offset]
  (get-offset-which-s-expression
    document
    offset
    "right-parenthesis"
    "left-parenthesis"
    -1
    -1
    (fn [element] (. element getStartOffset))
    (fn [pos] (- pos 1))
    (fn [fix cnt] (if (nil? fix) 0 (+ cnt 1)))))
      
(defn get-offset-forward-s-expression
  [document offset]
  (get-offset-which-s-expression
    document
    offset
    "left-parenthesis"
    "right-parenthesis"
    0
    1
    (fn [element] (. element getEndOffset))
    (fn [pos] -1)
    (fn [fix cnt] (+ cnt 1))))


(defn get-offset-parent-s-expression
  [document offset]
  (let [start (loop [pos offset]
                (let [next-pos (get-offset-backward-s-expression document pos)]
                  (if (= pos next-pos)
                      pos
                      (recur next-pos))))]
    (loop [pos start]
      (let [element (. document getCharacterElement pos)
            start   (. element getStartOffset)
            attrs   (. element getAttributes)
            name    (. attrs getAttribute "name")]
        (if (= name "left-parenthesis")
            start
            (recur (- start 1)))))))

(defn get-offset-child-s-expression
  [document offset]
  (loop [pos offset]
    (let [element (. document getCharacterElement pos)
          end     (. element getEndOffset)
          attrs   (. element getAttributes)
          name    (. attrs getAttribute "name")]
      (cond (= name "left-parenthesis")
              end
            (= name "right-parenthesis")
              offset
            :else
              (recur end )))))
              
(defn parse-at
  [document offset length]
  (let [pre-start (get-offset-backward-s-expression document offset)
        start     (get-offset-backward-s-expression document pre-start)
        ;start     (get-offset-parent-s-expression document offset)
        pre-end   (get-offset-forward-s-expression document (+ offset length))
        end       (get-offset-forward-s-expression document pre-end)]
    (print-token document "TARGET " start end)
    ;(parse-token document start end)
    (remove-attributes document start end)
    (parse-document document start end)
    ))



(defaction show-attribute
  [text-pane]
  (let [pos      (. text-pane getCaretPosition)
        document (. text-pane getDocument)
        element  (. document getCharacterElement pos)
        start    (. element getStartOffset)
        end      (. element getEndOffset)
        attrs    (. element getAttributes)
        name     (. attrs getAttribute "name")]
    (println "----------")
    (println "pos:" pos ", start:" start ", end:" end)
    (println "name:" name)
    (print-token document "token" start end)))
