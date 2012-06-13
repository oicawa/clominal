(ns clominal.utils.env)

(defn windows?
  []
  (let [os-name (.. (System/getProperty "os.name") toLowerCase)]
    (= 0 (. os-name indexOf "windows"))))