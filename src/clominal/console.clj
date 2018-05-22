(ns clominal.console
  (:use [clojure.contrib.def]
        [clominal.utils])
  (:require [clominal.config :as config])
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
  [tabs]
  (let [is-marked   (atom false)
        out         (atom nil)
        console     (proxy [JTextArea IMarkable clominal.keys.IKeybindComponent] []
                      (isMark [] @is-marked)
                      (setMark [marked]
                        (reset! is-marked marked))
                      (setInputMap [inputmap] (. this setInputMap JComponent/WHEN_FOCUSED inputmap))
                      (setActionMap [actionmap] (proxy-super setActionMap actionmap)))
        root        (proxy [JScrollPane IAppPane IOutputComponent] [console]
                      (getTabs [] tabs)
                      (getTabIndex [] (. tabs indexOfComponent this))
                      (getInfo []
                        { :generator 'clominal.console/make-console :id nil })
                      (canOpen [params] true)
                      (open [id] nil)
                      (canClose [] true)
                      (close []
                        (.. tabs (remove this)))
                      (requestFocusInWindow []
                        (. console requestFocusInWindow))
                      (setFocus []
                        (. tabs setSelectedIndex (. this getTabIndex))
                        (. console requestFocusInWindow))
                      (getOut []
                        (if (nil? @out)
                            (reset! out (make-output-stream-with this)))
                        @out)
                      (setToSystemOut []
                        (System/setOut (PrintStream. (. this getOut))))
                      (setToSystemErr []
                        (System/setErr (PrintStream. (. this getOut))))
                      (flush []
                        ; (SwingUtilities/invokeLater
                        ;   #(do
                        ;     (. console append (. @out toString))
                        ;     (. @out reset)))
                        (do
                          (. console append (. @out toString))
                          (. @out reset))))
        ;
        ; Others
        ;
        default-map (. console-maps get "default")
        ]
    ;
    ; Console
    ;
    (doto console
      (.setName "console")
      (.setInputMap  JComponent/WHEN_FOCUSED (. default-map getInputMap))
      (.setActionMap (. default-map getActionMap))
      (.setEditable true)
      (.setFont (config/get-base-font)))

    (doto root
      ;(.setToSystemOut)
      ;(.setToSystemErr)
      ;(.setEditable false)
      )
    
    ))



