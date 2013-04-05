(ns clominal.frame
  (:use [clojure.contrib.def])
  (:import (javax.swing JComponent JFrame JTabbedPane JTextArea JButton ImageIcon JPanel JTextField JList
                        WindowConstants AbstractAction KeyStroke JOptionPane JScrollPane)
           ;(java.awt Font Color Graphics GraphicsEnvironment GridBagLayout Point)
           (java.awt Toolkit GridBagLayout Font)
           (java.awt.event InputEvent KeyEvent WindowAdapter))
  (:require [clominal.editors.editor :as editor]
            [clominal.keys :as keys])
  (:use [clominal.utils])
  (:use [clominal.dialog]))

(definterface ITabbedPane
  (getCurrentPanel []))

(defn set-font
  [component parameters]
  (let [name (parameters 0)
        type (parameters 1)
        size (parameters 2)]
    (. component setFont (Font. name type size))))

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



(defn add-component
  [tabs title component]
  (. tabs addTab title component)
  (let [index         (- (. tabs getTabCount) 1)
        ;tabcomponent (. tabs getComponentAt index)
        ]
    (. tabs setSelectedIndex index)
    (. component requestFocusInWindow)
    component))

;;
;; Error Buffer
;;

(defvar error-buffer-title "* ERROR *")

(definterface IErrorBuffer
  (getTabs [])
  (getRoot [])
  (getTabIndex [])
  (isMark [])
  (setMark [position]))

(definterface IErrorPane
  (isDirty [])
  (getErrorBuffer [])
  (getScroll []))


;
; Error Buffer Key Maps
;
(def error-buffer-maps (keys/make-keymaps (JTextArea.) JComponent/WHEN_FOCUSED))

(defn make-error-buffer
  [tabs]
  (let [is-marked    (atom false)
        error-buffer (proxy [JTextArea IErrorBuffer clominal.keys.IKeybindComponent] []
                       (setInputMap [inputmap] (. this setInputMap JComponent/WHEN_FOCUSED inputmap))
                       (setActionMap [actionmap] (proxy-super setActionMap actionmap))
                       (getTabs [] tabs)
                       (getRoot []
                         (.. this getParent getParent getParent))
                       (getTabIndex []
                         (let [tabs (. this getTabs)
                               root (. this getRoot)]
                           (. tabs indexOfComponent root)))
                       (isMark [] @is-marked)
                       (setMark [marked]
                         (reset! is-marked marked)))
        scroll       (JScrollPane. error-buffer)
        ;
        ; Root Panel
        ;
        root-panel   (proxy [JPanel IErrorPane] []
                       (isDirty [] false)
                       (getErrorBuffer []
                         error-buffer)
                       (requestFocusInWindow []
                         (. error-buffer requestFocusInWindow))
                       (getScroll [] scroll))
        ;
        ; Others
        ;
        default-map       (. error-buffer-maps get "default")
        default-fonts     {:linux   ["YOzFontCF" Font/PLAIN 16]
                           :windows ["ＭＳ ゴシック" Font/PLAIN 14]}
        ]
    ;
    ; Error Buffer
    ;
    (doto error-buffer
      (.setName "error-buffer")
      (.setInputMap  JComponent/WHEN_FOCUSED (. default-map getInputMap))
      (.setActionMap (. default-map getActionMap))
      (.setEditable false))

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
        scroll))
    (doseq [component [error-buffer]]
      (set-font component (default-fonts (get-os-keyword))))

    root-panel
    ))

(defn get-error-buffer
  [tabs]
  (let [max-index (- (. tabs getTabCount) 1)]
    (loop [i 0]
      (cond (< max-index i)
              (let [error-buffer (make-error-buffer tabs)]
                (add-component tabs error-buffer-title error-buffer)
                error-buffer)
            (= error-buffer-title (. tabs getTitleAt i))
              (. tabs getTabComponent i)
            :else
              (recur (+ i 1))))))


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
                (JOptionPane/showMessageDialog nil (str (. e getMessage) "\n\n" (. e printStackTrace))  "Error..." JOptionPane/OK_OPTION)
                (throw e))))))))

(defn print-error-message
  [tabs exception]
  (let [error-buffer (. (get-error-buffer tabs) getErrorBuffer)
        builder      (StringBuilder.)]
    (doto builder
      (.append (. exception getMessage))
      (.append \newline))
    (doseq [stackTrace (. exception getStackTrace)]
      (. builder append stackTrace)
      (. builder append \newline))
    (. error-buffer setText (. builder toString))))

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
        (print-error-message tabs e)))

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
                              (System/exit 0)))))))