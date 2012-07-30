(ns clominal.editors.AskMiniBufferAction
  (:import (javax.swing AbstractAction Action JComponent SwingUtilities)
           (javax.swing.text Keymap))
  (:require [clominal.keys.keymap :as keymap])
  (:gen-class
   :extends javax.swing.AbstractAction
   :state state
   :init init
   :constructors {[javax.swing.Action String String] []}
   ))

(defn -init
  [action caption default-value]
  [[] (ref {:action action
            :caption caption
            :default-value default-value})])

(defn -actionPerformed
  [this evt]
  (let [{:keys [action caption default-value]} @(.state this)
        text-editor (. evt getSource)
        mini-buffer (. text-editor getMiniBuffer)]
    (doto mini-buffer
      (.prompt caption)
      (.text default-value)
      (.enable true))))

