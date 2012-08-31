(ns clominal.action
  (:import (javax.swing AbstractAction))
  (:gen-class))

(defn create
  "Create new action that calls proc function."
  [proc]
  (proxy [AbstractAction] []
    (actionPerformed [evt]
      (proc evt (. evt getSource)))))

