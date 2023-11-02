CREATE TABLE tab_customer_additional_email (
    c_customer_id BIGINT       NOT NULL,
    c_position    INTEGER      NOT NULL,
    c_email       VARCHAR(255) NOT NULL,
    CONSTRAINT pk_customer_additional_email
        PRIMARY KEY (c_customer_id, c_position),
    CONSTRAINT fk_customer_additional_email_customer
        FOREIGN KEY (c_customer_id) REFERENCES tab_customer (c_id) ON DELETE CASCADE
);

CREATE TABLE tab_customer_additional_phone_number (
    c_customer_id  BIGINT      NOT NULL,
    c_position     INTEGER     NOT NULL,
    c_type         VARCHAR(32) NOT NULL,
    c_phone_number VARCHAR(50) NOT NULL,
    CONSTRAINT pk_customer_additional_phone_number
        PRIMARY KEY (c_customer_id, c_position),
    CONSTRAINT fk_customer_additional_phone_number_customer
        FOREIGN KEY (c_customer_id) REFERENCES tab_customer (c_id) ON DELETE CASCADE
);
