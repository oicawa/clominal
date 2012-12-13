(ns clominal.editors.editor
  (:import (java.awt Font Color Graphics GraphicsEnvironment GridBagLayout Point)
           (java.awt.event ActionEvent)
           (java.awt.im InputMethodRequests)
           (java.beans PropertyChangeListener)
           (java.util HashMap)
           (javax.swing InputMap ActionMap JComponent JTextPane JScrollPane Action
                        JLabel JTextField JPanel JOptionPane SwingConstants JFileChooser
                        SwingUtilities AbstractAction)
           (javax.swing.border LineBorder MatteBorder EmptyBorder CompoundBorder)
           (javax.swing.event CaretListener DocumentListener)
           (javax.swing.text StyleConstants Utilities DefaultEditorKit)
           (java.io File FileInputStream FileWriter FileNotFoundException))
  (:require [clominal.utils.guiutils :as guiutils]
            [clominal.utils.env :as env]
            [clominal.keys.keymap :as keymap]
            ))

;;------------------------------
;;
;; Maps (InputMaps & ActionMaps)
;;
;;------------------------------
(def maps (doto (HashMap.)
            (.put "default" (let [editor (JTextPane.)]
                              [(. editor getInputMap JComponent/WHEN_FOCUSED)
                               (. editor getActionMap)]))))

;;------------------------------
;;
;; Editor actions
;;
;;------------------------------

