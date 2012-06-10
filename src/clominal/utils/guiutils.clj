(ns clominal.utils.guiutils)

;;------------------------------
;; The macro for using GridBagLayout/GridBagConstraint easy.
;; (cf -> http://stuartsierra.com/2010/01/05/taming-the-gridbaglayout)
;;------------------------------
; (grid-bag-layout
;   :insets (Insets. 5 5 5 5)
;   :anchor :WEST
;   :gridx 0, :gridy 1, :weightx 0.0, :fill :NONE
;   (JLabel. "姓")
;   :gridx 1, :gridy 1, :weightx 1.0, :fill :HORIZONTAL
;   family-name-label
;   :gridx 0, :gridy 2, :weightx 0.0, :fill :NONE
;   (JLabel. "名")
;   :gridx 1, :gridy 2, :weightx 1.0, :fill :HORIZONTAL
;   first-names-label
;   :gridx 0, :gridy 3, :weightx 0.0, :fill :NONE
;   (JLabel. "郵便番号")
;   :gridx 1, :gridy 3, :weightx 1.0, :fill :HORIZONTAL
;   zip-code-label
;   :gridx 0, :gridy 4, :weightx 0.0, :fill :NONE
;   (JLabel. "住所")
;   :gridx 1, :gridy 4, :weightx 1.0, :fill :HORIZONTAL
;    address-label)




(defmacro set-grid! [constraints field value]
  `(set! (. ~constraints ~(symbol (name field)))
         ~(if (keyword? value)
            `(. java.awt.GridBagConstraints
                ~(symbol (name value)))
            value)))

(defmacro grid-bag-layout [container & body]
  (let [c (gensym "c")
        cntr (gensym "cntr")]
    `(let [~c (new java.awt.GridBagConstraints)
           ~cntr ~container]
       ~@(loop [result '() body body]
           (if (empty? body)
             (reverse result)
             (let [expr (first body)]
               (if (keyword? expr)
                 (recur (cons `(set-grid! ~c ~expr
                                          ~(second body))
                              result)
                        (next (next body)))
                 (recur (cons `(.add ~cntr ~expr ~c)
                              result)
                        (next body)))))))))


(defn get-component
  [root-container & all-names]
  (loop [container  root-container
         name       (first all-names)
         rest-names (rest all-names)]
    (if (= nil name)
        nil
        (let [children (. container getComponents)
              targets  (filter #(= name (. %1 getName)) children)]
          (if (= 1 (count targets))
              (recur (first targets) (first rest-names) (rest rest-names))
              nil)))))
              
                    
