(ns ^:figwheel-no-load train-race.app
  (:require [train-race.core :as core]
            [devtools.core :as devtools]
            [figwheel.client :as figwheel :include-macros true]))

(enable-console-print!)

(figwheel/watch-and-reload
  :websocket-url "ws://localhost:3449/figwheel-ws")

(devtools/install!)

(core/start!)
