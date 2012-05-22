(ns clominal.gui.actions.tabs)

(defn add
  [tabs]
  (.. tabs (addTab "追加新規テキスト" (JEditorPane.))))

(defn remove
  [tabs]
  (.. tabs (addTab "追加新規テキスト" (JEditorPane.))))
