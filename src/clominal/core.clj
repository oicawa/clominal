(System/setProperty "awt.useSystemAAFontSettings" "on")
(javax.swing.UIManager/setLookAndFeel "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel")

(ns clominal.core
  (:require [clominal.platforms.frame :as frame]
            [clominal])
  (:import (javax.swing SwingUtilities)))

(defn main [& args]
  (SwingUtilities/invokeLater
    #(let [max  (count args)
           mode (if (= 0 max) nil ((vec args) 0))]
       (frame/assign mode)
       (. frame/*frame* setVisible true))))

(main "d")