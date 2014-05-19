SET search_path = public, pg_catalog;

--
-- Updates apps uuid foreign keys.
--
UPDATE app_steps SET app_id =
    (SELECT a.id FROM apps a WHERE transformation_task_id = a.hid);
UPDATE ratings SET app_id =
    (SELECT a.id FROM apps a WHERE transformation_activity_id = a.hid);
UPDATE app_category_app SET app_id =
    (SELECT a.id FROM apps a WHERE template_id = a.hid);
UPDATE workflow_io_maps m SET app_id =
    (SELECT a.id FROM apps a
     LEFT JOIN transformation_activity_mappings tm
     ON tm.transformation_activity_id = a.hid
     WHERE m.hid = tm.mapping_id);
UPDATE suggested_groups SET app_id =
    (SELECT a.id FROM apps a WHERE transformation_activity_id = a.hid);
UPDATE app_references SET app_id =
    (SELECT a.id FROM apps a WHERE transformation_activity_id = a.hid);

