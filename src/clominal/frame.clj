(ns clominal.frame
  (:use [clojure.contrib.def]
        [clominal.utils]
        [clominal.dialog])
  (:import (javax.swing JComponent JFrame JTabbedPane JTextArea JButton ImageIcon JPanel JTextField JList
                        WindowConstants AbstractAction KeyStroke JOptionPane JScrollPane SwingUtilities)
           (javax.swing.event DocumentListener)
           ;(java.awt Font Color Graphics GraphicsEnvironment GridBagLayout Point)
           (java.awt Toolkit GridBagLayout Font)
           (java.awt.event InputEvent KeyEvent WindowAdapter)
           (clominal.utils ITabbedPane IAppPane)
           )
  (:require [clominal.editors.editor :as editor]
            [clominal.keys :as keys]
            [clominal.console :as console]))



;;
;; Actions
;; 
(defaction remove-selected-tab [tabs]
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
        tool-list))
    (. dialog setVisible true)
    (let [input-value (. textbox getText)]
      (if (not (= nil input-value))
          (JOptionPane/showMessageDialog tabs input-value)))))


(defn get-confirm-to-close-tabs
  [tabs]
  (let [cnt (. tabs getTabCount)]
    (loop [idx (- cnt 1)
           ans '()]
      (if (< idx 0)
          ans
          (let [tab (. tabs getComponentAt idx)]
            (if (. tab canClose)
                (recur (- idx 1) ans)
                (recur (- idx 1) (cons tab ans))))))))



(defaction load-module [tabs]
  (let [panel  (make-list-panel)
        dialog (make-dialog "Plugins" panel)]
    (. dialog setVisible true)
    (let [input-value (. panel getInputValue)]
      (if (= nil input-value)
          nil
          (let [namespace-name input-value]
            (try
              (require (symbol namespace-name) :reload-all)
              (JOptionPane/showMessageDialog tabs (format "'%s' is reloaded successful." input-value))
              (catch Exception e
                (. e printStackTrace))))))))

(definterface IFrame
  (showConsole []))

(defn make-frame
  "Create clominal main frame."
  [mode]
  (let [exception    (atom nil)
        ime-mode     (atom nil)
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
        console      (console/make-console)
        frame        (proxy [JFrame IFrame] []
                       (showConsole []
                         (let [max-index (- (. tabs getTabCount) 1)]
                           (loop [i 0]
                             (cond (< max-index i)
                                    (let [console-panel (proxy [JPanel IAppPane] []
                                                          (canClose [] true)
                                                          (getTabs [] tabs)
                                                          (getTabIndex []
                                                            (. tabs indexOfComponent this)))]
                                      (doto console-panel
                                        (.setLayout (GridBagLayout.))
                                        (.setName "console-panel")
                                        (grid-bag-layout
                                          :fill :BOTH
                                          :gridx 0 :gridy 0 :weightx 1.0 :weighty 1.0
                                          console))
                                      (add-component tabs console/console-title console-panel))
                                  (= console/console-title (. tabs getTitleAt i))
                                    (. tabs setSelectedIndex i)
                                  :else
                                    (recur (+ i 1)))))))
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

    (. console setToSystemErr)

    (try
      (println "[ START ] Loading settings...")
      (require 'settings)
      (println "[  END  ] Loaded settings")
      (catch Exception e
        (reset! exception e)))

    (let [tabs-map (. maps get "default")]
      (doto tabs
        (.setTabPlacement JTabbedPane/TOP)
        (.setTabLayoutPolicy JTabbedPane/SCROLL_TAB_LAYOUT)))

    (doto frame
      (.setTitle "clominal")
      (.setDefaultCloseOperation WindowConstants/DO_NOTHING_ON_CLOSE)
      (.setSize frame-width frame-height)
      (.setLocationRelativeTo nil)
      ;(.add tabs)
      (.setLayout (GridBagLayout.))
      (grid-bag-layout
        :fill :BOTH
        :gridx 0 :gridy 0 :weightx 1.0 :weighty 1.0
        tabs)
      (.setIconImage (. (ImageIcon. "./resources/clojure-icon.gif") getImage))
      (.addWindowListener (proxy [WindowAdapter] []
                            (windowOpened [evt]
                              (if (not (nil? @exception))
                                  (do
                                    (println "Exception occured.")
                                    (. @exception printStackTrace))))
                            (windowClosing [evt]
                              (if (= '() (get-confirm-to-close-tabs tabs))
                                  (. frame dispose)
                                  (let [option (JOptionPane/showConfirmDialog
                                                 frame 
                                                 "There are some unsaved documents.\nWould you save these modified documents?")]
                                    (cond (= option JOptionPane/YES_OPTION)
                                            nil
                                          (= option JOptionPane/NO_OPTION)
                                            (. frame dispose)
                                          :else
                                            nil))))
                            (windowClosed [evt]
                              (System/exit 0)))))
    ))

