(ns clominal.utils.env
  (:require [clojure.contrib.string :as string])
  (:import (java.io File)))

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

(def file-separator (System/getProperty "file.separator"))

(defn get-absolute-path
  [path]
  (let [fields (seq (. path split file-separator))
        head   (first fields)
        body   (rest fields)]
    (if (= head "~")
        (string/join file-separator (cons (System/getenv "HOME") body))
        (. (File. path) getAbsolutePath))))