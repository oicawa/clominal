(ns clominal.gui.MainFrame
  (:require [clominal.gui.actions.AddTabAction])
  (:import (javax.swing JFrame JTabbedPane JEditorPane KeyStroke)
           (java.awt.event InputEvent KeyEvent)))

(defn createTab
  "Create tabs."
  []
  (doto (JTabbedPane.)
    (.setTabPlacement JTabbedPane/TOP)
    (.setTabLayoutPolicy JTabbedPane/SCROLL_TAB_LAYOUT)
    (.addTab "新規テキスト" (JEditorPane.))
    ))

(defn create
  "Create clominal main frame."
  []
  (def tabs (createTab))
  (def inputmap (. tabs getInputMap))
  (def actionmap (. tabs getActionMap))
  (println inputmap)
  (println actionmap)

  (def addtabaction (clominal.gui.actions.AddTabAction/create))
  (doto actionmap
    (.put "add-tab" addtabaction))

  (doto inputmap
    (.put (KeyStroke/getKeyStroke KeyEvent/VK_T InputEvent/CTRL_DOWN_MASK) "add-tab"))
  ;   ;(.put (KeyStroke/getKeyStroke 'T' (Event/CTRL_MASK)) "add-tab")
  ;   ;(.put (KeyStroke/getKeyStroke))

  (def frame (proxy [JFrame] []))

  (doto frame
    (.setTitle "clominal")
    (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
    (.setSize 600 400)
    (.setLocationRelativeTo nil)
    (.add tabs)))


