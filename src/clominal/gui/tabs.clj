(ns clominal.gui.tabs
  (:use [clojure.contrib.def])
  (:require [clominal.action :as action]
            [clominal.gui.editor :as editor])
  (:import (javax.swing JComponent JTabbedPane JPanel JEditorPane KeyStroke AbstractAction)
           (java.awt.event InputEvent KeyEvent)))

(defvar *tabs* (doto (JTabbedPane.)
                 (.setTabPlacement JTabbedPane/TOP)
                 (.setTabLayoutPolicy JTabbedPane/SCROLL_TAB_LAYOUT)))

(defn- add-key-bind
  "Add key bind action to this main frame."
  [keys get-action-proc]
  (let [inputmap  (. *tabs* getInputMap JComponent/WHEN_IN_FOCUSED_WINDOW)
        actionmap (. *tabs* getActionMap)
        name      (:name (meta get-action-proc))]
    (doto actionmap
      (.put name (get-action-proc)))
    (doto inputmap
      (.put (KeyStroke/getKeyStroke keys) name))))

(defn- create-action
  "Create new tab action."
  [proc]
  (proxy [AbstractAction] []
    (actionPerformed [evt]
      (proc (. evt getSource)))))

(defn add
  "Add new tab."
  []
  (create-action (fn [tabs]
                   (println "called add-tab.")
                   (println tabs)
                   (let [editor (editor/create)]
                     (. tabs addTab editor/new-title editor)
                     (. tabs setSelectedIndex (- (. tabs getTabCount) 1))
                     (. editor requestFocusInWindow)))))

(defn remove-current
  "Remove current selected tab."
  []
  (create-action (fn [tabs]
                   (println "called remove-tab.")
                   (let [index (. tabs getSelectedIndex)]
                     (. tabs (remove index))))))

; (defn- create
;   "Create tabs pane."
;   []
;   (let [editor (editor/create)]
;     (action/add editor "control T" add-tab)
;     (action/add editor "control W" remove-tab)
;     (. editor requestFocusInWindow)
;     (doto (JTabbedPane.)
;       (.setTabPlacement JTabbedPane/TOP)
;       (.setTabLayoutPolicy JTabbedPane/SCROLL_TAB_LAYOUT)
;       (.addTab new-title editor))))



; Default key bind action
(add-key-bind "control T" add)
(add-key-bind "control W" remove-current)

