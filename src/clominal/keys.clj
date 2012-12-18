(ns clominal.keys
  (:use [clojure.contrib.def]
        [clominal.utils])
  (:import (javax.swing AbstractAction InputMap ActionMap JComponent KeyStroke SwingUtilities)
           (java.awt Toolkit)
           (java.awt.event InputEvent KeyEvent)))

(definterface IKeybindComponent
  (setImeEnable [value])
  (setInputMap [inputmap])
  (setActionMap [actionmap])
  (setKeyStroke [keystroke]))

(defn make-key-action
  [act inputmap actionmap]
  (let [is-last? (not (instance? KeyStroke act))]
    (proxy [AbstractAction] []
      (actionPerformed [evt]
        (let [component (. evt getSource)]
          (doto component
            (.setImeEnable is-last?)
            (.setInputMap inputmap)
            (.setActionMap actionmap))
          (if is-last?
              (do
                (. component setKeyStroke nil)
                (. act actionPerformed evt))
              (. component setKeyStroke act)))))))

(def mask-keys
  {'Ctrl  InputEvent/CTRL_DOWN_MASK
   'Alt   InputEvent/ALT_DOWN_MASK
   'Shift InputEvent/SHIFT_DOWN_MASK})

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


(defn define-keybind
  "Define key bind for specified operation for one stroke or more."
  [maps key-bind operation]
  (let [default-maps      (. maps get "default")
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
                  (. actionmap put stroke-name (make-key-action operation default-inputmap default-actionmap)))
                (let [map-vec        (let [m (. maps get stroke-name)]
                                       (if (= nil m)
                                           (do
                                             (. maps put stroke-name [(InputMap.) (ActionMap.)])
                                             (. maps get stroke-name))
                                           m))
                      next-inputmap  (map-vec 0)
                      next-actionmap (map-vec 1)
                      middle-action  (make-key-action stroke next-inputmap next-actionmap)]
                  (. inputmap  put stroke stroke-name)
                  (. actionmap put stroke-name middle-action)
                  (recur next-inputmap next-actionmap (first strokes) (rest strokes))))))
        (do
          (. default-inputmap  put all-strokes (str all-strokes))
          (. default-actionmap put (str all-strokes) (make-key-action operation default-inputmap default-actionmap))))))
