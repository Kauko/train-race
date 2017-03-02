(ns user
  (:require [mount.core :as mount]
            [train-race.figwheel :refer [start-fw stop-fw cljs]]
            train-race.core))

(defn start []
  (mount/start-without #'train-race.core/http-server
                       #'train-race.core/repl-server))

(defn stop []
  (mount/stop-except #'train-race.core/http-server
                     #'train-race.core/repl-server))

(defn restart []
  (stop)
  (start))


