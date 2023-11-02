INSERT INTO tab_customer (c_id, c_first_name, c_last_name, c_email, c_phone_number, c_birth_date)
VALUES (nextval('SEQ_CUSTOMER_C_ID'), 'Max', 'Mustermann', 'max.mustermann@openknowledge.de', '+49 123 4567890', '1990-05-15');

INSERT INTO tab_customer (c_id, c_first_name, c_last_name, c_email, c_phone_number, c_birth_date)
VALUES (nextval('SEQ_CUSTOMER_C_ID'), 'Erika', 'Musterfrau', 'erika.musterfrau@openknowledge.de', '+49 987 6543210', '1985-11-23');
