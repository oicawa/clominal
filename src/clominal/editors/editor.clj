(ns clominal.editors.editor
  (:import (java.lang Thread)
           (java.awt Font Color Graphics GraphicsEnvironment GridBagLayout Point)
           (java.awt.event ActionEvent)
           (java.awt.im InputMethodRequests)
           (java.beans PropertyChangeListener)
           (java.util HashMap)
           (javax.swing JList InputMap ActionMap JComponent JTextPane JEditorPane JTextArea JScrollPane Action
                        JLabel JTextField JPanel JOptionPane SwingConstants JFileChooser
                        SwingUtilities AbstractAction JColorChooser)
           (javax.swing.border LineBorder MatteBorder EmptyBorder CompoundBorder)
           (javax.swing.event CaretListener DocumentListener)
           (javax.swing.text StyleConstants Utilities DefaultEditorKit DefaultHighlighter$DefaultHighlightPainter SimpleAttributeSet
                             DefaultStyledDocument StyleContext)
           (javax.swing.undo UndoManager)
           (java.io File FileInputStream FileWriter FileNotFoundException StringReader)
           (clojure.lang LineNumberingPushbackReader LispReader))
  (:require [clominal.keys :as keys])
  (:require [clominal.editors.lexer :as lexer])
  (:require [clojure.contrib.string :as string])
  (:use [clominal.utils]))


;;------------------------------
;;
;; Global values
;;
;;------------------------------

;
; Constant
;
(def new-title "Untitled")

;
; Key Maps
;
(def maps (keys/make-keymaps (JTextPane.) JComponent/WHEN_FOCUSED))

;
; Font utilities
;
(defn get-font-names
  []
  (.. GraphicsEnvironment getLocalGraphicsEnvironment getAvailableFontFamilyNames))

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

;;------------------------------
;;
;; Text editor creator
;;
;;------------------------------

;; Interfaces
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
  (documentChanged [] ))

(definterface ITextPane
  (getPath [])
  (setPath [target])
  (getModified [])
  (setModified [modified?])
  (save [])
  (saveAs [])
  (getStatusBar [])
  (getTabs [])
  (getRoot [])
  (getTabIndex [])
  (getUndoManager [])
  (isMark [])
  (setMark [position]))

(definterface ITextEditor
  (getModified [])
  (getTextPane [])
  (getScroll [])
  (getPath []))

;
; Text Line Number
;
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


;
; Improved InputMethodRequests.
;
; (When using the input method, for display the candidate conversion window at the correct position.
;  But this is only in windows. 
;  I don't know that why the JVM for linux doesn't call InputMethodRequests/getTextLocation method.)
;
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

(defn make-color-parentheses-thread
  [text-pane position]
  (Thread. (fn []
             (lexer/set-color-parentheses text-pane position))))

