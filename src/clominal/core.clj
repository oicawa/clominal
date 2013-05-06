(System/setProperty "awt.useSystemAAFontSettings" "on")
(javax.swing.UIManager/setLookAndFeel "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel")

(ns clominal.core
  (:require [clominal.frame :as frame])
  (:use     [clominal.utils])
  (:import (javax.swing SwingUtilities)))

(defn debug-print
  [x]
  `(let [res# ~x]
     (println "?=" res#)
     res#))

(defn main [& args]
  (SwingUtilities/invokeLater
    #(let [max  (count args)
           mode (if (= 0 max) nil ((vec args) 0))
           ex   (atom nil)]
       (reset! *frame* (frame/make-frame mode))
       (. @*frame* setVisible true)
       ; (try
       ;   (println "[ START ] Load settings...")
       ;   (require 'settings)
       ;   (println "[  END  ] Load settings")
       ;   (catch Exception e
       ;     (reset! ex e)
       ;     (. e printStackTrace)))
       ; (if (not (nil? @ex))
       ;     (do 
       ;       (println "<< FAILED1 >>" @ex)
       ;       ; (doseq [stack (vec (. ex getStackTrace))]
       ;       ;   (println (type stack)))
       ;       (println "<< FAILED2 >>")
       ;         ))

          )))

(main "d")