(ns clominal.utils.env
  (:require [clojure.contrib.string :as string]))

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
