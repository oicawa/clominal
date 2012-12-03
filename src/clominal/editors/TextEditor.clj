(ns clominal.editors.TextEditor
  (:import (java.awt Font GraphicsEnvironment GridBagLayout)
           (java.awt.event ActionEvent)
           (java.awt.im InputMethodRequests)
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
   :exposes-methods {getInputMethodRequests superGetInputMethodRequests}
   :methods [[getModeLine [] javax.swing.JPanel]
             [setModeLine [javax.swing.JPanel] void]
             [getMiniBuffer [] javax.swing.JPanel]
             [setMiniBuffer [javax.swing.JPanel] void]
             [currentPath [] String]
             [currentPath [String] void]
             [save [String] boolean]
             [openFile [String] boolean]
             [saveFile [] boolean]
             [saveAsFile [java.awt.event.ActionEvent] void]
             ;[getCustomInputMethodRequests [] java.awt.im.InputMethodRequests]
             ;[superGetInputMethodRequests [] java.awt.im.InputMethodRequests]
             ]))


(defn -init []
  [[]
   (ref {:mode-line   (ref nil)
         :mini-buffer (ref nil)
         :path        (ref nil)
         :im-requests (ref nil)})])

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
                       (str "~" env/os-file-separator)
                       (fn [evt mini-buffer text-editor]
                         (let [path (. mini-buffer text)]
                           (println "Save as:" path)
                           (. text-editor currentPath path)
                           (. text-editor saveFile))))]
    (. saveAsAction actionPerformed evt)))

(defn -openFile
  [this file-path]
  (let [{:keys [path]} @(.state this)
        doc    (. this getDocument)
        editor (. this getEditorKit)
        file   (File. file-path)]
    (do
      (. this currentPath file-path)
      (try
        (with-open [stream (FileInputStream. (. this currentPath))]
          (do
            (. this read stream doc)
            (.. this getModeLine getFileNameLabel (setText (. file getName)))
            true))
        (catch FileNotFoundException _ true)
        (catch Exception e
          (. e printStackTrace)
          (. JOptionPane (showMessageDialog nil (. e getMessage) "Error..." JOptionPane/OK_OPTION)))))))
  
(defn -getInputMethodRequests
  [this]
  (let [{:keys [im-requests]} @(.state this)
        original-imr (.superGetInputMethodRequests this)
        editor this]
    (if (not (= nil @im-requests))
        @im-requests
        (let [improved-imr (proxy [InputMethodRequests] []
                             (cancelLatestCommittedText [attributes]
                               (println "called improved-imr's 'cancelLatestCommittedText' method.")
                               (. original-imr cancelLatestCommittedText attributes))
                             (getCommittedText [beginIndex endIndex attributes]
                               (println "called improved-imr's 'getCommittedText' method.")
                               (. original-imr getCommittedText beginIndex endIndex attributes))
                             (getCommittedTextLength []
                               (println "called improved-imr's 'getCommittedTextLength' method.")
                               (. original-imr getCommittedTextLength))
                             (getInsertPositionOffset []
                               (println "called improved-imr's 'getInsertPositionOffset' method.")
                               (. original-imr getInsertPositionOffset))
                             (getLocationOffset [x y]
                               (println "called improved-imr's 'getLocationOffset' method.")
                               (. original-imr getLocationOffset x y))
                             (getSelectedText [attributes]
                               (println "called improved-imr's 'getSelectedText' method.")
                               (. original-imr getSelectedText attributes))
                             (getTextLocation [offset]
                               (let [rect     (. original-imr getTextLocation offset)
                                     position (.. editor getCaret getMagicCaretPosition)]
                                 (println "X:" (. position getX) "/ Y:" (. position getY))
                                 (. rect setLocation (. position getX) (. position getY))
                                 rect)))
              ]
          (dosync (ref-set im-requests improved-imr))
          improved-imr))))
