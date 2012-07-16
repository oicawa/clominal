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
    #(do
      (let [max  (count args)
            mode (if (= 0 max) nil ((vec args) 0))]
        (println "Count of Arguments = " max)
        (println "1st Argument = " mode)
        (frame/assign mode)
        (. frame/*frame* setVisible true)))))
