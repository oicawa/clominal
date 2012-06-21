(System/setProperty "awt.useSystemAAFontSettings" "on")
(javax.swing.UIManager/setLookAndFeel "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel")

(ns clominal.core
  (:require [clominal.action :as action]
            [clominal.keys.keymap :as keymap]
            [clominal.platforms.frame :as frame])
  (:import (javax.swing SwingUtilities))
  (:gen-class))

(defn -main [& args]
  (SwingUtilities/invokeLater
    #(. frame/*frame* setVisible true)))