;
; Text Editor
;
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
        modified     (atom false)
        ime-mode     (atom nil)
        um           (UndoManager.)
        is-marked    (atom false)
        text-pane    (proxy [JTextPane ITextPane clominal.keys.IKeybindComponent] []
                       (getPath []
                         @file-path)
                       (setPath [path]
                         (let [full-path (get-absolute-path path)]
                           (dosync (reset! file-path full-path))))
                       (getModified [] @modified)   
                       (setModified [modified?]
                         (reset! modified modified?)
                         (if (= nil @file-path)
                             new-title
                             (let [title (. (File. @file-path) getName)
                                   root  (.. this getRoot)
                                   index (. tabs indexOfComponent root)]
                               (. tabs setTitleAt index (str title (if modified? " *" ""))))))
                       (save
                         []
                         (try
                           (with-open [stream (FileWriter. (. this getPath))]
                             (doto this
                               (.write stream)
                               (.setModified false)))
                           (catch Exception e
                             (. e printStackTrace)
                             (. JOptionPane (showMessageDialog nil (. e getMessage) "Error..." JOptionPane/OK_OPTION)))))
                       (saveAs
                         []
                         (let [chooser (JFileChooser. (str "~" os-file-separator))
                               result  (. chooser showSaveDialog nil)]
                           (if (= JFileChooser/APPROVE_OPTION result)
                               (doto this
                                 (.setPath (.. chooser getSelectedFile getAbsolutePath))
                                 (.save)
                                 (.setModified false)))))
                       (getInputMethodRequests []
                         (if (= nil @improved-imr)
                             (let [original-imr (proxy-super getInputMethodRequests)]
                               (reset! improved-imr (make-improved-imr original-imr))
                               @improved-imr)
                             @improved-imr))
                       (setImeEnable [value]
                         (if (windows?)
                             (let [ic (. this getInputContext)]
                               (if value
                                   (do
                                     (. ic setCompositionEnabled (if (= nil @ime-mode) (. ic isCompositionEnabled) @ime-mode))
                                     (reset! ime-mode nil))
                                   (do
                                     (reset! ime-mode (. ic isCompositionEnabled))
                                     (. ic setCompositionEnabled false))))
                             (. this setEditable value)))
                       (setInputMap [inputmap] (. this setInputMap JComponent/WHEN_FOCUSED inputmap))
                       (setActionMap [actionmap] (proxy-super setActionMap actionmap))
                       (setKeyStroke [keystroke]
                         (if (= nil keystroke)
                             (. keystrokes setText "")
                             (let [current (. keystrokes getText)]
                               (. keystrokes setText (if (= current "")
                                                         (keys/str-keystroke keystroke)
                                                         (str current ", " (keys/str-keystroke keystroke)))))))
                       (getTabs [] tabs)
                       (getRoot []
                         (.. this getParent getParent getParent))
                       (getTabIndex []
                         (let [tabs (. this getTabs)
                               root (. this getRoot)]
                           (.. tabs indexOfComponent root)))
                       (getUndoManager []
                         um)
                       (isMark [] @is-marked)
                       (setMark [marked]
                         (reset! is-marked marked)))

        scroll       (JScrollPane. text-pane
                                   JScrollPane/VERTICAL_SCROLLBAR_ALWAYS
                                   JScrollPane/HORIZONTAL_SCROLLBAR_ALWAYS)
        ;
        ; Root Panel
        ;
        root-panel   (proxy [JPanel ITextEditor] []
                       (getTextPane []
                         text-pane)
                       (getModified []
                         (. text-pane getModified))
                       (requestFocusInWindow []
                         (. text-pane requestFocusInWindow))
                       (getScroll [] scroll)
                       (getPath [] @file-path))
                ;
        ; Line Numbers
        ;
        text-line-number (make-text-line-number text-pane 3)
        
        ;
        ; Others
        ;
        default-map       (. maps get "default")
        default-fonts     {:linux   ;["Takaoゴシック" Font/PLAIN 14]
                                    ["YOzFontCF" Font/PLAIN 16]
                           :windows ["ＭＳ ゴシック" Font/PLAIN 14]}
        ]
    ;
    ; Editor Area
    ;
    (doto text-pane
      (.setName "text-pane")
      (.setInputMap  JComponent/WHEN_FOCUSED (. default-map getInputMap))
      (.setActionMap (. default-map getActionMap))
      (.enableInputMethods true)
      (.addCaretListener (proxy [CaretListener] []
                           (caretUpdate [evt]
                             (let [th1 (make-color-parentheses-thread (. evt getSource) (. evt getDot))]
                               (. th1 start)))))
      ; (.addCaretListener (proxy [CaretListener] []
      ;                      (caretUpdate [evt]
      ;                        (lexer/set-color-parentheses (. evt getSource) (. evt getDot)))))
      )

    (doto (. text-pane getDocument)
      (.addDocumentListener (proxy [DocumentListener] []
                              (changedUpdate [evt] )
                              (insertUpdate [evt] (. text-pane setModified true))
                              (removeUpdate [evt] (. text-pane setModified true))))
      ; (.addDocumentListener (proxy [DocumentListener] []
      ;                         (changedUpdate [evt] )
      ;                         (insertUpdate [evt] (. text-pane setModified true))
      ;                         (removeUpdate [evt] (. text-pane setModified true))))
      (.addUndoableEditListener um))


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
      (grid-bag-layout
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
      (grid-bag-layout
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
      (set-font component (default-fonts (get-os-keyword))))

    root-panel
    ))



;;------------------------------
;;
;; Editor actions
;;
;;------------------------------
(defn get-default-editor-action
  [action-string]
  (assert (string? action-string))
  (let [default-actmap (.. maps (get "default") getActionMap)]
    (. default-actmap get action-string)))

