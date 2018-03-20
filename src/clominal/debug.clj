(ns clominal.debug
  (:require [clojure.pprint :as pprint]))

(defn pprint
  [x]
  `(let [res# ~x]
     (println "?=")
     (pprint/pprint res#)
     res#))
