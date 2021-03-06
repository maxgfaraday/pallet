(ns pallet.actions.direct.rsync
  (:require
   [clojure.tools.logging :as logging]
   [pallet.action :refer [get-action-options implement-action]]
   [pallet.actions :refer [rsync rsync-to-local]]
   [pallet.crate :refer [target-node]]
   [pallet.core.session :refer [admin-user target-ip]]
   [pallet.node :refer [ssh-port]]
   [pallet.script.lib :refer [sudo]]
   [pallet.stevedore :as stevedore :refer [fragment]]))

(def ^{:private true}
  cmd "/usr/bin/rsync -e '%s' -F -F %s %s %s@%s:%s")

(def ^{:private true}
  cmd-to-local "/usr/bin/rsync -e '%s' -F -F %s '%s@%s:%s' %s")

(defn default-options
  [session]
  {:r true :delete true :copy-links true
   :rsync-path (let [sudo-user (or (:sudo-user (get-action-options))
                                   (:sudo-user (admin-user session)))]
                 (fragment ((sudo :no-promt true :user ~sudo-user) "rsync")))
   :owner true
   :perms true})

(implement-action rsync :direct
                  {:action-type :script :location :origin}
                  [session from to {:keys [port] :as options}]
  (logging/debugf "rsync %s to %s" from to)
  (let [extra-options (dissoc options :port)
        ssh (str "/usr/bin/ssh -o \"StrictHostKeyChecking no\" "
                 (if-let [port (or port (ssh-port (target-node)))]
                   (format "-p %s" port)))
        cmd (format
             cmd ssh
             (stevedore/map-to-arg-string
              (merge (default-options session) extra-options))
             from (:username (admin-user session))
             (target-ip session) to)]
    (logging/debugf "rsync %s" cmd)
    [[{:language :bash}
      (stevedore/checked-commands (format "rsync %s to %s" from to) cmd)]
     session]))

(implement-action rsync-to-local :direct
                  {:action-type :script :location :origin}
                  [session from to {:keys [port] :as options}]
  (logging/debugf "rsync %s to %s" from to)
  (let [extra-options (dissoc options :port)
        ssh (str "/usr/bin/ssh -o \"StrictHostKeyChecking no\" "
                 (if-let [port (or port (ssh-port (target-node)))]
                   (format "-p %s" port)))
        cmd (format
             cmd-to-local ssh
             (stevedore/map-to-arg-string
              (merge (default-options session) {:delete false} extra-options))
             (:username (admin-user session))
             (target-ip session)
             from
             to)]
    (logging/debugf "rsync-to-local %s" cmd)
    [[{:language :bash}
      (stevedore/checked-commands (format "rsync %s to %s" from to) cmd)]
     session]))
