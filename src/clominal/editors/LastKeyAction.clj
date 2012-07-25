(ns clominal.editors.LastKeyAction
  (:import (javax.swing AbstractAction Action JComponent SwingUtilities)
           (javax.swing.text Keymap))
  (:require [clominal.keys.keymap :as keymap])
  (:gen-class
   :extends javax.swing.AbstractAction
   :state state
   :init init
   :constructors {[javax.swing.Action javax.swing.InputMap javax.swing.ActionMap] []}
   ))

(defn -init
  [action inputmap actionmap]
  [[] (ref {:action    action
            :inputmap  inputmap
            :actionmap actionmap})])

(defn -actionPerformed
  [this evt]
  (let [{:keys    [action inputmap actionmap]} @(.state this)
        text-editor (. evt getSource)
        mini-buffer (. text-editor getMiniBuffer)]
    (doto text-editor
      (.setEditable true)
      (.setInputMap JComponent/WHEN_FOCUSED inputmap)
      (.setActionMap actionmap))
    (doto mini-buffer
      (.setText ""))
    (. action actionPerformed evt)))

