ALTER TABLE project_filter_configuration ADD constraint project_fc_pk  PRIMARY KEY(id);

CREATE TABLE FOLLOWUP_DAILY_SYNTHESIS (
  id                   NUMBER       NOT NULL,
  followup_date        DATE         NOT NULL, 
  project_id           NUMBER       NOT NULL,
  sum_effort_done      NUMBER(4,2)  NOT NULL,
  sum_effort_backlog   NUMBER(4,2)  NOT NULL,
  CONSTRAINT fds_pk    PRIMARY KEY (id),
  CONSTRAINT fds_project_fk FOREIGN KEY (project_id) REFERENCES project_filter_configuration (id)
);

CREATE INDEX fds_project_date_idx  on followup_daily_synthesis (project_id,followup_date);