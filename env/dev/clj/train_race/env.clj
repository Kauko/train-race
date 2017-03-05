(ns train-race.env
  (:require [selmer.parser :as parser]
            [train-race.log :as log :refer [logger]]
            [train-race.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info logger ::system.startup {:msg "train-race starting using the development profile"}))
   :stop
   (fn []
     (log/info logger ::system.shutdown {:msg "train-race shutting down"}))
   :middleware wrap-dev})