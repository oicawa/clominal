(ns clominal.editors.TextEditor
  (:import (java.awt Font GraphicsEnvironment GridBagLayout)
           (java.awt.event ActionEvent)
           (javax.swing JTextPane JPanel JTextField JOptionPane)
           (java.io File FileInputStream FileWriter FileNotFoundException)
           (clominal.editors AskMiniBufferAction))
  (:require [clominal.utils.guiutils :as guiutils]
            [clominal.utils.env :as env])
  (:gen-class
   :extends javax.swing.JTextPane
   :init init
   ;:post-init after-ctor
   :state state
   :methods [[getModeLine [] javax.swing.JPanel]
             [setModeLine [javax.swing.JPanel] void]
             [getMiniBuffer [] javax.swing.JPanel]
             [setMiniBuffer [javax.swing.JPanel] void]
             [currentPath [] String]
             [currentPath [String] void]
             [save [String] boolean]
             [openFile [String] boolean]
             [saveFile [] boolean]
             [saveAsFile [java.awt.event.ActionEvent] void]]))


(defn -init []
  [[]
   (ref {:mode-line   (ref nil)
         :mini-buffer (ref nil)
         :path        (ref nil)})])

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

(defn -currentPath
  ([this]
    (let [{:keys [path]} @(.state this)]
      @path))
  ([this file-path]
    (let [{:keys [path]} @(.state this)
          full-path (env/get-absolute-path file-path)]
      (dosync (ref-set path full-path)))))

(defn -saveFile
  [this]
  (try
    (with-open [stream (FileWriter. (. this currentPath))]
      (do
        (. this write stream)
        true))
    (catch Exception e
      (. e printStackTrace)
      (. JOptionPane (showMessageDialog nil (. e getMessage) "Error..." JOptionPane/OK_OPTION))
      false)))

(defn -saveAsFile
  [this evt]
  (let [saveAsAction (AskMiniBufferAction. 
                       "Save as:"
                       "~/"
                       (fn [mini-buffer text-editor]
                         (let [path (. mini-buffer text)]
                           (. text-editor saveFile path))))]
    (. saveAsAction actionPerformed evt)))

(defn -openFile
  [this file-path]
  (let [{:keys [path]} @(.state this)
        doc    (. this getDocument)
        editor (. this getEditorKit)]
    (do
      (. this currentPath file-path)
      (try
        (with-open [stream (FileInputStream. (. this currentPath))]
          (do
            (. this read stream doc)
            true))
        (catch FileNotFoundException _ true)
        (catch Exception e
          (. e printStackTrace)
          (. JOptionPane (showMessageDialog nil (. e getMessage) "Error..." JOptionPane/OK_OPTION)))))))
  

