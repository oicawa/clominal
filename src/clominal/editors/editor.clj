(ns clominal.editors.editor
  (:import (java.awt Font Color Graphics GraphicsEnvironment GridBagLayout Point)
           (java.awt.event ActionEvent)
           (java.awt.im InputMethodRequests)
           (java.beans PropertyChangeListener)
           (java.util HashMap)
           (javax.swing InputMap ActionMap JComponent JTextPane JScrollPane Action
                        JLabel JTextField JPanel JOptionPane SwingConstants JFileChooser SwingUtilities)
           (javax.swing.border LineBorder MatteBorder EmptyBorder CompoundBorder)
           (javax.swing.event CaretListener DocumentListener)
           (javax.swing.text StyleConstants Utilities)
           (java.io File FileInputStream FileWriter FileNotFoundException)
           (clominal.editors AskMiniBufferAction))
  (:require [clominal.utils.guiutils :as guiutils]
            [clominal.utils.env :as env]
            [clominal.editors.utils :as editorutils]))

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
  (save [])
  (saveAs [])
  (open [target]))

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
  []
  (let [;
        ; Status Bar
        ;
        statusbar     (JPanel.)
        char-code     (JLabel. "--")
        separator     (JLabel. ":")
        modified?     (JLabel. "**-")
        file-name     (JLabel. "(New)")
        cursor-format "[Line:%d, Column:%d]"
        cursor        (JLabel. "")
        filler        (JLabel. "")

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
                       (save
                         []
                         (try
                           (with-open [stream (FileWriter. @file-path)]
                             (do
                               (. this write stream)
                               true))
                           (catch Exception e
                             (. e printStackTrace)
                             (. JOptionPane (showMessageDialog nil (. e getMessage) "Error..." JOptionPane/OK_OPTION))
                             false)))
                       (saveAs
                         []
                         (let [chooser (JFileChooser. (str "~" env/os-file-separator))
                               result  (. chooser showSaveDialog nil)]
                           (if (= JFileChooser/APPROVE_OPTION result)
                               (do
                                 (. this setPath (.. chooser getSelectedFile getAbsolutePath))
                                 (. this save)))))
                       (open
                         [target]
                         (let [doc  (. this getDocument)
                               kit  (. this getEditorKit)
                               file (File. target)]
                           (do
                             (reset! file-path target)
                             (try
                               (with-open [stream (FileInputStream. @file-path)]
                                 (do
                                   (. this read stream doc)
                                   (. file-name setText (. file getName))
                                   true))
                               (catch FileNotFoundException _ true)
                               (catch Exception e
                                 (. e printStackTrace)
                                 (. JOptionPane (showMessageDialog nil (. e getMessage) "Error..." JOptionPane/OK_OPTION)))))))
                       (getInputMethodRequests []
                         (if (= nil @improved-imr)
                             (let [original-imr (proxy-super getInputMethodRequests)]
                               (reset! improved-imr (make-improved-imr original-imr))
                               @improved-imr)
                             @improved-imr)))
        scroll       (JScrollPane. text-pane
                                   JScrollPane/VERTICAL_SCROLLBAR_ALWAYS
                                   JScrollPane/HORIZONTAL_SCROLLBAR_ALWAYS)
        ;
        ; Line Numbers
        ;
        text-line-number (make-text-line-number text-pane 3)
        
        ;
        ; Root Panel
        ;
        root-panel   (proxy [JPanel] []
                       (requestFocusInWindow []
                         (. text-pane requestFocusInWindow)))
        ;
        ; Others
        ;
        map-vec           (@editorutils/ref-maps "default")
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
        char-code
        :gridx 1, :gridy 0
        separator
        :gridx 2, :gridy 0
        modified?
        :gridx 3, :gridy 0
        file-name
        :gridx 4, :gridy 0
        cursor
        :gridx 5, :gridy 0
        :fill :HORIZONTAL
        :weightx 1.0
        filler))

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
    (doseq [component [text-pane char-code separator modified? file-name cursor]]
      (editorutils/set-font component (default-fonts (env/get-os-keyword))))

    root-panel
    ))