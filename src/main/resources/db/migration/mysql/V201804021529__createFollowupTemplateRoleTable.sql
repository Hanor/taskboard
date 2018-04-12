CREATE TABLE followup_template_role (
    template_id bigint(20) NOT NULL,
    role varchar(255) NOT NULL,
    UNIQUE KEY template_role_unique (template_id, role),
    CONSTRAINT template_fk_role FOREIGN KEY (template_id) REFERENCES template (id)
);