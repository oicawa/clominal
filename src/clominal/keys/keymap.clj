(ns clominal.keys.keymap
  (:use [clojure.contrib.def])
  (:require [clominal.action :as action])
  (:import (javax.swing InputMap ActionMap JComponent KeyStroke SwingUtilities)
           (java.awt.event InputEvent KeyEvent)
           (clominal.keys LastKeyAction)))

(defn- get-maps
  "
  This function returns a vector constructed InputMap and ActionMap object from ref-map.
  If not found from the specified maps vector, it creates a new InputMap/ActionMap object, and add to ref-maps.

  ref-maps : A 'ref' map object (key:key bind name, value:InputMap and ActionMap vector) of the target component.
  name     : The key bind name.
  return   : [inputmap actionmap]
  "
  [ref-maps name]
  (let [map-vec (@ref-maps name)]
    (if (= nil map-vec)
        (let [map-vec [(InputMap.) (ActionMap.)]]
          (dosync (alter ref-maps assoc name map-vec))
          map-vec)
        map-vec)))


(defn- print-maps
  "
  This function is for debugging.
  "
  [stroke-name inputmap actionmap]
  (println "  stroke name = " stroke-name)
  (println "  inputmap    = " inputmap)
  (println "    keys      = " (map str (. inputmap keys)))
  (println "  actionmap   = " actionmap)
  (println "    keys      = " (map str (. actionmap keys))))


(defn create-middle-keystroke-action
  "
  This function creates an action for the specified middle keystroke.
  The created action must assign the appropriate InputMap object and ActionMap object to current editor.
  
  ref-maps : A 'ref' map object (key:key bind name, value:InputMap/ActionMap vector) of the target component.
  name     : Key bind name.
  return: nil
  "
  [ref-maps name]
  (let [map-vec   (get-maps ref-maps name)
        inputmap  (map-vec 0)
        actionmap (map-vec 1)]
    (action/create #(let [editor %1]
                      ; (println "----------")
                      ; (println "Do middle key stroke action.")
                      ; (print-maps name inputmap actionmap)
                      (doto editor
                        (.setInputMap  JComponent/WHEN_FOCUSED inputmap)
                        (.setActionMap actionmap))))))

(defn create-operation
  "
  This function create clojure map object as operation.

  ref-maps : A 'ref' map object (key:key bind name, value:InputMap/ActionMap vector) of the target component.
  action   : A Action object.
  return   : A operation object. (clojure map)
  "
  [ref-maps action]
  {:ref-maps ref-maps
   :action   action})

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

(defn get-key-stroke
  "
  This function get keystroke symbol or keystroke symbol list,
  and create KeyStroke object.
  
  key-bind : Keystroke symbol, or keystroke symbols list. [ex: '(Ctrl F)]
  return   : KeyStroke object, or KeyStroke objects list. [ex: #<KeyStroke ctrl pressed F>]
  "
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
  
(defn get-key-strokes
  "
  This function get symbol or symbol list as key-binds,
  and create KeyStroke object or those list.
  
  key-binds: Keystroke symbol, or keystroke symbols list. [ex: '((Ctrl X) (Ctrl F))]
  return:    KeyStroke object, or KeyStroke objects list. [ex: '(#<KeyStroke ctrl pressed F> #<KeyStroke ctrl pressed X>)]
  "
  [key-binds]
  (cond (symbol? key-binds) (KeyStroke/getKeyStroke (normal-keys key-binds) 0)
        (list? (first key-binds)) (map get-key-stroke key-binds)
        true (get-key-stroke key-binds)))


(defn def-key-bind
  "Define key bind for specified operation for one stroke or more."
  [key-bind operation]
  (let [ref-maps          (operation :ref-maps)
        action            (operation :action)
        default-maps      (@ref-maps "default")
        default-inputmap  (default-maps 0)
        default-actionmap (default-maps 1)
        all-strokes       (get-key-strokes key-bind)]
    ; (println "----------")
    ; (println "all-strokes = " all-strokes)
    (if (seq? all-strokes)
        (loop [inputmap    default-inputmap
               actionmap   default-actionmap
               stroke      (first all-strokes)
               strokes     (rest all-strokes)]
          (let [stroke-name (str stroke)]
            (if (= nil (first strokes))
                (do
                  (. inputmap  put stroke stroke-name)
                  (. actionmap put stroke-name (LastKeyAction. action default-inputmap default-actionmap))
                  ; (println "Regist last key stroke action.")
                  ; (print-maps stroke-name inputmap actionmap)
                  )
                (let [map-vec        (get-maps ref-maps stroke-name)
                      next-inputmap  (map-vec 0)
                      next-actionmap (map-vec 1)
                      middle-action  (create-middle-keystroke-action ref-maps stroke-name)]
                  (. inputmap  put stroke stroke-name)
                  (. actionmap put stroke-name middle-action)
                  ; (println "Regist middle key stroke action.")
                  ; (print-maps stroke-name inputmap actionmap)
                  (recur next-inputmap next-actionmap (first strokes) (rest strokes))))))
        (do
          (. default-inputmap  put all-strokes (str all-strokes))
          (. default-actionmap put (str all-strokes) action)
          ; (println "Regist Single key stroke action.")
          ; (print-maps (str all-strokes) default-inputmap default-actionmap)
          ))))
