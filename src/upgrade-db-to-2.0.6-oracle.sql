/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.0.6', 1, CURRENT_DATE);