(defn caret-action
  [text-pane evt normal selection]
  (if (. text-pane isMark)
      (. (get-default-editor-action selection) actionPerformed evt)
      (. (get-default-editor-action normal) actionPerformed evt)))

(defn get-current-element-index [text-pane]
  (let [current-pos (. text-pane getCaretPosition)
        root        (.. text-pane getDocument getDefaultRootElement)]
    (. root getElementIndex current-pos)))

(defmacro defaction-with-default
  [name bindings & body]
  (assert (vector? bindings))
  (let [cnt (count bindings)]
    (assert (and (<= 1 cnt) (<= cnt 2)))
    (let [source (bindings 0)]
      (if (= cnt 1)
          (let [evt (gensym "evt")]
            `(def ~name (proxy [AbstractAction] []
                          (actionPerformed [~evt]
                            ((fn [~source] ~@body)
                             (. ~evt getSource))))))
          (let [evt (bindings 1)]
            `(def ~name (proxy [AbstractAction] []
                          (actionPerformed [~evt]
                            ((fn [~source ~evt] ~@body)
                             (. ~evt getSource)
                             ~evt)))))))))

;;
;; Caret move action group.
;;

;; Charactor
(defaction forward-char [text-pane evt] (caret-action text-pane evt DefaultEditorKit/forwardAction DefaultEditorKit/selectionForwardAction))
(defaction backward-char [text-pane evt] (caret-action text-pane evt DefaultEditorKit/backwardAction DefaultEditorKit/selectionBackwardAction))

;; Word
(defaction begin-word [text-pane evt] (caret-action text-pane evt DefaultEditorKit/beginWordAction DefaultEditorKit/selectionBeginWordAction))
(defaction end-word [text-pane evt] (caret-action text-pane evt DefaultEditorKit/endWordAction DefaultEditorKit/selectionEndWordAction))
(defaction forward-word [text-pane evt] (caret-action text-pane evt DefaultEditorKit/nextWordAction DefaultEditorKit/selectionNextWordAction))
(defaction backward-word [text-pane evt] (caret-action text-pane evt DefaultEditorKit/previousWordAction DefaultEditorKit/selectionPreviousWordAction))

;; Line
(defaction previous-line [text-pane evt] (caret-action text-pane evt DefaultEditorKit/upAction DefaultEditorKit/selectionUpAction))
(defaction next-line [text-pane evt] (caret-action text-pane evt DefaultEditorKit/downAction DefaultEditorKit/selectionDownAction))
(defaction begin-line [text-pane evt] (caret-action text-pane evt DefaultEditorKit/beginLineAction DefaultEditorKit/selectionBeginLineAction))
(defaction end-line [text-pane evt] (caret-action text-pane evt DefaultEditorKit/endLineAction DefaultEditorKit/selectionEndLineAction))

;; Paragraph
(defaction begin-paragraph [text-pane evt]
  (caret-action text-pane evt DefaultEditorKit/beginParagraphAction DefaultEditorKit/selectionBeginParagraphAction))
(defaction end-paragraph [text-pane evt]
  (caret-action text-pane evt DefaultEditorKit/endParagraphAction DefaultEditorKit/selectionEndParagraphAction))

;; Page
(defaction previous-page [text-pane evt]
  (if (. text-pane isMark)
      (do
        (println "Next page with selecting action has *NOT Implemented*.")
        )
      ; (let [caret-position (. text-component getCaretPosition)
      ;       root           (.. text-component getDocument getDefaultRootElement)
      ;       offset         (. root getStartOffset)
      ;       offset-index   (. root getElementIndex rowStartOffset)
      ;       caret-index    (. root getElementIndex caretPosition)]

      ;   )
      (. (get-default-editor-action DefaultEditorKit/pageUpAction) actionPerformed evt)))

(defaction next-page [text-pane evt]
  (if (. text-pane isMark)
      (do
        (println "Next page with selecting action has *NOT Implemented*.")
        )
      (. (get-default-editor-action DefaultEditorKit/pageDownAction) actionPerformed evt)))

;; Document
(defaction begin-buffer [text-pane evt] (caret-action text-pane evt DefaultEditorKit/beginAction DefaultEditorKit/selectionBeginAction))
(defaction end-buffer [text-pane evt] (caret-action text-pane evt DefaultEditorKit/endAction DefaultEditorKit/selectionEndAction))

