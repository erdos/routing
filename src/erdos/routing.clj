(ns erdos.routing)

(defonce +default-routes+ {})

(def request-methods '#{GET POST PUT DELETE PATCH})
(def ^:dynamic *request*)

(defmacro defreq
  ([method url return-val]
   `(defreq +default-routes+ ~method ~url ~return-val))
  ([routing method url return-val]
   (assert (symbol? routing))
   (assert (symbol? method))
   (assert (string? url))
   (assert (.startsWith (str url) "/")
           "Url must start with '/' character!")
   (assert (or (contains? '#{ANY *} method)
               (contains? request-methods method))
           (str "Unexpected request method: " method))
   (if ('#{* ANY} method)
     (cons 'do (for [m request-methods] `(defreq ~routing ~m ~url ~return-val)))
     (let [method (keyword (.toLowerCase (name method)))
           r (for [itm (.split (str url) "/"), :when (seq itm)]
               (if (.startsWith (str itm) ":")
                 (keyword (.substring (str itm) 1))
                 (str itm)))
           assoc-path (map #(if (keyword? %) :* %) r)
           ks (filter keyword? r)]
       `(let [handler# ~return-val]
          (alter-var-root (var ~routing) assoc-in [~method ~@assoc-path :end]
                          {:fn handler# :ks ~(vec ks) :pt ~url})
          (var ~routing))))))

(defn- get-handler-step [url routing-map params]
  (if-let [[u & url :as url-rest] (seq url)]
    (or (when (contains? routing-map u)
          (get-handler-step url (get routing-map u) params))
        (when-let [any-route (:* routing-map)]
          (or (get-handler-step url any-route (conj params u))
              (get-handler-step nil any-route (conj params (clojure.string/join "/" url-rest))))))
    (when-let [end (:end routing-map)]
      {:handler       (:fn end)
       :route-pattern (:pt end)
       :route-params  (zipmap (:ks end) params)})))

(defn get-handler
  ([request] (get-handler +default-routes+ request))
  ([routing-map {:keys [request-method uri]}]
   (assert (map? routing-map))
   (let [route-parts (remove empty? (.split (str uri) "/"))]
     (get-handler-step route-parts (get routing-map request-method) []))))


(defn handle-routes
  ([req]
   (if-let [{:keys [handler route-params route-pattern]} (get-handler req)]
     (binding [*request* (assoc req :route-params route-params :route-pattern route-pattern)]
       (handler *request*))
     {:status 404 :body "Not Found"}))
  ([req success error]
   (if-let [{:keys [handler route-params]} (get-handler req)]
     (binding [*request* (assoc req :route-params route-params)]
       (handler *request* success error))
     (success {:status 404 :body "Route Not Found"}))))
