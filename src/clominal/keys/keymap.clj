(ns clominal.keys.keymap
  (:use [clojure.contrib.def])
  (:require [clominal.action :as action]
            [clominal.utils.env :as env])
  (:import (javax.swing InputMap ActionMap JComponent KeyStroke SwingUtilities)
           (java.awt Toolkit)
           (java.awt.event InputEvent KeyEvent)))

(defn get-maps
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
  (println "    keys:")
  (doseq [key (. inputmap keys)]
    (println "      " (str key)))
  (println "  actionmap   = " actionmap)
  (println "    keys:")
  (doseq [key (. actionmap keys)]
    (println "      " (str key))))


(def windows-composition-enabled? (ref nil))

; (defmacro enable-inputmethod
;   [component flag]
;   (if (env/windows?)
;   	  (let [icontext (gensym "icontext")
;             current  (gensym "current")
;             enabled? (gensym "enabled?")]
;         `(let [~icontext (. ~component getInputContext)
;                ~current  (. ~icontext isCompositionEnabled)]
;           (if ~flag
;             (. ~icontext (setCompositionEnabled @windows-composition-enabled?))
;             (do
;               (dosync (ref-set windows-composition-enabled? ~current))
;               (. ~icontext (setCompositionEnabled false))))))
;       `(. ~component enableInputMethods ~flag)))
(defn enable-inputmethod
  [component flag]
  ;(println (format "enableInputMethods(%s)" flag))
  (. component enableInputMethods flag))

        


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
   'Alt   InputEvent/ALT_DOWN_MASK})

(def normal-keys
  {\a KeyEvent/VK_A
   \b KeyEvent/VK_B
   \c KeyEvent/VK_C
   \d KeyEvent/VK_D
   \e KeyEvent/VK_E
   \f KeyEvent/VK_F
   \g KeyEvent/VK_G
   \h KeyEvent/VK_H
   \i KeyEvent/VK_I
   \j KeyEvent/VK_J
   \k KeyEvent/VK_K
   \l KeyEvent/VK_L
   \m KeyEvent/VK_M
   \n KeyEvent/VK_N
   \o KeyEvent/VK_O
   \p KeyEvent/VK_P
   \q KeyEvent/VK_Q
   \r KeyEvent/VK_R
   \s KeyEvent/VK_S
   \t KeyEvent/VK_T
   \u KeyEvent/VK_U
   \v KeyEvent/VK_V
   \w KeyEvent/VK_W
   \x KeyEvent/VK_X
   \y KeyEvent/VK_Y
   \z KeyEvent/VK_Z
   \; KeyEvent/VK_SEMICOLON
   \: KeyEvent/VK_COLON})

(defn get-key-stroke
  "
  This function get keystroke symbol or keystroke symbol list,
  and create KeyStroke object.
  
  key-bind : Keystroke char, or keystroke symbols and char list. [ex: '(Ctrl \f)]
  return   : KeyStroke object, or KeyStroke objects list. [ex: #<KeyStroke ctrl pressed F>]
  "
  [key-bind]
  (loop [body     key-bind
         last-key nil
         mask     0]
    (cond (char? body) (KeyStroke/getKeyStroke body)
          (= '() body) (KeyStroke/getKeyStroke (normal-keys last-key) mask)
          true         (recur (rest body)
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

(defn str-keystroke
  [keystroke]
  (let [mod (KeyEvent/getKeyModifiersText (. keystroke getModifiers))
        key (KeyEvent/getKeyText (. keystroke getKeyCode))]
    (str mod "+" key)))

(defn print-keystroke
  [keystroke]
  (let [mod (. keystroke getModifiers)
        key (. keystroke getKeyCode)]
    (println "----------")
    (println "Ctrl =" InputEvent/CTRL_DOWN_MASK)
    (println "Alt  =" InputEvent/ALT_DOWN_MASK)
    (println "Modifiers Value =" mod)
    (println "Modifiers Text  =" (KeyEvent/getKeyModifiersText mod))
    (println "KeyCode         =" key)
    (println "KeyText         =" (KeyEvent/getKeyText key))))

