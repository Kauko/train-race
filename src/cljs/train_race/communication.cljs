(ns train-race.communication
  (:require-macros
    [cljs.core.async.macros :as asyncm :refer [go go-loop]])
  (:require
    [cljs.core.async :as async :refer [<! >! put! chan]]
    [taoensso.sente  :as sente :refer [cb-success?]]))

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/api/chsk" ; Note the same path as before
                                  {:type :auto ; e/o #{:auto :ajax :ws}
                                   })]
  (def chsk       chsk)
  (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def chsk-state state)   ; Watchable, read-only atom
  )

(defonce listeners (atom {}))
(defonce router_ (atom nil))

(defn register-event-listener!
  [event fn]
  (if (@listeners event)
    (swap! listeners update event conj fn)
    (swap! listeners assoc event [fn])))

(defmulti -event-msg-handler
          "Multimethod to handle Sente `event-msg`s"
          :id ; Dispatch on event-id
          )

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event]}]
  (println "Unhandled event: %s" event))

(defmethod -event-msg-handler :chsk/state
  [{:as ev-msg :keys [?data]}]
  (if (:first-open? ?data)
    (println "Channel socket successfully established!: %s" ?data)
    (println "Channel socket state change: %s" ?data)))

(defmethod -event-msg-handler :chsk/recv
  [{:as ev-msg :keys [?data]}]
  (let [[event & payload] ?data]
    #_(println "New event from server: " event)
    (doseq [fn (@listeners event)]
      (apply fn payload))))

(defmethod -event-msg-handler :chsk/handshake
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (println "Handshake: %s" ?data)))

(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_
          (sente/start-client-chsk-router!
            ch-chsk event-msg-handler)))
