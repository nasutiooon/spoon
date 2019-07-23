(ns spoon.routes.user
  (:require
   [ring.util.http-response :as res]
   [ring.util.http-status :as st]
   [spoon.components.router :as c.router]
   [spoon.edge.db :as eg.db]
   [spoon.edge.signer :as eg.signer]
   [spoon.middleware.auth :as m.auth]))

(defn- register-user
  [{{:keys [user]} :body-params
    :keys          [db passport]}]
  (let [created (eg.db/create-user! db user)
        token   (eg.signer/auth-token passport created)]
    (res/ok {:token {:auth-token token}})))

(defn- request-auth-token
  [{:keys [passport identity]}]
  (let [token (eg.signer/auth-token passport identity)]
    (res/ok {:token {:auth-token token}})))

(defn- delete-user
  [{{:keys [username]} :path-params
    :keys              [db]}]
  (eg.db/delete-user-by-username! db username)
  (res/no-content))

(defn- update-user-password
  [{{:keys [user]}     :body-params
    {:keys [username]} :path-params
    :keys              [db passport identity]}]
  (let [result (eg.db/update-user-password-by-username! db username user)]
    (if (pos-int? result)
      (let [token (eg.signer/auth-token passport identity)]
        (res/ok {:token {:auth-token token}}))
      (res/forbidden))))

(defn- reset-user-password
  [{{:keys [user]}     :body-params
    {:keys [username]} :path-params
    :keys              [db passport identity]}]
  (let [result (eg.db/reset-user-password-by-username! db username user)
        token  (eg.signer/auth-token passport identity)]
    (res/ok {:token {:auth-token token}})))

(defn user-routes-hook
  []
  (c.router/routes-hook
   {:routes
    [["/api/user"
      {:name ::anon
       :post {:responses  {st/ok {:body {:token {:auth-token string?}}}}
              :parameters {:body {:user {:username string?
                                         :password string?}}}
              :handler    register-user}}]
     ["/api/user/:username"
      {:name       ::target
       :parameters {:path {:username string?}}
       :responses  {st/unauthorized {}
                    st/forbidden    {}}
       :post       {:responses {st/ok {:body {:token {:auth-token string?}}}}
                    :handler   request-auth-token}
       :put        {:responses  {st/ok {:body {:token {:auth-token string?}}}}
                    :parameters {:body {:user {:new-password string?
                                               :old-password string?}}}
                    :handler    update-user-password}
       :delete     {:responses {st/no-content {}}
                    :handler   delete-user}
       :middleware [m.auth/basic-auth-middleware
                    m.auth/auth-middleware
                    m.auth/creator-middleware]}]
     ["/api/user/:username/forgot"
      {:name       ::forget
       :parameters {:path {:username string?}}
       :responses  {st/unauthorized {}
                    st/forbidden    {}}
       :put        {:responses  {st/ok {:body {:token {:auth-token string?}}}}
                    :parameters {:body {:user {:new-password string?}}}
                    :handler    reset-user-password}
       :middleware [m.auth/reset-auth-middleware
                    m.auth/auth-middleware
                    m.auth/creator-middleware]}]]}))
