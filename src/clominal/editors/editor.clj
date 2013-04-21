(ns clominal.editors.editor
  (:use [clominal.utils])
  (:require [clominal.keys :as keys]
            [clojure.contrib.string :as string])
  (:import (java.lang Thread)
           (java.awt Font Color Graphics GraphicsEnvironment GridBagLayout Point)
           (java.awt.event ActionEvent)
           (java.awt.im InputMethodRequests)
           (java.beans PropertyChangeListener)
           (java.util HashMap)
           (javax.swing JList InputMap ActionMap JComponent Action
                        ; JTextPane JScrollPane
                        JLabel JTextField JPanel JCheckBox JOptionPane SwingConstants JFileChooser
                        SwingUtilities AbstractAction JColorChooser)
           (javax.swing.border LineBorder MatteBorder EmptyBorder CompoundBorder)
           (javax.swing.event CaretListener DocumentListener)
           (javax.swing.text StyleConstants Utilities DefaultEditorKit DefaultHighlighter$DefaultHighlightPainter SimpleAttributeSet
                             DefaultStyledDocument StyleContext)
           (javax.swing.undo UndoManager)
           (java.io File FileInputStream FileWriter FileNotFoundException StringReader)
           (clojure.lang LineNumberingPushbackReader LispReader)
           (org.fife.ui.rsyntaxtextarea RSyntaxTextArea SyntaxConstants TextEditorPane FileLocation Token RSyntaxUtilities)
           (org.fife.ui.rtextarea RTextScrollPane)
           (clominal.utils IMarkable IAppPane)))


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
(def maps (keys/make-keymaps (TextEditorPane.) JComponent/WHEN_FOCUSED))


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

; Interfaces

(definterface ITextEditorPane
  (load [file])
  (setFocus [])
  (getStatusBar [])
  (getUndoManager [])
  (getSubPanel [])
  (getFileFullPath [])
  )

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

(defn get-extension
  [file-path]
  (let [index (. file-path lastIndexOf ".")]
    (if (< 0 index)
        (. file-path substring (+ index 1))
        "")))

(defn get-mode-package
  [ext]
  (let [s (symbol "mode" (format "%s-mode" ext))]
    (println "mode-name:" s)
    (require s)))

