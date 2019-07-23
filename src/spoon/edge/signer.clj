(ns spoon.edge.signer
  (:require
   [clj-time.core :as time]
   [gilmour.jwt-encoder :as g.jwt]))

(defn- assoc-time
  [claims duration]
  (let [now (time/now)]
    (cond-> (assoc claims :iat now)
      duration (assoc :exp (time/plus now (time/seconds duration))))))

(defn- assoc-kind
  [claims kind]
  (assoc claims :knd kind))

(defn- parse-user-id
  [{:keys [id] :as user}]
  (letfn [(parse-id [x] (try (java.util.UUID/fromString x)
                             (catch Exception _ nil)))]
    (cond-> user
      id (update :id parse-id))))

(defn auth-token
  [signer claims]
  (g.jwt/encode signer (-> claims
                           (select-keys [:id :username])
                           (assoc-time (-> signer :duration :auth))
                           (assoc-kind "auth"))))

(defn reset-token
  [signer claims]
  (g.jwt/encode signer (-> claims
                           (select-keys [:id :username])
                           (assoc-time (-> signer :duration :reset))
                           (assoc-kind "reset"))))

(defn app-token
  [signer claims]
  (g.jwt/encode signer (-> claims
                           (select-keys [:username])
                           (assoc-time (-> signer :duration :reset)))))

(defn read-app-token
  [signer token]
  (when-let [payload (try
                       (g.jwt/decode signer token)
                       (catch Exception _ nil))]
    (parse-user-id payload)))

(defn read-reset-token
  [signer token]
  (when-let [payload (try
                       (g.jwt/decode signer token)
                       (catch Exception _ nil))]
    (when (= (:knd payload) "reset")
      (parse-user-id payload))))
