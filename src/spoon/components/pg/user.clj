(ns spoon.components.pg.user
  (:require
   [honeysql.core :as sql]
   [honeysql.helpers :as sql.help]
   [spoon.components.pg.common :as c.pg.common]))

(defn- crypt
  [s]
  (sql/call :crypt s (sql/call :gen_salt (sql/inline "'bf'"))))

(defn- with-exist-only
  [m]
  (sql.help/merge-where m [:= :is_deleted false]))

(defn- with-matching-password
  [m password]
  (sql.help/merge-where
   m
   [:= :password (sql/call :crypt password (sql/inline "password"))]))

(defn register-user
  [user]
  (let [as-row (juxt :username :password)]
    (-> (sql.help/insert-into :users)
        (sql.help/columns :username :password)
        (sql.help/values [(-> user (update :password crypt) (as-row))])
        (c.pg.common/returning :id :username :date_created :is_deleted))))

(defn find-user-by-username
  [username]
  (-> (sql.help/select :id :username :date_created :is_deleted)
      (sql.help/from :users)
      (sql.help/where [:= :username username])
      (with-exist-only)))

(defn delete-user-by-username
  [username]
  (-> (sql.help/update :users)
      (sql.help/sset {:is_deleted true})
      (sql.help/where [:= :username username])
      (with-exist-only)))

(defn reset-user-password-by-username
  [username {:keys [new-password]}]
  (-> (sql.help/update :users)
      (sql.help/sset {:password (crypt new-password)})
      (sql.help/where [:= :username username])
      (with-exist-only)))

(defn update-user-password-by-username
  [username {:keys [old-password] :as params}]
  (-> (reset-user-password-by-username username params)
      (with-matching-password old-password)))

(defn find-user-by-credential
  [{:keys [username password]}]
  (-> (find-user-by-username username)
      (with-matching-password password)))
