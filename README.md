# erdos.routing

Minimalist routing library.

## Usage

```
(require [erdos.routing :refer :all])

(defreq GET "/api/users" (fn [req] {:status 200 :body ...}))
(defreq POST "/api/users" (fn [req] ...))
(defreq GET "/api/users/:user-id" (fn [req] (get-user (-> req :route-params :user-id))))

```

Supported actions: `GET`, `POST`, `PUT`, `DELETE`, `*` (any).

## License

Copyright © 2018 Janos Erdos

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
