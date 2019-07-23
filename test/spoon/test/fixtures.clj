(ns spoon.test.fixtures
  (:require
   [aero.core :refer [read-config]]
   [clojure.java.io :as io]
   [clojure.test :as t]
   [com.stuartsierra.component :as c]
   [gilmour.dev.test :as g.test]
   [spoon.app :as app]
   [spoon.edge.db :as eg.db]
   [spoon.test.components.spoon-client :as t.c.client]))

(defn- load-config
  []
  (read-config (io/resource "spoon/config.edn") {:profile :test}))

(defn- system
  [{:spoon/keys [spoon-client] :as config}]
  (-> (app/system config)
      (assoc :spoon-client (t.c.client/spoon-client spoon-client))
      (c/system-using
       {:spoon-client [:guardian :passport]})
      (c/system-using
       {:spoon-client {:router :ring-router}})))

(defn- reset-db!
  [f]
  (let [db-generator (:db-generator (system (load-config)))]
    (eg.db/create! db-generator)
    (f)
    (eg.db/destroy! db-generator)))

(def ^:private -with-system
  (g.test/with-system #(system (load-config))))

(defn- migrate-db!
  [f]
  (eg.db/migrate! (:db-migrator (g.test/get-system)))
  (f))

(def with-system
  (t/join-fixtures [reset-db! -with-system migrate-db!]))
