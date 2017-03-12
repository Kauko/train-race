(ns train-race.game.board
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [train-race.communication :as comms]
            [train-race.log :refer [logger]]
            [train-race.log :as log]))

(defonce state (atom {}))

(defn setup []
  ; Set frame rate to 30 frames per second.
  (q/frame-rate 30)
  ; Set color mode to HSB (HSV) instead of default RGB.
  (q/color-mode :hsb)
  ; setup function returns initial state. It contains
  ; circle color and position.
  {:color 0
   :angle 0})

(defn update-state [atom]
  ; Update sketch state by changing circle color and position.
  (fn [state]
    {:color (:color @atom)
     :angle (+ (:angle state) 0.1)}))

(defn draw-state [state]
  ; Clear the sketch by filling it with light-grey color.
  (q/background 240)
  ; Set circle color.
  (q/fill (:color state) 255 255)
  ; Calculate x and y coordinates of the circle.
  (let [angle (:angle state)
        x (* 150 (q/cos angle))
        y (* 150 (q/sin angle))]
    ; Move origin point to the center of the sketch.
    (q/with-translation [(/ (q/width) 2)
                         (/ (q/height) 2)]
                        ; Draw the circle.
                        (q/ellipse x y 100 100))))

(defn initialise-board! []
  (log/info logger ::system.startup {:msg "Initialising quil board"})
  (q/defsketch quilcljs
               :host "quilcljs"
               :size [500 500]
               ; setup function called only once, during sketch initialization.
               :setup setup
               ; update-state is called on each iteration before draw-state.
               :update (update-state state)
               :draw draw-state
               ; This sketch uses functional-mode middleware.
               ; Check quil wiki for more info about middlewares and particularly
               ; fun-mode.
               :middleware [m/fun-mode])

  (log/info logger ::system.startup {:msg "Registering event listeners"})

  (comms/register-event-listener!
    :board/new-color
    (fn [{:keys [color]}]
      (swap! state assoc :color color))))
