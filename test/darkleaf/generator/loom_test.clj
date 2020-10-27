(ns darkleaf.generator.loom-test
  (:require
   [darkleaf.generator.core :as gen]
   [darkleaf.generator.loom :refer [generator yield]]
   [clojure.test :as t]
   [clojure.template :refer [do-template]])
  (:import [clojure.lang ExceptionInfo]))

(t/deftest loom-killer-feature-test
  (let [nested (fn [x]
                 (yield [:inc x]))
        f      (fn []
                 (mapv nested [0 1 2]))
        f*     (fn []
                 (generator
                  (f)))
        gen    (f*)]
    (t/is (= [:inc 0] (gen/value gen)))
    (gen/next gen 1)
    (t/is (= [:inc 1] (gen/value gen)))
    (gen/next gen 2)
    (t/is (= [:inc 2] (gen/value gen)))
    (gen/next gen 3)

    (t/is (= [1 2 3] (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest loom-not-in-scope-test
  (t/is (thrown-with-msg? IllegalStateException
                          #"^yield called without scope$"
                          (yield))))

;; ~~~~~~~~~~~~~~~~ core-test ~~~~~~~~~~~~~~~~

(defmacro with-wrappers [w-name & body]
  `(do-template [~w-name] (t/testing [:wrapper ~w-name]
                            ~@body)
                #'identity
                #'gen/wrap-stack))

(t/deftest null-test
  (with-wrappers wrap
    (let [f*  (fn []
                (generator))
          f*  (wrap f*)
          gen (f*)]
      (t/is (nil? (gen/value gen)))
      (t/is (gen/done? gen)))))

(t/deftest yield-0-&-next-1-test
  (with-wrappers wrap
    (let [f*  (fn []
                (generator
                  (yield)))
          f*  (wrap f*)
          gen (f*)]
      (t/is (nil? (gen/value gen)))
      (gen/next gen)
      (t/is (nil? (gen/value gen)))
      (t/is (gen/done? gen)))))

(t/deftest yield-1-&-next-2-test
  (with-wrappers wrap
    (let [f*  (fn []
                (generator
                  (yield :my-value)))
          f*  (wrap f*)
          gen (f*)]
      (t/is (= :my-value (gen/value gen)))
      (gen/next gen :my-covalue)
      (t/is (= :my-covalue (gen/value gen)))
      (t/is (gen/done? gen)))))

(t/deftest throw-test
  (with-wrappers wrap
    (let [f*  (fn []
                (generator
                  (yield :a)
                  (throw (ex-info "My error" {}))))
          f*  (wrap f*)
          gen (f*)]
      (t/is (= :a (gen/value gen)))
      (t/is (thrown? ExceptionInfo (gen/next gen)))
      (t/is (gen/done? gen)))))

(t/deftest gen-throw-&-catch-test
  (with-wrappers wrap
    (let [f*  (fn []
                (generator
                  (try
                    (yield)
                    (catch ExceptionInfo ex
                      (ex-message ex)))))
          f*  (wrap f*)
          gen (f*)]
      (gen/throw gen (ex-info "My error" {}))
      (t/is (= "My error" (gen/value gen)))
      (t/is (gen/done? gen)))))

(t/deftest gen-throw-&-finally-test
  (with-wrappers wrap
    (let [f*  (fn []
                (generator
                  (try
                    (yield)
                    (catch ExceptionInfo ex
                      (ex-message ex))
                    (finally
                      (yield :from-finally)))))
          f*  (wrap f*)
          gen (f*)]
      (gen/throw gen (ex-info "My error" {}))
      (t/is (= :from-finally (gen/value gen)))
      (gen/next gen)
      (t/is (= "My error" (gen/value gen)))
      (t/is (gen/done? gen)))))

(t/deftest return-1-test
  (with-wrappers wrap
    (let [f*  (fn []
                (generator
                  (yield :a)
                  (yield :b)
                  42))
          f*  (wrap f*)
          gen (f*)]
      (gen/return gen)
      (t/is (nil? (gen/value gen)))
      (t/is (gen/done? gen)))))

(t/deftest return-2-test
  (with-wrappers wrap
    (let [f*  (fn []
                (generator
                  (yield :a)
                  (yield :b)
                  42))
          f*  (wrap f*)
          gen (f*)]
      (gen/return gen 0)
      (t/is (= 0 (gen/value gen)))
      (t/is (gen/done? gen)))))

(t/deftest return-&-catch-test
  (with-wrappers wrap
    (let [f*  (fn []
                (generator
                  (try
                    (yield :a)
                    (yield :b)
                    42
                    ;; this types and its subtypes are only allowed
                    (catch Exception ex
                      nil))))
          f*  (wrap f*)
          gen (f*)]
      (gen/return gen 0)
      (t/is (= 0 (gen/value gen)))
      (t/is (gen/done? gen)))))

(t/deftest return-&-finally-test
  (with-wrappers wrap
    (let [f*  (fn []
                (generator
                  (try
                    (yield :a)
                    42
                    (finally
                      (yield :b)))))
          f*  (wrap f*)
          gen (f*)]
      (t/is (= :a (gen/value gen)))
      (gen/return gen 0)
      (t/is (= :b (gen/value gen)))
      (gen/next gen)
      (t/is (= 0 (gen/value gen)))
      (t/is (gen/done? gen)))))

(t/deftest illegal-state-test
  (with-wrappers wrap
    (let [f* (fn []
               (generator))
          f* (wrap f*)]
      (t/are [form] (thrown-with-msg? ExceptionInfo
                                      #"^Generator is done$"
                                      (let [gen (f*)]
                                        form))
        (gen/next gen)
        (gen/throw gen (ex-info "My error" {}))
        (gen/return gen)))))

(t/deftest stack-test
  (let [nested* (fn []
                  (generator
                    [(yield :a)
                     (yield :b)]))
        f*      (fn []
                  (generator
                    [(yield :start)
                     (yield (nested*))
                     (yield :finish)]))
        f*      (gen/wrap-stack f*)
        gen     (f*)]
    (t/is (= :start (gen/value gen)))
    (gen/next gen 1)
    (t/is (= :a (gen/value gen)))
    (gen/next gen 2)
    (t/is (= :b (gen/value gen)))
    (gen/next gen 3)
    (t/is (= :finish (gen/value gen)))
    (gen/next gen 4)
    (t/is (= [1 [2 3] 4] (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest stack-test-2
  (let [nested* (fn []
                  (generator
                    (yield :a)))
        f*      (fn []
                  (generator
                    (yield (nested*))))
        f*      (gen/wrap-stack f*)
        gen     (f*)]
    (t/is (= :a (gen/value gen)))
    (gen/next gen :my-value)
    (t/is (= :my-value (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest stack-&-finally-test
  (let [nested* (fn []
                  (generator
                    (yield :a)))
        f*      (fn []
                  (generator
                    (yield :start)
                    (try
                      (yield (nested*))
                      (finally
                        (yield :finish)))))
        f*      (gen/wrap-stack f*)
        gen     (f*)]
    (t/is (= :start (gen/value gen)))
    (gen/next gen)
    (t/is (= :a (gen/value gen)))
    (gen/next gen :my-value)
    (t/is (= :finish (gen/value gen)))
    (gen/next gen)
    (t/is (= :my-value (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest tail-call-test
  (let [nested* (fn []
                  (generator
                    (yield :a)))
        f*      (fn []
                  (generator
                    (yield :start)
                    (nested*)))
        f*      (gen/wrap-stack f*)
        gen     (f*)]
    (t/is (= :start (gen/value gen)))
    (gen/next gen)
    (t/is (= :a (gen/value gen)))
    (gen/next gen :my-value)
    (t/is (= :my-value (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest tail-call-&-finally-test
  (let [nested* (fn []
                  (generator
                    (yield :a)))
        f*      (fn []
                  (generator
                    (yield :start)
                    (try
                      (nested*)
                      (finally
                        (yield :finish)))))
        f*      (gen/wrap-stack f*)
        gen     (f*)]
    (t/is (= :start (gen/value gen)))
    (gen/next gen)
    (t/is (= :finish (gen/value gen)))
    (gen/next gen)
    (t/is (= :a (gen/value gen)))
    (gen/next gen :my-value)
    (t/is (= :my-value (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest thrown-in-stack-test
  (let [nested* (fn []
                  (generator
                    (yield :a)
                    (try
                      (throw (ex-info "My error" {}))
                      (finally
                        (yield :b)))))
        f*      (fn []
                  (generator
                    (yield :start)
                    (try
                      (yield (nested*))
                      (catch ExceptionInfo ex
                        (ex-message ex))
                      (finally
                        (yield :finish)))))
        f*      (gen/wrap-stack f*)
        gen     (f*)]
    (t/is (= :start (gen/value gen)))
    (gen/next gen)
    (t/is (= :a (gen/value gen)))
    (gen/next gen)
    (t/is (= :b (gen/value gen)))
    (gen/next gen)
    (t/is (= :finish (gen/value gen)))
    (gen/next gen)
    (t/is (= "My error" (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest thrown-in-stack-test-2
  (let [nested-1* (fn []
                    (generator
                      (yield :a)))
        nested-2* (fn []
                    (generator
                      (yield (nested-1*))
                      (throw (ex-info "My error" {}))))
        f*        (fn []
                    (generator
                      (yield :start)
                      (try
                        (yield (nested-2*))
                        (catch ExceptionInfo ex
                          (ex-message ex))
                        (finally
                          (yield :finish)))))
        f*        (gen/wrap-stack f*)
        gen       (f*)]
    (t/is (= :start (gen/value gen)))
    (gen/next gen)
    (t/is (= :a (gen/value gen)))
    (gen/next gen)
    (t/is (= :finish (gen/value gen)))
    (gen/next gen)
    (t/is (= "My error" (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest gen-throw-in-stack
  (let [nested* (fn []
                  (generator
                    (try
                      (yield :a)
                      (yield :b)
                      (finally
                        (yield :nested)))))
        f*      (fn []
                  (generator
                    (try
                      (yield (nested*))
                      42
                      (finally
                        (yield :finish)))))
        f*      (gen/wrap-stack f*)
        gen     (f*)]
    (t/is (= :a (gen/value gen)))
    (gen/throw gen (ex-info "My error" {}))
    (t/is (= :nested (gen/value gen)))
    (gen/next gen)
    (t/is (= :finish (gen/value gen)))
    (t/is (thrown? ExceptionInfo (gen/next gen)))))

(t/deftest gen-throw-in-stack-2
  (let [nested* (fn []
                  (generator
                    (try
                      (yield :a)
                      (yield :b))))
        f*      (fn []
                  (generator
                    (try
                      (yield (nested*))
                      42
                      (finally
                        (yield :finish)))))
        f*      (gen/wrap-stack f*)
        gen     (f*)]
    (t/is (= :a (gen/value gen)))
    (gen/throw gen (ex-info "My error" {}))
    (t/is (= :finish (gen/value gen)))
    (t/is (thrown? ExceptionInfo (gen/next gen)))))

(t/deftest return-in-stack
  (let [nested* (fn []
                  (generator
                    (yield :a)
                    (yield :b)))
        f*      (fn []
                  (generator
                    (try
                      (yield (nested*))
                      42
                      (finally
                        (yield :finish)))))
        f*      (gen/wrap-stack f*)
        gen     (f*)]
    (t/is (= :a (gen/value gen)))
    (gen/return gen 0)
    (t/is (= :finish (gen/value gen)))
    (gen/next gen)
    (t/is (= 0 (gen/value gen)))))

;; core analogs

(defn- f->f* [f]
  (fn [& args]
    (generator
      (apply f args))))

(defn- call [f*]
  (let [f*  (gen/wrap-stack f*)
        gen (f*)]
    (while (not (gen/done? gen))
      (gen/next gen))
    (gen/value gen)))

(t/deftest reduce*-test
  (let [str* (f->f* str)]
    (t/are [coll] (= (           reduce  str  coll)
                     (call #(gen/reduce* str* coll)))
      nil
      []
      [:a]
      [:a :b]
      [:a :b :c])
    (t/are [acc coll] (= (           reduce  str  acc coll)
                         (call #(gen/reduce* str* acc coll)))
      "" []
      "" [:a]
      "" [:a :b]
      "" [:a :b :c]))
  (let [with-reduced  (fn [_acc v]
                        (if (= :done v)
                          (reduced v)
                          v))
        with-reduced* (f->f* with-reduced)]
    (t/are [coll] (= (           reduce  with-reduced  coll)
                     (call #(gen/reduce* with-reduced* coll)))
      [:done]
      [1 :done]
      [1 2 3 :done 4 5])))

(t/deftest mapv*-test
  (let [str* (f->f* str)]
    (t/are [colls] (= (       apply     mapv  str  colls)
                      (call #(apply gen/mapv* str* colls)))

      [nil]
      [[]]
      [[0]]
      [[0 1]]
      [[0 1 2]]
      [#{1 2 3}]
      [{:a 1, :b 2}]

      [nil nil]
      [[] []]
      [[0] [1 2]]
      [[0 1] [2]]
      [#{1 2} [3 4]])))
