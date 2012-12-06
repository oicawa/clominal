(ns clominal.editors.editor
  (:import (java.awt Font Color GraphicsEnvironment GridBagLayout)
           (java.awt.event ActionEvent)
           (java.awt.im InputMethodRequests)
           (javax.swing InputMap ActionMap JComponent JTextPane JScrollPane Action
                        JLabel JTextField JPanel JOptionPane SwingConstants JFileChooser)
           (javax.swing.border LineBorder)
           (javax.swing.event CaretListener)
           (java.io File FileInputStream FileWriter FileNotFoundException)
           (clominal.editors AskMiniBufferAction))
  (:require [clominal.utils.guiutils :as guiutils]
            [clominal.utils.env :as env]
            [clominal.editors.utils :as editorutils]))

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
      (.addCaretListener (proxy [CaretListener] []
                           (caretUpdate [evt]
                             (let [src (. evt getSource)
                                   crt (. src getCaret)
                                   val (. src getText)
                                   dot (. crt getDot)
                                   row (count-by-pattern val "\n" 0 dot)
                                   clm (- dot (. val lastIndexOf "\n" dot))]
                               (. cursor setText (format cursor-format row clm)))))))

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