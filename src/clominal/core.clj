(System/setProperty "awt.useSystemAAFontSettings" "on")
(javax.swing.UIManager/setLookAndFeel "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel")

(ns clominal.core
  (:require [clominal.frame :as frame])
  (:import (javax.swing SwingUtilities)))

(defn debug-print
  [x]
  `(let [res# ~x]
     (println "?=" res#)
     res#))

(defn main [& args]
  (SwingUtilities/invokeLater
    #(let [max  (count args)
           mode (if (= 0 max) nil ((vec args) 0))]
       (. (frame/make-frame mode) setVisible true))))

(main "d")