(ns clominal
  (:require [clominal.editors.editor :as editor]
            [clominal.keys.keymap :as keymap]))

;;------------------------------
;;
;; Custom key binds
;;
;;------------------------------

(def editor-keybind-settings
  {
   ; '(Ctrl Shift \h) editor/beginLine
   ; '(Ctrl \h) editor/backward
   ; '(Ctrl \j) editor/down
   ; '(Ctrl \k) editor/up
   ; '(Ctrl \l) editor/forward
   ; '(Ctrl Shift \l) editor/endLine

   ; '(Alt Shift \h) editor/begin
   ; '(Alt \h) editor/previousWord
   ; '(Alt \j) editor/pageDown
   ; '(Alt \k) editor/pageUp
   ; '(Alt \l) editor/nextWord
   ; '(Alt Shift \l) editor/end

   ; '(Ctrl \b) editor/deletePrevChar
   ; '(Ctrl \d) editor/deleteNextChar

   ; '(Ctrl \c) editor/copy
   ; '(Ctrl \v) editor/paste
   ; '(Ctrl \x) editor/cut
   '((Alt \f) \o) editor/openFile
   '(Ctrl \o) editor/openFile
   '(Ctrl \s) editor/saveFile
   ; '(Ctrl \a) editor/selectAll
  })

(doseq [setting editor-keybind-settings]
  (let [keybind (setting 0)
        action  (setting 1)]
    (keymap/define-keybind editor/maps keybind action)))

