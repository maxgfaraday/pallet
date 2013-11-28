(ns pallet.ssh.node-state.state-root
  (:require
   [clojure.test :refer :all]
   [pallet.ssh.node-state.state-root :refer :all]))

(deftest create-path-with-template-test
  (is (create-path-with-template "a/b/c/d" "/c/d")))
