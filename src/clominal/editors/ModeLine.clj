(ns clominal.editors.ModeLine
  (:import (java.awt Font Color GraphicsEnvironment GridBagLayout)
           (javax.swing JPanel JTextPane JTextField JLabel)
           (javax.swing.border LineBorder))
  (:require [clominal.utils.guiutils :as guiutils])
  (:gen-class
   :extends javax.swing.JPanel
   :init init
   :post-init after-ctor
   :state state
   :methods [[getTextEditor [] javax.swing.JTextPane]
             [setTextEditor [javax.swing.JTextPane] void]
             [getMiniBuffer [] javax.swing.JPanel]
             [setMiniBuffer [javax.swing.JPanel] void]
             [getFileNameLabel [] javax.swing.JLabel]]))


(defn -init []
  [[]
   (ref {:text-editor (ref nil)
         :mini-buffer (ref nil)
         :char-code   (JLabel. "--")
         :separator   (JLabel. ":")
         :modified?   (JLabel. "--")
         :file-name   (JLabel. "Dummy.txt")
         :cursor      (JLabel. "[L12, C34]")
         :filler      (JLabel. "")})])

(defn -getTextEditor
  [this]
  (let [{:keys [text-editor]} @(.state this)]
    @text-editor))

(defn -setTextEditor
  [this text-editor-panel]
  (let [{:keys [text-editor]} @(.state this)]
    (dosync (ref-set text-editor text-editor-panel))))

(defn -getMiniBuffer
  [this]
  (let [{:keys [mini-buffer]} @(.state this)]
    @mini-buffer))

(defn -setMiniBuffer
  [this mini-buffer-panel]
  (let [{:keys [mini-buffer]} @(.state this)]
    (dosync (ref-set mini-buffer mini-buffer-panel))))

(defn -setFont
  [this font]
  (println "Called ModeLine/setFont.")
  (if (= nil (.state this))
      nil
      (let [{:keys [char-code
                    separator
                    modified?
                    file-name
                    cursor
                    filler]} @(.state this)]
        (. char-code setFont font)
        (. separator setFont font)
        (. modified? setFont font)
        (. file-name setFont font)
        (. cursor setFont font)
        (. filler setFont font)
        ; (let [height (.. input-line getPreferredSize getHeight)
        ;       width  (.. prompt getPreferredSize getWidth)]
        ;   (. filler setPreferredSize (java.awt.Dimension. width height)))
        (println "Completed ModeLine/setFont.")
         )))

(defn -getFileNameLabel
  [this]
  (let [{:keys [file-name]} @(.state this)]
    file-name))

(defn -after-ctor
  [this]
  (let [{:keys [char-code
                separator
                modified?
                file-name
                cursor
                filler]} @(.state this)]
    (doseq [cmp [char-code separator modified? file-name cursor filler]]
      (println (. cmp getText)))
    (doto this
      ;(.setBorder (LineBorder. Color/GRAY 2))
      (.setPreferredSize nil)
      (.setLayout (GridBagLayout.))
      (guiutils/grid-bag-layout
        :gridx 0, :gridy 0
        :anchor :WEST
        char-code
        :gridx 1, :gridy 0
        separator
        :gridx 2, :gridy 0
        modified?
        :gridx 3, :gridy 0
        file-name
        :gridx 4, :gridy 0
        cursor
        :gridx 5, :gridy 0
        :fill :HORIZONTAL
        :weightx 1.0
        filler
        ))
  ))

