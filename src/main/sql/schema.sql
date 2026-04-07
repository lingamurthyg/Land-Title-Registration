-- =============================================================================
-- Land Title Registry System — Database Schema
-- Compatible with: IBM DB2 11.x, Oracle 19c, PostgreSQL 14+
-- =============================================================================

-- -----------------------------------------------------------------------------
-- LAND_TITLE — Core title registry table
-- -----------------------------------------------------------------------------
CREATE TABLE LAND_TITLE (
    TITLE_NUMBER        VARCHAR(30)     NOT NULL,
    PARCEL_ID           VARCHAR(50)     NOT NULL,
    LEGAL_DESCRIPTION   CLOB,
    AREA_SQM            DECIMAL(15, 4),
    STATUS              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    LAND_USE_TYPE       VARCHAR(30),

    -- Location
    STREET_ADDRESS      VARCHAR(200),
    SUBURB              VARCHAR(100),
    CITY                VARCHAR(100),
    STATE_PROVINCE      VARCHAR(100),
    COUNTRY             VARCHAR(100),
    POSTAL_CODE         VARCHAR(20),
    LATITUDE            DECIMAL(10, 7),
    LONGITUDE           DECIMAL(10, 7),

    -- Ownership
    OWNER_NATIONAL_ID   VARCHAR(50)     NOT NULL,
    OWNER_FULL_NAME     VARCHAR(200)    NOT NULL,
    OWNER_EMAIL         VARCHAR(200),
    OWNER_PHONE         VARCHAR(50),

    -- Valuation
    ASSESSED_VALUE      DECIMAL(18, 2),
    MARKET_VALUE        DECIMAL(18, 2),
    CURRENCY_CODE       CHAR(3)         DEFAULT 'USD',

    -- Audit
    REGISTRATION_DATE   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    LAST_MODIFIED_DATE  TIMESTAMP,
    REGISTERED_BY       VARCHAR(100),
    LAST_MODIFIED_BY    VARCHAR(100),

    -- Encumbrance
    HAS_LIEN            SMALLINT        DEFAULT 0,
    HAS_MORTGAGE        SMALLINT        DEFAULT 0,
    ENCUMBRANCE_DETAIL  CLOB,
    REMARKS             VARCHAR(1000),

    CONSTRAINT PK_LAND_TITLE        PRIMARY KEY (TITLE_NUMBER),
    CONSTRAINT UQ_PARCEL_ID         UNIQUE (PARCEL_ID),
    CONSTRAINT CHK_STATUS           CHECK (STATUS IN
        ('ACTIVE','PENDING','TRANSFERRED','ENCUMBERED','CANCELLED')),
    CONSTRAINT CHK_LAND_USE         CHECK (LAND_USE_TYPE IN
        ('RESIDENTIAL','COMMERCIAL','AGRICULTURAL','INDUSTRIAL','MIXED_USE','GOVERNMENT')),
    CONSTRAINT CHK_HAS_LIEN         CHECK (HAS_LIEN IN (0, 1)),
    CONSTRAINT CHK_HAS_MORTGAGE     CHECK (HAS_MORTGAGE IN (0, 1))
);

-- Performance indexes
CREATE INDEX IDX_LT_OWNER      ON LAND_TITLE (OWNER_NATIONAL_ID);
CREATE INDEX IDX_LT_STATUS     ON LAND_TITLE (STATUS);
CREATE INDEX IDX_LT_CITY       ON LAND_TITLE (CITY);
CREATE INDEX IDX_LT_REG_DATE   ON LAND_TITLE (REGISTRATION_DATE DESC);

