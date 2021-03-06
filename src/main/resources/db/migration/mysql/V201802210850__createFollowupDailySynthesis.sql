CREATE TABLE followup_daily_synthesis (
  id                  bigint(20)   NOT NULL AUTO_INCREMENT,
  followup_date       DATE         NOT NULL, 
  project_id          BIGINT       NOT NULL,
  sum_effort_done     double       NOT NULL,
  sum_effort_backlog  double       NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fds_project_fk` FOREIGN KEY (`project_id`) REFERENCES `project_filter_configuration` (`id`)
);

CREATE INDEX fds_project_date_idx  on followup_daily_synthesis (project_id,followup_date);