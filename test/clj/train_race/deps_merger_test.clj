(ns train-race.deps-merger-test
  (:require [clojure.test :refer :all]
            [train-race.deps-merger :as deps]))

(deftest combine-works
  (is (= {:a 2 :b 1} (deps/combine {:a 1 :b 1} {:a 2})))
  (is (= [2 2 1] (deps/combine [1 1 1] [2 2])))
  (is (thrown? Exception (deps/combine [1 2 3] {:a 1}))))

(deftest defn-with-defaults-works
  (testing "Defaults map, no destructuring"
    (deps/defn-with-defaults my-fun [deps a] {:inc 1} (+ (:inc deps) a))
    (is (= 2 (my-fun 1)))
    (is (= 3 (my-fun {:inc 2} 1))))

  (testing "Defaults map, no destructuring, works with any param name"
    (deps/defn-with-defaults my-fun [diipadaapa a] {:inc 1} (+ (:inc diipadaapa) a))
    (is (= 2 (my-fun 1)))
    (is (= 3 (my-fun {:inc 2} 1))))

  (testing "Defaults map with multiple values"
    (deps/defn-with-defaults my-fun [deps c] {:a 1 :b 1} (+ (:a deps) (:b deps) c))
    (is (= 3 (my-fun 1)))
    (is (= 4 (my-fun {:b 2} 1)))
    (is (= 1 (my-fun {:a 0 :b 0} 1))))

  (testing "Map destructuring works"
    (deps/defn-with-defaults my-fun [{:keys [a b] :as hurdur} c] {:a 1 :b 1} (+ a b c))
    (is (= 3 (my-fun 1)))
    (is (= 4 (my-fun {:b 2} 1)))
    (is (= 1 (my-fun {:a 0 :b 0} 1))))

  (testing "Defaults vector, no destructuring"
    (deps/defn-with-defaults my-fun [deps c] [1 1] (+ (first deps) (second deps) c))
    (is (= 3 (my-fun 1)))
    (is (= 4 (my-fun [2] 1)))
    (is (= 1 (my-fun [0 0] 1))))

  (testing "Defaults vector, with destructuring"
    (deps/defn-with-defaults my-fun [[a b :as something] c] [1 1] (+ a b c))
    (is (= 3 (my-fun 1)))
    (is (= 4 (my-fun [2] 1)))
    (is (= 1 (my-fun [0 0] 1))))

  (testing "Destructuring fails with no :as"
    (is (thrown? AssertionError (macroexpand-1 '(deps/defn-with-defaults my-fun [[a b] c] [1 1] (+ a b c)))))
    (is (thrown? AssertionError (macroexpand-1 '(deps/defn-with-defaults my-fun [{:keys [inc]} c] {:inc 1} (+ a b c))))))

  (testing "Destructuring fails with no :as (no macroexpansion)"
    (is (thrown? AssertionError (#'deps/defn-with-defaults-impl 'my-fun nil '[[a b] c] '[1 1] ['(+ a b c)])))
    (is (thrown? AssertionError (#'deps/defn-with-defaults-impl 'my-fun nil '[{:keys [inc]} c] '{:inc 1} ['(+ a b c)]))))

  (testing "Macroexpansion fails if default and deps are not both either maps or vectors"
    (is (thrown? AssertionError (macroexpand-1 '(deps/defn-with-defaults my-fun [[a b :as deps] c] {:inc 1} (+ a b c)))))
    (is (thrown? AssertionError (macroexpand-1 '(deps/defn-with-defaults my-fun [{:keys [inc] :as deps} c] [1 1] (+ a b c))))))

  (testing "Macroexpansion fails if default and deps are not both either maps or vectors (no macroexpansion)"
    (is (thrown? AssertionError (#'deps/defn-with-defaults-impl 'my-fun nil '[[a b :as deps] c] '{:inc 1} ['(+ a b c)])))
    (is (thrown? AssertionError (#'deps/defn-with-defaults-impl 'my-fun nil '[{:keys [inc] :as deps} c] '[1 1] ['(+ a b c)])))))
