(ns spoon.test.components.spoon-client.request
  (:require
   [clj-http.client :as client]
   [reitit.core :as reitit]
   [spoon.edge.signer :as eg.signer]
   [spoon.routes.main :as r.main]
    [spoon.routes.user :as r.user]))

(defn- build-url
  ([{:keys [host-uri router]} handler path-params]
   (let [-router (:router router)
         path    (:path (reitit/match-by-name -router handler path-params))]
     (str host-uri path)))
  ([{:keys [host-uri router]} handler]
   (let [-router (:router router)
         path    (:path (reitit/match-by-name -router handler))]
     (str host-uri path))))

(defn- wrap-response
  [option {:keys [content-type]}]
  (assoc option :accept content-type :as content-type))

(defn- wrap-form-params
  [{:keys [form-params] :as option} {:keys [content-type]}]
  (if form-params
    (assoc option :content-type content-type)
    option))

(defn- assoc-authz-token
  [option scheme token]
  (assoc-in option [:headers "authorization"] (str scheme " " token)))

(defn- wrap-app-token-auth
  [{:keys [app-auth] :as option} {:keys [guardian]}]
  (if app-auth
    (let [token (if (string? app-auth)
                  app-auth
                  (eg.signer/app-token guardian app-auth))]
      (-> option
          (assoc-authz-token "SpoonApp" token)
          (dissoc :app-auth)))
    option))

(defn- wrap-reset-token-auth
  [{:keys [reset-auth] :as option} {:keys [passport]}]
  (if reset-auth
    (let [token (if (string? reset-auth)
                  reset-auth
                  (eg.signer/reset-token passport reset-auth))]
      (-> option
          (assoc-authz-token "SpoonReset" token)
          (dissoc :reset-auth)))
    option))

(defn- wrap-option
  [option settings]
  (-> option
      (wrap-response settings)
      (wrap-form-params settings)
      (wrap-app-token-auth settings)
      (wrap-reset-token-auth settings)))

(defn- request
  [f]
  (fn
    ([http-client handler path-params option]
     (let [option (wrap-option option http-client)
           url    (build-url http-client handler path-params)]
       (try
         (f url option)
         (catch Exception e
           (ex-data e)))))
    ([http-client handler option]
     (let [option (wrap-option option http-client)
           url    (build-url http-client handler)]
       (try
         (f url option)
         (catch Exception e
           (ex-data e)))))))

(def GET    (request client/get))
(def POST   (request client/post))
(def PUT    (request client/put))
(def DELETE (request client/delete))

(defn medical-checkup
  [spleen-client option]
  (GET spleen-client ::r.main/health option))

(defn request-reset-token
  [spleen-client username option]
  (POST spleen-client ::r.main/app {:username username} option))

(defn register-user
  [spleen-client option]
  (POST spleen-client ::r.user/anon option))

(defn request-auth-token
  [spleen-client username option]
  (POST spleen-client ::r.user/target {:username username} option))

(defn delete-user
  [spleen-client username option]
  (DELETE spleen-client ::r.user/target {:username username} option))

(defn update-user-password
  [spleen-client username option]
  (PUT spleen-client ::r.user/target {:username username} option))

(defn reset-user-password
  [spleen-client username option]
  (PUT spleen-client ::r.user/forget {:username username} option))
