(ns settings
  (:require [clominal.keys :as keys]
            [clominal.frame :as frame]
            [clominal.editors.editor :as editor]
            [clominal.editors.lexer :as lexer]
            ))

;;------------------------------
;;
;; Custom key binds
;;
;;------------------------------

(def editor-keybind-settings
  {
   '(Ctrl \h) editor/backward-char
   '(Ctrl \j) editor/next-line
   '(Ctrl \k) editor/previous-line
   '(Ctrl \l) editor/forward-char

   '(Alt \h) editor/backward-word
   '(Alt \l) editor/forward-word
   '(Alt Shift \h) editor/begin-line
   '(Alt Shift \l) editor/end-line

   '(Alt \j) editor/next-page
   '(Alt \k) editor/previous-page
   '(Alt Shift \j) editor/end-buffer
   '(Alt Shift \k) editor/begin-buffer

   '(Ctrl \b) editor/delete-previous-char
   '(Ctrl \d) editor/delete-next-char

   '(Ctrl \c) editor/copy
   '(Ctrl \v) editor/paste
   '(Ctrl \x) editor/cut
   '(Ctrl \s) editor/file-save
   '((Alt \f) \c) editor/close
   '(Ctrl \a) editor/selectAll
   '(Ctrl \z) editor/undo
   '(Ctrl \y) editor/redo
   '(Ctrl \p) editor/mark
   '(Ctrl \e) editor/escape
   '(Ctrl \g) editor/goto-line

   '((Ctrl \i) \l) editor/print-current-line-tokens
   '((Ctrl \i) \t) editor/print-current-caret-token
   
   ;'(Ctrl \m) editor/set-character-attribute
   ;'((Ctrl \t) \c) editor/show-color-dialog
  })

(doseq [setting editor-keybind-settings]
  (let [keybind (setting 0)
        action  (setting 1)]
    (keys/define-keybind editor/maps keybind action)))


(def frame-keybind-settings
  {
   '(Ctrl \t) frame/show-tools
   '(Ctrl \n) editor/file-new
   '(Ctrl \o) editor/file-open
   ;'(Ctrl \q) frame/quit
  })

(doseq [setting frame-keybind-settings]
  (let [keybind (setting 0)
        action  (setting 1)]
    (keys/define-keybind frame/maps keybind action)))

