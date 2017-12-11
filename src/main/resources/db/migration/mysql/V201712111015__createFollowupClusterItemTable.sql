CREATE TABLE followup_cluster_item (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    followup_config bigint(20) NOT NULL,
    sizing_field_name varchar(255) NOT NULL,
    parent_type_name varchar(255) NOT NULL,
    sizing varchar(10) NOT NULL,
    effort decimal(5,2) NOT NULL,
    cycle decimal(5,2) NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY followup_cluster_item_unique (followup_config, sizing_field_name, parent_type_name),
    CONSTRAINT followup_cluster_item_config_fk FOREIGN KEY (followup_config) REFERENCES template (id)
)