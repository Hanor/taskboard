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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import objective.taskboard.auth.Authorizer;
import objective.taskboard.followup.FollowUpDataHistoryGenerator;
import objective.taskboard.followup.FollowUpFacade;
import objective.taskboard.followup.FollowUpGenerator;
import objective.taskboard.followup.TemplateService;
import objective.taskboard.followup.data.Template;
import objective.taskboard.utils.DateTimeUtils;
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

    private static final Pattern COMPACT_DATE_PATTERN = Pattern.compile("(\\d+)(\\d\\d)(\\d\\d)");

    @Autowired
    private FollowUpFacade followUpFacade;

    @Autowired
    private FollowUpDataHistoryGenerator followUpDataHistoryGenerator;

    @Autowired
    private Authorizer authorizer;

    @Autowired
    private TemplateService templateService;

    @RequestMapping
    public ResponseEntity<Object> download(@RequestParam("projects") String projects, @RequestParam("template") String template,
            @RequestParam("date") Optional<String> date, @RequestParam("timezone") String zoneId) {

        if (ObjectUtils.isEmpty(projects))
            return new ResponseEntity<>("You must provide a list of projects separated by comma", BAD_REQUEST);
        if (ObjectUtils.isEmpty(template))
            return new ResponseEntity<>("Template not selected", BAD_REQUEST);

        String [] includedProjects = projects.split(",");
        Template templateFollowup = templateService.getTemplate(template);

        if (templateFollowup == null || !authorizer.hasAnyRoleInProjects(templateFollowup.getRoles(), asList(includedProjects)))
            return new ResponseEntity<>("Template or some project does not exist", BAD_REQUEST);

        ZoneId timezone = determineTimeZoneId(zoneId);

        try {
            FollowUpGenerator followupGenerator = followUpFacade.getGenerator(template, date);
            Resource resource = followupGenerator.generate(includedProjects, timezone);
            String filename = "Followup_"+template+"_" + templateDate(date, timezone)+".xlsm";

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

    private String templateDate(Optional<String> date, ZoneId timezone) {
        return date.flatMap(d -> {
            Matcher matcher = COMPACT_DATE_PATTERN.matcher(d);
            if (matcher.matches()) {
                String year = matcher.group(1);
                String month = matcher.group(2);
                String day = matcher.group(3);
                return Optional.of(DateTimeUtils.parseDate(year + "-" + month + "-" + day, timezone));
            }
            return Optional.empty();
        }).orElse(ZonedDateTime.now(timezone))
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
        followUpDataHistoryGenerator.scheduledGenerate();
        return "HISTORY GENERATOR STARTED";
    }
}
