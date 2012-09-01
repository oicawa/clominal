(ns clominal.editors.MiniBufferAction
  (:import (javax.swing AbstractAction Action JComponent SwingUtilities)
           (javax.swing.text Keymap))
  (:require [clominal.keys.keymap :as keymap]
            [clominal.action :as action])
  (:gen-class
   :extends javax.swing.AbstractAction
   :state state
   :init init
   :constructors {[clojure.lang.IFn] []}
   ))

(defn -init
  [func]
  [[] (ref {:func func})])


(defn -actionPerformed
  [this evt]
  (let [{:keys [func]} @(.state this)
        input-line  (. evt getSource)
        mini-buffer (. input-line getParent)
        text-editor (. mini-buffer getTextEditor)]
    (if (func evt mini-buffer text-editor)
        (do
          (doto mini-buffer
            (.removeActions)
            (.text nil)
            (.prompt nil)
            (.enable false))
          (doto text-editor
            (.requestFocusInWindow)))
        nil)))

