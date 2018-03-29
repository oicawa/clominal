(ns settings
  (:require [clominal.keys :as keys]
            [clominal.frame :as frame]
            [clominal.editors.editor :as editor]
            [clominal.editors.search :as search]
            ))

;;------------------------------
;; Custom key binds
;;------------------------------
; Editor (Multi Line)
(keys/defkeybinds [clominal.editors.editor/multi-line-maps]
  (Ctrl h) clominal.editors.editor/backward-char
  (Ctrl j) clominal.editors.editor/next-line
  (Ctrl k) clominal.editors.editor/previous-line
  (Ctrl l) clominal.editors.editor/forward-char

  (Alt h) clominal.editors.editor/backward-word
  (Alt l) clominal.editors.editor/forward-word
  (Alt Shift h) clominal.editors.editor/begin-line
  (Alt Shift l) clominal.editors.editor/end-line

  (Alt j) clominal.editors.editor/next-page
  (Alt k) clominal.editors.editor/previous-page
  (Alt Shift j) clominal.editors.editor/end-buffer
  (Alt Shift k) clominal.editors.editor/begin-buffer

  (Ctrl b) clominal.editors.editor/delete-previous-char
  (Ctrl d) clominal.editors.editor/delete-next-char

  (Ctrl c) clominal.editors.editor/copy
  (Ctrl v) clominal.editors.editor/paste
  (Ctrl x) clominal.editors.editor/cut
  (Ctrl s) clominal.editors.editor/file-save
  (Ctrl w) clominal.editors.editor/close
  (Ctrl a) clominal.editors.editor/selectAll
  (Ctrl z) clominal.editors.editor/undo
  (Ctrl y) clominal.editors.editor/redo
  (Ctrl p) clominal.editors.editor/mark
  (Ctrl e) clominal.editors.editor/escape
  (Ctrl g) clominal.editors.editor/goto-line
  (Ctrl f) clominal.editors.search/show-as-find
  (Ctrl r) clominal.editors.search/show-as-replace
   
  ;(Alt i) clominal.editors.editor/show-component-stack
  (Alt i) clominal.editors.editor/show-system-encoding

  F3         clominal.editors.search/find-next
  (Shift F3) clominal.editors.search/find-prev)

; Text Field (Single Line)
(keys/defkeybinds [clominal.editors.editor/single-line-maps]
  (Ctrl h) clominal.editors.editor/backward-char
  (Ctrl l) clominal.editors.editor/forward-char

  (Alt h) clominal.editors.editor/backward-word
  (Alt l) clominal.editors.editor/forward-word
  (Alt Shift h) clominal.editors.editor/begin-line
  (Alt Shift l) clominal.editors.editor/end-line

  (Ctrl b) clominal.editors.editor/delete-previous-char
  (Ctrl d) clominal.editors.editor/delete-next-char

  (Ctrl c) clominal.editors.editor/copy
  (Ctrl v) clominal.editors.editor/paste
  (Ctrl x) clominal.editors.editor/cut
  (Ctrl a) clominal.editors.editor/selectAll
  (Ctrl z) clominal.editors.editor/undo
  (Ctrl y) clominal.editors.editor/redo
  (Ctrl p) clominal.editors.editor/mark
  F3             clominal.editors.search/find-next-in-search-panel
  (Shift F3)     clominal.editors.search/find-prev-in-search-panel
  Return         clominal.editors.search/find-operate
  (Ctrl e)       clominal.editors.search/hide
  Esc            clominal.editors.search/hide)

; Frame
(keys/defkeybinds [clominal.frame/maps]
  (Ctrl t) clominal.frame/show-tools
  (Ctrl n) clominal.editors.editor/file-new
  (Ctrl o) clominal.editors.editor/file-open
  (Ctrl Shift l) clominal.frame/load-module
  )

(def ^{:dynamic true} *config* (atom nil))
