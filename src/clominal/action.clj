(ns clominal.action
  (:import (javax.swing AbstractAction KeyStroke)
           (java.awt.event InputEvent KeyEvent))
  (:gen-class))

(defn add
  "Add action."
  [target keys proc]
  (let [inputmap  (. target getInputMap)
        actionmap (. target getActionMap)
        name      (:name (meta proc))]
    (doto actionmap
      (.put name (proxy [AbstractAction] []
                   (actionPerformed [evt]
                     ; (proc (. evt getSource))
                     (proc)
                     ))))
    (doto inputmap
      (.put (KeyStroke/getKeyStroke keys) name))))
