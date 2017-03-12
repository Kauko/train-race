(ns train-race.communication
  (:require-macros
    [cljs.core.async.macros :as asyncm :refer [go go-loop]]
    [alter-cljs.core :refer [alter-var-root]])
  (:require
    [cljs.core.async :as async :refer [<! >! put! chan]]
    [taoensso.sente :as sente :refer [cb-success?]]

    [train-race.log :as log :refer [logger]]))

(defonce ^{:private true} +no-receive-channel+ (chan (async/dropping-buffer 1)))
(defonce ^{:private true} +no-send-fn+ (constantly nil))
(defonce ^{:private true} +no-chsk-state+ (atom nil))

(defonce receive-channel +no-receive-channel+)
(defonce sente-send! +no-send-fn+)
(defonce sente-state +no-chsk-state+)

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
  (log/warn logger ::communication.sente {:msg (str "Unhandled event: %s" event)
                                          :data ev-msg}))

(defmethod -event-msg-handler :chsk/state
  [{:as ev-msg :keys [?data]}]
  (if (:first-open? ?data)
    (log/info logger ::communication.sente {:msg (str "Channel socket successfully established!: " ?data)
                                             :data ev-msg})
    (log/info logger ::communication.sente {:msg (str "Channel socket state change: " ?data)
                                             :data ev-msg})))

(defmethod -event-msg-handler :chsk/recv
  [{:as ev-msg :keys [?data]}]
  (let [[event & payload] ?data]
    (log/trace logger ::communication.sente {:msg (str "New event from server: " ?data)
                                             :data ev-msg})
    (doseq [fn (@listeners event)]
      (apply fn payload))))

(defmethod -event-msg-handler :chsk/handshake
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (log/info logger ::communication.sente {:msg (str "Handshake: " ?data)
                                             :data ev-msg})))

(defn- initialise-socket! []
  (let [{:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket! "/websocket/conn"
                                    {:type :auto})]
    (alter-var-root #'receive-channel (constantly ch-recv))
    (alter-var-root #'sente-send! (constantly send-fn))
    (alter-var-root #'sente-state (constantly state))))

(defn- stop-socket! []
  (alter-var-root #'receive-channel (constantly +no-receive-channel+))
  (alter-var-root #'sente-send! (constantly +no-send-fn+))
  (alter-var-root #'sente-state (constantly +no-chsk-state+)))

(defn- stop-router! []
  (when-let [stop-f @router_]
    (log/info logger ::system.shutdown {:msg "Stopping sente router"})
    (stop-f)))

(defn- start-router! []
  (stop-router!)
  (log/info logger ::system.startup {:msg "Starting sente router"})
  (reset! router_
          (sente/start-client-chsk-router!
            receive-channel event-msg-handler)))

(defn start-websocket-connection! []
  (initialise-socket!)
  (add-watch sente-state ::sente-state
             (fn [_ _ old new]
               (log/trace logger ::communication.sente
                          {:msg (str "Sente state: " (pr-str new))
                           :data {:old old :new new}})))
  (start-router!))

(defn stop-websocket-connection! []
  (stop-router!)
  (remove-watch sente-state ::sente-state)
  (stop-socket!))