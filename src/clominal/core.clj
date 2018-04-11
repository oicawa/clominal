(System/setProperty "UnicodeWriter.writeUtf8BOM" "false")
(javax.swing.UIManager/setLookAndFeel "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel")
;;; The below class can
;(javax.swing.UIManager/setLookAndFeel "javax.swing.plaf.nimbus.NimbusLookAndFeel")

(ns clominal.core
  (:require [clominal.frame :as frame]
            [clominal.config :as config])
  (:use     [clominal.utils])
  (:import (java.io File)
           (javax.swing SwingUtilities)
           (com.alee.laf WebLookAndFeel)))

(defn debug-print
  [x]
  `(let [res# ~x]
     ;(println "?=" (quote ~x))
     (println "?=" res#)
     res#))

(defn init
  []
  (config/init))

(defn -main [& args]
  (init)
  (SwingUtilities/invokeLater
    #(let [max  (count args)
           mode (if (= 0 max) nil ((vec args) 0))
           ex   (atom nil)]
       (reset! *frame* (frame/make-frame mode))
       (. @*frame* setVisible true))))

