(ns clominal.utils.env
  (:require [clojure.contrib.string :as string])
  (:import (java.io File)
           (java.util.regex Pattern)))

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

(defn get-absolute-path
  [path]
  (let [fields (seq (. path split (Pattern/quote os-file-separator)))
        head   (first fields)
        body   (rest fields)]
    (if (= head "~")
        (string/join os-file-separator (cons (System/getProperty "user.home") body))
        (. (File. path) getAbsolutePath))))