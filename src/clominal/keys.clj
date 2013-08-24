(ns clominal.keys
  (:use [clojure.contrib.def]
        [clominal.utils])
  (:import (javax.swing AbstractAction InputMap ActionMap JComponent KeyStroke SwingUtilities ComponentInputMap)
           (java.awt Toolkit)
           (java.awt.event InputEvent KeyEvent)
           (java.util HashMap)))

(definterface IKeybindComponent
  (setImeEnable [value])
  (setInputMap [inputmap])
  (setActionMap [actionmap])
  (setKeyStroke [keystroke])
  (getKeyMaps []))

(definterface IKeyMaps
  (get [stroke-name]))

(definterface IKeyMap
  (getInputMap [])
  (getActionMap [])
  (put [stroke-name stroke key-action]))

(defn make-keymap
  [iptmap actmap]
  (proxy [IKeyMap] []
     (getInputMap [] iptmap)
     (getActionMap [] actmap)
     (put [stroke-name stroke key-action]
       (. iptmap put stroke stroke-name)
       (. actmap put stroke-name key-action))))
(defn make-keymap-with-component
  [component mode]
  (let [iptmap (if (= mode JComponent/WHEN_IN_FOCUSED_WINDOW)
                   (ComponentInputMap. component)
                   (InputMap.))
        actmap (ActionMap.)]
    (make-keymap iptmap actmap)))

(defn make-keymaps
  [component mode]
  (let [hash (HashMap.)
        maps (proxy [IKeyMaps] []
               (get [stroke-name]
                 (if (not (. hash containsKey stroke-name))
                     (. hash put stroke-name (make-keymap-with-component component mode)))
                 (. hash get stroke-name)))]
    (let [default-iptmap (. component getInputMap mode)
          default-actmap (. component getActionMap)]
      (. hash put "default" (make-keymap default-iptmap default-actmap)))
    maps))

(defn make-key-action
  [act keymap]
  (let [is-last? (not (instance? KeyStroke act))]
    (proxy [AbstractAction] []
      (actionPerformed [evt]
        (let [component (. evt getSource)]
          (doto component
            (.setImeEnable is-last?)
            (.setInputMap (. keymap getInputMap))
            (.setActionMap (. keymap getActionMap)))
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
  {'a       KeyEvent/VK_A
   'b       KeyEvent/VK_B
   'c       KeyEvent/VK_C
   'd       KeyEvent/VK_D
   'e       KeyEvent/VK_E
   'f       KeyEvent/VK_F
   'g       KeyEvent/VK_G
   'h       KeyEvent/VK_H
   'i       KeyEvent/VK_I
   'j       KeyEvent/VK_J
   'k       KeyEvent/VK_K
   'l       KeyEvent/VK_L
   'm       KeyEvent/VK_M
   'n       KeyEvent/VK_N
   'o       KeyEvent/VK_O
   'p       KeyEvent/VK_P
   'q       KeyEvent/VK_Q
   'r       KeyEvent/VK_R
   's       KeyEvent/VK_S
   't       KeyEvent/VK_T
   'u       KeyEvent/VK_U
   'v       KeyEvent/VK_V
   'w       KeyEvent/VK_W
   'x       KeyEvent/VK_X
   'y       KeyEvent/VK_Y
   'z       KeyEvent/VK_Z
   'Return  KeyEvent/VK_ENTER
   'Esc     KeyEvent/VK_ESCAPE
   'F1      KeyEvent/VK_F1
   'F2      KeyEvent/VK_F2
   'F3      KeyEvent/VK_F3
   'F4      KeyEvent/VK_F4
   'F5      KeyEvent/VK_F5
   'F6      KeyEvent/VK_F6
   'F7      KeyEvent/VK_F7
   'F8      KeyEvent/VK_F8
   'F9      KeyEvent/VK_F9
   'F10     KeyEvent/VK_F10
   'F11     KeyEvent/VK_F11
   'F12     KeyEvent/VK_F12
   })

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
    (cond (= '() body)
            (KeyStroke/getKeyStroke (normal-keys last-key) mask)
          (list? body)
            (recur (rest body)
                   (first body)
                   (if (= nil last-key)
                       mask
                       (+ mask (mask-keys last-key))))
          :else
            (KeyStroke/getKeyStroke body))))

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
  (let [default-map (. maps get "default")
        all-strokes (get-key-strokes key-bind)]
    (if (seq? all-strokes)
        (loop [this-map default-map
               stroke   (first all-strokes)
               strokes  (rest all-strokes)]
          (let [stroke-name (str stroke)]
            (if (= nil (first strokes))
                (. this-map put stroke-name stroke (make-key-action operation default-map))
                (let [next-map (. maps get stroke-name)]
                  (. this-map  put stroke-name stroke (make-key-action stroke next-map))
                  (recur next-map (first strokes) (rest strokes))))))
        (. default-map put (str all-strokes) all-strokes (make-key-action operation default-map)))))
