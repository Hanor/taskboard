/*-
 * [LICENSE]
 * Taskboard
 * ---
 * Copyright (C) 2015 - 2017 Objective Solutions
 * ---
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * [/LICENSE]
 */
package objective.taskboard.controller;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.util.Arrays.asList;
import static objective.taskboard.utils.DateTimeUtils.determineTimeZoneId;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import objective.taskboard.auth.Authorizer;
import objective.taskboard.followup.FollowUpFacade;
import objective.taskboard.followup.FollowUpHistoryKeeper;
import objective.taskboard.followup.TemplateService;
import objective.taskboard.followup.data.Template;

@RestController
@RequestMapping("/ws/followup")
public class FollowUpController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FollowUpController.class);

    private static final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('_')
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            .toFormatter();

    @Autowired
    private FollowUpFacade followUpFacade;

    @Autowired
    private FollowUpHistoryKeeper historyKeeper;

    @Autowired
    private Authorizer authorizer;

    @Autowired
    private TemplateService templateService;

    @RequestMapping
    public ResponseEntity<Object> download(@RequestParam("project") String projectKey, @RequestParam("template") String template,
            @RequestParam("date") Optional<LocalDate> date, @RequestParam("timezone") String zoneId) {

        if (ObjectUtils.isEmpty(projectKey))
            return new ResponseEntity<>("You must provide the project", BAD_REQUEST);
        if (ObjectUtils.isEmpty(template))
            return new ResponseEntity<>("Template not selected", BAD_REQUEST);

        Template templateFollowup = templateService.getTemplate(template);

        if (templateFollowup == null || !authorizer.hasAnyRoleInProjects(templateFollowup.getRoles(), asList(projectKey)))
            return new ResponseEntity<>("Template or project doesn't exist", HttpStatus.NOT_FOUND);

        ZoneId timezone = determineTimeZoneId(zoneId);

        try {
            Resource resource = followUpFacade.generateReport(template, date, timezone, projectKey);
            String filename = "Followup_" + template + "_" + projectKey + "_" + templateDate(date, timezone) + ".xlsm";

            return ResponseEntity.ok()
                  .contentLength(resource.contentLength())
                  .header("Content-Disposition","attachment; filename=\"" + filename + "\"")
                  .header("Set-Cookie", "fileDownload=true; path=/")
                  .body(resource);
        } catch (Exception e) {
            log.warn("Error generating followup spreadsheet", e);
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "text/html; charset=utf-8")
                    .body(StringUtils.defaultIfEmpty(e.getMessage(), e.toString()));
        }
    }

    private String templateDate(Optional<LocalDate> date, ZoneId timezone) {
        return date.map(d -> ZonedDateTime.of(d, LocalTime.MIDNIGHT, timezone))
                .orElse(ZonedDateTime.now(timezone))
                .format(formatter);
    }

    @RequestMapping("generic-template")
    public ResponseEntity<Object> genericTemplate() {
        try {
            Resource resource = followUpFacade.getGenericTemplate();
            return ResponseEntity.ok()
                  .contentLength(resource.contentLength())
                  .header("Content-Disposition","attachment; filename=generic-followup-template.xlsm")
                  .body(resource);
        } catch (Exception e) {
            log.warn("Error while serving genericTemplate", e);
            return new ResponseEntity<>(e.getMessage() == null ? e.toString() : e.getMessage(), INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping("has-generic-template")
    public ResponseEntity<Boolean> hasGenericTemplate() {
        return new ResponseEntity<Boolean>(followUpFacade.getGenericTemplate().exists(), OK);
    }

    @RequestMapping("generate-history")
    public String generateHistory() {
        historyKeeper.generate();
        return "HISTORY GENERATOR STARTED";
    }
}
