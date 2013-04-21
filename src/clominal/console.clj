(ns clominal.console
  (:use [clojure.contrib.def]
        [clominal.utils])
  (:import (javax.swing JComponent JTextArea JScrollPane JPanel SwingUtilities)
           (java.io ByteArrayOutputStream PrintStream)
           (java.awt Font GridBagLayout)
           (java.awt.event InputEvent KeyEvent WindowAdapter)
           (clominal.utils IAppPane IMarkable))
  (:require [clominal.keys :as keys]))


;;
;; Global constant
;;

(defvar console-title "* CONSOLE *")

(defn make-output-stream-with
  [component]
  (proxy [ByteArrayOutputStream] []
    (write
      ([bytes offset length]
        (proxy-super write bytes offset length)
        (. component flush))
      ([data]
        (proxy-super write data)
        (. component flush)))))

(definterface IOutputComponent
  (getOut [])
  (setToSystemOut [])
  (setToSystemErr [])
  (addDocumentListener [listener])
  (flush []))


;
; Console Key Maps
;
(def console-maps (keys/make-keymaps (JTextArea.) JComponent/WHEN_FOCUSED))

(defn make-console
  []
  (let [is-marked    (atom false)
        out          (atom nil)
        console      (proxy [JTextArea IMarkable IOutputComponent clominal.keys.IKeybindComponent] []
                       (isMark [] @is-marked)
                       (setMark [marked]
                         (reset! is-marked marked))
                       (getOut []
                         (if (nil? @out)
                             (reset! out (make-output-stream-with this)))
                         @out)
                       (setToSystemOut []
                         (System/setOut (PrintStream. (. this getOut)))
                         )
                       (setToSystemErr []
                         (System/setErr (PrintStream. (. this getOut)))
                         )
                       (flush []
                         (SwingUtilities/invokeLater
                           #(do
                             (. this append (. @out toString))
                             (. @out reset))))
                       (setInputMap [inputmap] (. this setInputMap JComponent/WHEN_FOCUSED inputmap))
                       (setActionMap [actionmap] (proxy-super setActionMap actionmap)))
        console-panel  (proxy [JScrollPane] [console]
                         (requestFocusInWindow []
                           (. console requestFocusInWindow)))
        ;
        ; Others
        ;
        default-map       (. console-maps get "default")
        default-fonts     {:linux   ["YOzFontCF" Font/PLAIN 16]
                           :windows ["ＭＳ ゴシック" Font/PLAIN 14]}
        ]
    ;
    ; Console
    ;
    (doto console
      (.setName "console")
      (.setInputMap  JComponent/WHEN_FOCUSED (. default-map getInputMap))
      (.setActionMap (. default-map getActionMap))
      (.setToSystemOut)
      (.setToSystemErr)
      ;(.setEditable false)
      (.setEditable true))

    (doseq [component [console]]
      (set-font component (default-fonts (get-os-keyword))))

    console-panel
    ))



