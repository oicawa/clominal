(ns clominal.editors.MiddleKeyAction
  (:import (javax.swing AbstractAction Action JComponent SwingUtilities KeyStroke)
           (javax.swing.text Keymap))
  (:require [clominal.keys.keymap :as keymap])
  (:gen-class
   :extends javax.swing.AbstractAction
   :state state
   :init init
   :constructors {[javax.swing.KeyStroke javax.swing.InputMap javax.swing.ActionMap] []}
   ))

(defn -init
  [keystroke inputmap actionmap]
  [[] (ref {:keystroke keystroke
            :inputmap  inputmap
            :actionmap actionmap})])

(defn -actionPerformed
  [this evt]
  (let [{:keys      [keystroke inputmap actionmap]} @(.state this)
        text-editor (. evt getSource)
        mini-buffer (. text-editor getMiniBuffer)]
    (println keystroke)
    (doto text-editor
      (.setEditable false)
      (.setInputMap JComponent/WHEN_FOCUSED inputmap)
      (.setActionMap actionmap))
    (doto mini-buffer
      (.setText (str keystroke)))))

