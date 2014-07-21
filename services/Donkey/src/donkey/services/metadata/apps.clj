(ns donkey.services.metadata.apps
  (:use [clojure-commons.validators :only [validate-map]]
        [donkey.auth.user-attributes :only [current-user]]
        [donkey.util :only [is-uuid?]]
        [korma.db :only [transaction]]
        [slingshot.slingshot :only [throw+ try+]])
  (:require [cemerick.url :as curl]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [donkey.clients.metadactyl :as metadactyl]
            [donkey.clients.osm :as osm]
            [donkey.persistence.jobs :as jp]
            [donkey.persistence.oauth :as op]
            [donkey.services.metadata.agave-apps :as aa]
            [donkey.services.metadata.combined-apps :as ca]
            [donkey.services.metadata.de-apps :as da]
            [donkey.util :as util]
            [donkey.util.config :as config]
            [donkey.util.db :as db]
            [donkey.util.service :as service]
            [mescal.de :as agave])
  (:import [java.util UUID]))

(defn- count-de-jobs
  [filter]
  (jp/count-de-jobs (:username-current-user) filter))

(defn- count-jobs
  [filter]
  (jp/count-jobs (:username current-user) filter))

(defn- job-timestamp
  [timestamp]
  (str (or (db/millis-from-timestamp timestamp) 0)))

