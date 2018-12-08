# erdos.routing

Minimalist routing library.

## Version

[![Clojars Project](https://img.shields.io/clojars/v/io.github.erdos/routing.svg)](https://clojars.org/io.github.erdos/routing)

## Usage

First, define the routes.

```
(require [erdos.routing :refer :all])

(defreq GET "/api/users" (fn [req] {:status 200 :body ...}))
(defreq POST "/api/users" (fn [req] ...))
(defreq GET "/api/users/:user-id" (fn [req] (get-user (-> req :route-params :user-id))))

```

Supported actions: `GET`, `POST`, `PUT`, `DELETE`, `*` (any).

Then, call `get-handler` on a request map to get the handler function or call `handle-routes` to call the specific ring handler. The rounting data will be found under the keys `:route-params` an `:route-pattern`. You can also use the dynamic var `*request*` to access the request map.

## License

Copyright © 2018 Janos Erdos

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