;; Selected line
(defaction goto-line [text-pane]
  (let [input-line (JOptionPane/showInputDialog nil "Input line number.")]
    (if (not (= nil input-line))
        (let [root        (.. text-pane getDocument getDefaultRootElement)
              cnt         (. root getElementCount)
              input-value (Integer/parseInt (. input-line trim))
              line        (- (min input-value cnt) 1)
              element     (. root getElement line)
              rect        (. text-pane modelToView (. element getStartOffset))
              view-rect   (.. text-pane getRoot getScroll getViewport getViewRect)]
          (. rect setSize 10 (. view-rect height))
          (. text-pane scrollRectToVisible rect)
          (. text-pane setCaretPosition (. element getStartOffset))))))

;;
;; Delete action group.
;;

;; Charactor
(defaction delete-previous-char [text-pane evt]
  (. (get-default-editor-action DefaultEditorKit/deletePrevCharAction) actionPerformed evt)
  (. text-pane setMark false))

(defaction delete-next-char [text-pane evt]
  (. (get-default-editor-action DefaultEditorKit/deleteNextCharAction) actionPerformed evt)
  (. text-pane setMark false))

;; Word
(def deletePrevWord (get-default-editor-action DefaultEditorKit/deletePrevWordAction))
(def deletenextword (get-default-editor-action DefaultEditorKit/deleteNextWordAction))


;;
;; Select group.
;;

(def selectWord (get-default-editor-action DefaultEditorKit/selectWordAction))
(def selectLine (get-default-editor-action DefaultEditorKit/selectLineAction))
(def selectParagraph (get-default-editor-action DefaultEditorKit/selectParagraphAction))
(def selectAll (get-default-editor-action DefaultEditorKit/selectAllAction))


;;
;; Move selection group.
;;



;;
;; Edit operation group.
;;

(defaction copy [text-pane evt]
  (. (get-default-editor-action DefaultEditorKit/copyAction) actionPerformed evt)
  (. text-pane setMark false))

(defaction cut [text-pane evt]
  (. (get-default-editor-action DefaultEditorKit/cutAction) actionPerformed evt)
  (. text-pane setMark false))

(def paste (get-default-editor-action DefaultEditorKit/pasteAction))


;;
;; Other group.
;;

(def defaultKeyTyped (get-default-editor-action DefaultEditorKit/defaultKeyTypedAction))
(def insertBreak (get-default-editor-action DefaultEditorKit/insertBreakAction))
(def insertTab (get-default-editor-action DefaultEditorKit/insertTabAction))
(def insertContent (get-default-editor-action DefaultEditorKit/insertContentAction))
(def beep (get-default-editor-action DefaultEditorKit/beepAction))
(def readOnly (get-default-editor-action DefaultEditorKit/readOnlyAction))
(def writable (get-default-editor-action DefaultEditorKit/writableAction))

;;
;; Edit action group.
;;

(defaction undo [text-pane]
  (let [um  (. text-pane getUndoManager)]
    (if (. um canUndo)
        (. um undo))))

(defaction redo [text-pane]
  (let [um  (. text-pane getUndoManager)]
    (if (. um canRedo)
        (. um redo))))

(defaction mark [text-pane]
  (. text-pane setMark true))

(defaction escape [text-pane]
  (. text-pane setMark false))




;;
;; File action group.
;;

(defn file-set
  [tabs file]
  (. tabs addTab nil (make-editor tabs))
  (let [idx    (- (. tabs getTabCount) 1)
        editor (. tabs getComponentAt idx)]
    (. tabs setSelectedIndex idx)
    (. editor requestFocusInWindow)
    (if (= nil file)
        (. tabs setTitleAt idx new-title)
        (let [file-path (. file getAbsolutePath)
              text-pane (. editor getTextPane)]
          (. text-pane setPath file-path)
          (try
            (with-open [stream (FileInputStream. file-path)]
              (let [document (. text-pane getDocument)]
                (doto text-pane
                  (.read stream document)
                  (.setModified false)))
              (doto (. text-pane getDocument)
                (.addDocumentListener (proxy [DocumentListener] []
                                        (changedUpdate [evt] )
                                        (insertUpdate [evt] (. text-pane setModified true))
                                        (removeUpdate [evt] (. text-pane setModified true))))
                (.addUndoableEditListener (. text-pane getUndoManager)))
              (lexer/parse text-pane 0 (.. text-pane getDocument getLength)))
            (catch FileNotFoundException _ true)
            (catch Exception e
              (. e printStackTrace)
              (. JOptionPane (showMessageDialog nil (. e getMessage) "Error..." JOptionPane/OK_OPTION))))))))
      
