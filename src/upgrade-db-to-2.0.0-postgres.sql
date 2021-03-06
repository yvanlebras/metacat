CREATE TABLE systemMetadata (
	guid   text,          -- the globally unique string identifier of the object that the system metadata describes
	serial_version VARCHAR(256), --the serial version of the object
	date_uploaded TIMESTAMP, -- the date/time the document was first submitted
	rights_holder VARCHAR(250), --the user who has rights to the document, usually the first persons to upload it
	checksum VARCHAR(512), --the checksum of the doc using the given algorithm (see below)
	checksum_algorithm VARCHAR(250), --the algorithm used to calculate the checksum
	origin_member_node VARCHAR(250), --the member node where the document was first uploaded
	authoritive_member_node VARCHAR(250), --the member node that currently controls the document
	date_modified TIMESTAMP, -- the last date/time that the file was changed
	submitter VARCHAR(256), -- the user who originally submitted the doc
	object_format VARCHAR(256), --the format of the object
	size VARCHAR(256), --the size of the object
	archived boolean,	 -- specifies whether this an archived object
	replication_allowed boolean,	 -- replication allowed
	number_replicas INT8, 	-- the number of replicas allowed
	obsoletes   text,       -- the identifier that this record obsoletes
	obsoleted_by   text,     -- the identifier of the record that replaces this record
	CONSTRAINT systemMetadata_pk PRIMARY KEY (guid)
);

CREATE TABLE systemMetadataReplicationPolicy (
	guid text,	-- the globally unique string identifier of the object that the system metadata describes
	member_node VARCHAR(250),	 -- replication member node
	policy text,	 -- the policy (preferred, blocked, etc...TBD)
	CONSTRAINT systemMetadataReplicationPolicy_fk 
		FOREIGN KEY (guid) REFERENCES systemMetadata DEFERRABLE
);

CREATE TABLE systemMetadataReplicationStatus (
	guid text,	-- the globally unique string identifier of the object that the system metadata describes
	member_node VARCHAR(250),	 -- replication member node
	status VARCHAR(250),	 -- replication status
	date_verified TIMESTAMP, 	-- the date replication was verified   
	CONSTRAINT systemMetadataReplicationStatus_fk 
		FOREIGN KEY (guid) REFERENCES systemMetadata DEFERRABLE
);

/*
 * Remove the old Identifier table (pre-2.0)
 */
DROP TABLE IF EXISTS identifier;
/*
 * Create the new one (DataONE features)
 */
CREATE TABLE identifier (
   guid   text,          -- the globally unique string identifier
   docid  VARCHAR(250),  -- the local document id #
   rev    INT8,          -- the revision part of the local identifier
   CONSTRAINT identifier_pk PRIMARY KEY (guid)
);

/*
 * add the nodedatadate column to the tables that need it 
 */
ALTER TABLE xml_nodes ADD COLUMN nodedatadate TIMESTAMP;
ALTER TABLE xml_nodes_revisions ADD COLUMN nodedatadate TIMESTAMP;
ALTER TABLE xml_path_index ADD COLUMN nodedatadate TIMESTAMP;
CREATE INDEX xml_path_index_idx5 ON xml_path_index (nodedatadate);

/**
 * track the user-agent for the request
 */
ALTER TABLE access_log ADD COLUMN user_agent VARCHAR(512);

/*
 * Register the new schema
 */
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('Schema', '@eml2_1_1namespace@', '/schema/eml-2.1.1/eml.xsd');  

/**
 * Generate GUIDs for docid.rev
 */
INSERT INTO identifier (docid, rev, guid) 
	SELECT docid, rev, docid || '.' || rev FROM xml_documents
	UNION	
	SELECT docid, rev, docid || '.' || rev FROM xml_revisions;
--INSERT 0 156644

/**
 *  Add guid in xml_access table
 */
ALTER TABLE xml_access ADD COLUMN guid text;
/**
 *  Expand accessfileid in xml_access table to hold guids
 */
ALTER TABLE xml_access ALTER COLUMN accessfileid TYPE text;

/**
 * Upgrade xml_access records to use GUID from identifier table
 * NOTE: This duplicates existing access rules for every revision of a document
 */
