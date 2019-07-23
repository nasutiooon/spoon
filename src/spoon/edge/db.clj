(ns spoon.edge.db
  (:require
   [gilmour.migratus :as g.migratus]
   [gilmour.postgres :as g.pg]
   [gilmour.sql :as g.sql]
   [jdbc.core :as jdbc]
   [migratus.core :as migratus]
   [spoon.components.pg.common :as c.pg.common]
   [spoon.components.pg.user :as c.pg.user])
  (:import
   [gilmour.migratus Migratus]
   [gilmour.postgres Postgres PostgresGenerator]))

(defprotocol UserRepository
  (create-user! [this params])
  (find-user-by-username [this username])
  (delete-user-by-username! [this username])
  (update-user-password-by-username! [this username params])
  (reset-user-password-by-username! [this username params]))

(defprotocol AuthService
  (find-user-by-credential [this credential]))

(defprotocol Migratable
  (migrate! [this])
  (rollback! [this]))

(defprotocol Creatable
  (create! [this])
  (destroy! [this]))

(defn- get-connection
  [component]
  (jdbc/connection (g.sql/pool component)))

(extend-protocol UserRepository
  Postgres
  (create-user! [this params]
    (with-open [conn (get-connection this)]
      (c.pg.common/fetch-one conn (c.pg.user/register-user params))))
  (find-user-by-username [this username]
    (with-open [conn (get-connection this)]
      (let [m (c.pg.user/find-user-by-username username)]
        (c.pg.common/fetch-one conn m))))
  (delete-user-by-username! [this username]
    (with-open [conn (get-connection this)]
      (let [m (c.pg.user/delete-user-by-username username)]
        (c.pg.common/execute! conn m))))
  (update-user-password-by-username! [this username params]
    (with-open [conn (get-connection this)]
      (let [m (c.pg.user/update-user-password-by-username username params)]
        (c.pg.common/execute! conn m))))
  (reset-user-password-by-username! [this username params]
    (with-open [conn (get-connection this)]
      (let [m (c.pg.user/reset-user-password-by-username username params)]
        (c.pg.common/execute! conn m)))))

(extend-protocol AuthService
  Postgres
  (find-user-by-credential [this credential]
    (with-open [conn (get-connection this)]
      (let [m (c.pg.user/find-user-by-credential credential)]
        (c.pg.common/fetch-one conn m)))))

(extend-protocol Migratable
  Migratus
  (migrate! [this]
    (g.migratus/migrate! this))
  (rollback! [this]
    (g.migratus/rollback! this)))

(extend-protocol Creatable
  PostgresGenerator
  (create! [this]
    (g.pg/create! this))
  (destroy! [this]
    (g.pg/destroy! this)))
