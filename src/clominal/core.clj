(System/setProperty "awt.useSystemAAFontSettings" "on")
(javax.swing.UIManager/setLookAndFeel "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel")

(ns clominal.core
  (:require [clominal.action :as action]
            [clominal.keys.keymap :as keymap]
            [clominal.platforms.frame :as frame])
  (:gen-class))

(defn -main [& args]
  (. frame/*frame* setVisible true))
