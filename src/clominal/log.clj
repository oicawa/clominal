(ns clominal.log
  (:gen-class)
  (:require [clojure.pprint :as pprint]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import (java.io File InputStream)
           (java.nio.file Paths Path Files StandardCopyOption CopyOption)
           (java.util.jar JarFile JarEntry)
           (java.util UUID Calendar)))

(defn- write
  [label base-format args]
  (let [ex-format (format "[%s] %s" label base-format)
        new-args  (cons ex-format args)
        message   (apply format new-args)]
    (println message)))

(defn fatal
  [base-format & args]
  (write "F" base-format args))

(defn error
  [base-format & args]
  (write "E" base-format args))

(defn warn
  [base-format & args]
  (write "W" base-format args))

(defn info
  [base-format & args]
  (write "I" base-format args))

(defn debug
  [base-format & args]
  ;(write "D" base-format args)
  )
