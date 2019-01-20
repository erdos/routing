(ns erdos.routing)

(defonce +default-routes+ {})

(def request-methods '#{GET POST PUT DELETE PATCH})
(def ^:dynamic *request*)


(defn defreq-fn
  ([method url return-val]
   (defreq-fn #'+default-routes+ ~method ~url ~return-val))
  ([routing method url return-val]
   (assert (var? routing))
   (assert (symbol? method))
   (assert (string? url))
   (assert (.startsWith (str url) "/")
           "Url must start with '/' character!")
   (assert (or (contains? '#{ANY *} method)
               (contains? request-methods method))
           (str "Unexpected request method: " method))
   (if ('#{* ANY} method)
     (doseq [m request-methods]
       (defreq-fn routing m url return-val))
     (let [method (keyword (.toLowerCase (name method)))
           r (for [itm (.split (str url) "/"), :when (seq itm)]
               (if (.startsWith (str itm) ":")
                 (keyword (.substring (str itm) 1))
                 (str itm)))
           assoc-path (map #(if (keyword? %) :* %) r)
           ks (filter keyword? r)
           manual-meta (meta return-val)
           path (concat [method] assoc-path [:end])]
       (alter-var-root routing assoc-in path
                       {:fn (if manual-meta
                              (with-meta return-val manual-meta)
                              return-val)
                        :pt (str url)
                        :ks (vec ks)})))))

(defmacro defreq
  ([method url return-val]
   `(defreq +default-routes+ ~method ~url ~return-val))
  ([routing method url return-val]
   (let [routing (if (symbol? routing) (resolve routing) routing)]
     `(defreq-fn '~routing '~method '~url ~return-val))))

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

(defn wrap-routing-meta [handler]
  (fn [request]
    (if-let [{:keys [handler route-params route-pattern]} (get-handler req)]
      (binding [*request* (assoc req :route-params route-params :route-pattern route-pattern)]
        (handler *request*))
      {:status 404 :body "Not Found"})))
