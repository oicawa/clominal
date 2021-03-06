(ns clominal.config
  (:use [clominal.utils]
        [clojure.pprint :only (pprint)])
  (:require [clominal.fonts :as fonts]
            [clominal.log :as log])
  (:import [java.io File StringWriter]
           [java.awt Font Toolkit]))

(def ^{:dynamic true :private true} *properties* (atom nil))

(defn- get-config-dir-path
  []
  (let [path (append-path home-directory-path ".clominal")
        file (File. path)]
    (if (not (. file exists))
        (. file mkdir))
    (if (not (. file isDirectory))
        (do
          (. file delete)
          (. file mkdir)))
    path))

(defn- get-properties-file-path
  [config-dir-path]
  (let [path (append-path config-dir-path "properties")
        file (File. path)]
    (if (not (. file exists))
        (do
          (. file createNewFile)
          (let [size   (. (Toolkit/getDefaultToolkit) getScreenSize)
                width  (if (< (. size width) 800)
                           (. size width)
                           800)
                height (* (. size height) 0.9)
                x      (/ (- (. size width) width) 2)
                y      (/ (- (. size height) height) 2)]
            (spit path { :frame { :x x :y y :width width :height height} }))))
    path))

(defn get-prop
  [& keys]
  (loop [prop       @*properties*
         target-key (first keys)
         rest-keys  (rest keys)]
    (cond (nil? prop)
            nil
          (empty? rest-keys)
            (prop target-key)
          :else
            (recur (prop target-key) (first rest-keys) (rest rest-keys)))))

(defn set-prop
  [& params]
  (letfn [(set-prop-recur
            [prop target-key rest-keys]
            (if (empty? rest-keys)
                (assoc prop target-key (last params)) 
                (assoc prop target-key (set-prop-recur (prop target-key) (first rest-keys) (rest rest-keys)))))]
    (let [all-keys (butlast params)
          new-prop (set-prop-recur @*properties* (first all-keys) (rest all-keys))]
      (reset! *properties* new-prop))))

(defn save-prop
  []
  (let [config-dir-path (get-config-dir-path)
        properties-path (get-properties-file-path config-dir-path)
        writer          (StringWriter.)]
    (pprint @*properties* writer)
    (spit properties-path (. writer toString))))

(defn get-base-font
  []
  (fonts/get-base-font
    (get-prop :font :name)
    (get-prop :font :size)))

(defn init
  []
  (let [config-dir-path (get-config-dir-path)
        properties-path (get-properties-file-path config-dir-path)]
    (reset! *properties* (read-string (slurp properties-path)))
    ; Font
    (let [font-name (get-prop :font :name)
          font-size (get-prop :font :size)]
      (log/info "font-name=[%s], font-size=%d" font-name font-size)
      (if (nil? font-name)
          (set-prop :font :name fonts/default-name))
      (if (nil? font-size)
          (set-prop :font :size fonts/default-size))
      (if (or (nil? font-name) (nil? font-size))
          (save-prop)
          (reset! *properties* (read-string (slurp properties-path)))))))
      
















