(ns clominal.fonts
  (:require [clojure.string :as string]
            [clojure.pprint :as pprint]
            [clominal.log :as log])
  (:import (java.io File)
           (java.awt Font GraphicsEnvironment)
           (sun.font FontManagerFactory)
           (java.util Locale)))

(def default-name Font/MONOSPACED)
(def default-size 16)

(defn get-ttf-font-file-paths
  []
  (let [font-manager (FontManagerFactory/getInstance)
        ;; Separator character
        ;; - Linux   ... ':'
        ;; - Windows ... ??? (It seems that the font path is only one. For now, we don't judge separator character for windows.)
        font-dir-paths  (string/split (. font-manager getPlatformFontPath true) #":")
        font-file-paths (map (fn [font-dir-path]
                               (let [directory (File. font-dir-path)
                                     files     (. directory listFiles)]
                                 (map (fn [file] (. file getAbsolutePath))
                                      (vec files))))
                             font-dir-paths)
        ttf-font-paths  (filter #(let [lastIndex (. %1 lastIndexOf ".")
                                       ext       (if (< lastIndex 0)
                                                     nil
                                                     (.. %1 (substring lastIndex) (toLowerCase)))]
                                     (or (= ext ".otf")
                                         (= ext ".ttf")))
                                (flatten font-file-paths))]
    (vec ttf-font-paths)))
  
(defn get-ttf-font-info
  [font-path]
  (let [font       (Font/createFont Font/TRUETYPE_FONT (File. font-path))
        font-name  (. font getName)
        font-value { "name"   font-name
                     "face"   (. font getFontName (Locale/JAPANESE))
                     "family" (. font getFamily (Locale/JAPANESE))
                     "path"   font-path}]
    font-value))

(defn get-ttf-font-map
  [ttf-font-paths]
  (loop [font-map   {}
         font-paths ttf-font-paths]
    (if (= (count font-paths) 0)
        font-map
        (let [rest-paths (subvec font-paths 1)
              font-path  (font-paths 0)
              font-value (get-ttf-font-info font-path)]
          (recur (assoc font-map (font-value "name") font-value) rest-paths)))))

(def ttf-font-map (ref nil))

(defn get-font-file-path
  [font-name]
  (get-in (deref ttf-font-map) [font-name "path"] nil))

(def base-font (atom nil))
(defn get-base-font
  [font-name font-size]
  (if (nil? @base-font)
      (let [font-info ((deref ttf-font-map) font-name)]
        (pprint/pprint font-info)
        (if (nil? font-info)
            (reset! base-font (Font. Font/MONOSPACED Font/PLAIN font-size))
            (let [font (Font/createFont Font/TRUETYPE_FONT (File. (font-info "path")))]
              (. (GraphicsEnvironment/getLocalGraphicsEnvironment) registerFont font)
              (reset! base-font (Font. (font-info "family") Font/PLAIN font-size))))))
  @base-font)


(defn init
  []
  (dosync (ref-set ttf-font-map (get-ttf-font-map (get-ttf-font-file-paths))))
  true)

