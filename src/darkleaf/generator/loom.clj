(ns darkleaf.generator.loom
  (:import
   [java.util.function Supplier]
   [darkleaf.generator LoomGenerator]))

(defn yield
  ([] (yield nil))
  ([x] (LoomGenerator/yield x)))

(defmacro generator [& body]
  `(LoomGenerator. (reify Supplier
                     (get [_]
                       ~@body))))
