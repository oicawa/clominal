(ns clominal.gui.editor
  (:use [clojure.contrib.def])
  (:require [clominal.action :as action])
  (:import (javax.swing JComponent JTextPane JScrollPane KeyStroke AbstractAction SwingUtilities)
           (javax.swing.text DefaultEditorKit JTextComponent)
           (java.awt.event InputEvent KeyEvent)))

; キーマップは静的なマップが取得できるが、
; アクションマップは静的なマップが取れるのか？
(defvar *editor-key-map* (JTextComponent/getKeymap JTextComponent/DEFAULT_KEYMAP))
(defvar *editor-action-map* (SwingUtilities/getUIActionMap (JTextPane.)))

(defvar *forward-char*  (. *editor-action-map* get DefaultEditorKit/forwardAction))
(defvar *backward-char* (. *editor-action-map* get DefaultEditorKit/backwardAction))

(defvar *forward-char-key*  (KeyStroke/getKeyStroke KeyEvent/VK_F InputEvent/CTRL_DOWN_MASK))
(defvar *backward-char-key* (KeyStroke/getKeyStroke KeyEvent/VK_B InputEvent/CTRL_DOWN_MASK))

(def new-title "新規テキスト")

(defn create
  "Create editor pane."
  []
  (JScrollPane. (JTextPane.) JScrollPane/VERTICAL_SCROLLBAR_ALWAYS JScrollPane/HORIZONTAL_SCROLLBAR_ALWAYS))

(. *editor-key-map* addActionForKeyStroke *forward-char-key* *forward-char*)
(. *editor-key-map* addActionForKeyStroke *backward-char-key* *backward-char*)