(defn- format-job
  [app-tables job]
  {:analysis_details (:app-description job)
   :analysis_id      (:app-id job)
   :analysis_name    (:app-name job)
   :description      (:description job)
   :enddate          (job-timestamp (:end-date job))
   :id               (:id job)
   :name             (:job-name job)
   :resultfolderid   (:result-folder-path job)
   :startdate        (job-timestamp (:start-date job))
   :status           (:status job)
   :username         (:username job)
   :deleted          (:deleted job)
   :wiki_url         (:app-wiki-url job)
   :app_disabled     (:disabled (first (remove nil? (map #(% (:app-id job)) app-tables))))})

(defn- load-app-details
  [agave jobs]
  [(->> (filter (fn [{:keys [job-type]}] (= jp/de-job-type job-type)) jobs)
        (map :app-id)
        (da/load-app-details))
   (aa/load-app-details agave)])

(defn- list-all-jobs
  [agave limit offset sort-field sort-order filter]
  (let [user       (:username current-user)
        jobs       (jp/list-jobs user limit offset sort-field sort-order filter)
        app-tables (load-app-details agave jobs)]
    (remove nil? (map (partial format-job app-tables) jobs))))

(defn- list-de-jobs
  [limit offset sort-field sort-order filter]
  (let [user       (:username current-user)
        jobs       (jp/list-de-jobs user limit offset sort-field sort-order filter)
        app-tables [(da/load-app-details (map :analysis_id jobs))]]
    (mapv (partial format-job app-tables) jobs)))

(defn- update-job-status
  ([{:keys [id status end-date]}]
     (let [uuid     (UUID/fromString id)
           end-date (db/timestamp-from-str (str end-date))]
       (jp/update-job-step uuid id status end-date)
       (jp/update-job uuid status end-date)))
  ([agave username job job-step status end-time]
     (let [max-step-number (jp/get-max-step-number (:id job))]
       (aa/update-agave-job-status agave username job job-step max-step-number status end-time))))

(defn- unrecognized-job-type
  [job-type]
  (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
           :argument   "job_type"
           :value      job-type}))

(defn- process-job
  ([agave-client job-id processing-fns]
     (process-job agave-client job-id (jp/get-job-by-id (UUID/fromString job-id)) processing-fns))
  ([agave-client job-id job {:keys [process-agave-job process-de-job preprocess-job]
                             :or {preprocess-job identity}}]
     (when-not job
       (service/not-found "job" job-id))
     (if (util/is-uuid? (:analysis_id job))
       (process-de-job (preprocess-job job))
       (process-agave-job agave-client (preprocess-job job)))))

(defn- agave-authorization-uri
  [state-info]
  (let [username (:username current-user)
        state    (op/store-authorization-request username state-info)]
    (-> (curl/url (config/agave-oauth-base) "authorize")
        (assoc :query {:response_type "code"
                       :client_id     (config/agave-key)
                       :redirect-uri  (config/agave-redirect-uri)
                       :state         state})
        (str))))

(defn- agave-authorization-redirect
  [state-info]
  (throw+ {:error_code ce/ERR_TEMPORARILY_MOVED
           :location   (agave-authorization-uri state-info)}))

(defn- add-predicate
  [predicate f]
  (fn [& args]
    (when (predicate)
      (apply f args))))

(defprotocol AppLister
  "Used to list apps available to the Discovery Environment."
  (listAppGroups [_])
  (listApps [_ group-id params])
  (searchApps [_ search-term])
  (updateFavorites [_ app-id favorite?])
  (rateApp [_ app-id rating comment-id])
  (deleteRating [_ app-id])
  (getApp [_ app-id])
  (getAppDeployedComponents [_ app-id])
  (getAppDetails [_ app-id])
  (listAppDataObjects [_ app-id])
  (editWorkflow [_ app-id])
  (copyWorkflow [_ app-id])
  (submitJob [_ workspace-id submission])
  (countJobs [_ filter])
  (listJobs [_ limit offset sort-field sort-order filter])
  (syncJobStatus [_ job])
  (updateJobStatus [_ username job job-step status end-time])
  (getJobParams [_ job-id])
  (getAppRerunInfo [_ job-id]))
;; AppLister

(deftype DeOnlyAppLister []
  AppLister

  (listAppGroups [_]
    (metadactyl/get-only-app-groups))

  (listApps [_ group-id params]
    (metadactyl/apps-in-group group-id params))

  (searchApps [_ search-term]
    (metadactyl/search-apps search-term))

  (updateFavorites [_ app-id favorite?]
    (metadactyl/update-favorites app-id favorite?))

  (rateApp [_ app-id rating comment-id]
    (metadactyl/rate-app app-id rating comment-id))

  (deleteRating [_ app-id]
    (metadactyl/delete-rating app-id))

  (getApp [_ app-id]
    (metadactyl/get-app app-id))

  (getAppDeployedComponents [_ app-id]
    (metadactyl/get-deployed-components-in-app app-id))

  (getAppDetails [_ app-id]
    (metadactyl/get-app-details app-id))

  (listAppDataObjects [_ app-id]
    (metadactyl/list-app-data-objects))

  (editWorkflow [_ app-id]
    (metadactyl/edit-workflow app-id))

  (copyWorkflow [_ app-id]
    (metadactyl/copy-workflow app-id))

  (submitJob [_ workspace-id submission]
    (da/submit-job workspace-id submission))

  (countJobs [_ filter]
    (count-de-jobs filter))

  (listJobs [_ limit offset sort-field sort-order filter]
    (list-de-jobs limit offset sort-field sort-order filter))

  (syncJobStatus [_ job]
    (when (= (:job_type job) jp/de-job-type)
      (da/sync-de-job-status job)))

  (updateJobStatus [_ username job job-step status end-time]
    (da/update-job-status username job job-step status end-time))

  (getJobParams [_ job-id]
    (ca/get-job-params nil (jp/get-job-by-id (UUID/fromString job-id))))

  (getAppRerunInfo [_ job-id]
    (ca/get-app-rerun-info nil (jp/get-job-by-id (UUID/fromString job-id)))))
;; DeOnlyAppLister

(deftype DeHpcAppLister [agave-client user-has-access-token?]
  AppLister

  (listAppGroups [_]
    (-> (metadactyl/get-only-app-groups)
        (update-in [:groups] conj (.hpcAppGroup agave-client))))

  (listApps [_ group-id params]
    (if (= group-id (:id (.hpcAppGroup agave-client)))
      (.listApps agave-client)
      (metadactyl/apps-in-group group-id params)))

  (searchApps [_ search-term]
    (let [def-result {:template_count 0 :templates {}}
          de-apps    (metadactyl/search-apps search-term)
          hpc-apps   (if (user-has-access-token?)
                       (aa/search-apps agave-client search-term def-result)
                       def-result)]
      {:template_count (apply + (map :template_count [de-apps hpc-apps]))
       :templates      (mapcat :templates [de-apps hpc-apps])}))

  (updateFavorites [_ app-id favorite?]
    (if (is-uuid? app-id)
      (metadactyl/update-favorites app-id favorite?)
      (throw+ {:error_code ce/ERR_BAD_REQUEST
               :reason     "HPC apps cannot be marked as favorites"})))

  (rateApp [_ app-id rating comment-id]
    (if (is-uuid? app-id)
      (metadactyl/rate-app app-id rating comment-id)
      (throw+ {:error_code ce/ERR_BAD_REQUEST
               :reason     "HPC apps cannot be rated"})))

  (deleteRating [_ app-id]
    (if (is-uuid? app-id)
      (metadactyl/delete-rating app-id)
      (throw+ {:error_code ce/ERR_BAD_REQUEST
               :reason     "HPC apps cannot be rated"})))

  (getApp [_ app-id]
    (ca/get-app agave-client app-id))

  (getAppDeployedComponents [_ app-id]
    (if (is-uuid? app-id)
      (metadactyl/get-deployed-components-in-app app-id)
      {:deployed_components [(.getAppDeployedComponent agave-client app-id)]}))

  (getAppDetails [_ app-id]
    (if (is-uuid? app-id)
      (metadactyl/get-app-details app-id)
      (.getAppDetails agave-client app-id)))

  (listAppDataObjects [_ app-id]
    (if (is-uuid? app-id)
      (metadactyl/list-app-data-objects app-id)
      (.listAppDataObjects agave-client app-id)))

  (editWorkflow [_ app-id]
    (aa/add-workflow-templates agave-client (metadactyl/edit-workflow app-id)))

  (copyWorkflow [_ app-id]
    (aa/add-workflow-templates agave-client (metadactyl/copy-workflow app-id)))

  (submitJob [_ workspace-id submission]
    (ca/submit-job agave-client workspace-id submission))

  (countJobs [_ filter]
    (count-jobs filter))

  (listJobs [_ limit offset sort-field sort-order filter]
    (if (user-has-access-token?)
      (list-all-jobs agave-client limit offset sort-field sort-order filter)
      (list-de-jobs limit offset sort-field sort-order filter)))

  (syncJobStatus [_ job]
    (let [sync-agave (add-predicate user-has-access-token? aa/sync-agave-job-status)]
      (process-job agave-client (:id job) job
                   {:process-de-job    da/sync-de-job-status
                    :process-agave-job sync-agave})))

  (updateJobStatus [_ username job job-step status end-time]
    (ca/update-job-status agave-client username job job-step status end-time))

  (getJobParams [_ job-id]
    (process-job agave-client job-id
                 {:process-de-job    (partial ca/get-job-params agave-client)
                  :process-agave-job aa/get-agave-job-params}))

  (getAppRerunInfo [_ job-id]
    (process-job agave-client job-id
                 {:process-de-job    (partial ca/get-app-rerun-info agave-client)
                  :process-agave-job aa/get-agave-app-rerun-info})))
;; DeHpcAppLister

(defn- has-access-token
  [{:keys [api-name] :as server-info} username]
  (seq (op/get-access-token api-name username)))

(defn- get-access-token
  [{:keys [api-name] :as server-info} state-info username]
  (if-let [token-info (op/get-access-token api-name username)]
    (assoc (merge server-info token-info)
      :token-callback  (partial op/store-access-token api-name username)
      :reauth-callback (partial agave-authorization-redirect state-info))
    (agave-authorization-redirect state-info)))

(defn- get-de-hpc-app-lister
  [state-info username]
  (DeHpcAppLister. (agave/de-agave-client-v2
                    (config/agave-base-url)
                    (config/agave-storage-system)
                    (partial get-access-token (config/agave-oauth-settings) state-info username)
                    (config/agave-jobs-enabled))
                   (partial has-access-token (config/agave-oauth-settings) username)))

(defn- get-app-lister
  ([]
     (get-app-lister ""))
  ([state-info]
     (get-app-lister state-info (:username current-user)))
  ([state-info username]
     (if (config/agave-enabled)
       (get-de-hpc-app-lister state-info username)
       (DeOnlyAppLister.))))

(defn get-only-app-groups
  []
  (service/success-response (.listAppGroups (get-app-lister "type=apps"))))

(defn apps-in-group
  [group-id params]
  (-> (get-app-lister (str "type=apps&app-category=" group-id))
      (.listApps group-id params)
      (service/success-response)))

(defn search-apps
  [{search-term :search}]
  (when (string/blank? search-term)
    (throw+ {:error_code ce/ERR_MISSING_QUERY_PARAMETER
             :param      :search}))
  (service/success-response (.searchApps (get-app-lister) search-term)))

(defn update-favorites
  [body]
  (let [request (service/decode-json body)]
    (.updateFavorites (get-app-lister)
                      (service/required-field request :analysis_id)
                      (service/required-field request :user_favorite))))

(defn rate-app
  [body]
  (let [request (service/decode-json body)]
    (.rateApp (get-app-lister)
              (service/required-field request :analysis_id)
              (service/required-field request :rating)
              (service/required-field request :comment_id))))

(defn delete-rating
  [body]
  (let [request (service/decode-json body)]
    (.deleteRating (get-app-lister) (service/required-field request :analysis_id))))

(defn get-app
  [app-id]
  (service/success-response (.getApp (get-app-lister) app-id)))

(defn get-deployed-components-in-app
  [app-id]
  (service/success-response (.getAppDeployedComponents (get-app-lister) app-id)))

(defn get-app-details
  [app-id]
  (service/success-response (.getAppDetails (get-app-lister) app-id)))

(defn submit-job
  [workspace-id body]
  (service/success-response
   (.submitJob (get-app-lister) workspace-id (service/decode-json body))))

(defn list-jobs
  [{:keys [limit offset sort-field sort-order filter]
    :or   {limit      "0"
           offset     "0"
           sort-field :startdate
           sort-order :desc}}]
  (let [limit      (Long/parseLong limit)
        offset     (Long/parseLong offset)
        sort-field (keyword sort-field)
        sort-order (keyword sort-order)
        app-lister (get-app-lister)
        filter     (when-not (nil? filter) (service/decode-json filter))]
    (service/success-response
     {:analyses  (.listJobs app-lister limit offset sort-field sort-order filter)
      :timestamp (str (System/currentTimeMillis))
      :total     (.countJobs app-lister filter)})))

(defn- get-unique-job-step
  "Gest a unique job step for an external ID. An exception is thrown if no job step
   is found or if multiple job steps are found."
  [external-id]
  (let [job-steps (jp/get-job-steps-by-external-id external-id)]
    (when (empty? job-steps)
      (service/not-found "job step" external-id))
    (when (> (count job-steps) 1)
      (service/not-unique "job step" external-id))
    (first job-steps)))

(defn update-de-job-status
  "Updates the job status. Important note: this function currently assumes that the
   external identifier is unique."
  [external-id status end-date]
  (let [job-step                   (get-unique-job-step external-id)
        {:keys [username] :as job} (jp/get-job-by-id (:job-id job-step))
        end-date                   (db/timestamp-from-str end-date)]
    (service/assert-found job "job" (:job-id job-step))
    (try+
     (.updateJobStatus (get-app-lister "" username) username job job-step status end-date)
     (catch Object o
       (let [msg (str "DE job status update failed for " external-id)]
         (log/warn o msg)
         (throw+))))))

(defn update-agave-job-status
  [uuid status end-time external-id]
  (let [uuid                       (UUID/fromString uuid)
        job-step                   (jp/get-job-step uuid external-id)
        {:keys [username] :as job} (jp/get-job-by-id uuid)
        end-time                   (db/timestamp-from-str end-time)]
    (service/assert-found job "job" uuid)
    (service/assert-found job-step "job step" (str uuid "/" external-id))
    (try+
     (.updateJobStatus (get-app-lister "" username) username job job-step status end-time)
     (catch Object o
       (let [msg (str "Agave job status update failed for " uuid "/" external-id)]
         (log/warn o msg)
         (throw+))))))

(defn- sync-job-status
  [job]
  (try+
   (log/debug "synchronizing the job status for" (:id job))
   (.syncJobStatus (get-app-lister "" (:username job)) job)
   (catch Object e
     (log/error e "unable to sync the job status for job" (:id job)))))

(defn sync-job-statuses
  []
  (try+
   (dorun (map sync-job-status (jp/list-incomplete-jobs)))
   (catch Object e
     (log/error e "error while obtaining the lsit of jobs to synchronize."))))

(defn- log-already-deleted-jobs
  [ids]
  (let [jobs-by-id (into {} (map (juxt :id identity) (jp/list-jobs-to-delete ids)))
        log-it     (fn [desc id] (log/warn "attempt to delete" desc "job" id "ignored"))]
    (dorun (map (fn [id] (cond (nil? (jobs-by-id id))            (log-it "missing" id)
                               (get-in jobs-by-id [id :deleted]) (log-it "deleted" id)))
                ids))))

(defn delete-jobs
  [body]
  (let [body (service/decode-json body)
        _    (validate-map body {:executions vector?})
        ids  (set (map #(UUID/fromString %) (:executions body)))]
    (log-already-deleted-jobs ids)
    (jp/delete-jobs ids)
    (service/success-response)))

(defn- validate-job-existence
  [id]
  (when-not (jp/get-job-by-id id)
    (service/not-found "job" id)))

(defn- validate-job-update
  [body]
  (let [supported-fields #{:name :description}
        invalid-fields   (remove supported-fields (keys body))]
    (when (seq invalid-fields)
      (throw+ {:error_code ce/ERR_BAD_OR_MISSING_FIELD
               :reason     (str "unrecognized fields: " invalid-fields)}))))

(defn update-job
  [id body]
  (let [id   (UUID/fromString id)
        body (service/decode-json body)]
    (validate-job-existence id)
    (validate-job-update body)
    (jp/update-job id body)))

(defn get-property-values
  [job-id]
  (service/success-response (.getJobParams (get-app-lister) job-id)))

(defn get-app-rerun-info
  [job-id]
  (service/success-response (.getAppRerunInfo (get-app-lister) job-id)))

(defn list-app-data-objects
  [app-id]
  (service/success-response (.listAppDataObjects (get-app-lister) app-id)))

(defn edit-workflow
  [app-id]
  (service/success-response (.editWorkflow (get-app-lister) app-id)))

(defn copy-workflow
  [app-id]
  (service/success-response (.copyWorkflow (get-app-lister) app-id)))
