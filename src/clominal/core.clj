(compile 'clominal.editors.MiniBufferAction)
(compile 'clominal.editors.AskMiniBufferAction)
(compile 'clominal.editors.MiniBuffer)
(compile 'clominal.editors.ModeLine)
(compile 'clominal.editors.TextEditor)
(compile 'clominal.editors.MiddleKeyAction)
(compile 'clominal.editors.LastKeyAction)
(compile 'clominal.editors.EditorPanel)

(System/setProperty "awt.useSystemAAFontSettings" "on")
(javax.swing.UIManager/setLookAndFeel "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel")

(ns clominal.core
  (:require [clominal.action :as action]
            [clominal.keys.keymap :as keymap]
            [clominal.platforms.frame :as frame])
  (:import (javax.swing SwingUtilities))
  ;(:gen-class)
  )

(defn main [& args]
  (SwingUtilities/invokeLater
    #(let [max  (count args)
           mode (if (= 0 max) nil ((vec args) 0))]
       (frame/assign mode)
       (. frame/*frame* setVisible true))))

(main "d")