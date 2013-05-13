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
           (clominal.utils IMarkable IAppPane)
           (clominal.keys IKeybindComponent)))


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
(def multi-line-maps (keys/make-keymaps (TextEditorPane.) JComponent/WHEN_FOCUSED))
(def single-line-maps (keys/make-keymaps (JTextField.) JComponent/WHEN_FOCUSED))


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

(definterface ITextEditor
  (load [file])
  (setFocus [])
  (getStatusBar [])
  (getSubPanel [])
  (getFileFullPath [])
  )

(definterface ITextEditorPane
  (getRoot [])
  (getUndoManager [])
  (isNew [])
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
  (let [ext              (get-extension (. text-pane getFileFullPath))
        namespace-name   (format "mode.%s_mode" ext)
        init-mode-symbol (symbol namespace-name "init-mode")
        ]
    (if (try
          (require (symbol namespace-name))
          true
          (catch FileNotFoundException _ false))
        (apply (find-var init-mode-symbol) [text-pane]))))

(defn open-file-with-editor-mode
  [text-pane file]
  (let [file-location    (FileLocation/create (. file getAbsolutePath))
        file-name        (. file getName)
        ext              (get-extension file-name)
        namespace-name   (format "mode.%s_mode" ext)
        init-mode-symbol (symbol namespace-name "init-mode")]
    (try
      (require (symbol namespace-name))
      (apply (find-var init-mode-symbol) [text-pane])
      true
      (catch FileNotFoundException _ false))
    (. text-pane load file-location (. text-pane getEncoding))
    ))

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
        is-new       (atom true)
        text-pane    (atom nil)
        sub-panel    (JPanel. (GridBagLayout.))
        root-panel   (atom nil)
        ;
        ; Others
        ;
        default-map   (. multi-line-maps get "default")
        default-fonts {:linux   ;["Takaoゴシック" Font/PLAIN 14]
                                ["YOzFontCF" Font/PLAIN 16]
                       :windows ["ＭＳ ゴシック" Font/PLAIN 14]}
        ]
    ;
    ; Editor Area
    ;
    (reset! text-pane (proxy [TextEditorPane ITextEditorPane IMarkable IKeybindComponent] []
                        (getRoot [] @root-panel)
                        (getUndoManager [] um)
                        (isNew [] @is-new)
                        (getKeyMaps [] multi-line-maps)
                        (setDirty [dirty?]
                          (proxy-super setDirty dirty?)
                          (let [index    (. tabs getSelectedIndex)
                                filename (if (. this isLocalAndExists) (. this getFileName) new-title)
                                title    (str filename (if dirty? " *" ""))]
                            (. tabs setTitleAt index title)
                            (if (not dirty?)
                                (reset! is-new false))))
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
                          (reset! is-marked marked))))
 
    (doto @text-pane
      (.setName "text-pane")
      (.setInputMap  JComponent/WHEN_FOCUSED (. default-map getInputMap))
      (.setActionMap (. default-map getActionMap))
      (.enableInputMethods true)
      (.setSyntaxEditingStyle SyntaxConstants/SYNTAX_STYLE_NONE)
      (.setPaintTabLines true))

    (doto (. @text-pane getDocument)
      (.addDocumentListener (proxy [DocumentListener] []
                              (changedUpdate [evt] )
                              (insertUpdate [evt]
                                (. @text-pane setDirty true))
                              (removeUpdate [evt]
                                (. @text-pane setDirty true))))
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
    (reset! root-panel (proxy [JPanel IAppPane ITextEditor] []
                         (canClose [] (not (. @text-pane isDirty)))
                         (getTabs [] tabs)
                         (getTabIndex [] (. tabs indexOfComponent this))
                         (load [file]
                           (. tabs addTab nil this)
                           (let [index (. this getTabIndex)]
                             (. tabs setSelectedIndex index)
                             (if (nil? file)
                                 (. tabs setTitleAt index new-title)
                                 (do
                                   (open-file-with-editor-mode @text-pane file)
                                   (reset! is-new false)))))
                         (setFocus []
                           (. tabs setSelectedIndex (. this getTabIndex))
                           (. @text-pane requestFocusInWindow))
                         (getStatusBar [] statusbar)
                         (getSubPanel [] sub-panel)
                         (getFileFullPath []
                           (. @text-pane getFileFullPath))))
    (doto @root-panel
      (.setLayout (GridBagLayout.))
      (.setName "root-panel")
      (grid-bag-layout
        :fill :BOTH
        :gridx 0
        :gridy 0
        :weightx 1.0
        :weighty 1.0
        (RTextScrollPane. @text-pane)
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
    (doseq [component [@text-pane keystrokes char-code]]
      (set-font component (default-fonts (get-os-keyword))))

    @root-panel
    ))



;;------------------------------
;;
;; Editor actions
;;
;;------------------------------

(defn get-default-editor-action
  [maps action-string]
  (.. maps (get "default") getActionMap (get action-string)))

(defn get-caret-action
  [text-pane evt normal selection]
  (assert (string? normal))
  (assert (string? selection))
  (if (. text-pane isMark)
      (. (get-default-editor-action (. text-pane getKeyMaps) selection) actionPerformed evt)
      (. (get-default-editor-action (. text-pane getKeyMaps) normal)  actionPerformed evt)))

(defn caret-action
  [text-pane evt normal selection]
  (get-caret-action text-pane evt normal selection))

(defn get-current-element-index [text-pane]
  (let [current-pos (. text-pane getCaretPosition)
        root        (.. text-pane getDocument getDefaultRootElement)]
    (. root getElementIndex current-pos)))


;;
;; Caret move action group.
;;

;; Charactor
(defaction forward-char [text-pane evt]
  (caret-action text-pane evt DefaultEditorKit/forwardAction DefaultEditorKit/selectionForwardAction))
(defaction backward-char [text-pane evt]
  (caret-action text-pane evt DefaultEditorKit/backwardAction DefaultEditorKit/selectionBackwardAction))

;; Word
(defaction begin-word [text-pane evt]
  (caret-action text-pane evt DefaultEditorKit/beginWordAction DefaultEditorKit/selectionBeginWordAction))
(defaction end-word [text-pane evt]
  (caret-action text-pane evt DefaultEditorKit/endWordAction DefaultEditorKit/selectionEndWordAction))
(defaction forward-word [text-pane evt]
  (caret-action text-pane evt DefaultEditorKit/nextWordAction DefaultEditorKit/selectionNextWordAction))
(defaction backward-word [text-pane evt]
  (caret-action text-pane evt DefaultEditorKit/previousWordAction DefaultEditorKit/selectionPreviousWordAction))

;; Line
(defaction previous-line [text-pane evt]
  (caret-action text-pane evt DefaultEditorKit/upAction DefaultEditorKit/selectionUpAction))
(defaction next-line [text-pane evt]
  (caret-action text-pane evt DefaultEditorKit/downAction DefaultEditorKit/selectionDownAction))
(defaction begin-line [text-pane evt]
  (caret-action text-pane evt DefaultEditorKit/beginLineAction DefaultEditorKit/selectionBeginLineAction))
(defaction end-line [text-pane evt]
  (caret-action text-pane evt DefaultEditorKit/endLineAction DefaultEditorKit/selectionEndLineAction))

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
      (. (get-default-editor-action (. text-pane getKeyMaps) DefaultEditorKit/pageUpAction) actionPerformed evt)))