(defn apply-editor-mode
  [text-pane]
  (println "apply-editor-mode")
  (let [ext                   (get-extension (. text-pane getFileFullPath))
        namespace-name        (format "mode.%s_mode" ext)
        get-mode-name-symbol  (symbol namespace-name "get-mode-name")
        init-mode-name-symbol (symbol namespace-name "init-mode")
        ]
    (if (try
          (require (symbol namespace-name))
          true
          (catch FileNotFoundException _ false))
        (apply (find-var init-mode-name-symbol) [text-pane]))))

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
        improved-imr (atom nil)
        ime-mode     (atom nil)
        um           (UndoManager.)
        is-marked    (atom false)
        text-pane    (proxy [TextEditorPane IMarkable clominal.keys.IKeybindComponent] []
                       (setDirty [dirty?]
                         (proxy-super setDirty dirty?)
                         (let [index    (. tabs getSelectedIndex)
                               filename (if (. this isLocalAndExists) (. this getFileName) new-title)
                               title    (str filename (if dirty? " *" ""))]
                           (. tabs setTitleAt index title)))
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
                       (isMark [] @is-marked)
                       (setMark [marked]
                         (reset! is-marked marked)))
        scroll       (RTextScrollPane. text-pane)
        sub-panel    (JPanel. (GridBagLayout.))
        ;
        ; Root Panel
        ;
        root-panel   (proxy [JPanel IAppPane ITextEditorPane] []
                       (canClose [] (not (. text-pane isDirty)))
                       (getTabs [] tabs)
                       (getTabIndex [] (. tabs indexOfComponent this))
                       (load [file]
                         (. tabs addTab nil this)
                         (let [index (. this getTabIndex)]
                           (. tabs setSelectedIndex index)
                           (if (nil? file)
                               (. tabs setTitleAt index new-title)
                               (let [file-location (FileLocation/create (. file getAbsolutePath))
                                     file-name     (. file-location getFileName)]
                                 (. text-pane load file-location nil)
                                 (. tabs setTitleAt index file-name)
                                 (apply-editor-mode text-pane)))))
                       (setFocus []
                         (. tabs setSelectedIndex (. this getTabIndex))
                         (. text-pane requestFocusInWindow))
                       (getStatusBar [] statusbar)
                       (getUndoManager [] um)
                       (getSubPanel [] sub-panel)
                       (getFileFullPath []
                         (. text-pane getFileFullPath)))
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
      (.setSyntaxEditingStyle SyntaxConstants/SYNTAX_STYLE_NONE)
      (.setPaintTabLines true))

    (doto (. text-pane getDocument)
      (.addDocumentListener (proxy [DocumentListener] []
                              (changedUpdate [evt] )
                              (insertUpdate [evt]
                                (. text-pane setDirty true))
                              (removeUpdate [evt]
                                (. text-pane setDirty true))))
      (.addUndoableEditListener um))

    ;
    ; Sub Panel
    ;
    (doto sub-panel
      (.setBorder (LineBorder. Color/GRAY))
      (.setVisible false))

    ;
    ; StatusBar
    ;
    (doto statusbar
      (.setName "statusbar")
      (.setPreferredSize nil)
      (.setLayout (GridBagLayout.))
      (grid-bag-layout
        :gridy 0
        :gridx 0
        :anchor :WEST
        keystrokes
        :gridx 1
        :fill :HORIZONTAL
        :weightx 1.0
        filler
        :gridx 2
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
        sub-panel
        :fill :HORIZONTAL
        :weightx 1.0
        :weighty 0.0
        :gridy 2
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

; (defmacro defaction-with-default
;   [name bindings & body]
;   (assert (vector? bindings))
;   (let [cnt (count bindings)]
;     (assert (and (<= 1 cnt) (<= cnt 2)))
;     (let [source (bindings 0)]
;       (if (= cnt 1)
;           (let [evt (gensym "evt")]
;             `(def ~name (proxy [AbstractAction] []
;                           (actionPerformed [~evt]
;                             ((fn [~source] ~@body)
;                              (. ~evt getSource))))))
;           (let [evt (bindings 1)]
;             `(def ~name (proxy [AbstractAction] []
;                           (actionPerformed [~evt]
;                             ((fn [~source ~evt] ~@body)
;                              (. ~evt getSource)
;                              ~evt)))))))))

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
        (println "Next page with selecting action has *NOT Implemented*."))
      (. (get-default-editor-action DefaultEditorKit/pageUpAction) actionPerformed evt)))

