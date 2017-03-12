(ns train-race.deps-merger)

(defn combine [defaults others]
  (cond
    (and (map? defaults) (map? others))
    (merge defaults others)

    (and (sequential? defaults) (sequential? others))
    (let [length (max (count defaults) (count others))]
      (mapv
        (fn [a b] (or b a))
        (concat defaults (take (- length (count defaults)) (repeat nil)))
        (concat others (take (- length (count others)) (repeat nil)))))

    :else (throw (Exception. "Can only combine if both are either associative or sequential."))))

(defn- defn-with-defaults-impl
  [name doc-string defaults params body]
  (let [deps-param# (first params)
        actual-deps# (cond
                       (map? deps-param#) (:as deps-param#)
                       (vector? deps-param#) (do
                                               (assert (= :as (-> deps-param# butlast last)))
                                               (last deps-param#))
                       :else deps-param#)]
    (assert (some? actual-deps#) "First parameter in params-vector must be a symbol, or contain an :as binding.")
    (assert (or (symbol? deps-param#)
                (and (sequential? defaults) (sequential? deps-param#))
                (and (map? defaults) (map? deps-param#)))
            "If the first parameter is destructured, it must be the same type as the defaults.")
    `(defn ~name
       {:doc ~doc-string}
       (~(vec (rest params)) (~name ~defaults ~@(rest params)))
       (~params (let [~(first params) (combine ~defaults ~actual-deps#)]
                  ~@body)))))

(defmacro defn-with-defaults
  "Usage:
  (defn-with-defaults run-query! {:conn db} [{:keys [db] :as deps} q] (query db q))
  (run-query! 'SELECT * FROM foo')\n  (run-query! {:conn test-connection} 'SELECT * FROM foo')

  Defines a multi-arity function with two implementations. Meant for easy passing
  of default values. If the first parameter is destructured, you must use :as.

  The first implementation with n-1 parameters calls the implementation with n parameters,
  the first parameter being the defaults collection. If the n-arity function is called,
  the first parameter is combined with the defaults."
  [name doc-string? paramas defaults & body]
  (let [doc-string* (if (string? doc-string?) doc-string? nil)
        params* (if (string? doc-string?) paramas doc-string?)
        defaults* (if (string? doc-string?) defaults paramas)
        body* (if (string? doc-string?) body (into [defaults] body))]
    (defn-with-defaults-impl name doc-string* defaults* params* body*)))