-- -----------------------------------------------------------------------------
-- TITLE_TRANSFER_HISTORY — Full chain-of-title audit
-- -----------------------------------------------------------------------------
CREATE TABLE TITLE_TRANSFER_HISTORY (
    TRANSFER_ID             BIGINT          NOT NULL
                                            GENERATED ALWAYS AS IDENTITY
                                            (START WITH 1 INCREMENT BY 1),
    TITLE_NUMBER            VARCHAR(30)     NOT NULL,

    -- Previous owner snapshot
    PREV_OWNER_NATIONAL_ID  VARCHAR(50),
    PREV_OWNER_NAME         VARCHAR(200),

    -- New owner
    NEW_OWNER_NATIONAL_ID   VARCHAR(50)     NOT NULL,
    NEW_OWNER_NAME          VARCHAR(200)    NOT NULL,
    NEW_OWNER_EMAIL         VARCHAR(200),
    NEW_OWNER_PHONE         VARCHAR(50),

    TRANSFER_TYPE           VARCHAR(30),
    TRANSFER_STATUS         VARCHAR(30)     NOT NULL DEFAULT 'INITIATED',

    -- Financials
    TRANSFER_PRICE          DECIMAL(18, 2),
    CURRENCY_CODE           CHAR(3)         DEFAULT 'USD',
    STAMP_DUTY_PAID         DECIMAL(18, 2),

    -- Dates
    TRANSFER_DATE           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    EFFECTIVE_DATE          TIMESTAMP,

    -- Legal
    DEED_NUMBER             VARCHAR(100),
    NOTARY_NATIONAL_ID      VARCHAR(50),
    NOTARY_NAME             VARCHAR(200),

    -- Workflow
    INITIATED_BY            VARCHAR(100),
    APPROVED_BY             VARCHAR(100),
    APPROVED_DATE           TIMESTAMP,
    REJECTION_REASON        VARCHAR(500),
    REMARKS                 VARCHAR(1000),

    CONSTRAINT PK_TRANSFER           PRIMARY KEY (TRANSFER_ID),
    CONSTRAINT FK_TRANSFER_TITLE     FOREIGN KEY (TITLE_NUMBER)
                                     REFERENCES LAND_TITLE (TITLE_NUMBER),
    CONSTRAINT CHK_TRANSFER_TYPE     CHECK (TRANSFER_TYPE IN
        ('SALE','INHERITANCE','DONATION','COURT_ORDER','GOVERNMENT_ACQUISITION','CORRECTION')),
    CONSTRAINT CHK_TRANSFER_STATUS   CHECK (TRANSFER_STATUS IN
        ('INITIATED','DOCUMENTS_PENDING','UNDER_REVIEW','APPROVED','REJECTED','COMPLETED'))
);

CREATE INDEX IDX_TH_TITLE_NUM    ON TITLE_TRANSFER_HISTORY (TITLE_NUMBER);
CREATE INDEX IDX_TH_STATUS       ON TITLE_TRANSFER_HISTORY (TRANSFER_STATUS);
CREATE INDEX IDX_TH_NEW_OWNER    ON TITLE_TRANSFER_HISTORY (NEW_OWNER_NATIONAL_ID);

-- -----------------------------------------------------------------------------
-- REGISTRY_AUDIT_LOG — Immutable audit trail for all title events
-- -----------------------------------------------------------------------------
CREATE TABLE REGISTRY_AUDIT_LOG (
    LOG_ID          BIGINT          NOT NULL
                                    GENERATED ALWAYS AS IDENTITY,
    EVENT_TIMESTAMP TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    EVENT_TYPE      VARCHAR(50)     NOT NULL,
    TITLE_NUMBER    VARCHAR(30),
    TRANSFER_ID     BIGINT,
    PERFORMED_BY    VARCHAR(100)    NOT NULL,
    DESCRIPTION     VARCHAR(2000),
    OLD_VALUE       CLOB,
    NEW_VALUE       CLOB,
    IP_ADDRESS      VARCHAR(50),
    SESSION_ID      VARCHAR(100),

    CONSTRAINT PK_AUDIT_LOG PRIMARY KEY (LOG_ID)
);

CREATE INDEX IDX_AL_TITLE       ON REGISTRY_AUDIT_LOG (TITLE_NUMBER);
CREATE INDEX IDX_AL_TIMESTAMP   ON REGISTRY_AUDIT_LOG (EVENT_TIMESTAMP DESC);
CREATE INDEX IDX_AL_USER        ON REGISTRY_AUDIT_LOG (PERFORMED_BY);

