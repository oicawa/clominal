(ns clominal.gui.actions.AddTabAction
  (:import (javax.swing AbstractAction JEditorPane)))

(defn create
  "Add tab action."
  []
  (proxy [AbstractAction] []
    (actionPerformed [evt]
      (println "Add tab action.")
      (println evt)
      (.. evt getSource (addTab "追加新規テキスト" (JEditorPane.)))
      )))