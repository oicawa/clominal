(ns clominal.editors.EditorPanel
  (:import (java.awt Font GraphicsEnvironment GridBagLayout)
           (javax.swing InputMap ActionMap JComponent JTextPane JScrollPane Action JLabel JTextField JPanel)
           (javax.swing.text DefaultEditorKit)
           ; (clominal.editors.StatusPanel)
           )
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
   :methods [[getStatusPanel [] javax.swing.JPanel]
             [getCommandLine [] javax.swing.JTextField]
             [getEditor [] javax.swing.JTextPane]]))


(defn -init []
  [[]
   (ref {:editor       (JTextPane.)
         :status-panel (JPanel.)
         :command-line (JTextField.)
        })])

(defn getEditor
  [this]
  (let [{:keys [editor status-panel command-line]} @(.state this)]
    editor))

(defn getStatusPanel
  [this]
  (let [{:keys [editor status-panel command-line]} @(.state this)]
    status-panel))

(defn getCommandLine
  [this]
  (let [{:keys [editor status-panel command-line]} @(.state this)]
    command-line))

(def default-fonts {:linux   ["Takaoゴシック" Font/PLAIN 16]
                    :windows ["ＭＳ ゴシック" Font/PLAIN 14]})

;;
;; Constractor.
;;
(defn -after-ctor
  [this]
  (let [{:keys [editor status-panel command-line]}
                          @(.state this)
        map-vec           (@editorutils/ref-maps "default")
        default-inputmap  (map-vec 0)
        default-actionmap (map-vec 1)
        scroll            (JScrollPane. editor JScrollPane/VERTICAL_SCROLLBAR_ALWAYS JScrollPane/HORIZONTAL_SCROLLBAR_ALWAYS)]

    (doto editor
      (.setInputMap  JComponent/WHEN_FOCUSED default-inputmap)
      (.setActionMap default-actionmap)
      (.enableInputMethods true))

    (doto command-line
      (.setText "コマンドライン")
      (.setName "command-line"))

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
        status-panel
        :gridy 2
        command-line))
    (apply editorutils/set-font editor (default-fonts (env/get-os-keyword)))
    ))
