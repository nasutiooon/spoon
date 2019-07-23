(ns spoon.middleware.auth
  (:require
   [buddy.auth :refer [authenticated?]]
   [buddy.auth.backends :as backend]
   [buddy.auth.middleware :as auth]
   [ring.util.http-response :as res]
   [spoon.edge.db :as eg.db]
   [spoon.edge.signer :as eg.signer]))

(def auth-middleware
  {:name        ::auth
   :description "middleware to check if a user is authenticated"
   :wrap        (fn [handler]
                  (fn [request]
                    (if (authenticated? request)
                      (handler request)
                      (res/unauthorized))))})

(def creator-middleware
  {:name        ::creator
   :description "middleware to check if you are accessing your own resource"
   :wrap        (fn [handler]
                  (fn [{{path-username :username} :path-params
                       {self-username :username} :identity
                       :as                       request}]
                    (if (or (not (authenticated? request))
                            (nil? path-username)
                            (nil? self-username)
                            (= path-username self-username))
                      (handler request)
                      (res/forbidden))))})

(def basic-auth-middleware
  {:name        ::basic
   :description "middleware to authenticate user with basic auth"
   :wrap        (fn [handler]
                  (let [authfn  (fn [{:keys [db]} credential]
                                  (eg.db/find-user-by-credential db credential))
                        backend (backend/basic {:authfn authfn})]
                    (auth/wrap-authentication handler backend)))})

(def reset-auth-middleware
  {:name        ::reset
   :description "middleware to authenticate user with reset claims auth"
   :wrap        (fn [handler]
                  (let [authfn  (fn [{:keys [passport]} token]
                                  (eg.signer/read-reset-token passport token))
                        backend (backend/token {:authfn     authfn
                                                :token-name "SpoonReset"})]
                    (auth/wrap-authentication handler backend)))})

(def app-auth-middleware
  {:name        ::app
   :description "middleware to authenticate user with app claims auth"
   :wrap        (fn [handler]
                  (let [authfn  (fn [{:keys [db guardian]} token]
                                  (some->>
                                   (eg.signer/read-app-token guardian token)
                                   (:username)
                                   (eg.db/find-user-by-username db)))
                        backend (backend/token {:authfn     authfn
                                                :token-name "SpoonApp"})]
                    (auth/wrap-authentication handler backend)))})
