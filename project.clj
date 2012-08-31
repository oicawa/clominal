(defproject clominal "1.0.0-SNAPSHOT"
  :description "Clominal will be a clojure terminal constructed by Swing components."
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]]
  :main clominal.core
  :aot [clominal.editors.MiniBufferAction
        clominal.editors.AskMiniBufferAction
        clominal.editors.MiniBuffer
        clominal.editors.ModeLine
        clominal.editors.TextEditor
        clominal.editors.MiddleKeyAction
        clominal.editors.LastKeyAction
        clominal.editors.EditorPanel
        ])
