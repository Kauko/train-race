(ns train-race.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [train-race.layout :refer [error-page]]
            [train-race.routes.home :refer [home-routes]]
            [train-race.routes.services :refer [service-routes]]
            [compojure.route :as route]
            [train-race.env :refer [defaults]]
            [mount.core :as mount]
            [train-race.middleware :as middleware]))

(mount/defstate init-app
                :start ((or (:init defaults) identity))
                :stop  ((or (:stop defaults) identity)))

(def app-routes
  (routes
    (-> #'home-routes
        (wrap-routes middleware/wrap-csrf)
        (wrap-routes middleware/wrap-formats))
    #'service-routes
    (route/not-found
      (:body
        (error-page {:status 404
                     :title "page not found"})))))


(defn app [] (middleware/wrap-base #'app-routes))
