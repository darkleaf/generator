(ns darkleaf.generator.stack
  (:refer-clojure :exclude [pop! peek]))

(defn make []
  #js [])

(defn push! [stack value]
  (.unshift stack value))

(defn pop! [stack]
  (.shift stack))

(defn peek [stack]
  (aget stack 0))
