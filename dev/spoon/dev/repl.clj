(ns spoon.dev.repl
  (:require
   [aero.core :as a]
   [clojure.java.io :as io]
   [gilmour.dev.repl :as g.repl]
   [spoon.app :as app]
   [spoon.edge.db :as eg.db]))

(defn load-config
  []
  (a/read-config (io/resource "spoon/config.edn") {:profile :dev}))

(comment

  (-> (load-config)
      (app/system)
      (:db-generator)
      (eg.db/create!))

  (g.repl/set-init!
   (fn [_] (app/system (load-config))))

  (g.repl/start!)

  (g.repl/stop!)

  (g.repl/restart!)

  (-> (g.repl/get-system)
      (:db-migrator)
      (eg.db/migrate!))

  (eg.db/create-user! (:db (g.repl/get-system))
                      {:username "foobar"
                       :password "somerandompassword"})

  )
