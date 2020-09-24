(ns darkleaf.generator.core-test
  (:require
   [darkleaf.generator.core :as gen :refer [generator yield]]
   [clojure.test :as t])
  (:import
   #?(:clj [clojure.lang ExceptionInfo])))

(t/deftest null-test
  (let [gen (generator)]
    (t/is (nil? (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest yield-0-&-next-1-test
  (let [gen (generator
              (yield))]
    (t/is (nil? (gen/value gen)))
    (gen/next gen)
    (t/is (nil? (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest yield-1-&-next-2-test
  (let [gen (generator
              (yield :my-value))]
    (t/is (= :my-value (gen/value gen)))
    (gen/next gen :my-covalue)
    (t/is (= :my-covalue (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest catch-test
  (let [gen (generator
              (try
                (yield)
                (catch ExceptionInfo ex
                  (ex-message ex))))]
    (gen/throw gen (ex-info "My error" {}))
    (t/is (= "My error" (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest finally-test
  (let [gen (generator
              (try
                (yield)
                (catch ExceptionInfo ex
                  (ex-message ex))
                (finally
                  (yield :from-finally))))]
    (gen/throw gen (ex-info "My error" {}))
    (t/is (= :from-finally (gen/value gen)))
    (gen/next gen)
    (t/is (= "My error" (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest return-1-test
  (let [gen (generator
              (yield :a)
              (yield :b)
              42)]
    (gen/return gen)
    (t/is (nil? (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest return-2-test
  (let [gen (generator
              (yield :a)
              (yield :b)
              42)]
    (gen/return gen 0)
    (t/is (= 0 (gen/value gen)))
    (t/is (gen/done? gen))))

(t/deftest illegal-state-test
  (let [gen (generator)]
    (t/are [form] (thrown-with-msg? ExceptionInfo
                                    #"^Generator is done$"
                                    form)
      (gen/next gen)
      (gen/throw gen (ex-info "My error" {}))
      (gen/return gen))))
