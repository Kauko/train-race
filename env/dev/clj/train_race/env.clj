(ns train-race.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [train-race.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[train-race started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[train-race has shut down successfully]=-"))
   :middleware wrap-dev})
