(ns train-race.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[train-race started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[train-race has shut down successfully]=-"))
   :middleware identity})
