--liquibase formatted sql

--changeset buta:001-01
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT count(*) FROM pg_available_extensions WHERE name = 'pg_trgm'
CREATE EXTENSION IF NOT EXISTS pg_trgm;
--rollback DROP EXTENSION IF EXISTS pg_trgm;
