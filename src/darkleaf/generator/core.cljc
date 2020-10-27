(ns darkleaf.generator.core
  (:refer-clojure :exclude [next -next])
  (:require
   [darkleaf.generator.proto :as p]
   [darkleaf.generator.impl :as i])
  #?(:cljs (:require-macros [darkleaf.generator.core :refer [generator]])))

(def ^{:arglists '([gen])} done? p/-done?)
(def ^{:arglists '([gen])} value p/-value)
(def ^{:arglists '([gen throwable])} throw p/-throw)

(defn next
  ([gen] (p/-next gen nil))
  ([gen covalue] (p/-next gen covalue)))

(defn return
  ([gen] (p/-return gen nil))
  ([gen result] (p/-return gen result)))

(defn yield
  ([] nil)
  ([value] value))

(defn- js? [env]
  (contains? env :ns))

(defmacro ^{:style/indent 0} generator [& body]
  (i/body->generator (js? &env) `yield body))

(def ^{:arglists '([f*])} wrap-stack i/wrap-stack)

;; core analogs

(defn reduce*
  ([f* coll]
   (generator
     (case (count coll)
       0 (f*)
       1 (first coll)
       (reduce* f* (first coll) (rest coll)))))
  ([f* val coll]
   (generator
     (loop [acc  val
            coll coll]
       (cond
         (reduced? acc) (unreduced acc)
         (empty? coll)  acc
         :else          (recur (yield (f* acc (first coll)))
                               (rest coll)))))))

(defn mapv*
  ([f* coll]
   (generator
     (let [reducer* (fn [acc item]
                      (generator
                        (conj! acc (yield (f* item)))))
           acc      (transient [])
           result   (yield (reduce* reducer* acc coll))]
       (persistent! result))))
  ([f* coll & colls]
   (->> (apply map list coll colls)
        (mapv* #(apply f* %)))))
