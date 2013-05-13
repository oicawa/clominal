(ns settings
  (:require [clominal.keys :as keys]
            [clominal.frame :as frame]
            [clominal.editors.editor :as editor]
            [clominal.editors.search :as search]
            ))

;;------------------------------
;;
;; Custom key binds
;;
;;------------------------------

(def editor-keybind-settings
  {
   '(Ctrl h) editor/backward-char
   '(Ctrl j) editor/next-line
   '(Ctrl k) editor/previous-line
   '(Ctrl l) editor/forward-char

   '(Alt h) editor/backward-word
   '(Alt l) editor/forward-word
   '(Alt Shift h) editor/begin-line
   '(Alt Shift l) editor/end-line

   '(Alt j) editor/next-page
   '(Alt k) editor/previous-page
   '(Alt Shift j) editor/end-buffer
   '(Alt Shift k) editor/begin-buffer

   '(Ctrl b) editor/delete-previous-char
   '(Ctrl d) editor/delete-next-char

   '(Ctrl c) editor/copy
   '(Ctrl v) editor/paste
   '(Ctrl x) editor/cut
   '(Ctrl s) editor/file-save
   '(Ctrl w) editor/close
   '(Ctrl a) editor/selectAll
   '(Ctrl z) editor/undo
   '(Ctrl y) editor/redo
   '(Ctrl p) editor/mark
   '(Ctrl e) editor/escape
   '(Ctrl g) editor/goto-line
   '(Ctrl f) search/show-find
   '(Ctrl r) search/show-replace
   '(Ctrl Shift f) search/show-find-in-selection
   '(Ctrl Shift r) search/show-replace-in-selection
   
   ;'(Alt i) editor/show-component-stack
   '(Alt i) editor/show-system-encoding
  })

(doseq [setting editor-keybind-settings]
  (let [keybind (setting 0)
        action  (setting 1)]
    (keys/define-keybind editor/multi-line-maps keybind action)))


(def search-keybind-settings
  {
   '(Ctrl h) editor/backward-char
   '(Ctrl l) editor/forward-char

   '(Alt h) editor/backward-word
   '(Alt l) editor/forward-word
   '(Alt Shift h) editor/begin-line
   '(Alt Shift l) editor/end-line

   '(Ctrl b) editor/delete-previous-char
   '(Ctrl d) editor/delete-next-char

   '(Ctrl c) editor/copy
   '(Ctrl v) editor/paste
   '(Ctrl x) editor/cut
   '(Ctrl a) editor/selectAll
   '(Ctrl z) editor/undo
   '(Ctrl y) editor/redo
   '(Ctrl p) editor/mark
   'Return   search/operate-forward
   '(Shift Return) search/operate-backword
   '(Ctrl e) search/escape
  })
(doseq [setting search-keybind-settings]
  (let [keybind (setting 0)
        action  (setting 1)]
    (keys/define-keybind editor/single-line-maps keybind action)))


(def tool-keybind-settings
  {
   '(Ctrl h) editor/backward-char
   '(Ctrl l) editor/forward-char

   '(Alt h) editor/backward-word
   '(Alt l) editor/forward-word
   '(Alt Shift h) editor/begin-line
   '(Alt Shift l) editor/end-line

   '(Ctrl b) editor/delete-previous-char
   '(Ctrl d) editor/delete-next-char

   '(Ctrl c) editor/copy
   '(Ctrl v) editor/paste
   '(Ctrl x) editor/cut
   '(Ctrl a) editor/selectAll
   '(Ctrl z) editor/undo
   '(Ctrl y) editor/redo
   '(Ctrl p) editor/mark
   'Return   search/operate-forward
   '(Shift Return) search/operate-backword
   '(Ctrl e) search/escape
  })
(doseq [setting search-keybind-settings]
  (let [keybind (setting 0)
        action  (setting 1)]
    (keys/define-keybind editor/single-line-maps keybind action)))


(def frame-keybind-settings
  {
   '(Ctrl t) frame/show-tools
   '(Ctrl n) editor/file-new
   '(Ctrl o) editor/file-open
   '(Ctrl Shift l) frame/load-module
   ;'(Ctrl Shift \r) frame/load-module
  })

(doseq [setting frame-keybind-settings]
  (let [keybind (setting 0)
        action  (setting 1)]
    (keys/define-keybind frame/maps keybind action)))

