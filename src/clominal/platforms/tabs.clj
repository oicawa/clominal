(ns clominal.platforms.tabs
  (:use [clojure.contrib.def])
  (:require [clominal.action :as action]
            [clominal.editors.editor :as editor]
            )
  (:import (javax.swing JComponent JTabbedPane JPanel JEditorPane KeyStroke AbstractAction)
           (java.awt.event InputEvent KeyEvent)))

;; ------------------------------
;; TabbedPane
(defvar ^:dynamic *tabs* (doto (JTabbedPane.)
                           (.setTabPlacement JTabbedPane/TOP)
                           (.setTabLayoutPolicy JTabbedPane/SCROLL_TAB_LAYOUT)))


;; ------------------------------
;; InputMap & ActionMap
(defvar ^:dynamic *tabs-input-map*  (. *tabs* getInputMap JComponent/WHEN_IN_FOCUSED_WINDOW))
(defvar ^:dynamic *tabs-action-map* (. *tabs* getActionMap))


;; ------------------------------
;; Actions
(defvar ^:dynamic *add-tab* (action/create (fn [evt tabs]
                                             (let [editor (editor/make-editor tabs)]
                                               (. tabs addTab editor/new-title editor)
                                               (. tabs setSelectedIndex (- (. tabs getTabCount) 1))
                                               (. editor requestFocusInWindow)
                                               ))))

(defvar ^:dynamic *remove-tab* (action/create (fn [evt tabs]
                                                (let [index (. tabs getSelectedIndex)]
                                                  (. tabs (remove index))))))

(defvar ^:dynamic *forward-tab* (action/create (fn [evt tabs]
                                                 (let [index (. tabs getSelectedIndex)]
                                                   (. tabs (remove index))))))
;; ------------------------------
;; Regist actions
(. *tabs-action-map* put "add-tab" *add-tab*)
(. *tabs-action-map* put "remove-tab" *remove-tab*)


;; ------------------------------
;; Key strokes
(defvar ^:dynamic *add-tab-key*    (KeyStroke/getKeyStroke KeyEvent/VK_T InputEvent/CTRL_DOWN_MASK))
(defvar ^:dynamic *remove-tab-key* (KeyStroke/getKeyStroke KeyEvent/VK_W InputEvent/CTRL_DOWN_MASK))


;; ------------------------------
;; Regist key strokes
(. *tabs-input-map* put *add-tab-key* "add-tab")
(. *tabs-input-map* put *remove-tab-key* "remove-tab")
