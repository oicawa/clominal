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
  (:require [clominal.keys :as keys])
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
(def maps (make-maps (JTextPane.) JComponent/WHEN_FOCUSED))

;
; Font utilities
;
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
  (getTabIndex []))

(definterface ITextEditor
  (getModified [])
  (getTextPane []))

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
                       (setEditEnable [value]
                         (. this setEditable value))
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
                           (.. tabs indexOfComponent root))))

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
      (.enableInputMethods true))
    (doto (. text-pane getDocument)
      (.addDocumentListener (proxy [DocumentListener] []
                              (changedUpdate [evt] )
                              (insertUpdate [evt] (. text-pane setModified true))
                              (removeUpdate [evt] (. text-pane setModified true)))))


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
(defn make-editor-action
  [action-string]
  (assert (string? action-string))
  (let [default-actionmap ((. maps get "default") 1)]
    (. default-actionmap get action-string)))

;;
;; Caret move action group.
;;

;; Charactor
(def forward (make-editor-action DefaultEditorKit/forwardAction))
(def backward (make-editor-action DefaultEditorKit/backwardAction))

;; Word
(def beginWord (make-editor-action DefaultEditorKit/beginWordAction))
(def endWord (make-editor-action DefaultEditorKit/endWordAction))
(def nextWord (make-editor-action DefaultEditorKit/nextWordAction))
(def previousWord (make-editor-action DefaultEditorKit/previousWordAction))

;; Line
(def up (make-editor-action DefaultEditorKit/upAction))
(def down (make-editor-action DefaultEditorKit/downAction))
(def beginLine (make-editor-action DefaultEditorKit/beginLineAction))
(def endLine (make-editor-action DefaultEditorKit/endLineAction))

;; Paragraph
(def beginParagraph (make-editor-action DefaultEditorKit/beginParagraphAction))
(def endParagraph (make-editor-action DefaultEditorKit/endParagraphAction))

;; Page
(def pageDown (make-editor-action DefaultEditorKit/pageDownAction))
(def pageUp (make-editor-action DefaultEditorKit/pageUpAction))

;; Document
(def begin (make-editor-action DefaultEditorKit/beginAction))
(def end (make-editor-action DefaultEditorKit/endAction))


;;
;; Delete action group.
;;

;; Charactor
(def deletePrevChar (make-editor-action DefaultEditorKit/deletePrevCharAction))
(def deleteNextChar (make-editor-action DefaultEditorKit/deleteNextCharAction))

;; Word
(def deletePrevWord (make-editor-action DefaultEditorKit/deletePrevWordAction))
(def deletenextword (make-editor-action DefaultEditorKit/deleteNextWordAction))


;;
;; Select group.
;;

(def selectWord (make-editor-action DefaultEditorKit/selectWordAction))
(def selectLine (make-editor-action DefaultEditorKit/selectLineAction))
(def selectParagraph (make-editor-action DefaultEditorKit/selectParagraphAction))
(def selectAll (make-editor-action DefaultEditorKit/selectAllAction))


;;
;; Move selection group.
;;

;; Selection
(def selectionBegin (make-editor-action DefaultEditorKit/selectionBeginAction))
(def selectionEnd (make-editor-action DefaultEditorKit/selectionEndAction))


;; Charactor
(def selectionForward (make-editor-action DefaultEditorKit/selectionForwardAction))
(def selectionBackward (make-editor-action DefaultEditorKit/selectionBackwardAction))

;; Word
(def selectionBeginWord (make-editor-action DefaultEditorKit/selectionBeginWordAction))
(def selectionEndWord (make-editor-action DefaultEditorKit/selectionEndWordAction))
(def selectionNextWord (make-editor-action DefaultEditorKit/selectionNextWordAction))
(def selectionPreviousWord (make-editor-action DefaultEditorKit/selectionPreviousWordAction))

;; Line
(def selectionBeginLine (make-editor-action DefaultEditorKit/selectionBeginLineAction))
(def selectionEndLine (make-editor-action DefaultEditorKit/selectionEndLineAction))
(def selectionUp (make-editor-action DefaultEditorKit/selectionUpAction))
(def selectionDown (make-editor-action DefaultEditorKit/selectionDownAction))

;; Paragraph
(def selectionBeginParagraph (make-editor-action DefaultEditorKit/selectionBeginParagraphAction))
(def selectionEndParagraph (make-editor-action DefaultEditorKit/selectionEndParagraphAction))


;;
;; Edit operation group.
;;

(def copy (make-editor-action DefaultEditorKit/copyAction))
(def cut (make-editor-action DefaultEditorKit/cutAction))
(def paste (make-editor-action DefaultEditorKit/pasteAction))


;;
;; Other group.
;;

(def defaultKeyTyped (make-editor-action DefaultEditorKit/defaultKeyTypedAction))
(def insertBreak (make-editor-action DefaultEditorKit/insertBreakAction))
(def insertTab (make-editor-action DefaultEditorKit/insertTabAction))
(def insertContent (make-editor-action DefaultEditorKit/insertContentAction))
(def beep (make-editor-action DefaultEditorKit/beepAction))
(def readOnly (make-editor-action DefaultEditorKit/readOnlyAction))
(def writable (make-editor-action DefaultEditorKit/writableAction))


(defn create-document
  [tabs title]
  (let [editor (make-editor tabs)]
    (. tabs addTab title editor)
    (. tabs setSelectedIndex (- (. tabs getTabCount) 1))
    (. editor requestFocusInWindow)
    editor))

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
              (doto text-pane
                (.read stream (. text-pane getDocument))
                (.setModified false))
              (doto (. text-pane getDocument)
                (.addDocumentListener (proxy [DocumentListener] []
                                        (changedUpdate [evt] )
                                        (insertUpdate [evt] (. text-pane setModified true))
                                        (removeUpdate [evt] (. text-pane setModified true))))))
            (catch FileNotFoundException _ true)
            (catch Exception e
              (. e printStackTrace)
              (. JOptionPane (showMessageDialog nil (. e getMessage) "Error..." JOptionPane/OK_OPTION))))))))
      
;;
;; File action group.
;;

(defaction file-new
  [tabs]
  (file-set tabs nil))

(defaction file-open
  [tabs]
  (let [chooser   (JFileChooser. (str "~" os-file-separator))
        result    (. chooser showOpenDialog nil)]
    (if (= JFileChooser/APPROVE_OPTION result)
        (file-set tabs (.. chooser getSelectedFile)))))

(defaction saveFile
  [text-pane]
  (if (= nil (. text-pane getPath))
      (. text-pane saveAs)
      (. text-pane save)))

(defaction changeBuffer
  [text-pane]
  (println "called 'changeBuffer'."))

(defaction close
  [text-pane]
  (if (. text-pane getModified)
      (let [option (JOptionPane/showConfirmDialog (. text-pane getTabs)
                                                  "This document is modified.\nDo you save?")]
        (cond (= option JOptionPane/YES_OPTION)    (do 
                                                     (if (= nil (. text-pane getPath))
                                                         (. text-pane saveAs)
                                                         (. text-pane save))
                                                     (.. text-pane getTabs (remove (. text-pane getRoot))))
              (= option JOptionPane/NO_OPTION)     (.. text-pane getTabs (remove (. text-pane getRoot)))
              :else                                nil))
      (.. text-pane getTabs (remove (. text-pane getRoot)))))
                
      
(defaction select-tab [tabs]
  (let [index (. tabs getSelectedIndex)]
    (. tabs (remove index))))

