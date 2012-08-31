(ns clominal.editors.AskMiniBufferAction
  (:import (javax.swing AbstractAction Action JComponent SwingUtilities)
           (javax.swing.text Keymap)
           (clominal.editors MiniBufferAction))
  (:require [clominal.keys.keymap :as keymap]
            [clominal.action :as action])
  (:gen-class
   :extends javax.swing.AbstractAction
   :state state
   :init init
   :constructors {[String String clojure.lang.IFn] []}
   ))

(defn -init
  [caption default-value func]
  [[] (ref {:caption       caption
            :default-value default-value
            :func          func})])


(defn -actionPerformed
  [this evt]
  (let [{:keys [caption default-value func]} @(.state this)
        text-editor (. evt getSource)
        mini-buffer (. text-editor getMiniBuffer)]
    (doto mini-buffer
      (.setAction (MiniBufferAction. func))
      (.prompt caption)
      (.text default-value)
      (.enable true))))

