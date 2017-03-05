(ns train-race.env
  (:require [train-race.log :as log :refer [logger]]))

(def defaults
  {:init
   (fn []
     (log/info logger ::system.startup {:msg "train-race starting"}))
   :stop
   (fn []
     (log/info logger ::system.shutdown {:msg "train-race shutting down"}))
   :middleware identity})