(defaction next-page [text-pane evt]
  (if (. text-pane isMark)
      (do
        (println "Next page with selecting action has *NOT Implemented*."))
      (. (get-default-editor-action (. text-pane getKeyMaps) DefaultEditorKit/pageDownAction) actionPerformed evt)))

;; Document
(defaction begin-buffer [text-pane evt]
  (caret-action text-pane evt DefaultEditorKit/beginAction DefaultEditorKit/selectionBeginAction))
(defaction end-buffer [text-pane evt]
  (caret-action text-pane evt DefaultEditorKit/endAction DefaultEditorKit/selectionEndAction))

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
              ;view-rect   (.. text-pane getRoot getScroll getViewport getViewRect)
              view-rect   (.. text-pane getParent getViewRect)]
          (. rect setSize 10 (. view-rect height))
          (. text-pane scrollRectToVisible rect)
          (. text-pane setCaretPosition (. element getStartOffset))))))

;;
;; Delete action group.
;;

;; Charactor
(defaction delete-previous-char [text-pane evt]
  (. (get-default-editor-action (. text-pane getKeyMaps) DefaultEditorKit/deletePrevCharAction) actionPerformed evt)
  (. text-pane setMark false))

(defaction delete-next-char [text-pane evt]
  (. (get-default-editor-action (. text-pane getKeyMaps) DefaultEditorKit/deleteNextCharAction) actionPerformed evt)
  (. text-pane setMark false))

;; Word
(defaction deletePrevWord [text-pane evt]
  (. (get-default-editor-action (. text-pane getKeyMaps) DefaultEditorKit/deletePrevWordAction) actionPerformed evt))
(defaction deletenextword [text-pane evt]
  (. (get-default-editor-action (. text-pane getKeyMaps) DefaultEditorKit/deleteNextWordAction) actionPerformed evt))


;;
;; Select group.
;;

(defaction selectWord [text-pane evt]
  (. (get-default-editor-action (. text-pane getKeyMaps) DefaultEditorKit/selectWordAction) actionPerformed evt))
