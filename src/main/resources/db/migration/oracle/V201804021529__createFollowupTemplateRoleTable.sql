CREATE TABLE followup_template_role (
    template_id NUMBER NOT NULL,
    role VARCHAR2(255) NOT NULL,
    CONSTRAINT template_role_unique UNIQUE (template_id, role),
    CONSTRAINT template_fk_role FOREIGN KEY (template_id) REFERENCES template (id)
);