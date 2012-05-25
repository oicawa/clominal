(ns clominal.core
  (:use [clominal.gui.frame])
  (:import (javax.swing UIManager))
  (:gen-class))

(defn -main [& args]
  ;(System/setProperty "awt.useSystemAAFontSettings" "on")
  ;(UIManager/setLookAndFeel "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel")
  (. *frame* setVisible true))
