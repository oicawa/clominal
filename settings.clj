(ns settings
  (:require [clominal.keys :as keys]
            [clominal.frame :as frame]
            [clominal.editors.editor :as editor]
            ))

;;------------------------------
;;
;; Custom key binds
;;
;;------------------------------

(def editor-keybind-settings
  {
   '(Ctrl \h) editor/backward
   '(Ctrl \j) editor/down
   '(Ctrl \k) editor/up
   '(Ctrl \l) editor/forward

   '(Alt \h) editor/previousWord
   '(Alt \j) editor/pageDown
   '(Alt \k) editor/pageUp
   '(Alt \l) editor/nextWord

   '(Alt Shift \h) editor/beginLine
   '(Alt Shift \j) editor/endLine
   '(Alt Shift \k) editor/begin
   '(Alt Shift \l) editor/end

   '(Ctrl \b) editor/deletePrevChar
   '(Ctrl \d) editor/deleteNextChar

   '(Ctrl \c) editor/copy
   '(Ctrl \v) editor/paste
   '(Ctrl \x) editor/cut
   '(Ctrl \s) editor/saveFile
   '((Alt \x) \c) editor/close

   '(Ctrl \a) editor/selectAll
  })

(doseq [setting editor-keybind-settings]
  (let [keybind (setting 0)
        action  (setting 1)]
    (keys/define-keybind editor/maps keybind action)))


(def frame-keybind-settings
  {
   '(Ctrl \n) editor/file-new
   '(Ctrl \o) editor/file-open
   ;'(Ctrl \q) frame/quit
  })

(doseq [setting frame-keybind-settings]
  (let [keybind (setting 0)
        action  (setting 1)]
    (keys/define-keybind frame/maps keybind action)))

