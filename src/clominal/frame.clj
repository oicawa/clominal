(ns clominal.frame
  (:use [clojure.contrib.def])
  (:import (javax.swing JComponent JFrame JTabbedPane JEditorPane KeyStroke JButton ImageIcon JPanel AbstractAction)
           (java.awt.event InputEvent KeyEvent))
  (:require [clominal.editors.editor :as editor]
            [clominal.keys :as keys])
  (:use [clominal.utils]))

;;
;; InputMap & ActionMap
;;
(def maps (make-maps (JTabbedPane.) JComponent/WHEN_IN_FOCUSED_WINDOW))

;;
;; Actions
;;
(defaction add-tab [tabs]
  (let [editor (editor/make-editor tabs)]
    (. tabs addTab editor/new-title editor)
    (. tabs setSelectedIndex (- (. tabs getTabCount) 1))
    (. editor requestFocusInWindow)))

(defaction remove-tab [tabs]
  (let [index (. tabs getSelectedIndex)]
    (. tabs (remove index))))

(defaction forward-tab [tabs]
  (let [index (. tabs getSelectedIndex)]
    (. tabs (remove index))))


(defn make-frame
  "Create clominal main frame."
  [mode]
  (let [tabs           (proxy [JTabbedPane clominal.keys.IKeybindComponent] []
                         (setEditEnable [value])
                         (setInputMap [inputmap] (. this setInputMap JComponent/WHEN_IN_FOCUSED_WINDOW inputmap))
                         (setActionMap [actionmap] (proxy-super setActionMap actionmap))
                         (setKeyStroke [keystroke]))
        tabs-inputmap  ((. maps get "default") 0)
        tabs-actionmap ((. maps get "default") 1)
        frame          (JFrame.)
        close-option   (if (= mode "d")
                           JFrame/DISPOSE_ON_CLOSE
                           JFrame/EXIT_ON_CLOSE)]
    (doto tabs
      (.setTabPlacement JTabbedPane/TOP)
      (.setTabLayoutPolicy JTabbedPane/SCROLL_TAB_LAYOUT)
      (.setInputMap JComponent/WHEN_IN_FOCUSED_WINDOW tabs-inputmap)
      (.setActionMap tabs-actionmap))

    (doto frame
      (.setTitle "clominal")
      (.setDefaultCloseOperation close-option)
      (.setSize 600 400)
      (.setLocationRelativeTo nil)
      (.add tabs)
      (.setIconImage (. (ImageIcon. "./resources/clojure-icon.gif") getImage)))))

