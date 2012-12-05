(ns clominal.editors.editor
  (:import (java.awt Font Color GraphicsEnvironment GridBagLayout)
           (java.awt.event ActionEvent)
           (java.awt.im InputMethodRequests)
           (javax.swing InputMap ActionMap JComponent JTextPane JScrollPane Action JLabel JTextField JPanel JOptionPane SwingConstants)
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

(definterface IEditor
  (^javax.swing.JTextPane getTextPane []))

(defn make-editor
  []
  (let [;
        ; Text Editor
        ;
        improved-imr (atom nil)
        text-editor  (proxy [JTextPane] []
                       (getInputMethodRequests []
                         (if (= nil @improved-imr)
                             (let [original-imr (proxy-super getInputMethodRequests)]
                               (reset! improved-imr (make-improved-imr original-imr))
                               @improved-imr)
                             @improved-imr)))
        scroll       (JScrollPane. text-editor
                                   JScrollPane/VERTICAL_SCROLLBAR_ALWAYS
                                   JScrollPane/HORIZONTAL_SCROLLBAR_ALWAYS)
        ;
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
        ; Root Panel
        ;
        root-panel    (proxy [JPanel IEditor] []
                        (^javax.swing.JTextPane getTextEditor [] text-editor))

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
    (doto text-editor
      (.setName "text-editor")
      (.setInputMap  JComponent/WHEN_FOCUSED default-inputmap)
      (.setActionMap default-actionmap)
      (.enableInputMethods true)
      (.addCaretListener (proxy [CaretListener] []
                           (caretUpdate [evt]
                             (let [src (. evt getSource)
                                   crt (. src getCaret)
                                   pos (. crt getMagicCaretPosition)]
                               (println "pos:" pos)
                               (. cursor setText (format cursor-format (. pos x) (. pos y))))))))

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
    (apply editorutils/set-font text-editor (default-fonts (env/get-os-keyword)))
    (apply editorutils/set-font statusbar   (default-fonts (env/get-os-keyword)))
    root-panel
    ))