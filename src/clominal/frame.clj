(ns clominal.frame
  (:use [clojure.contrib.def])
  (:import (javax.swing JComponent JFrame JTabbedPane JEditorPane JButton ImageIcon JPanel JTextField JList
                        WindowConstants AbstractAction KeyStroke JOptionPane)
           (java.awt Toolkit GridBagLayout)
           (java.awt.event InputEvent KeyEvent WindowAdapter))
  (:require [clominal.editors.editor :as editor]
            [clominal.keys :as keys])
  (:use [clominal.utils])
  (:use [clominal.dialog]))

(definterface ITabbedPane
  (getCurrentPanel []))

;;
;; Actions
;; 
(defaction select-tab [tabs]
  (let [index (. tabs getSelectedIndex)]
    (. tabs (remove index))))


(defaction show-tools [tabs]
  (let [panel     (JPanel.)
        textbox   (JTextField.)
        tool-list (JList.)
        dialog    (make-dialog "Tools" panel)]
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
        tool-list
        ))

    (. dialog setVisible true)
    (let [input-value (. textbox getText)]
      (if (not (= nil input-value))
          (JOptionPane/showMessageDialog tabs input-value)))))


(defn get-modified-tabs
  [tabs]
  (let [cnt (. tabs getTabCount)]
    (loop [idx (- cnt 1)
           ans '()]
      (if (< idx 0)
          ans
          (let [tab (. tabs getComponentAt idx)]
            (if (. tab isDirty)
                (recur (- idx 1) (cons tab ans))
                (recur (- idx 1) ans)))))))

(defn make-frame
  "Create clominal main frame."
  [mode]
  (let [ime-mode     (atom nil)
        tabs         (proxy [JTabbedPane ITabbedPane clominal.keys.IKeybindComponent] []
                       (getCurrentPanel []
                         (let [index (. this getSelectedIndex)]
                           (if (< index 0)
                               nil
                               (. this getComponentAt index))))
                       (setImeEnable [value])
                       (setInputMap [inputmap]
                         (. this setInputMap JComponent/WHEN_IN_FOCUSED_WINDOW inputmap))
                       (setActionMap [actionmap]
                         (proxy-super setActionMap actionmap))
                       (setKeyStroke [keystroke]))
        frame        (JFrame.)
        close-option (if (= mode "d")
                         JFrame/DISPOSE_ON_CLOSE
                         JFrame/EXIT_ON_CLOSE)
        screen-size  (. (Toolkit/getDefaultToolkit) getScreenSize)
        frame-height (* (. screen-size height) 0.9)
        frame-width  (if (< (. screen-size width) 800)
                         (. screen-size width)
                         800)]
    ;;
    ;; InputMap & ActionMap
    ;;
    (def maps (keys/make-keymaps tabs JComponent/WHEN_IN_FOCUSED_WINDOW))

    (try
      (require 'settings)
      (catch Exception e
        (JOptionPane/showMessageDialog nil (str (. e getMessage) "\n\n" (. e printStackTrace))  "Error..." JOptionPane/OK_OPTION)
        (throw e)))

    (let [tabs-map (. maps get "default")]
      (doto tabs
        (.setTabPlacement JTabbedPane/TOP)
        (.setTabLayoutPolicy JTabbedPane/SCROLL_TAB_LAYOUT)))

    (doto frame
      (.setTitle "clominal")
      (.setDefaultCloseOperation WindowConstants/DO_NOTHING_ON_CLOSE)
      (.setSize frame-width frame-height)
      (.setLocationRelativeTo nil)
      (.add tabs)
      (.setIconImage (. (ImageIcon. "./resources/clojure-icon.gif") getImage))
      (.addWindowListener (proxy [WindowAdapter] []
                            (windowClosing [evt]
                              (if (= '() (get-modified-tabs tabs))
                                  (. frame dispose)
                                  (let [option (JOptionPane/showConfirmDialog
                                                 frame 
                                                 "There are some unsaved documents.\nWould you save these modified documents?")]
                                    (cond (= option JOptionPane/YES_OPTION) nil
                                          (= option JOptionPane/NO_OPTION)  (. frame dispose)
                                          :else                             nil))
                                  ))
                            (windowClosed [evt]
                              (System/exit 0)))))
    ))

