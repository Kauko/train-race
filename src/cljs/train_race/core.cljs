(ns train-race.core
  (:require [train-race.communication :as comms]
            [train-race.game.board :as board]
            [train-race.log :as log]))

(defn start! []
  (log/start-stdoutlogger! {})
  (comms/start-websocket-connection!)
  (board/initialise-board!))