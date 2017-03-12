(ns train-race.log
  (:require #?@(:clj
                [[clojure.tools.logging :as log]
                 [train-race.config :refer [env]]
                 [mount.core :as mount]]))
  #?(:cljs
     (:require-macros
       [alter-cljs.core :refer [alter-var-root]])))

(defonce log-level (atom :debug))

(defonce log-level-enum
         {:trace 0
          :debug 1
          :info 2
          :warn 3
          :error 4
          :fatal 5})

(defn- >=enum [enum state this-level]
  (>= (enum this-level) (enum @state)))

(def log-this-level? (partial >=enum log-level-enum log-level))

(defn set-log-level! [lvl]
  (assert (#{:trace :debug :info :warn :error :fatal} lvl) (str "Unknown logging level: " lvl))
  (reset! log-level lvl))

(defprotocol Logger
  (log [this msg]))

(defrecord StdOutLogger [] Logger
  (log [this {:keys [service msg level ex]}]
    (when (log-this-level? level)
      (case level
       :trace
       #?(:clj  (log/trace service " :: " msg)
          :cljs (js/console.log (name service) " :: " msg))

       :debug
       #?(:clj  (log/debug service " :: " msg)
          :cljs (js/console.log (name service) " :: " msg))

       :info
       #?(:clj  (log/info service " :: " msg)
          :cljs (js/console.info (name service) " :: " msg))

       :warn
       #?(:clj  (log/warn service " :: " msg)
          :cljs (js/console.warn (name service) " :: " msg))

       :error
       #?(:clj  (if ex (log/error ex service " :: " msg) (log/error service " :: " msg))
          :cljs (do
                  (if ex
                    (let [msg (str msg " | " (.-message ex))
                          stack (.-stack ex)]
                      (js/console.error (name service) " :: " msg)
                      (when stack (js/console.error "Error stacktrace\n" (pr-str stack))))
                    (js/console.error (name service) " :: " msg))
                  (js/console.trace)))

       :fatal
       #?(:clj  (if ex (log/fatal ex service " :: " msg) (log/fatal service " :: " msg))
          :cljs (do
                  (if ex
                    (let [msg (str msg " | " (.-message ex))
                          stack (.-stack ex)]
                      (js/console.error (name service) " :: " msg)
                      (when stack (js/console.error "Error stacktrace\n" (pr-str stack))))
                    (js/console.error (name service) " :: " msg))
                  (js/console.trace)))))))

(defrecord NullLogger [] Logger
  (log [this msg] nil))

(defn trace [logger service {:keys [msg]}]
  (if logger
    (log logger {:service service :level :trace :msg msg})
    #?(:clj (log/error "CALLED LOGGING WITHOUT INITIALISED LOGGER")
       :cljs (js/console.error "CALLED LOGGING WITHOUT INITIALISED LOGGER"))))

(defn debug [logger service {:keys [msg]}]
  (if logger
    (log logger {:service service :level :error :msg msg})
    #?(:clj (log/error "CALLED LOGGING WITHOUT INITIALISED LOGGER")
       :cljs (js/console.error "CALLED LOGGING WITHOUT INITIALISED LOGGER"))))

(defn info [logger service {:keys [msg]}]
  (if logger
    (log logger {:service service :level :info :msg msg})
    #?(:clj (log/error "CALLED LOGGING WITHOUT INITIALISED LOGGER")
       :cljs (js/console.error "CALLED LOGGING WITHOUT INITIALISED LOGGER"))))

(defn warn [logger service {:keys [msg]}]
  (if logger
    (log logger {:service service :level :warn :msg msg})
    #?(:clj (log/error "CALLED LOGGING WITHOUT INITIALISED LOGGER")
       :cljs (js/console.error "CALLED LOGGING WITHOUT INITIALISED LOGGER"))))

(defn error
  [logger service {:keys [msg ex]}]
  (if logger
    (log logger {:service service :level :error :msg msg
                :exception ex})
    #?(:clj (log/error "CALLED LOGGING WITHOUT INITIALISED LOGGER")
       :cljs (js/console.error "CALLED LOGGING WITHOUT INITIALISED LOGGER"))))

(defn failure
  [logger service {:keys [msg ex]}]
  (if logger
    (log logger {:service service :level :trace :msg msg
                :exception ex})
    #?(:clj (log/error "CALLED LOGGING WITHOUT INITIALISED LOGGER")
       :cljs (js/console.error "CALLED LOGGING WITHOUT INITIALISED LOGGER"))))

#?(:clj (declare stop-stdoutlogger!))
#?(:clj (declare start-stdoutlogger!))

#?(:clj
   (mount/defstate ^{:on-reload :noop}
   logger
     :start
     (start-stdoutlogger! (env :logging))
     :stop
     (stop-stdoutlogger!))

   :cljs
   (def logger nil))

(defn start-stdoutlogger! [config]
  (let [logger* (->StdOutLogger)
        level (or :debug (:logging-level config))]
    (set-log-level! level)
    (info logger* ::system.startup {:msg "StdOutLogger initialising"})
    #?(:cljs (do (alter-var-root #'logger (constantly logger*)) logger)
       :clj logger*)))

(defn stop-stdoutlogger! []
  (info logger ::system.shutdown {:msg "Shutting down StdOutLogger; overwriting logger with NullLogger"})
  (let [logger* (->NullLogger)]
    #?(:cljs (do (alter-var-root #'logger (constantly logger*)) logger)
       :clj logger*)))