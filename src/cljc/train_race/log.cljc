(ns train-race.log
  (:require [clojure.tools.logging :as log]
            [train-race.config :refer [env]]
            [mount.core :as mount]))

(defprotocol Logger
  (log [this msg]))

(defrecord StdOutLogger [] Logger
  (log [this {:keys [service msg level ex]}]
    (case level
      :trace (log/trace service " :: " msg)
      :debug (log/debug service " :: " msg)
      :info (log/info service " :: " msg)
      :warn (log/warn service " :: " msg)
      :error (if ex (log/error ex service " :: " msg) (log/error service " :: " msg))
      :fatal (if ex (log/fatal ex service " :: " msg) (log/fatal service " :: " msg)))))

(defrecord NullLogger [] Logger
  (log [this msg] nil))

(defn trace [logger service {:keys [msg]}]
  (log logger {:service service :level :trace :msg msg}))

(defn debug [logger service {:keys [msg]}]
  (log logger {:service service :level :error :msg msg}))

(defn info [logger service {:keys [msg]}]
  (log logger {:service service :level :info :msg msg}))

(defn warn [logger service {:keys [msg]}]
  (log logger {:service service :level :warn :msg msg}))

(defn error
  ([logger service {:keys [msg ex]}]
   (log logger {:service service :level :error :msg msg
                :exception ex})))

(defn failure
  ([logger service {:keys [msg ex]}]
   (log logger {:service service :level :trace :msg msg
                :exception ex})))

(defn start-stdoutlogger [config]
  (let [logger (->StdOutLogger)]
    (info logger ::system.startup {:msg "StdOutLogger initialising"})
    logger))

(declare stop-stdoutlogger)

(mount/defstate ^{:on-reload :noop}
                logger
                :start
                (start-stdoutlogger (env :logging))
                :stop
                (stop-stdoutlogger))

(defn stop-stdoutlogger []
  (info logger ::system.shutdown {:msg "Shutting down StdOutLogger; overwriting logger with NullLogger"})
  (->NullLogger))