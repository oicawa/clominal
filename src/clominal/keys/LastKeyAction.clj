(ns clominal.keys.LastKeyAction
  (:import (javax.swing AbstractAction Action)
           (javax.swing.text Keymap))
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
  (let [{:keys [action inputmap actionmap]} @(.state this)
        control (. evt getSource)]
    (. action actionPerformed evt)
    (. control setInputMap inputmap)
    (. control setActionMap actionmap)))

