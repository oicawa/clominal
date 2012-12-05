(ns clominal.editors.EditorPanel
  (:import (java.awt Font GraphicsEnvironment GridBagLayout)
           (javax.swing InputMap ActionMap JComponent JTextPane JScrollPane Action JLabel JTextField JPanel)
           (javax.swing.text DefaultEditorKit)
           (clominal.editors TextEditor ModeLine ;MiniBuffer
            ))
  (:require [clominal.keys.keymap :as keymap]
            [clominal.action :as action]
            [clominal.utils.guiutils :as guiutils]
            [clominal.utils.env :as env]
            [clominal.editors.utils :as editorutils])
  (:use [clojure.contrib.def])
  (:gen-class
   :extends javax.swing.JPanel
   :init init
   :post-init after-ctor
   :state state
   :methods [[getTextEditor [] clominal.editors.TextEditor]
             [getModeLine [] clominal.editors.ModeLine]
             ;[getMiniBuffer [] clominal.editors.MiniBuffer]
             ]))


(defn -init []
  [[]
   (ref {:text-editor (TextEditor.)
         :mode-line   (ModeLine.)
         ;:mini-buffer (MiniBuffer.)
        })])

(defn -getTextEditor
  [this]
  (let [{:keys [text-editor]} @(.state this)]
    text-editor))

(defn -getModeLine
  [this]
  (let [{:keys [mode-line]} @(.state this)]
    mode-line))

; (defn -getMiniBuffer
;   [this]
;   (let [{:keys [mini-buffer]} @(.state this)]
;     mini-buffer))

(def default-fonts {:linux   ["Takaoゴシック" Font/PLAIN 14]
                    :windows ["ＭＳ ゴシック" Font/PLAIN 14]})

;;
;; Constractor.
;;
(defn -after-ctor
  [this]
  (let [{:keys [text-editor mode-line ;mini-buffer
                ]}
                          @(.state this)
        map-vec           (@editorutils/ref-maps "default")
        default-inputmap  (map-vec 0)
        default-actionmap (map-vec 1)
        scroll            (JScrollPane. text-editor JScrollPane/VERTICAL_SCROLLBAR_ALWAYS JScrollPane/HORIZONTAL_SCROLLBAR_ALWAYS)]

    (doto text-editor
      (.setName "text-editor")
      (.setInputMap  JComponent/WHEN_FOCUSED default-inputmap)
      (.setActionMap default-actionmap)
      (.enableInputMethods true)
      (.setModeLine mode-line)
      ;(.setMiniBuffer mini-buffer)
      )

    (doto mode-line
      (.setName "mode-line")
      (.setTextEditor text-editor)
      ;(.setMiniBuffer mini-buffer)
      )

    ; (doto mini-buffer
    ;   (.prompt "")
    ;   (.setName "mini-buffer")
    ;   (.setTextEditor text-editor)
    ;   (.setModeLine mode-line))

    (doto this
      (.setLayout (GridBagLayout.))
      (.setName "editor-panel")
      (guiutils/grid-bag-layout
        :fill :BOTH
        :gridx 0
        :gridy 0
        :weightx 1.0
        :weighty 1.0
        scroll
        :fill :HORIZONTAL
        :weightx 1.0
        :weighty 0.0
        :gridy 1
        mode-line
        ;:gridy 2
        ;mini-buffer
        ))
    (apply editorutils/set-font text-editor (default-fonts (env/get-os-keyword)))
    (apply editorutils/set-font mode-line   (default-fonts (env/get-os-keyword)))
    ;(apply editorutils/set-font mini-buffer (default-fonts (env/get-os-keyword)))
))
