-- V2: store status/category columns as plain text instead of MySQL ENUM.
--
-- Databases created by Hibernate's ddl-auto made these ENUM('A','B',...) columns. Adding a new
-- value to a Java enum then fails with "Data truncated" until the column is changed by hand.
-- As VARCHAR, new values just work. On a database created from V1 these are already VARCHAR,
-- so this changes nothing there.

ALTER TABLE agent_drafts
    MODIFY approval_status VARCHAR(30) NOT NULL,
    MODIFY responsibility VARCHAR(30) NOT NULL;

ALTER TABLE app_users
    MODIFY role VARCHAR(30) NOT NULL;

ALTER TABLE maintenance_requests
    MODIFY ai_status VARCHAR(30) NULL,
    MODIFY category VARCHAR(30) NULL,
    MODIFY status VARCHAR(30) NOT NULL,
    MODIFY urgency VARCHAR(30) NULL;

ALTER TABLE tenancies
    MODIFY status VARCHAR(30) NOT NULL;

ALTER TABLE vendors
    MODIFY category VARCHAR(30) NOT NULL;

ALTER TABLE work_order_updates
    MODIFY status VARCHAR(30) NOT NULL;
