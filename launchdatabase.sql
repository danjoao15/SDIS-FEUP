CREATE TABLE filesstored(
	id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
	fileid VARCHAR(16) NOT NULL,
	responsible BOOLEAN DEFAULT false,
	storing BOOLEAN DEFAULT true,
	requestingpeer VARCHAR(16),
	desiredrepdeg INTEGER,
	lasttimestored TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE peers(
	id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
	peerid VARCHAR(16) NOT NULL,
	ip VARCHAR(15) NOT NULL,
	port INT NOT NULL
);

CREATE TABLE backupsrequested(
	id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
	fileid VARCHAR(16) NOT NULL,
	filename VARCHAR(128) NOT NULL,
	chunksNum INTEGER NOT NULL,
	desiredrepdeg INTEGER NOT NULL,
	encrypt_key VARCHAR(256)
);

CREATE TABLE chunksstored(
	id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
	chunkid INT NOT NULL,
	fileid VARCHAR(16) NOT NULL,
	currentrepdeg INTEGER,
	size INTEGER NOT NULL
);

ALTER TABLE chunksstored
   ADD CONSTRAINT chunksstored_PK Primary Key (id);
ALTER TABLE chunksstored
   ADD CONSTRAINT chunksstored_UNIQUE UNIQUE (chunkid,fileid);

ALTER TABLE filesstored
   ADD CONSTRAINT filesstored_PK Primary Key (id);
ALTER TABLE filesstored
   ADD CONSTRAINT filesstored_UNIQUE UNIQUE (fileid);

ALTER TABLE peers
   ADD CONSTRAINT peers_PK Primary Key (id);
ALTER TABLE peers
   ADD CONSTRAINT peers_UNIQUE1 UNIQUE (peerid);
ALTER TABLE peers
   ADD CONSTRAINT peers_UNIQUE2 UNIQUE (ip,port);

ALTER TABLE backupsrequested
   ADD CONSTRAINT backupsrequested_PK PRIMARY KEY (id);
ALTER TABLE backupsrequested
   ADD CONSTRAINT backupsrequested_UNIQUE UNIQUE (fileid);

ALTER TABLE filesstored
   ADD CONSTRAINT filesstored_FK Foreign Key (requestingpeer)
   REFERENCES peers(peerid);

ALTER TABLE chunksstored
   ADD CONSTRAINT chunksstored_FK Foreign Key (fileid) REFERENCES filesstored(fileid) ON DELETE CASCADE;
