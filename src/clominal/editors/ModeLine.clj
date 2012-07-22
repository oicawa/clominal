(ns clominal.editors.ModeLine
  (:import (java.awt Font GraphicsEnvironment GridBagLayout)
           (javax.swing JPanel JTextPane JTextField))
  (:require [clominal.utils.guiutils :as guiutils])
  (:gen-class
   :extends javax.swing.JPanel
   :init init
   ;:post-init after-ctor
   :state state
   :methods [[getTextEditor [] javax.swing.JTextPane]
             [setTextEditor [javax.swing.JTextPane] void]
             [getMiniBuffer [] javax.swing.JTextField]
             [setMiniBuffer [javax.swing.JTextField] void]]))


(defn -init []
  [[]
   (ref {:text-editor (ref nil)
         :mini-buffer (ref nil)})])

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
  [this mini-buffer-text]
  (let [{:keys [mini-buffer]} @(.state this)]
    (dosync (ref-set mini-buffer mini-buffer-text))))

