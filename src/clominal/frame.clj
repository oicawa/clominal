(ns clominal.frame
  (:use [clojure.contrib.def]
        [clominal.utils]
        [clominal.dialog])
  (:import (java.io File)
           (javax.swing JComponent JFrame JTabbedPane JTextArea JButton ImageIcon JPanel JTextField JList JLabel
                        WindowConstants AbstractAction KeyStroke JOptionPane JScrollPane SwingUtilities BorderFactory)
           (javax.swing.event DocumentListener)
           (java.awt Toolkit Dimension GridBagLayout BorderLayout Font)
           (java.awt.event InputEvent KeyEvent WindowAdapter ActionListener)
           (clominal.utils ITabbedPane IAppPane)
           )
  (:require [clominal.editors.editor :as editor]
            [clominal.keys :as keys]
            [clominal.config :as config]
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
        :anchor :WEST
        :gridx 0 :gridy 0 :weightx 1.0 :weighty 0.0 :fill :HORIZONTAL
        textbox
        :gridx 0 :gridy 1 :weightx 1.0 :weighty 1.0 :fill :BOTH
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

(defn save-prop
  [frame tabs]
  (let [rect (. frame getBounds)]
    ; frame
    (config/set-prop :frame :x (. rect x))
    (config/set-prop :frame :y (. rect y))
    (config/set-prop :frame :width (. rect width))
    (config/set-prop :frame :height (. rect height))
    ; tabs
    (config/set-prop :tabs :info (. tabs getInfoList))
    (config/set-prop :tabs :index (. tabs getSelectedIndex))
    (config/save-prop)))

(definterface ITabPanel
  (setTitle [title])
  (getTitle [])
  (addCloseButtonListener [proc]))
(defn- make-tab
  [tabs]
  (let [label  (doto (JLabel.)
                 (.setBorder (BorderFactory/createEmptyBorder 0 0 0 4)))
        icon   (ImageIcon. "./resources/window-close.png")
        size   (Dimension. (. icon getIconWidth) (. icon getIconHeight))
        button (doto (JButton. icon)
                 (.setPreferredSize size))
        tab    (proxy [JPanel ITabPanel] []
                 (setTitle [title]
                   (. label setText title))
                 (getTitle []
                   (. label getText))
                 (addCloseButtonListener [proc]
                   (doto button
                     (.setPreferredSize size)
                     (.addActionListener (proxy [ActionListener] []
                                           (actionPerformed [e]
                                             (proc e)))))))]
    (. tabs setTabComponentAt (- (. tabs getTabCount) 1) tab)
    (doto tab
      (.setLayout (BorderLayout.))
      (.setOpaque false)
      (.add label BorderLayout/WEST)
      (.add button BorderLayout/EAST)
      (.setBorder (BorderFactory/createEmptyBorder 2 1 1 1)))))

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
                       (getInfoList []
                         (loop [index     (- (. this getTabCount) 1)
                                tabs-info []]
                           (if (< index 0)
                               tabs-info
                               (let [app (. this getComponentAt index)]
                                 (recur (- index 1) (cons (. app getInfo) tabs-info))))))
                       (setImeEnable [value])
                       (setInputMap [inputmap]
                         (. this setInputMap JComponent/WHEN_IN_FOCUSED_WINDOW inputmap))
                       (setActionMap [actionmap]
                         (proxy-super setActionMap actionmap))
                       (setKeyStroke [keystroke])
                       (addTab [title content]
                         (proxy-super addTab nil content)
                         (let [tab (doto (make-tab this)
                                     (.addCloseButtonListener
                                       (fn [e]
                                         (.. content close)))
                                     (.setTitle title))]
                           (. this setTabComponentAt (- (. this getTabCount) 1) tab)))
                       (setTitleAt [index title]
                         (let [tab (. this getTabComponentAt index)]
                           (. tab setTitle title))))
        console      (console/make-console tabs)
        frame        (proxy [JFrame IFrame] []
                       (showConsole []
                         (let [max-index (- (. tabs getTabCount) 1)]
                           (loop [i 0]
                             (cond (< max-index i)
                                    (add-component tabs console/console-title console)
                                   (= console/console-title (. tabs getTitleAt i))
                                    (. tabs setSelectedIndex i)
                                   :else
                                    (recur (+ i 1)))))))
        close-option (if (= mode "d")
                         JFrame/DISPOSE_ON_CLOSE
                         JFrame/EXIT_ON_CLOSE)]
    ;;
    ;; InputMap & ActionMap
    ;;
    (def maps (keys/make-keymaps tabs JComponent/WHEN_IN_FOCUSED_WINDOW))

    ;(. console setToSystemErr)

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
      (.setBounds (config/get-prop :frame :x)
                  (config/get-prop :frame :y)
                  (config/get-prop :frame :width)
                  (config/get-prop :frame :height))
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
                                    (. @exception printStackTrace)))
                              (let [tabs-info (config/get-prop :tabs :info)
                                    index     (config/get-prop :tabs :index)]
                                (doseq [info tabs-info]
                                  (let [generator (find-var (info :generator))
                                        app       (apply generator [tabs])]
                                    (. tabs addTab nil app)
                                    (. app open (info :id))))
                                (. tabs setSelectedIndex index)))
                            (windowClosing [evt]
                              (if (= '() (get-confirm-to-close-tabs tabs))
                                  (do
                                    (save-prop frame tabs)
                                    (. frame dispose))
                                  (let [option (JOptionPane/showConfirmDialog
                                                 frame 
                                                 "There are some unsaved documents.\nWould you save these modified documents?")]
                                    (cond (= option JOptionPane/YES_OPTION)
                                            nil
                                          (= option JOptionPane/NO_OPTION)
                                            (do
                                              (save-prop frame tabs)
                                              (. frame dispose))
                                          :else
                                            nil))))
                            (windowClosed [evt]
                              (System/exit 0)))))
    ))

