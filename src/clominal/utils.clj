(ns clominal.utils
  (:require [clojure.contrib.string :as string]
            [clominal.debug :as debug])
  (:import (java.io File)
           (java.awt Font GraphicsEnvironment)
           (java.util HashMap)
           (java.util.regex Pattern)
           (javax.swing AbstractAction)))

(def ^:dynamic *frame*   (atom nil))
;(def ^:dynamic *console* (atom nil))

(defn error
  [e]
  (. e printStackTrace)
  (if-not (nil? @*frame*)
          (. @*frame* showConsole)))

; (defmulti error class)
; (defmethod error ^Exception
;   [e]
;   (. e printStackTrace)
;   (. clominal.core/*frame* showConsole))
; (defmethod error ^String
;   [message]
;   (. System/err println message)
;   (. clominal.core/*frame* showConsole))

;;
;; Common Interfaces
;;

(definterface ITabbedPane
  (getCurrentPanel [])
  (getInfoList []))

(definterface IMarkable
  (isMark [])
  (setMark [marked]))

(definterface IAppPane
  (canClose [])
  (getTabs [])
  (getTabIndex [])
  (getInfo [])
  (open [id])
  (canOpen [params])
  (close []))


(defn add-component
  [tabs title component]
  (. tabs addTab title component)
  (let [index         (- (. tabs getTabCount) 1)
        ;tabcomponent (. tabs getComponentAt index)
        ]
    (. tabs setSelectedIndex index)
    (. component requestFocusInWindow)
    component))



;;------------------------------
;; The macro for using GridBagLayout/GridBagConstraint easy.
;; (cf -> http://stuartsierra.com/2010/01/05/taming-the-gridbaglayout)
;;------------------------------
; (grid-bag-layout
;   :insets (Insets. 5 5 5 5)
;   :anchor :WEST
;   :gridx 0, :gridy 1, :weightx 0.0, :fill :NONE
;   (JLabel. "姓")
;   :gridx 1, :gridy 1, :weightx 1.0, :fill :HORIZONTAL
;   family-name-label
;   :gridx 0, :gridy 2, :weightx 0.0, :fill :NONE
;   (JLabel. "名")
;   :gridx 1, :gridy 2, :weightx 1.0, :fill :HORIZONTAL
;   first-names-label
;   :gridx 0, :gridy 3, :weightx 0.0, :fill :NONE
;   (JLabel. "郵便番号")
;   :gridx 1, :gridy 3, :weightx 1.0, :fill :HORIZONTAL
;   zip-code-label
;   :gridx 0, :gridy 4, :weightx 0.0, :fill :NONE
;   (JLabel. "住所")
;   :gridx 1, :gridy 4, :weightx 1.0, :fill :HORIZONTAL
;    address-label)





(defmacro set-grid! [constraints field value]
  `(set! (. ~constraints ~(symbol (name field)))
         ~(if (keyword? value)
            `(. java.awt.GridBagConstraints
                ~(symbol (name value)))
            value)))

(defmacro grid-bag-layout [container & body]
  (let [c (gensym "c")
        cntr (gensym "cntr")]
    `(let [~c (new java.awt.GridBagConstraints)
           ~cntr ~container]
       ~@(loop [result '() body body]
           (if (empty? body)
             (reverse result)
             (let [expr (first body)]
               (if (keyword? expr)
                 (recur (cons `(set-grid! ~c ~expr
                                          ~(second body))
                              result)
                        (next (next body)))
                 (recur (cons `(.add ~cntr ~expr ~c)
                              result)
                        (next body)))))))))


(defn get-component
  [root-container & all-names]
  (loop [container  root-container
         name       (first all-names)
         rest-names (rest all-names)]
    (if (= nil name)
        nil
        (let [children (. container getComponents)
              targets  (filter #(= name (. %1 getName)) children)]
          (if (= 1 (count targets))
              (recur (first targets) (first rest-names) (rest rest-names))
              nil)))))
              
                    
(defn windows?
  []
  (let [os-name (.. (System/getProperty "os.name") toLowerCase)]
    (= 0 (. os-name indexOf "windows"))))

(defn get-os-keyword
  []
  (let [os-raw-name (.. (System/getProperty "os.name") toLowerCase)
        os-name     (if (= 0 (. os-raw-name indexOf "windows"))
                        "windows"
                        os-raw-name)]
    (keyword os-name)))

(def os-file-separator (System/getProperty "file.separator"))

(def home-directory-path (System/getProperty "user.home"))

(defn get-absolute-path
  [path]
  (let [fields (seq (. path split (Pattern/quote os-file-separator)))
        head   (first fields)
        body   (rest fields)]
    (if (= head "~")
        (string/join os-file-separator (cons (System/getProperty "user.home") body))
        (. (File. path) getAbsolutePath))))

(defn append-path
  [base-path added-path]
  (str base-path os-file-separator added-path))


(defn get-font-names
  []
  (.. GraphicsEnvironment getLocalGraphicsEnvironment getAvailableFontFamilyNames))

(defn get-frame
  [component]
  (println "----------")
  (loop [current component]
    (println (type current))
    (let [parent (. current getParent)]
      (if (nil? parent)
          current
          (recur parent)))))


(defmacro defaction
  [name bindings & body]
  (assert (vector? bindings))
  (let [cnt (count bindings)]
    (assert (and (<= 1 cnt) (<= cnt 2)))
    (let [source (bindings 0)]
      (if (= cnt 1)
          (let [evt (gensym "evt")
                e   (gensym "e")]
            `(def ~name (proxy [AbstractAction] []
                          (actionPerformed [~evt]
                            (try
                              ((fn [~source] ~@body)
                               (. ~evt getSource))
                              (catch Exception ~e
                                (. ~e printStackTrace)
                                (error ~e)))))))
          (let [evt (bindings 1)
                e   (gensym "e")]
            `(def ~name (proxy [AbstractAction] []
                          (actionPerformed [~evt]
                            (try
                              ((fn [~source ~evt] ~@body)
                               (. ~evt getSource)
                               ~evt)
                              (catch Exception ~e
                                (. ~e printStackTrace)
                                (error ~e)))))))))))
