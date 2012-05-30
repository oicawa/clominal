(ns clominal.platforms.frame
  (:use [clojure.contrib.def])
  (:require [clominal.platforms.tabs :as tabs])
  (:import (javax.swing JComponent JFrame JTabbedPane JEditorPane KeyStroke JButton ImageIcon)
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
    (.setIconImage (. (ImageIcon. "./resources/clojure-icon.gif") getImage))
    ))

(defvar *frame* (create) "'clominal' main frame.")

