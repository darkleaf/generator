(ns darkleaf.generator.impl
  (:require
   [darkleaf.generator.proto :as p]
   [cloroutine.core :refer [cr]]))

#?(:clj (set! *warn-on-reflection* true))

(def interrupted-exception
  #?(:clj  (Error. "Interrupted generator")
     :cljs ::interrupted-generator))

(declare ->Generator)

(defn body->generator [js? yield body]
  `(let [done?#      (atom false)
         value#      (atom nil)
         covalue-fn# (atom (fn [] nil))
         return#     (atom nil)
         resume#     (fn [] (@covalue-fn#))
         coroutine#  (cr {~yield resume#}
                         (try
                           ~@body
                           (catch ~(if js? :default 'Error) ex#
                             (if (= interrupted-exception ex#)
                               (do (reset! value# @return#)
                                   @return#)
                               (throw ex#)))
                           (finally
                             (reset! done?# true))))]
     (reset! value# (coroutine#))
     (->Generator done?# value# covalue-fn# return# coroutine#)))

(defn- reject-done [gen]
  (if (p/done? gen)
    (throw (ex-info "Generator is done" {:type :illegal-state}))))

(deftype Generator [done? value covalue-fn return coroutine]
  p/Generator
  (done? [_] @done?)
  (value [_] @value)
  (next [this covalue]
    (reject-done this)
    (reset! covalue-fn (fn [] covalue))
    (reset! value (coroutine))
    nil)
  (throw [this throwable]
    (reject-done this)
    (reset! covalue-fn (fn [] (throw throwable)))
    (reset! value (coroutine))
    nil)
  (return [this result]
    (reject-done this)
    (reset! return result)
    (reset! covalue-fn (fn [] (throw interrupted-exception)))
    (reset! value (coroutine))
    nil))
