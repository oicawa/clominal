(ns clominal.platforms.frame
  (:use [clojure.contrib.def])
  (:require [clominal.platforms.tabs :as tabs])
  (:import (javax.swing JComponent JFrame JTabbedPane JEditorPane KeyStroke JButton ImageIcon)
           (java.awt.event InputEvent KeyEvent)))

(defn create
  "Create clominal main frame."
  [mode]
  (let [close-option (if (= mode "d")
                         JFrame/DISPOSE_ON_CLOSE
                         JFrame/EXIT_ON_CLOSE)]
    (doto (JFrame.)
      (.setTitle "clominal")
      (.setDefaultCloseOperation close-option)
      (.setSize 600 400)
      (.setLocationRelativeTo nil)
      (.add tabs/*tabs*)
      (.setIconImage (. (ImageIcon. "./resources/clojure-icon.gif") getImage)))))

(def *frame* "'clominal' main frame.")

(defn assign
  [mode]
  (def *frame* (create mode)))