(defaction file-new
  [tabs]
  (file-set tabs nil))

(defaction file-open
  [tabs]
  (let [panel   (. tabs getCurrentPanel)
        path    (if (= nil panel) home-directory-path (. panel getPath))
        chooser (JFileChooser. path)
        result  (. chooser showOpenDialog nil)]
    (if (= JFileChooser/APPROVE_OPTION result)
        (file-set tabs (.. chooser getSelectedFile)))))

(defaction file-save
  [text-pane]
  (if (= nil (. text-pane getPath))
      (. text-pane saveAs)
      (. text-pane save)))

(defaction close
  [text-pane]
  (if (. text-pane getModified)
      (let [option (JOptionPane/showConfirmDialog (. text-pane getTabs)
                                                  "This document is modified.\nDo you save?")]
        (cond (= option JOptionPane/YES_OPTION) (do 
                                                  (if (= nil (. text-pane getPath))
                                                      (. text-pane saveAs)
                                                      (. text-pane save))
                                                  (.. text-pane getTabs (remove (. text-pane getRoot))))
              (= option JOptionPane/NO_OPTION)  (.. text-pane getTabs (remove (. text-pane getRoot)))
              :else                             nil))
      (.. text-pane getTabs (remove (. text-pane getRoot)))))
                
      
(defaction select-tab [tabs]
  (let [index (. tabs getSelectedIndex)]
    (. tabs (remove index))))


(defaction print-font-names [text-pane]
  (doseq [font (get-font-names)]
    (println font)))

(defaction colored-tokens [text-pane]
  (let [EOF             (Object.)
        reader          (StringReader. (. text-pane getText))
        pushback-reader (LineNumberingPushbackReader. reader)]
    (loop [r (LispReader/read pushback-reader false nil true)]
      (if (= EOF r)
          nil
          (let [row (. pushback-reader getLineNumber)
                ;col (. pushback-reader getColumnNumber)
                ]
            ;(println "row:" row ", col:" col ", r:" r)
            (println "row:%d, emit:%s, row:%s" row (. r canEmit))
            (recur (LispReader/read pushback-reader false nil true)))))))


          



(defaction show-attribute [text-pane]
  (let [pos   (. text-pane getCaretPosition)
        attr  (. text-pane getCharacterAttributes)]
    (println "----------")
    (println "pos:" pos)
    (println "attr:" attr)))

(defaction show-paragraph-attribute [text-pane]
  (let [pos   (. text-pane getCaretPosition)
        attr  (. text-pane getParagraphAttributes)]
    (println "----------")
    (println "pos:" pos)
    (println "attr:" attr)))

(defaction set-character-attribute [text-pane]
  (let [start (. text-pane getSelectionStart)
        end   (. text-pane getSelectionEnd)]
    (println (format "-----\nstart:%d, end:%d" start end))
    (if (= start end)
        nil
        (let [attr (SimpleAttributeSet.)
              doc  (. text-pane getDocument)]
          (StyleConstants/setForeground attr Color/RED)
          (. doc setCharacterAttributes start (- end start) attr false)))))

(defaction set-paragraph-attribute [text-pane]
  (let [start (. text-pane getSelectionStart)
        end   (. text-pane getSelectionEnd)]
    (println (format "-----\nstart:%d, end:%d" start end))
    (if (= start end)
        nil
        (let [attr (SimpleAttributeSet.)
              doc  (. text-pane getDocument)]
          (StyleConstants/setForeground attr Color/BLUE)
          (. doc setParagraphAttributes start (- end start) attr false)))))

(defaction show-color-dialog [text-pane]
  (let [color (JColorChooser/showDialog text-pane "Select Color" Color/WHITE)
        pos   (. text-pane getCaretPosition)]
    (if (nil? color)
        nil
        (let [doc   (. text-pane getDocument)
              val   (format "(Color %d %d %d)" (. color getRed) (. color getGreen) (. color getBlue))
              style (. (StyleContext/getDefaultStyleContext) getStyle StyleContext/DEFAULT_STYLE)]
          (. doc insertString pos val style)))))
