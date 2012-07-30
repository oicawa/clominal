(ns clominal.editors.TextEditor
  (:import (java.awt Font GraphicsEnvironment GridBagLayout)
           (javax.swing JTextPane JPanel JTextField))
  (:require [clominal.utils.guiutils :as guiutils])
  (:gen-class
   :extends javax.swing.JTextPane
   :init init
   ;:post-init after-ctor
   :state state
   :methods [[getModeLine [] javax.swing.JPanel]
             [setModeLine [javax.swing.JPanel] void]
             [getMiniBuffer [] javax.swing.JPanel]
             [setMiniBuffer [javax.swing.JPanel] void]]))


(defn -init []
  [[]
   (ref {:mode-line   (ref nil)
         :mini-buffer (ref nil)})])

(defn -getModeLine
  [this]
  (let [{:keys [mode-line]} @(.state this)]
    @mode-line))

(defn -setModeLine
  [this mode-line-panel]
  (let [{:keys [mode-line]} @(.state this)]
    (dosync (ref-set mode-line mode-line-panel))))

(defn -getMiniBuffer
  [this]
  (let [{:keys [mini-buffer]} @(.state this)]
    @mini-buffer))

(defn -setMiniBuffer
  [this mini-buffer-panel]
  (let [{:keys [mini-buffer]} @(.state this)]
    (dosync (ref-set mini-buffer mini-buffer-panel))))

