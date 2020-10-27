(ns darkleaf.generator.loom
  (:require
   [darkleaf.generator.impl :as i])
  (:import
   [java.util.function Supplier]
   [darkleaf.generator LoomGenerator]))

(defn yield
  ([] (yield nil))
  ([x] (LoomGenerator/yield x)))

(defmacro generator [& body]
  `(-> (reify Supplier
         (get [_]
           ~@body))
       (LoomGenerator.)
       (i/wrap-gen-reject-done)))
