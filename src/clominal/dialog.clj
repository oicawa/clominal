(ns clominal.dialog
  (:import (java.awt Font Color GraphicsEnvironment GridBagLayout)
           (java.awt.event ActionEvent)
           (java.awt.im InputMethodRequests)
           (javax.swing JDialog InputMap ActionMap JComponent JTextPane JScrollPane Action JLabel JTextField JPanel JOptionPane JList
                        WindowConstants SwingConstants)
           (javax.swing.border LineBorder)
           (java.io File FileInputStream FileWriter FileNotFoundException))
  (:require [clominal.utils :as utils])
  (:use [clominal.utils]))

(definterface IDialog
  (getPanel []))

(definterface IDialogListPanel
  (getInputValue []))

(defn make-dialog
  [title panel]
  (doto (proxy [JDialog IDialog] []
          (getPanel [] panel))
    (.setTitle title)
    (.setModal true)
    (.setDefaultCloseOperation WindowConstants/DISPOSE_ON_CLOSE)
    (.setSize 400 300)
    (.add panel)
    (.setLocationRelativeTo nil)))


(defn make-list-panel
  []
  (let [textbox  (JTextField.)
        itemlist (JList.)
        panel    (proxy [JPanel IDialogListPanel] []
                   (getInputValue []
                     (. textbox getText)))
        ]
    (doto panel
      ;(.setPreferredSize nil)
      (.setLayout (GridBagLayout.))
      (grid-bag-layout
        :gridx 0, :gridy 0
        :anchor :WEST
        :weightx 1.0 :weighty 0.0
        :fill :HORIZONTAL
        textbox
        :gridx 0, :gridy 1
        :weightx 1.0 :weighty 1.0
        :fill :BOTH
        itemlist
        ))))

