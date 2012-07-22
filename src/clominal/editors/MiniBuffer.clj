(ns clominal.editors.MiniBuffer
  (:import (java.awt Font GraphicsEnvironment GridBagLayout)
           (javax.swing JTextField JTextPane))
  (:require [clominal.utils.guiutils :as guiutils])
  (:gen-class
   :extends javax.swing.JTextField
   :init init
   ;:post-init after-ctor
   :state state
   :methods [[getTextEditor [] javax.swing.JTextPane]
             [setTextEditor [javax.swing.JTextPane] void]
             [getModeLine [] javax.swing.JPanel]
             [setModeLine [javax.swing.JPanel] void]]))


(defn -init []
  [[]
   (ref {:text-editor (ref nil)
         :mode-line   (ref nil)})])

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

