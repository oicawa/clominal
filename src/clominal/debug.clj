(ns clominal.debug
  (:require [clojure.pprint :as pprint]))

(defmacro ?=
  [x]
  `(let [res# ~x
         m#   ~(meta &form)]
    (println (format "?=[%s:%d,%d]" ~*file* (:line m#) (:column m#)))
    (pprint/pprint res#)
    res#))

(defn pprint
  [x]
  `(let [res# ~x]
     (println "?=")
     (pprint/pprint res#)
     res#))
