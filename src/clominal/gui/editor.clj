(ns clominal.gui.editor
  (:use [clojure.contrib.def])
  (:require [clominal.keymap :as keymap])
  (:import (javax.swing JComponent JTextPane JScrollPane KeyStroke AbstractAction SwingUtilities)
           (javax.swing.text DefaultEditorKit JTextComponent)
           (java.awt.event InputEvent KeyEvent)))

; キーマップは静的なマップが取得できるが、
; アクションマップは静的なマップが取れるのか？
(defvar- key-map    (JTextComponent/getKeymap JTextComponent/DEFAULT_KEYMAP))
(defvar- action-map (SwingUtilities/getUIActionMap (JTextPane.)))

(defn- create-editor-operation
  "Create operation for editor."
  [name]
  (keymap/create-operation key-map (. action-map get name)))

;Editor actions
(defvar forward-char  (create-editor-operation DefaultEditorKit/forwardAction))
(defvar backward-char (create-editor-operation DefaultEditorKit/backwardAction))

(def new-title "新規テキスト")

(defn create
  "Create editor pane."
  []
  (JScrollPane. (JTextPane.) JScrollPane/VERTICAL_SCROLLBAR_ALWAYS JScrollPane/HORIZONTAL_SCROLLBAR_ALWAYS))


(keymap/def-key-bind '(Ctrl F) forward-char)
(keymap/def-key-bind '(Ctrl P) forward-char)
(keymap/def-key-bind '(Ctrl N) forward-char)
(keymap/def-key-bind '(Ctrl B) backward-char)


