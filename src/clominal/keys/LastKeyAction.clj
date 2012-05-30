(ns clominal.keys.LastKeyAction
  (:import (javax.swing AbstractAction Action)
           (javax.swing.text Keymap))
  (:gen-class
   :extends javax.swing.AbstractAction
   :state state
   :init init
   :constructors {[javax.swing.Action javax.swing.text.Keymap] []}
   ))

(defn -init
  [action keymap]
  [[] (ref {:action action
            :keymap keymap})])

(defn -actionPerformed
  [this evt]
  (let [{:keys [action keymap]} @(.state this)
        control (. evt getSource)]
    (. action actionPerformed evt)
    (. control setKeymap keymap)))

