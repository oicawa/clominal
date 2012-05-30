(ns clominal.core
  (:require [clominal.action :as action]
            [clominal.keys.keymap :as keymap]
            [clominal.platforms.frame :as frame])
  (:import (javax.swing UIManager))
  (:gen-class))

;(compile 'clominal.keys.LastKeyAction)

(defn -main [& args]
  ;(System/setProperty "awt.useSystemAAFontSettings" "on")
  ;(UIManager/setLookAndFeel "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel")
  (. frame/*frame* setVisible true))