-- -----------------------------------------------------------------------------
-- SEQUENCE for title number generation
-- DB2 sequence; for Oracle use: CREATE SEQUENCE SEQ_TITLE_NUM ...
-- -----------------------------------------------------------------------------
CREATE SEQUENCE SEQ_TITLE_NUM
    START WITH 1000
    INCREMENT BY 1
    NO CYCLE
    NO CACHE;

-- -----------------------------------------------------------------------------
-- SEED DATA — Sample titles for development/testing
-- -----------------------------------------------------------------------------
INSERT INTO LAND_TITLE (
    TITLE_NUMBER, PARCEL_ID, LEGAL_DESCRIPTION, AREA_SQM,
    STATUS, LAND_USE_TYPE, STREET_ADDRESS, SUBURB, CITY,
    STATE_PROVINCE, COUNTRY, POSTAL_CODE,
    OWNER_NATIONAL_ID, OWNER_FULL_NAME, OWNER_EMAIL, OWNER_PHONE,
    ASSESSED_VALUE, MARKET_VALUE, CURRENCY_CODE,
    REGISTERED_BY
) VALUES (
    'LTR-2024-REG-001000', 'PCL-NRB-0042-2024',
    'All that piece of land situated in the District of Nairobi, registered as Parcel No. 42, Block NRB-7, as shown on Survey Plan No. 10042/2024.',
    485.50, 'ACTIVE', 'RESIDENTIAL',
    '14 Riverside Drive', 'Westlands', 'Nairobi', 'Nairobi County',
    'Kenya', '00100',
    'KE-NID-1984-004421', 'James Kariuki Mwangi',
    'jkariuki@example.co.ke', '+254 722 100 200',
    4250000.00, 5100000.00, 'KES',
    'system'
);

INSERT INTO LAND_TITLE (
    TITLE_NUMBER, PARCEL_ID, LEGAL_DESCRIPTION, AREA_SQM,
    STATUS, LAND_USE_TYPE, STREET_ADDRESS, SUBURB, CITY,
    STATE_PROVINCE, COUNTRY, POSTAL_CODE,
    OWNER_NATIONAL_ID, OWNER_FULL_NAME, OWNER_EMAIL, OWNER_PHONE,
    ASSESSED_VALUE, MARKET_VALUE, CURRENCY_CODE,
    REGISTERED_BY, HAS_MORTGAGE
) VALUES (
    'LTR-2024-REG-001001', 'PCL-MSA-0018-2024',
    'All that piece of land situated in the District of Mombasa, registered as Parcel No. 18, Block MSA-3, as shown on Survey Plan No. 10018/2024.',
    1200.00, 'ENCUMBERED', 'COMMERCIAL',
    '8 Nyali Road', 'Nyali', 'Mombasa', 'Mombasa County',
    'Kenya', '80100',
    'KE-NID-1976-007832', 'Fatuma Aisha Salim',
    'fatuma.salim@example.co.ke', '+254 733 200 300',
    9800000.00, 12500000.00, 'KES',
    'system', 1
);

INSERT INTO LAND_TITLE (
    TITLE_NUMBER, PARCEL_ID, LEGAL_DESCRIPTION, AREA_SQM,
    STATUS, LAND_USE_TYPE, STREET_ADDRESS, SUBURB, CITY,
    STATE_PROVINCE, COUNTRY, POSTAL_CODE,
    OWNER_NATIONAL_ID, OWNER_FULL_NAME, OWNER_EMAIL, OWNER_PHONE,
    ASSESSED_VALUE, MARKET_VALUE, CURRENCY_CODE,
    REGISTERED_BY
) VALUES (
    'LTR-2024-REG-001002', 'PCL-KSM-0055-2024',
    'Farm land registered as Parcel No. 55, Division KSM-11, Kisumu District.',
    50000.00, 'ACTIVE', 'AGRICULTURAL',
    'Off Kisumu-Kakamega Highway', 'Mamboleo', 'Kisumu', 'Kisumu County',
    'Kenya', '40100',
    'KE-NID-1965-003310', 'Peter Otieno Achieng',
    'p.otieno@example.co.ke', '+254 700 500 600',
    2100000.00, 2800000.00, 'KES',
    'system'
);

COMMIT;
