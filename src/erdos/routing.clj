(ns erdos.routing)

(defonce +default-routes+ {})

(def request-methods '#{GET POST PUT DELETE PATCH})
(def ^:dynamic *request*)

(defn defreq'
  ([method url return-val]
   (defreq' #'+default-routes+ ~method ~url ~return-val))
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
       (defreq' routing m url return-val))
     (let [method (keyword (.toLowerCase (name method)))
           r (for [itm (.split (str url) "/"), :when (seq itm)]
               (if (.startsWith (str itm) ":")
                 (keyword (.substring (str itm) 1))
                 (str itm)))
           assoc-path  (map #(if (keyword? %) :* %) r)
           ks          (filter keyword? r)
           manual-meta (meta return-val)
           path        (concat [method] assoc-path [:end])]
       (alter-var-root routing assoc-in path
                       {:fn return-val
                        :mm manual-meta
                        :pt (str url)
                        :ks (vec ks)})))))

(defmacro defreq
  ([method url return-val]
   `(defreq +default-routes+ ~method ~url ~return-val))
  ([routing method url return-val]
   (let [routing (if (symbol? routing) (resolve routing) routing)]
     `(defreq' '~routing '~method '~url ~return-val))))

(defn- get-handler-step [url routing-map params]
  (if-let [[u & url :as url-rest] (seq url)]
    (or (when (contains? routing-map u)
          (get-handler-step url (get routing-map u) params))
        (when-let [any-route (:* routing-map)]
          (or (get-handler-step url any-route (conj params u))
              (get-handler-step nil any-route (conj params (clojure.string/join "/" url-rest))))))
    (when-let [end (:end routing-map)]
      {:route/handler (:fn end)
       :route/meta    (:mm end)
       :route/pattern (:pt end)
       :route/params  (zipmap (:ks end) params)})))

;; get map containing :route/handler, :route/meta, :route/pattern, :route/params
(defn get-handler
  ([request] (get-handler +default-routes+ request))
  ([routing-map {:keys [request-method uri]}]
   (assert (map? routing-map) (str "Wrong routing map."))
   (let [route-parts (remove empty? (.split (str uri) "/"))]
     (get-handler-step route-parts (get routing-map request-method) []))))

(def not-found {:status 404 :body "Not Found"})

(defn wrap-routing [handler & {:keys [routes]}]
  (fn 
    ([request]
     (binding [*request* (into request (get-handler (or routes +default-routes+) request))]
       (handler *request*)))
    ([request success error]
     (binding [*request* (into request (get-handler (or routes +default-routes+) request))]
       (handler *request* success error)))))

(defn routing-handler
  ([request]
   (if-let [h (:route/handler request)]
     (h request)
     not-found))
   ([request success error]
    (if-let [h (:route/handler request)]
      (h request success error)
      (success not-found))))

(def handle-routes (wrap-routing routing-handler))