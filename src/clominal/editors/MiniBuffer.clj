(ns clominal.editors.MiniBuffer
  (:import (java.awt Font Color GraphicsEnvironment GridBagLayout)
           (javax.swing JPanel JLabel JTextField SwingConstants)
           (javax.swing.border LineBorder))
  (:require [clominal.utils.guiutils :as guiutils])
  (:gen-class
   :extends javax.swing.JPanel
   :init init
   :post-init after-ctor
   :state state
   :methods [[getTextEditor [] javax.swing.JTextPane]
             [setTextEditor [javax.swing.JTextPane] void]
             [getModeLine [] javax.swing.JPanel]
             [setModeLine [javax.swing.JPanel] void]
             [prompt [String] void]
             [prompt [] String]
             [text [String] void]
             [text [] String]
             [enable [Boolean] void]
             [enable [] Boolean]
             [setBackgroundColor [java.awt.Color] void]
             ]))


(defn -init []
  [[]
   (ref {:text-editor (ref nil)
         :mode-line   (ref nil)
         :prompt      (JLabel.)
         :input-line  (JTextField.)
         :filler      (JLabel. " ")})])

(defn -getTextEditor
  [this]
  (let [{:keys [text-editor]} @(.state this)]
    @text-editor))

(defn -setTextEditor
  [this text-editor-pane]
  (let [{:keys [text-editor]} @(.state this)]
    (dosync (ref-set text-editor text-editor-pane))))

(defn -getModeLine
  [this]
  (let [{:keys [mode-line]} @(.state this)]
    @mode-line))

(defn -setModeLine
  [this mode-line-panel]
  (let [{:keys [mode-line]} @(.state this)]
    (dosync (ref-set mode-line mode-line-panel))))

(defn -prompt
  ([this]
   (let [{:keys [prompt]} @(.state this)]
     (. prompt getText)))
  ([this text]
   (let [{:keys [prompt]} @(.state this)]
     (. prompt setText text))))

(defn -text
  ([this]
   (let [{:keys [input-line]} @(.state this)]
     (. input-line getText)))
  ([this value]
   (let [{:keys [input-line]} @(.state this)]
     (. input-line setText value))))

(defn -enable
  ([this]
   (let [{:keys [input-line filler]} @(.state this)]
     (and (. input-line getVisible)
          (not (. filler getVisible)))))
  ([this value]
   (let [{:keys [input-line filler]} @(.state this)]
     (doto filler
       (.setVisible (not value)))
     (doto input-line
       (.setVisible value)
       (.requestFocusInWindow))
     )))

(defn -setFont
  [this font]
  (if (= nil (.state this))
      nil
     (let [{:keys [prompt input-line filler]} @(.state this)]
       (. prompt setFont font)prompt
       (. input-line setFont font)
       (let [height (.. input-line getPreferredSize getHeight)
             width  (.. prompt getPreferredSize getWidth)]
         (. filler setPreferredSize (java.awt.Dimension. width height))))))

(defn -setBackgroundColor
  [this color]
  (if (= nil (.state this))
      nil
      (let [{:keys [prompt input-line filler]} @(.state this)]
        (. prompt setBackgroundColor color)
        (. input-line setBackgroundColor color)
        (. filler setBackgroundColor color))))

(defn -after-ctor
  [this]
  (let [{:keys [prompt input-line filler]} @(.state this)]
    (doto this
      (.setBorder (LineBorder. Color/GRAY 1))
      (.setPreferredSize nil)
      (.setBackground Color/WHITE)
      (.setLayout (GridBagLayout.))
      (guiutils/grid-bag-layout
        :gridx 0, :gridy 0
        :anchor :WEST
        prompt
        :gridx 1, :gridy 0
        :fill :HORIZONTAL
        :weightx 1.0
        input-line
        :gridx 2, :gridy 0
        :fill :HORIZONTAL
        :weightx 1.0
        filler
        ))
    (doto prompt
      (.setText "Prompt...")
      (.setHorizontalAlignment SwingConstants/LEFT))
    (doto input-line
      (.setVisible false))
    (doto filler
      (.setVisible true))
  ))