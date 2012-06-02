(ns clominal.keys.keymap
  (:use [clojure.contrib.def])
  (:require [clominal.action :as action])
  (:import (javax.swing InputMap ActionMap JComponent KeyStroke SwingUtilities)
           (java.awt.event InputEvent KeyEvent)
           (clominal.keys LastKeyAction)))

(defn- get-maps
  "
  This function returns a vector constructed InputMap and ActionMap object from clojure map object.
  If not found from the specified maps, it creates a new InputMap/ActionMap object, and add to maps.

  maps:    The target InputMap/ActionMap map object.
  name:    The name of InputMap/ActionMap object.
  return:  [inputmap actionmap]"
  [maps name]
  (let [map-vec (maps name)]
    (if (= nil map-vec)
        (let [map-vec [(InputMap.) (ActionMap.)]]
          (def maps (assoc maps name map-vec))
          map-vec)
        map-vec)))

(defn create-middle-keystroke-action
  "
  This function creates an action for the specified middle keystroke.
  A middle keystroke action must assign the preper InputMap and ActionMap to current editor.
  
  maps:   A InputMap/ActionMap maps of the target component.
  name:   Key bind name.
  return: nil
  "
  [maps name]
  (let [target    (get-maps maps name)
        inputmap  (target 0)
        actionmap (target 1)]
    (action/create #((doto %1
                       (.setInputMap  JComponent/WHEN_FOCUSED inputmap)
                       (.setActionMap actionmap))))))

(defn create-operation
  "
  This function create clojure map object as operation.

  maps:                  Clojure map. Key is a name, and Value is a vector constructed InputMap object and ActionMap object.
  action:                Action object.
  return:                Operation object. (clojure map)
  "
  [maps action]
  {:maps                  maps
   :action                action})

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
  This function get keystroke symbol or those list,
  and create KeyStroke object.
  
  key-bind: Keystroke symbol, or keystroke symbols list. [ex: '(Ctrl F)]
  return:   KeyStroke object, or KeyStroke objects list. [ex: #<KeyStroke ctrl pressed F>]
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
  (let [maps              (operation :maps)
        action            (operation :action)
        default-maps      (maps "default")
        default-inputmap  (default-maps 0)
        default-actionmap (default-maps 1)
        all-strokes       (get-key-strokes key-bind)]
    (if (seq? all-strokes)
        (loop [inputmap    default-inputmap
               actionmap   default-actionmap
               stroke      (first all-strokes)
               strokes     (rest all-strokes)]
          (let [stroke-name (str stroke)]
            (if (= nil (first strokes))
                (do
                  (. inputmap  put stroke stroke-name)
                  (. actionmap put stroke-name (LastKeyAction. action default-inputmap default-actionmap)))
                (let [map-vec       (get-maps maps stroke-name)
                      new-inputmap  (map-vec 0)
                      new-actionmap (map-vec 1)
                      middle-action (create-middle-keystroke-action maps stroke-name)]
                  (. inputmap  put stroke stroke-name)
                  (. actionmap put stroke-name middle-action)
                  (recur new-inputmap new-actionmap (first strokes) (rest strokes))))))
        (do
          (. default-inputmap  put all-strokes (str all-strokes))
          (. default-actionmap put (str all-strokes) action)))))
