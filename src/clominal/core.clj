(ns clominal.core
  (:require [clominal.gui.MainFrame])
  (:import (javax.swing UIManager))
  (:gen-class))

(defn -main [& args]
  (System/setProperty "awt.useSystemAAFontSettings" "on")
  (UIManager/setLookAndFeel "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel")
  (. (clominal.gui.MainFrame/create) setVisible true))
