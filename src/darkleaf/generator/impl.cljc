(ns darkleaf.generator.impl
  (:require
   [darkleaf.generator.proto :as p]
   [darkleaf.generator.stack :as stack]
   [cloroutine.core :refer [cr]]))

(def interrupted-exception
  #?(:clj  (Error. "Interrupted generator")
     :cljs ::interrupted-generator))

(declare ->Generator)

(defn body->generator [js? yield body]
  `(let [done?#      (volatile! false)
         value#      (volatile! nil)
         covalue-fn# (volatile! (fn [] nil))
         return#     (volatile! nil)
         resume#     (fn [] (@covalue-fn#))
         coroutine#  (cr {~yield resume#}
                         (try
                           ~@body
                           (catch ~(if js? :default 'Error) ex#
                             (if (identical? interrupted-exception ex#)
                               (vreset! value# @return#)
                               (throw ex#)))
                           (finally
                             (vreset! done?# true))))]
     (vreset! value# (coroutine#))
     (->Generator done?# value# covalue-fn# return# coroutine#)))

(defn- check-not-done! [gen]
  (when (p/-done? gen)
    (throw (ex-info "Generator is done" {:type :illegal-state}))))

(deftype Generator [done? value covalue-fn return coroutine]
  p/Generator
  (-done? [_] @done?)
  (-value [_] @value)
  (-next [this covalue]
    (check-not-done! this)
    (vreset! covalue-fn (fn [] covalue))
    (vreset! value (coroutine))
    nil)
  (-throw [this throwable]
    (check-not-done! this)
    (vreset! covalue-fn (fn [] (throw throwable)))
    (vreset! value (coroutine))
    nil)
  (-return [this result]
    (check-not-done! this)
    (vreset! return result)
    (vreset! covalue-fn (fn [] (throw interrupted-exception)))
    (vreset! value (coroutine))
    nil))

(defn- generator? [x]
  (instance? Generator x))

(defn- one? [coll]
  (= 1 (count coll)))

(defn- join [stack]
  (let [gen   (stack/peek stack)
        value (p/-value gen)]
    (if (p/-done? gen)
      (cond
        (generator? value)
        (do (stack/pop! stack)
            (stack/push! stack value)
            (recur stack))
        (one? stack)
        nil
        :else
        (do (stack/pop! stack)
            (p/-next (stack/peek stack) value)
            (recur stack)))
      (cond
        (generator? value)
        (do (stack/push! stack value)
            (recur stack))
        :else
        nil))))

(defn wrap-stack [f*]
  (fn [& args]
    (let [gen   (apply f* args)
          stack (stack/make)]
      (stack/push! stack gen)
      (join stack)
      (reify
        p/Generator
        (-done? [_]
          (-> stack (stack/peek) (p/-done?)))
        (-value [_]
          (-> stack (stack/peek) (p/-value)))
        (-next [this covalue]
          (try
            (-> stack (stack/peek) (p/-next covalue))
            (join stack)
            (catch #?(:clj Throwable :cljs :default) ex
              (if (one? stack)
                (throw ex)
                (do (stack/pop! stack)
                    (p/-throw this ex))))))
        (-throw [_ throwable]
          (let [gen       (stack/peek stack)
                throwable (try
                            (p/-throw gen throwable)
                            nil
                            (catch #?(:clj Throwable :cljs :default) ex ex))]
            (when (some? throwable)
              (if (one? stack)
                (throw throwable))
              (stack/pop! stack)
              (recur throwable))))
        (-return [_ result]
          (let [gen (stack/peek stack)]
            (p/-return gen result)
            (when-not (one? stack)
              (stack/pop! stack)
              (recur result))))))))
