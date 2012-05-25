(ns clominal.keymap
  (:use [clojure.contrib.def])
  (:import (javax.swing KeyStroke SwingUtilities)
           (java.awt.event InputEvent KeyEvent)))

(defn create-operation
  "Create operation."
  [keymap action]
  {:keymap keymap
   :action action})

; (import (javax.swing KeyStroke SwingUtilities)
;         (java.awt.event InputEvent KeyEvent))

(def mask-keys
  {'Ctrl  InputEvent/CTRL_DOWN_MASK
   'Alt   InputEvent/ALT_DOWN_MASK
   'Shift InputEvent/SHIFT_DOWN_MASK})

(def normal-keys
  {'A KeyEvent/VK_A
   'B KeyEvent/VK_B
   'C KeyEvent/VK_C
   'D KeyEvent/VK_D
   'E KeyEvent/VK_E
   'F KeyEvent/VK_F
   'G KeyEvent/VK_G
   'H KeyEvent/VK_H
   'I KeyEvent/VK_I
   'J KeyEvent/VK_J
   'K KeyEvent/VK_K
   'L KeyEvent/VK_L
   'M KeyEvent/VK_M
   'N KeyEvent/VK_N
   'O KeyEvent/VK_O
   'P KeyEvent/VK_P
   'Q KeyEvent/VK_Q
   'R KeyEvent/VK_R
   'S KeyEvent/VK_S
   'T KeyEvent/VK_T
   'U KeyEvent/VK_U
   'V KeyEvent/VK_V
   'W KeyEvent/VK_W
   'X KeyEvent/VK_X
   'Y KeyEvent/VK_Y
   'Z KeyEvent/VK_Z})

; ;Open file key bind.   
; (def key-bind-0 'F)
; (def key-bind-1 '(Ctrl Alt F))
; (def key-bind-2 '((Ctrl X) (Ctrl F)))

(defn- get-key-stroke
  [key-bind]
  (loop [body     key-bind
         last-key nil
         mask     0]
    (cond (symbol? body) (KeyStroke/getKeyStroke (normal-keys body) 0)
          (= '() body)   (KeyStroke/getKeyStroke (normal-keys last-key) mask)
          true           (recur (rest body)
                                (first body)
                                (if (= nil last-key)
                                    mask
                                    (+ mask (mask-keys last-key)))))))
  
(defn- get-key-strokes
  [key-binds]
  (cond (symbol? key-binds) (KeyStroke/getKeyStroke (normal-keys key-binds) 0)
        (list? (first key-binds)) (map get-key-stroke key-binds)
        true (get-key-stroke key-binds)))
  
(defn def-key-bind
  "Define key bind for specified operation."
  [key-bind operation]
  (let [keymap (:keymap operation)
        action (:action operation)
        keystroke (get-key-strokes key-bind)]
    (. keymap addActionForKeyStroke keystroke action)))
