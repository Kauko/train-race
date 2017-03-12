(ns train-race.routes.websocket
  (:require [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.immutant :refer [get-sch-adapter]]
            [mount.core :as mount]

            [clojure.core.async :as async :refer [go-loop <! timeout chan]]

            [train-race.deps-merger :refer [defn-with-defaults]]
            [train-race.log :as log :refer [logger]]))

(def ^{:private true} +no-ch-recv+ (chan (async/dropping-buffer 0)))
(def ^{:private true} +no-send-fn+ (constantly nil))
(def ^{:private true} +no-ajax-post-fn+ (constantly nil))
(def ^{:private true} +no-ajax-get-or-ws-handshake-fn+ (constantly nil))
(def ^{:private true} +no-connected-uids+ (atom nil))

(def ring-ajax-post +no-ajax-post-fn+)
(def ring-ajax-get-or-ws-handshake +no-ajax-get-or-ws-handshake-fn+)
(def receive-channel +no-ch-recv+)
(def sente-send! +no-send-fn+)
(def connected-uids +no-connected-uids+)

(defn define-sente-vars!
  [{:keys [ch-recv send-fn connected-uids ajax-post-fn ajax-get-or-ws-handshake-fn]
    :as data}]
  (let [alter! (fn [s new-val]
                 (alter-var-root s (constantly new-val)))]
    (alter! #'ring-ajax-post ajax-post-fn)
    (alter! #'ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
    (alter! #'receive-channel ch-recv)
    (alter! #'sente-send! send-fn)
    (alter! #'connected-uids connected-uids))

  data)

(defn watch-atoms!
  [names-atoms-functions]
  (doseq [[name atom fn] names-atoms-functions]
    (add-watch atom name fn)))

(defn unwatch-atoms!
  [names-atoms-functions]
  (doseq [[name atom _] names-atoms-functions]
    (remove-watch atom name)))

(defn-with-defaults start-sente-channel-socket!
  [{:keys [adapter options lgr] :as deps}]
  {:adapter (get-sch-adapter) :options {} :lgr logger}

  (log/info logger ::system.startup {:msg "Initialising sente websocket"
                                     :adaptarer adapter
                                     :options options})
  (sente/make-channel-socket! adapter options))

(defn stop-sente-channel-socket! []
  (log/info logger ::system.shutdown {:msg "Shutting down sente websocket"})
  {:ch-recv +no-ch-recv+
   :send-fn +no-send-fn+
   :ajax-post-fn +no-ajax-post-fn+
   :ajax-get-or-ws-handshake-fn +no-ajax-get-or-ws-handshake-fn+
   :connected-uids +no-connected-uids+})

(defn fps-to-ms [fps]
  (/ 1000 fps))

(defn-with-defaults connected-count
  [{:keys [logger] :as deps} _ _ old new]
  {:logger logger}
  (log/info logger
            ::routes.websocket
            {:msg (str "Connected clients: " (count (:any old)) " => " (count (:any new)))
             :data {:old old :new new}}))

(go-loop [state {:color 0}]
  (doseq [client (:any @connected-uids)]
    (sente-send! client [:board/new-color state]))
  (<! (timeout (fps-to-ms 60)))
  (recur {:color (mod (+ (:color state) 0.7) 255)}))

(mount/defstate
  ^{:on-reload :noop}
  websocket
  :start
  (do
    (-> (start-sente-channel-socket! {})
       define-sente-vars!)
    (watch-atoms! [[::connected-count connected-uids connected-count]]))
  :stop
  (do
    (unwatch-atoms! [[::connected-count connected-uids]])
    (-> (stop-sente-channel-socket!)
       define-sente-vars!)))
