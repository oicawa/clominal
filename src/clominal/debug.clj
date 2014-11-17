(ns clominal.debug)

(defn print
  [x]
  `(let [res# ~x]
     ;(println "?=" (quote ~x))
     (println "?=" res#)
     res#))