;;
;; Editor action creator
;;
(defmacro defaction
  [name bindings & body]
  (if (vector? bindings)
      (let [source (bindings 0)
            evt    (gensym "evt")]
        `(def ~name (proxy [AbstractAction] []
                      (actionPerformed [~evt]
                        ((fn [~source] ~@body)
                         (. ~evt getSource))))))
      (let [value (eval bindings)]
        (cond (string? value)          (let [default-actionmap ((. maps get "default") 1)]
                                         `(def ~name (. ~default-actionmap get ~bindings)))
              (instance? Action value) `(def ~name ~bindings)))))


;;
;; Caret move action group.
;;

;; Charactor
(defaction forward DefaultEditorKit/forwardAction)
(defaction backward DefaultEditorKit/backwardAction)

;; Word
(defaction beginWord DefaultEditorKit/beginWordAction)
(defaction endWord DefaultEditorKit/endWordAction)
(defaction nextWord DefaultEditorKit/nextWordAction)
(defaction previousWord DefaultEditorKit/previousWordAction)

;; Line
(defaction up DefaultEditorKit/upAction)
(defaction down DefaultEditorKit/downAction)
(defaction beginLine DefaultEditorKit/beginLineAction)
(defaction endLine DefaultEditorKit/endLineAction)

;; Paragraph
(defaction beginParagraph DefaultEditorKit/beginParagraphAction)
(defaction endParagraph DefaultEditorKit/endParagraphAction)

;; Page
(defaction pageDown DefaultEditorKit/pageDownAction)
(defaction pageUp DefaultEditorKit/pageUpAction)

;; Document
(defaction begin DefaultEditorKit/beginAction)
(defaction end DefaultEditorKit/endAction)


;;
;; Delete action group.
;;

;; Charactor
(defaction deletePrevChar DefaultEditorKit/deletePrevCharAction)
(defaction deleteNextChar DefaultEditorKit/deleteNextCharAction)

;; Word
(defaction deletePrevWord DefaultEditorKit/deletePrevWordAction)
(defaction deletenextword DefaultEditorKit/deleteNextWordAction)


;;
;; Select group.
;;

(defaction selectWord DefaultEditorKit/selectWordAction)
(defaction selectLine DefaultEditorKit/selectLineAction)
(defaction selectParagraph DefaultEditorKit/selectParagraphAction)
(defaction selectAll DefaultEditorKit/selectAllAction)


;;
;; Move selection group.
;;

;; Selection
(defaction selectionBegin DefaultEditorKit/selectionBeginAction)
(defaction selectionEnd DefaultEditorKit/selectionEndAction)


;; Charactor
(defaction selectionForward DefaultEditorKit/selectionForwardAction)
(defaction selectionBackward DefaultEditorKit/selectionBackwardAction)

;; Word
(defaction selectionBeginWord DefaultEditorKit/selectionBeginWordAction)
(defaction selectionEndWord DefaultEditorKit/selectionEndWordAction)
(defaction selectionNextWord DefaultEditorKit/selectionNextWordAction)
(defaction selectionPreviousWord DefaultEditorKit/selectionPreviousWordAction)

;; Line
(defaction selectionBeginLine DefaultEditorKit/selectionBeginLineAction)
(defaction selectionEndLine DefaultEditorKit/selectionEndLineAction)
(defaction selectionUp DefaultEditorKit/selectionUpAction)
(defaction selectionDown DefaultEditorKit/selectionDownAction)

;; Paragraph
(defaction selectionBeginParagraph DefaultEditorKit/selectionBeginParagraphAction)
(defaction selectionEndParagraph DefaultEditorKit/selectionEndParagraphAction)


;;
;; Edit operation group.
;;

(defaction copy DefaultEditorKit/copyAction)
(defaction cut DefaultEditorKit/cutAction)
(defaction paste DefaultEditorKit/pasteAction)


;;
;; Other group.
;;

(defaction defaultKeyTyped DefaultEditorKit/defaultKeyTypedAction)
(defaction insertBreak DefaultEditorKit/insertBreakAction)
(defaction insertTab DefaultEditorKit/insertTabAction)
(defaction insertContent DefaultEditorKit/insertContentAction)
(defaction beep DefaultEditorKit/beepAction)
(defaction readOnly DefaultEditorKit/readOnlyAction)
(defaction writable DefaultEditorKit/writableAction)


;;
;; File action group.
;;

(defaction openFile
  [text-pane]
  (let [chooser   (JFileChooser. (str "~" env/os-file-separator))
        result    (. chooser showOpenDialog nil)]
    (if (= JFileChooser/APPROVE_OPTION result)
        (. text-pane open (.. chooser getSelectedFile getAbsolutePath)))))

(defaction saveFile
  [text-pane]
  (if (= nil (. text-pane getPath))
      (. text-pane saveAs)
      (. text-pane save)))

(defaction changeBuffer
  [text-pane]
  (println "called 'changeBuffer'."))

;;
;; Font utilities
;;
(def new-title "Untitled")

(defn get-font-names
  []
  (doseq [font (.. GraphicsEnvironment getLocalGraphicsEnvironment getAvailableFontFamilyNames)]
    (println font)))

(defn set-font
  [component parameters]
  (let [name (parameters 0)
        type (parameters 1)
        size (parameters 2)]
    (. component setFont (Font. name type size))))

(defn printSize
  [component name]
  (let [psize  (. component getPreferredSize)
        pheight (. psize getHeight)
        pwidth  (. psize getWidth)
        size   (. component getSize)
        height (. size getHeight)
        width  (. size getWidth)]
    (println name " preferred:[" pwidth "," pheight "], normal:[" width "," height "]")))


;;
;; Interfaces
;;
(definterface ITextLineNumber
  (setBorderGap [value])
  (setPreferredWidth [])
  (setCurrentLineForeground [value])
  (setDigitAlignment [value])
  (setMinimumDisplayDigits [value])
  (isCurrentLine [rowStartOffset])
  (getCurrentLineForeground [])
  (getTextLineNumber [rowStartOffset])
  (getOffsetX [availableWidth stringWidth])
  (getOffsetY [rowStartOffset fontMetrics])
  (documentChanged [])
  )

(defn make-text-line-number
  [text-component input-minimumDisplayDigits]
  (let [LEFT                  0.0
        CENTER                0.5
        RIGHT                 1.0
        OUTER                 (MatteBorder. 0 0 0 2 Color/GRAY)
        HEIGHT                (- Integer/MAX_VALUE 1000000)
        ; Properties that can be changed
        updateFont            (atom nil)
        borderGap             (atom nil)
        currentLineForeground (atom nil)
        digitAlignment        (atom nil)
        minimumDisplayDigits  (atom input-minimumDisplayDigits)
        ; Keep history information to reduce the number of times the component needs to be repainted
        lastDigits            (atom nil)
        lastHeight            (atom nil)
        lastLine              (atom nil)
        fonts                 (HashMap.)
        text-line-number      (proxy [JPanel ITextLineNumber CaretListener DocumentListener PropertyChangeListener] []
                                (getUpdateFont [] @updateFont)
                                (setUpdateFont [value] (dosync (reset! updateFont value)))
                                (getBorderGap [] @borderGap)
                                (setBorderGap [value]
                                  (let [inner (EmptyBorder. 0 value 0 value)]
                                    (dosync
                                      (reset! borderGap value)
                                      (reset! lastDigits 0))
                                    (doto this
                                      (.setBorder (CompoundBorder. OUTER inner))
                                      (.setPreferredWidth))))
                                (getCurrentLineForeground  []
                                  (if (= nil @currentLineForeground)
                                      (. this getForeground)
                                      @currentLineForeground))
                                (setCurrentLineForeground [value] (dosync (reset! currentLineForeground value)))
                                (getDigitAlignment [] @digitAlignment)
                                (setDigitAlignment [value]
                                  (let [calc-value (cond (< 1.0 value) 1.0
                                                         (< value 0.0) -1.0
                                                         :else         value)]
                                    (dosync (reset! digitAlignment calc-value))))
                                (getMinimumDisplayDigits [] @minimumDisplayDigits)
                                (setMinimumDisplayDigits [value]
                                  (dosync (reset! minimumDisplayDigits value))
                                  (. this setPreferredWidth))
                                (setPreferredWidth []
                                  (let [root   (.. text-component getDocument getDefaultRootElement)
                                        lines  (. root getElementCount)
                                        digits (Math/max (. (String/valueOf lines) length) @minimumDisplayDigits)]
                                    (if (not (= @lastDigits digits))
                                        (let [fontMetrics    (. this getFontMetrics (. this getFont))
                                              width          (* (.. fontMetrics (charWidth \0)) digits)
                                              insets         (. this getInsets)
                                              preferredWidth (+ (. insets left) (. insets right) width)
                                              dimension      (. this getPreferredSize)]
                                          (. dimension setSize preferredWidth HEIGHT)
                                          (. this setPreferredSize dimension)
                                          (. this setSize dimension)
                                          (dosync (reset! lastDigits digits))))))
                                (paintComponent [graphics]
                                  (proxy-super paintComponent graphics)
                                  (let [font           (. text-component getFont)
                                        fontMetrics    (. text-component getFontMetrics font)
                                        insets         (. this getInsets)
                                        availableWidth (- (.. this getSize width) (. insets left) (. insets right))
                                        clip           (. graphics getClipBounds)
                                        rowStartOffset (. text-component viewToModel (Point. 0 (. clip y)))
                                        endOffset      (. text-component viewToModel (Point. 0 (+ (. clip y) (. clip height))))]
                                    (loop [startOffset rowStartOffset]
                                      (if (< endOffset startOffset)
                                          nil
                                         (do
                                           (. graphics setColor (if (. this isCurrentLine startOffset)
                                                                    (. this getCurrentLineForeground)
                                                                    (. this getForeground)))
                                           (let [lineNumber  (. this getTextLineNumber startOffset)
                                                 stringWidth (.. fontMetrics (stringWidth lineNumber))
                                                 x           (+ (. this getOffsetX availableWidth stringWidth) (. insets left))
                                                 y           (. this getOffsetY startOffset fontMetrics)]
                                             (. graphics drawString lineNumber (. x intValue) (. y intValue))
                                             )
                                           (recur (+ 1 (Utilities/getRowEnd text-component startOffset))))))))
                                (isCurrentLine [rowStartOffset]
                                  (let [caretPosition (. text-component getCaretPosition)
                                        root          (.. text-component getDocument getDefaultRootElement)
                                        offset-index  (. root getElementIndex rowStartOffset)
                                        caret-index   (. root getElementIndex caretPosition)]
                                    (= offset-index caret-index)))
                                (getTextLineNumber [rowStartOffset]
                                  (let [root  (.. text-component getDocument getDefaultRootElement)
                                        index (. root getElementIndex rowStartOffset)
                                        line  (. root getElement index)]
                                    (if (= (. line getStartOffset) rowStartOffset)
                                        (String/valueOf (+ 1 index))
                                        "")))
                                (getOffsetX [availableWidth stringWidth]
                                  (* (- availableWidth stringWidth) @digitAlignment))
                                (getOffsetY [rowStartOffset fontMetrics]
                                  (let [r          (. text-component modelToView rowStartOffset)
                                        lineHeight (. fontMetrics getHeight)
                                        y          (+ (. r y) (. r height))
                                        descent    0]
                                    (if (= (. r height) lineHeight)
                                        (- y (. fontMetrics getDescent))
                                        (let [root  (.. text-component getDocument getDefaultRootElement)
                                              index (. root getElementIndex rowStartOffset)
                                              line  (. root getElement index)]
                                          (loop [i       0
                                                 discent 0]
                                            (if (<= (. line getElementCount) i)
                                                (- y discent)
                                                (let [child      (. line getElement i)
                                                      attributes (. child getAttributes)
                                                      fontFamily (. attributes getAttribute StyleConstants/FontFamily)
                                                      fontSize   (. attributes getAttribute StyleConstants/FontSize)
                                                      key        (str fontFamily fontSize)
                                                      fm         (let [fm1 (. fonts get key)]
                                                                    (if (= nil fm1)
                                                                        (let [font (Font. fontFamily Font/PLAIN fontSize)
                                                                              fm2  (. text-component getFontMetrics font)]
                                                                          (. fonts put key fm2)
                                                                          fm2)
                                                                        fm1))]
                                                  (recur (+ 1 i) (Math/max descent (. fm getDescent))))))))))
                                (caretUpdate [evt]
                                  (let [caretPosition (. text-component getCaretPosition)
                                        root          (.. text-component getDocument getDefaultRootElement)
                                        currentLine   (. root getElementIndex caretPosition)]
                                    (if (not (= @lastLine currentLine))
                                        (do
                                          (. this repaint)
                                          (dosync (reset! lastLine currentLine))))))
                                (changedUpdate [evt] (. this documentChanged))
                                (insertUpdate [evt] (. this documentChanged))
                                (removeUpdate [evt] (. this documentChanged))
                                (documentChanged []
                                  (SwingUtilities/invokeLater
                                    (fn []
                                      (let [preferredHeight (.. text-component getPreferredSize height)]
                                        (if (not (= @lastHeight preferredHeight))
                                            (do
                                              (doto this
                                                (.setPreferredWidth)
                                                (.repaint))
                                              (dosync (reset! lastHeight preferredHeight))))))))
                                (propertyChange [evt]
                                  (if (instance? Font (. evt getNewValue))
                                      (if (= nil @updateFont)
                                          (let [newFont (. evt getNewValue)]
                                            (dosync (reset! lastDigits 0))
                                            (doto this
                                              (.setFont newFont)
                                              (.setPreferredWidth)))
                                          (. this repaint)))))
         ]
    (doto text-line-number
      (.setFont (. text-component getFont))
      (.setBorderGap 5)
      (.setCurrentLineForeground Color/RED)
      (.setDigitAlignment RIGHT)
      (.setMinimumDisplayDigits @minimumDisplayDigits))

    (.. text-component getDocument (addDocumentListener text-line-number))
    (.. text-component (addCaretListener text-line-number))
    (.. text-component (addPropertyChangeListener "font" text-line-number))

    text-line-number))


(defn make-improved-imr
  [original-imr]
  (proxy [InputMethodRequests] []
    (cancelLatestCommittedText [attributes]
      (. original-imr cancelLatestCommittedText attributes))
    (getCommittedText [beginIndex endIndex attributes]
      (. original-imr getCommittedText beginIndex endIndex attributes))
    (getCommittedTextLength []
      (. original-imr getCommittedTextLength))
    (getInsertPositionOffset []
      (. original-imr getInsertPositionOffset))
    (getLocationOffset [x y]
      (. original-imr getLocationOffset x y))
    (getSelectedText [attributes]
      (. original-imr getSelectedText attributes))
    (getTextLocation [offset]
      (let [rect (. original-imr getTextLocation offset)]
        (. rect setLocation (- (. rect x) 10) (- (. rect y) 45))
        rect))))

(definterface ITextPane
  (getPath [])
  (setPath [target])
  (modified [modified?])
  (save [])
  (saveAs [])
  (open [target])
  (getStatusBar [])
  (setKeyStroke [keystroke])
  )

(defn count-by-pattern
  [value pattern start end]
  (loop [idx start
         cnt 0]
    (if (< end idx)
        cnt
        (let [new-idx (. value indexOf pattern idx)]
          (if (< new-idx 0)
              cnt
              (recur (+ new-idx 1) (+ cnt 1)))))))

(defn make-editor
  [tabs]
  (let [;
        ; Status Bar
        ;
        statusbar     (JPanel.)
        keystrokes    (JLabel. "")
        filler        (JLabel. "")
        char-code     (JLabel. "--")

        ;
        ; Text Editor
        ;
        file-path    (atom nil)
        improved-imr (atom nil)
        text-pane    (proxy [JTextPane ITextPane] []
                       (getPath []
                         @file-path)
                       (setPath [path]
                         (let [full-path (env/get-absolute-path path)]
                           (dosync (reset! file-path full-path))))
                       (modified [modified?]
                         (if (= nil @file-path)
                             new-title
                             (let [title (. (File. @file-path) getName)
                                   root  (.. this getParent getParent getParent)
                                   index (. tabs indexOfComponent root)]
                               (. tabs setTitleAt index (str title (if modified? " *" ""))))))
                       (save
                         []
                         (try
                           (with-open [stream (FileWriter. (. this getPath))]
                             (doto this
                               (.write stream)
                               (.modified false)))
                           (catch Exception e
                             (. e printStackTrace)
                             (. JOptionPane (showMessageDialog nil (. e getMessage) "Error..." JOptionPane/OK_OPTION)))))
                       (saveAs
                         []
                         (let [chooser (JFileChooser. (str "~" env/os-file-separator))
                               result  (. chooser showSaveDialog nil)]
                           (if (= JFileChooser/APPROVE_OPTION result)
                               (doto this
                                 (.setPath (.. chooser getSelectedFile getAbsolutePath))
                                 (.save)
                                 (.modified false)))))
                       (open
                         [target]
                         (let [doc       (. this getDocument)
                               kit       (. this getEditorKit)
                               file      (File. target)
                               text-pane this]
                           (do
                             (. this setPath target)
                             (try
                               (with-open [stream (FileInputStream. @file-path)]
                                 (doto this
                                   (.read stream doc)
                                   (.modified false))
                                 (doto (. this getDocument)
                                   (.addDocumentListener (proxy [DocumentListener] []
                                                           (changedUpdate [evt] )
                                                           (insertUpdate [evt] (. text-pane modified true))
                                                           (removeUpdate [evt] (. text-pane modified true))))))
                               (catch FileNotFoundException _ true)
                               (catch Exception e
                                 (. e printStackTrace)
                                 (. JOptionPane (showMessageDialog nil (. e getMessage) "Error..." JOptionPane/OK_OPTION)))))))
                       (getInputMethodRequests []
                         (if (= nil @improved-imr)
                             (let [original-imr (proxy-super getInputMethodRequests)]
                               (reset! improved-imr (make-improved-imr original-imr))
                               @improved-imr)
                             @improved-imr))
                       (setKeyStroke [keystroke]
                         (if (= nil keystroke)
                             (. keystrokes setText "")
                             (let [current (. keystrokes getText)]
                               (. keystrokes setText (if (= current "")
                                                         (keymap/str-keystroke keystroke)
                                                         (str current ", " (keymap/str-keystroke keystroke))))))))
        scroll       (JScrollPane. text-pane
                                   JScrollPane/VERTICAL_SCROLLBAR_ALWAYS
                                   JScrollPane/HORIZONTAL_SCROLLBAR_ALWAYS)
        ;
        ; Root Panel
        ;
        root-panel   (proxy [JPanel] []
                       (requestFocusInWindow []
                         (. text-pane requestFocusInWindow)))
                ;
        ; Line Numbers
        ;
        text-line-number (make-text-line-number text-pane 3)
        
        ;
        ; Others
        ;
        map-vec           (. maps get "default")
        default-inputmap  (map-vec 0)
        default-actionmap (map-vec 1)
        default-fonts     {:linux   ["Takaoゴシック" Font/PLAIN 14]
                           :windows ["ＭＳ ゴシック" Font/PLAIN 14]}
        ]
    ;
    ; Editor Area
    ;
    (doto text-pane
      (.setName "text-pane")
      (.setInputMap  JComponent/WHEN_FOCUSED default-inputmap)
      (.setActionMap default-actionmap)
      (.enableInputMethods true)
      ; (.addCaretListener (proxy [CaretListener] []
      ;                      (caretUpdate [evt]
      ;                        (let [src (. evt getSource)
      ;                              crt (. src getCaret)
      ;                              val (. src getText)
      ;                              dot (. crt getDot)
      ;                              row (count-by-pattern val "\n" 0 dot)
      ;                              clm (- dot (. val lastIndexOf "\n" dot))]
      ;                          (. cursor setText (format cursor-format row clm))))))
                               )
    (doto (. text-pane getDocument)
      (.addDocumentListener (proxy [DocumentListener] []
                              (changedUpdate [evt] )
                              (insertUpdate [evt] (. text-pane modified true))
                              (removeUpdate [evt] (. text-pane modified true)))))


    ;
    ; Scroll Pane
    ;
    (. scroll setRowHeaderView text-line-number)

    ;
    ; StatusBar
    ;
    (doto statusbar
      (.setName "statusbar")
      (.setPreferredSize nil)
      (.setLayout (GridBagLayout.))
      (guiutils/grid-bag-layout
        :gridx 0, :gridy 0
        :anchor :WEST
        keystrokes
        :gridx 1, :gridy 0
        :fill :HORIZONTAL
        :weightx 1.0
        filler
        :gridx 2, :gridy 0
        :weightx 0.0
        char-code
        ))

    ;
    ; Root Panel
    ;
    (doto root-panel
      (.setLayout (GridBagLayout.))
      (.setName "root-panel")
      (guiutils/grid-bag-layout
        :fill :BOTH
        :gridx 0
        :gridy 0
        :weightx 1.0
        :weighty 1.0
        scroll
        :fill :HORIZONTAL
        :weightx 1.0
        :weighty 0.0
        :gridy 1
        statusbar))
    (doseq [component [text-pane keystrokes char-code]]
      (set-font component (default-fonts (env/get-os-keyword))))

    root-panel
    ))

