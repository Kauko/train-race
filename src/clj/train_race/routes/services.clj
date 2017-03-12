(ns train-race.routes.services
  (:require [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [compojure.api.meta :refer [restructure-param]]
            [buddy.auth.accessrules :refer [restrict]]
            [buddy.auth :refer [authenticated?]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.immutant :refer [get-sch-adapter]]
            [mount.core :as mount]
            [ring.util.http-response :refer :all]

            [clojure.core.async :as async :refer [put! >! <! timeout chan go go-loop take!]]

            [train-race.log :as log :refer [logger]]
            [train-race.deps-merger :refer [defn-with-defaults]]
            [train-race.routes.websocket :as websocket]))

(defn access-error [_ _]
  (unauthorized {:error "unauthorized"}))

(defn wrap-restricted [handler rule]
  (restrict handler {:handler  rule
                     :on-error access-error}))

(defmethod restructure-param :auth-rules
  [_ rule acc]
  (update-in acc [:middleware] conj [wrap-restricted rule]))

(defmethod restructure-param :current-user
  [_ binding acc]
  (update-in acc [:letks] into [binding `(:identity ~'+compojure-api-request+)]))

(defapi service-routes
  {:swagger {:ui "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version "1.0.0"
                           :title "Game API"
                           :description ""}}}}
  (GET "/health" []
       (ok "I'm alive"))
  (GET "/authenticated" []
    :auth-rules authenticated?
    :current-user user
    (ok {:user user}))
  (context "/websocket" []
    (GET "/conn" req (websocket/ring-ajax-get-or-ws-handshake req))
    (POST "/conn" req (websocket/ring-ajax-post req)))

  (context "/sample-api" []
    :tags ["thingie"]

    (GET "/plus" []
      :return Long
      :query-params [x :- Long, {y :- Long 1}]
      :summary "x+y with query-parameters. y defaults to 1."
      (ok (+ x y)))

    (POST "/minus" []
      :return Long
      :body-params [x :- Long, y :- Long]
      :summary "x-y with body-parameters."
      (ok (- x y)))

    (GET "/times/:x/:y" []
      :return Long
      :path-params [x :- Long, y :- Long]
      :summary "x*y with path-parameters"
      (ok (* x y)))

    (POST "/divide" []
      :return Double
      :form-params [x :- Long, y :- Long]
      :summary "x/y with form-parameters"
      (ok (/ x y)))

    (GET "/power" []
      :return Long
      :header-params [x :- Long, y :- Long]
      :summary "x^y with header-parameters"
      (ok (long (Math/pow x y))))))