INSERT INTO xml_access (
	guid, principal_name, permission, perm_type, perm_order, 
	docid, accessfileid, begin_time, end_time, ticket_count, subtreeid, startnodeid, endnodeid
)
	SELECT 
		id.guid, xa.principal_name, xa.permission, xa.perm_type, xa.perm_order, 
		xa.docid, xa.accessfileid, xa.begin_time, xa.end_time, xa.ticket_count, xa.subtreeid, xa.startnodeid, xa.endnodeid
	FROM identifier id, xml_access xa
	WHERE id.docid = xa.docid;
--INSERT 0 311224

/**
 * Include inline data access rows -- they have a special guid: 'scope.docid.rev.index'
 */
INSERT INTO xml_access (
	guid, principal_name, permission, perm_type, perm_order, 
	docid, accessfileid, begin_time, end_time, ticket_count, subtreeid, startnodeid, endnodeid
)
	SELECT 
		docid, principal_name, permission, perm_type, perm_order, 
		docid, accessfileid, begin_time, end_time, ticket_count, subtreeid, startnodeid, endnodeid
	FROM xml_access
	WHERE docid like '%.%.%.%';
--INSERT 0 18


/**
 * Update the accessfileid to use their guid
 * NOTE: uses the last revision's guid as the new value for accessfileid
 * uses a temporary table
 **/

CREATE TABLE max_identifier (guid text, docid VARCHAR(250), rev INT8);

/** insert the max rev identifier for each document **/
INSERT INTO max_identifier (docid, rev, guid)
SELECT docid, MAX(rev), docid || '.' || MAX(rev)
FROM identifier
GROUP BY docid;
--INSERT 0 57841

/** create some indexes to speed up the join **/
CREATE INDEX maxid_docid_index ON max_identifier (docid);
CREATE INDEX maxid_guid_index ON max_identifier (guid);
CREATE INDEX accessfileid_index on xml_access (accessfileid);
CREATE INDEX xml_access_guid_index on xml_access (guid);

UPDATE xml_access xa
SET accessfileid = maxid.guid
FROM max_identifier maxid
WHERE xa.accessfileid = maxid.docid
AND xa.guid IS NOT null;
--UPDATE 310427

DROP INDEX maxid_docid_index;
DROP INDEX maxid_guid_index;
DROP INDEX accessfileid_index;
DROP INDEX xml_access_guid_index;

DROP TABLE max_identifier;

/**
 * Remove old access rules
 */
DELETE FROM xml_access WHERE guid is null;
--DELETE 105432

/**
 * clean up the xml_access table
 */
ALTER TABLE xml_access DROP COLUMN docid;


/**
 * expand xml_path_index 'path' column to hold larger strings 
 */
ALTER TABLE xml_path_index ALTER COLUMN path TYPE text;

/**
 * include 2.0.0beta4 DTD
 */
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-access-@eml-beta4-version@//EN',
         '/dtd/eml-access-@eml-beta4-version@.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-attribute-@eml-beta4-version@//EN',
          '/dtd/eml-attribute-@eml-beta4-version@.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-constraint-@eml-beta4-version@//EN',
          '/dtd/eml-constraint-@eml-beta4-version@.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-coverage-@eml-beta4-version@//EN',
          '/dtd/eml-coverage-@eml-beta4-version@.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-dataset-@eml-beta4-version@//EN',
          '/dtd/eml-dataset-@eml-beta4-version@.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-entity-@eml-beta4-version@//EN',
          '/dtd/eml-entity-@eml-beta4-version@.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-literature-@eml-beta4-version@//EN',
          '/dtd/eml-literature-@eml-beta4-version@.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-physical-@eml-beta4-version@//EN',
          '/dtd/eml-physical-@eml-beta4-version@.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-project-@eml-beta4-version@//EN',
          '/dtd/eml-project-@eml-beta4-version@.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-protocol-@eml-beta4-version@//EN',
          '/dtd/eml-protocol-@eml-beta4-version@.dtd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('DTD', '-//ecoinformatics.org//eml-software-@eml-beta4-version@//EN',
          '/dtd/eml-software-@eml-beta4-version@.dtd');

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.0.0', 1, CURRENT_DATE);

