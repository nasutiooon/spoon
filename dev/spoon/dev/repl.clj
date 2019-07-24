(ns spoon.dev.repl
  (:require
   [aero.core :as a]
   [clojure.java.io :as io]
   [com.stuartsierra.component :as c]
   [gilmour.dev.repl :as g.repl]
   [spoon.app :as app]
   [spoon.components.db :as c.db]))

(defn load-config
  []
  (a/read-config (io/resource "spoon/config.edn") {:profile :dev}))

(defn system
  [{:spoon/keys [db] :as config}]
  (-> (app/system config)
      (assoc :db-migrator-runner (c.db/db-part :db-migrator-runner db))
      (c/system-using
       {:db-migrator-runner [:db-migrator]})))

(comment

  (g.repl/set-init!
   (fn [_] (system (load-config))))

  (g.repl/start!)

  (g.repl/stop!)

  (g.repl/restart!)

  )
