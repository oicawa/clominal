(ns clominal.gui.frame
  (:use [clojure.contrib.def])
  (:require ;[clominal.gui.actions.AddTabAction]
            [clominal.gui.tabs :as tabs])
  (:import (javax.swing JComponent JFrame JTabbedPane JEditorPane KeyStroke JButton)
           (java.awt.event InputEvent KeyEvent)))

(defn- create
  "Create clominal main frame."
  []
  (doto (JFrame.)
    (.setTitle "clominal")
    (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
    (.setSize 600 400)
    (.setLocationRelativeTo nil)
    (.add tabs/*tabs*)
    ))

(defvar *frame* (create) "'clominal' main frame.")

