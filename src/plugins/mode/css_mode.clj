(ns plugins.mode.css_mode
  (:import (org.fife.ui.rsyntaxtextarea SyntaxConstants))
  (:require [clominal.editors.basic_mode :as basic_mode]))

(def mode-name "css-mode")

;; ------------------------------
;; Initialize Mode
;; ------------------------------

(defn init-mode
  [text-pane]
  (basic_mode/init-mode text-pane mode-name SyntaxConstants/SYNTAX_STYLE_CSS))
