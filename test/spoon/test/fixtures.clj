(ns spoon.test.fixtures
  (:require
   [aero.core :refer [read-config]]
   [clojure.java.io :as io]
   [clojure.test :as t]
   [com.stuartsierra.component :as c]
   [gilmour.dev.test :as g.test]
   [spoon.components.db :as c.db]
   [spoon.dev.repl :as d.repl]
   [spoon.test.components.spoon-client :as t.c.client]))

(defn- load-config
  []
  (read-config (io/resource "spoon/config.edn") {:profile :test}))

(defn- system
  [{:spoon/keys [db spoon-client] :as config}]
  (-> (d.repl/system config)
      (assoc :db-generator-runner (c.db/db-part :db-generator-runner db)
             :spoon-client (t.c.client/spoon-client spoon-client))
      (c/system-using
       {:db-generator-runner [:db-generator]
        :db-impl             [:db-generator-runner]
        :spoon-client        [:guardian :passport]})
      (c/system-using
       {:spoon-client {:router :ring-router}})))

(def with-system
  (g.test/with-system #(system (load-config))))
