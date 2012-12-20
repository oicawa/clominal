(ns clominal.frame
  (:use [clojure.contrib.def])
  (:import (javax.swing JComponent JFrame JTabbedPane JEditorPane KeyStroke JButton ImageIcon JPanel AbstractAction WindowConstants JOptionPane)
           (java.awt.event InputEvent KeyEvent WindowAdapter))
  (:require [clominal.editors.editor :as editor]
            [clominal.keys :as keys])
  (:use [clominal.utils]))

;;
;; Actions
;;
(defaction select-tab [tabs]
  (let [index (. tabs getSelectedIndex)]
    (. tabs (remove index))))


(defn get-modified-tabs
  [tabs]
  (let [cnt (. tabs getTabCount)]
    (loop [idx (- cnt 1)
           ans '()]
      (if (< idx 0)
          ans
          (let [tab (. tabs getComponentAt idx)]
            (if (. tab getModified)
                (recur (- idx 1) (cons tab ans))
                (recur (- idx 1) ans)))))))

(defn make-frame
  "Create clominal main frame."
  [mode]
  (let [ime-mode     (atom nil)
        tabs         (proxy [JTabbedPane clominal.keys.IKeybindComponent] []
                       (setImeEnable [value])
                       (setInputMap [inputmap]
                         (. this setInputMap JComponent/WHEN_IN_FOCUSED_WINDOW inputmap))
                       (setActionMap [actionmap]
                         (proxy-super setActionMap actionmap))
                       (setKeyStroke [keystroke]))
        frame        (JFrame.)
        close-option (if (= mode "d")
                         JFrame/DISPOSE_ON_CLOSE
                         JFrame/EXIT_ON_CLOSE)]
    ;;
    ;; InputMap & ActionMap
    ;;
    (def maps (keys/make-keymaps tabs JComponent/WHEN_IN_FOCUSED_WINDOW))
    (require 'settings)
    (let [tabs-map (. maps get "default")]
      (doto tabs
        (.setTabPlacement JTabbedPane/TOP)
        (.setTabLayoutPolicy JTabbedPane/SCROLL_TAB_LAYOUT)
        ; (.setInputMap JComponent/WHEN_IN_FOCUSED_WINDOW (. tabs-map getInputMap))
        ; (.setActionMap (. tabs-map getActionMap))
        ))

    (doto frame
      (.setTitle "clominal")
      ;(.setDefaultCloseOperation close-option)
      (.setDefaultCloseOperation WindowConstants/DO_NOTHING_ON_CLOSE)
      (.setSize 600 400)
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