(defaction next-page [text-pane evt]
  (if (. text-pane isMark)
      (do
        (println "Next page with selecting action has *NOT Implemented*."))
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

(defn save-document
  [text-pane]
  (. text-pane save))

(defn save-as-document
  [text-pane]
  (let [chooser (JFileChooser. (str "~" os-file-separator))
        result  (. chooser showSaveDialog nil)]
    (if (= JFileChooser/APPROVE_OPTION result)
        (doto text-pane
          (.saveAs (FileLocation/create (.. chooser getSelectedFile getAbsolutePath)))
          (.setDirty false)))))

; (defn file-set
;   [tabs file]
;   (. tabs addTab nil (make-editor tabs))
;   (let [idx    (- (. tabs getTabCount) 1)
;         editor (. tabs getComponentAt idx)]
;     (. tabs setSelectedIndex idx)
;     (. editor requestFocusInWindow)
;     (if (= nil file)
;         (. tabs setTitleAt idx new-title)
;         (let [file-location (FileLocation/create (. file getAbsolutePath))
;               text-pane     (. editor getTextPane)
;               index         (. text-pane getTabIndex)
;               file-name     (. file-location getFileName)]
;           (. text-pane load file-location nil)
;           (.. text-pane getTabs (setTitleAt index file-name))
;           (doto (. text-pane getDocument)
;             (.addDocumentListener (proxy [DocumentListener] []
;                                     (changedUpdate [evt] )
;                                     (insertUpdate [evt] (. text-pane setDirty true))
;                                     (removeUpdate [evt] (. text-pane setDirty true))))
;             (.addUndoableEditListener (. text-pane getUndoManager)))
;           (apply-editor-mode text-pane)))))
(defn file-set
  [tabs file]
  (doto (make-editor tabs)
    (.load file)
    (.setFocus)))
      
(defaction file-new
  [tabs]
  (file-set tabs nil))

(defaction file-open
  [tabs]
  (let [panel   (. tabs getCurrentPanel)
        path    (if (= nil panel) home-directory-path (. panel getFileFullPath))
        chooser (JFileChooser. path)
        result  (. chooser showOpenDialog nil)]
    (if (= JFileChooser/APPROVE_OPTION result)
        (file-set tabs (.. chooser getSelectedFile)))))

(defaction file-save
  [text-pane]
  (if (. text-pane isLocalAndExists)
      (save-document text-pane)
      (do
        (save-as-document text-pane)
        (apply-editor-mode text-pane))))

(defaction close
  [text-pane]
  (if (. text-pane isDirty)
      (let [option (JOptionPane/showConfirmDialog (. text-pane getTabs)
                                                  "This document is modified.\nDo you save?")]
        (cond (= option JOptionPane/YES_OPTION)
                (do 
                  (if (= nil (. text-pane getFileFullPath))
                      (save-as-document text-pane)
                      (save-document text-pane))
                  (.. text-pane getTabs (remove (. text-pane getRoot))))
              (= option JOptionPane/NO_OPTION)
                (.. text-pane getTabs (remove (. text-pane getRoot)))
              :else
                nil))
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


(defn get-token
  [text-pane offset]
  (let [doc          (. text-pane getDocument)
        map          (. doc getDefaultRootElement)
        line-index   (. map getElementIndex offset)
        ; line-element (. map getElement line-index)
        ; init-start   (. line-element getStartOffset)
        ; init-end     (. line-element getEndOffset)
        token-list   (. doc getTokenListForLine line-index)]
    (RSyntaxUtilities/getTokenAtOffset token-list offset)))

; (defn get-token
;   [text-pane offset]
;   (let [line (. text-pane getLineOfOffset offset)]
;     (loop [token  (. text-pane getTokenListForLine line)]
;       (cond (= nil token)
;               nil
;             (. token containsPosition offset)
;               token
;             :else
;               (recur (. token getNextToken))))))

(defaction print-current-line-tokens [text-pane]
  (let [offset (. text-pane getCaretPosition)
        line   (. text-pane getLineOfOffset offset)]
    (println "--- line:" line "---")
    (loop [token  (. text-pane getTokenListForLine line)]
      (if (= nil token)
          nil
          (do
            (println "Token:" token)
            (recur (. token getNextToken)))))))

(defaction print-current-caret-token [text-pane]
  (let [token (get-token text-pane (. text-pane getCaretPosition))]
    (println "Caret Token:" token)))

(defn show-sub-panel
  [text-pane target-panel]
  (let [root      (. text-pane getRoot)
        sub-panel (. root getSubPanel)
        hidden?   (not (. sub-panel isVisible))]
    (doto sub-panel
      (.removeAll)
      (grid-bag-layout
        :gridx 0 :gridy 0 :anchor :WEST :fill :HORIZONTAL :weightx 1.0
        target-panel))
    (if hidden?
        (. sub-panel setVisible true))
    (. target-panel setFocus)))

(defaction show-component-stack [text-pane]
  (get-frame text-pane))