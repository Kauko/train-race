(ns train-race.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [train-race.core-test]))

(doo-tests 'train-race.core-test)

