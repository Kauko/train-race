(ns train-race.app
  (:require [train-race.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))