(defaction selectLine [text-pane evt]
  (. (get-default-editor-action (. text-pane getKeyMaps) DefaultEditorKit/selectLineAction) actionPerformed evt))
(defaction selectParagraph [text-pane evt]
  (. (get-default-editor-action (. text-pane getKeyMaps) DefaultEditorKit/selectParagraphAction) actionPerformed evt))
(defaction selectAll [text-pane evt]
  (. (get-default-editor-action (. text-pane getKeyMaps) DefaultEditorKit/selectAllAction) actionPerformed evt))


;;
;; Move selection group.
;;



;;
;; Edit operation group.
;;

(defaction copy [text-pane evt]
  (. (get-default-editor-action (. text-pane getKeyMaps) DefaultEditorKit/copyAction) actionPerformed evt)
  (. text-pane setMark false))

(defaction cut [text-pane evt]
  (. (get-default-editor-action (. text-pane getKeyMaps) DefaultEditorKit/cutAction) actionPerformed evt)
  (. text-pane setMark false))

(defaction paste [text-pane evt]
  (. (get-default-editor-action (. text-pane getKeyMaps) DefaultEditorKit/pasteAction) actionPerformed evt))


;;
;; Other group.
;;

(defaction defactionaultKeyTyped [text-pane evt]
  (. (get-default-editor-action (. text-pane getKeyMaps) DefaultEditorKit/defaultKeyTypedAction) actionPerformed evt))
(defaction insertBreak [text-pane evt]
  (. (get-default-editor-action (. text-pane getKeyMaps) DefaultEditorKit/insertBreakAction) actionPerformed evt))
(defaction insertTab [text-pane evt]
  (. (get-default-editor-action (. text-pane getKeyMaps) DefaultEditorKit/insertTabAction) actionPerformed evt))
(defaction insertContent [text-pane evt]
  (. (get-default-editor-action (. text-pane getKeyMaps) DefaultEditorKit/insertContentAction) actionPerformed evt))
(defaction beep [text-pane evt]
  (. (get-default-editor-action (. text-pane getKeyMaps) DefaultEditorKit/beepAction) actionPerformed evt))
(defaction readOnly [text-pane evt]
  (. (get-default-editor-action (. text-pane getKeyMaps) DefaultEditorKit/readOnlyAction) actionPerformed evt))
(defaction writable [text-pane evt]
  (. (get-default-editor-action (. text-pane getKeyMaps) DefaultEditorKit/writableAction) actionPerformed evt))

;;
;; Edit action group.
;;

(defaction undo [text-pane]
  (let [um (. text-pane getUndoManager)]
    (if (. um canUndo)
        (. um undo))))

(defaction redo [text-pane]
  (let [um (. text-pane getUndoManager)]
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
  (println "File Open")
  (let [panel   (. tabs getCurrentPanel)
        path    (if (= nil panel) home-directory-path (. panel getFileFullPath))
        chooser (JFileChooser. path)
        result  (. chooser showOpenDialog nil)]
    (if (= JFileChooser/APPROVE_OPTION result)
        (file-set tabs (.. chooser getSelectedFile)))))

(defaction file-save
  [text-pane]
  (if ;(. text-pane isNew)
      (. text-pane isDirty)
      (save-document text-pane)
      (do
        (save-as-document text-pane)
        (apply-editor-mode text-pane))))

(defaction close
  [text-pane]
  (if (. text-pane isDirty)
      (let [option (JOptionPane/showConfirmDialog (.. text-pane getRoot getTabs)
                                                  "This document is modified.\nDo you save?")]
        (cond (= option JOptionPane/YES_OPTION)
                (do 
                  (if ;(= nil (. text-pane getFileFullPath))
                      (. text-pane isNew)
                      (save-as-document text-pane)
                      (save-document text-pane))
                  (.. text-pane getRoot getTabs (remove (. text-pane getRoot))))
              (= option JOptionPane/NO_OPTION)
                (.. text-pane getRoot getTabs (remove (. text-pane getRoot)))
              :else
                nil))
      (.. text-pane getRoot getTabs (remove (. text-pane getRoot)))))
                
      
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
          (let [row (. pushback-reader getLineNumber)]
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
        sub-panel (. root getSubPanel)]
    (println "target-panel:" (type target-panel))
    (println "sub-panel:" (type sub-panel))
    (doto sub-panel
      (.removeAll)
      (grid-bag-layout
        :gridx 0 :gridy 0 :anchor :WEST :fill :HORIZONTAL :weightx 1.0
        target-panel)
        )
    (. sub-panel setVisible false)
    (. sub-panel setVisible true)
    (. target-panel setFocus)
    (. sub-panel validate)
    ))

(defaction show-component-stack [text-pane]
  (get-frame text-pane))

(defaction show-system-encoding [text-pane]
  (println "System Encoding:" (System/getProperty "file.encoding")))
