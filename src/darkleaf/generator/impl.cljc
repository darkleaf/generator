(ns darkleaf.generator.impl
  (:require
   [darkleaf.generator.proto :as p]
   [cloroutine.core :refer [cr]]))

(set! *warn-on-reflection* true)

(def interrupted-exception
  (InterruptedException. "Interrupted generator"))

(declare ->Generator)

(defn body->generator [yield body]
  `(let [done?#      (atom false)
         value#      (atom nil)
         covalue-fn# (atom (fn [] nil))
         return#     (atom nil)
         resume#     (fn [] (@covalue-fn#))
         coroutine#  (cr {~yield resume#}
                         (try
                           ~@body
                           (catch InterruptedException ex#
                             (reset! value# @return#)
                             @return#)
                           (finally
                             (reset! done?# true))))]
     (reset! value# (coroutine#))
     (->Generator done?# value# covalue-fn# return# coroutine#)))

(deftype Generator [done? value covalue-fn return coroutine]
  p/Generator
  (done? [_] @done?)
  (value [_] @value)
  (next [this] (.next this nil))
  (next [_ covalue]
    (if @done? (throw (IllegalStateException. "Generator is done")))
    (reset! covalue-fn (fn [] covalue))
    (reset! value (coroutine))
    nil)
  (throw [_ throwable]
    (if @done? (throw (IllegalStateException. "Generator is done")))
    (reset! covalue-fn (fn [] (throw throwable)))
    (reset! value (coroutine))
    nil)
  (return [this] (.return this nil))
  (return [_ result]
    (if @done? (throw (IllegalStateException. "Generator is done")))
    (reset! return result)
    (reset! covalue-fn (fn [] (throw interrupted-exception)))
    (reset! value (coroutine))
    nil))
