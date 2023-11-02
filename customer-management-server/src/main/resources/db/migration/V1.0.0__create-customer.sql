CREATE SEQUENCE SEQ_CUSTOMER_C_ID
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE tab_customer (
    c_id           BIGINT       NOT NULL,
    c_first_name   VARCHAR(255) NOT NULL,
    c_last_name    VARCHAR(255) NOT NULL,
    c_email        VARCHAR(255) NOT NULL,
    c_phone_number VARCHAR(50),
    c_birth_date   DATE,
    CONSTRAINT pk_customer_c_id PRIMARY KEY (c_id),
    CONSTRAINT uq_customer_c_email UNIQUE (c_email)
);
