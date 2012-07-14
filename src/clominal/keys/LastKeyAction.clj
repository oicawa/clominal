(ns clominal.keys.LastKeyAction
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
        component (. evt getSource)]
    (. action actionPerformed evt)
    (SwingUtilities/invokeLater
      #(do
        (. component setEditable true)
        (. component setInputMap JComponent/WHEN_FOCUSED inputmap)
        (. component setActionMap actionmap)
        ; (keymap/enable-inputmethod control true)
        ;(println "isEditable =" (. component isEditable))
        ))
    ))

