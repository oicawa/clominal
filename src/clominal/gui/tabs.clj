(ns clominal.gui.tabs
  (:use [clojure.contrib.def])
  (:require [clominal.action :as action]
            [clominal.gui.editor :as editor])
  (:import (javax.swing JComponent JTabbedPane JPanel JEditorPane KeyStroke AbstractAction)
           (java.awt.event InputEvent KeyEvent)))

;; ------------------------------
;; TabbedPane
(defvar *tabs* (doto (JTabbedPane.)
                 (.setTabPlacement JTabbedPane/TOP)
                 (.setTabLayoutPolicy JTabbedPane/SCROLL_TAB_LAYOUT)))


;; ------------------------------
;; InputMap & ActionMap
(defvar *tabs-input-map*  (. *tabs* getInputMap JComponent/WHEN_IN_FOCUSED_WINDOW))
(defvar *tabs-action-map* (. *tabs* getActionMap))


;; ------------------------------
;; Actions
(defvar *add-tab* (action/create (fn [tabs]
                                   (println "called add-tab.")
                                   (let [editor (editor/create)]
                                     (. tabs addTab editor/new-title editor)
                                     (. tabs setSelectedIndex (- (. tabs getTabCount) 1))
                                     (. editor requestFocusInWindow)))))

(defvar *remove-tab* (action/create (fn [tabs]
                                      (println "called remove-tab.")
                                      (let [index (. tabs getSelectedIndex)]
                                        (. tabs (remove index))))))

(defvar *forward-tab* (action/create (fn [tabs]
                                       (println "called forward-tab.")
                                       (let [index (. tabs getSelectedIndex)]
                                         (. tabs (remove index))))))
;; ------------------------------
;; Regist actions
(. *tabs-action-map* put "add-tab" *add-tab*)
(. *tabs-action-map* put "remove-tab" *remove-tab*)


;; ------------------------------
;; Key strokes
(defvar *add-tab-key*    (KeyStroke/getKeyStroke KeyEvent/VK_T InputEvent/CTRL_DOWN_MASK))
(defvar *remove-tab-key* (KeyStroke/getKeyStroke KeyEvent/VK_W InputEvent/CTRL_DOWN_MASK))


;; ------------------------------
;; Regist key strokes
(. *tabs-input-map* put *add-tab-key* "add-tab")
(. *tabs-input-map* put *remove-tab-key* "remove-tab")
