(ns metadactyl.tools
  (:use [kameleon.core]
        [kameleon.entities]
        [kameleon.queries]
        [kameleon.util.search]
        [metadactyl.persistence.app-metadata :only [add-tool]]
        [metadactyl.util.conversions :only [remove-nil-vals]]
        [clojure.string :only [upper-case]]
        [korma.core]
        [korma.db :only [transaction]])
  (:require [clojure-commons.error-codes :as cc-errs]
            [metadactyl.util.service :as service]))

(defn- add-search-where-clauses
  "Adds where clauses to a base tool search query to restrict results to tools that contain the
   given search term in their name or description."
  [base-query search-term]
  (let [search-term (format-query-wildcards search-term)
        search-term (str "%" search-term "%")
        sql-lower #(sqlfn lower %)]
    (where base-query
      (or
        {(sql-lower :tools.name) [like (sqlfn lower search-term)]}
        {(sql-lower :tools.description) [like (sqlfn lower search-term)]}))))

(defn tool-listing-base-query
  "Obtains a listing query for tools, with optional search and paging params."
  ([]
   (-> (select* tools)
       (fields [:tools.id :id]
               [:tools.name :name]
               [:tools.description :description]
               [:tools.location :location]
               [:tool_types.name :type]
               [:tools.version :version]
               [:tools.attribution :attribution])
       (join tool_types)))
  ([{search-term :search :keys [sort-field sort-dir limit offset]}]
   (let [sort-field (when sort-field (keyword (str "tools." sort-field)))
         sort-dir (when sort-dir (keyword (upper-case sort-dir)))]
     (-> (tool-listing-base-query)
         (add-search-where-clauses search-term)
         (add-query-sorting sort-field sort-dir)
         (add-query-limit limit)
         (add-query-offset offset)))))

(defn search-tools
  "Obtains a listing of tools for the tool search service."
  [params]
  (service/success-response
    {:tools
     (map remove-nil-vals
       (select (tool-listing-base-query params)))}))

(defn add-tools
  "Adds a list of tools to the database, returning a list of IDs of the tools added."
  [{:keys [tools]}]
  (transaction
    (let [tool-ids (map add-tool tools)]
      (service/success-response {:tool_ids tool-ids}))))
