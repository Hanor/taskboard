/*-
 * [LICENSE]
 * Taskboard
 * - - -
 * Copyright (C) 2015 - 2016 Objective Solutions
 * - - -
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
package objective.taskboard.jira;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import objective.taskboard.data.Issue;

@ConfigurationProperties(prefix = "jira")
@Validated
public class JiraProperties {
    @NotNull
    @NotEmpty
    private String url;
    @NotNull
    @Valid
    private Lousa lousa;
    @NotNull
    @Valid
    private CustomField customfield = new CustomField();
    @NotNull
    @Valid
    private IssueLink issuelink;
    @NotNull
    @Valid
    private IssueType issuetype;

    @NotNull
    @NotEmpty
    private List<Long> statusesCompletedIds;
    @NotNull
    @NotEmpty
    private List<Long> statusesCanceledIds;
    @NotNull
    @NotEmpty
    private List<Long> statusesDeferredIds;
    @NotNull
    @NotEmpty
    private List<String> transitionsWithRequiredCommentNames;
    @NotNull
    @NotEmpty
    private List<String> transitionsDoneNames;
    @NotNull
    @NotEmpty
    private List<String> transitionsCancelNames;
    @NotNull
    @Valid
    private Resolutions resolutions;
    @NotNull
    @Valid
    private Followup followup;
    @NotNull
    @Valid
    private StatusPriorityOrder statusPriorityOrder;

    private Wip wip;

    public static class StatusPriorityOrder {
        private String[] demands;
        private String[] tasks;
        private String[] subtasks;
        private Map<String, Integer> demandPriorityByStatus;
        private Map<String, Integer> taskPriorityByStatus;
        private Map<String, Integer> subtaskPriorityByStatus;

        private Map<String, Integer> initMap(String[] statusInOrder) {
            Map<String, Integer> map = new HashMap<>();
            for(int i = 0; i < statusInOrder.length; i++) {
                map.put(statusInOrder[i], i);
            }
            return map;
        }

        private void ensureDemandInitialized() {
            if (demandPriorityByStatus == null)
                demandPriorityByStatus = initMap(demands);
        }

        private void ensureTaskInitialized() {
            if (taskPriorityByStatus == null)
                taskPriorityByStatus = initMap(tasks);
        }

        private void ensureSubtaskInitialized() {
            if (subtaskPriorityByStatus == null)
                subtaskPriorityByStatus = initMap(subtasks);
        }

        public Integer getDemandPriorityByStatus(String status) {
            ensureDemandInitialized();
            return demandPriorityByStatus.get(status);
        }

        public Integer getTaskPriorityByStatus(String status) {
            ensureTaskInitialized();
            return taskPriorityByStatus.get(status);
        }
        public Integer getSubtaskPriorityByStatus(String status) {
            ensureSubtaskInitialized();
            return subtaskPriorityByStatus.get(status);
        }
        public String[] getDemandsInOrder() {
            return demands;
        }
        public String[] getTasksInOrder() {
            return tasks;
        }
        public String[] getSubtasksInOrder() {
            return subtasks;
        }
        public void setDemands(String[] demands) {
            this.demands = demands;
        }
        public void setTasks(String[] tasks) {
            this.tasks = tasks;
        }
        public void setSubtasks(String[] subtasks) {
            this.subtasks = subtasks;
        }

    }

    @Valid
    private List<SubtaskCreation> subtaskCreation = new ArrayList<>();


    public static class Lousa {
        @NotNull
        @NotEmpty
        private String username;
        @NotNull
        @NotEmpty
        private String password;

        public String getUsername() {
            return this.username;
        }

        public String getPassword() {
            return this.password;
        }

        public void setUsername(final String username) {
            this.username = username;
        }

        public void setPassword(final String password) {
            this.password = password;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Lousa lousa = (Lousa) o;

            if (username != null ? !username.equals(lousa.username) : lousa.username != null) return false;
            return password != null ? password.equals(lousa.password) : lousa.password == null;
        }

        @Override
        public int hashCode() {
            int result = username != null ? username.hashCode() : 0;
            result = 31 * result + (password != null ? password.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Lousa{" +
                    "username='" + username + '\'' +
                    ", password='" + password + '\'' +
                    '}';
        }
    }

    public static class CustomField {
        @NotNull
        @Valid
        private TShirtSize tShirtSize;
        @NotNull
        @Valid
        private ClassOfServiceDetails classOfService = new ClassOfServiceDetails();
        @NotNull
        @Valid
        private Blocked blocked;
        @NotNull
        @Valid
        private CustomFieldDetails lastBlockReason;
        @NotNull
        @Valid
        private CustomFieldDetails coAssignees;

        private CustomFieldDetails release = new CustomFieldDetails("");

        private CustomFieldDetails additionalEstimatedHours = new CustomFieldDetails("");

        public static class CustomFieldDetails {
            @NotNull
            @NotEmpty
            private String id;

            public CustomFieldDetails() {
            }

            @java.beans.ConstructorProperties({"id"})
            public CustomFieldDetails(final String id) {
                this.id = id;
            }

            public String getId() {
                return this.id;
            }

            public void setId(final String id) {
                this.id = id;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                CustomFieldDetails that = (CustomFieldDetails) o;

                return id != null ? id.equals(that.id) : that.id == null;
            }

            @Override
            public int hashCode() {
                return id != null ? id.hashCode() : 0;
            }

            @Override
            public String toString() {
                return "CustomFieldDetails{" +
                        "id='" + id + '\'' +
                        '}';
            }
        }


        public static class ClassOfServiceDetails extends CustomFieldDetails {
            private String defaultValue = "Standard";
            private Map<Long, String> colors;

            public String getDefaultValue() {
                return this.defaultValue;
            }

            public Map<Long, String> getColors() {
                return this.colors;
            }

            public void setDefaultValue(final String defaultValue) {
                this.defaultValue = defaultValue;
            }

            public void setColors(final Map<Long, String> colors) {
                this.colors = colors;
            }

            @Override
            public String toString() {
                return "ClassOfServiceDetails{" +
                        "defaultValue='" + defaultValue + '\'' +
                        ", colors=" + colors +
                        '}';
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                if (!super.equals(o)) return false;

                ClassOfServiceDetails that = (ClassOfServiceDetails) o;

                if (defaultValue != null ? !defaultValue.equals(that.defaultValue) : that.defaultValue != null)
                    return false;
                return colors != null ? colors.equals(that.colors) : that.colors == null;
            }

            @Override
            public int hashCode() {
                int result = super.hashCode();
                result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
                result = 31 * result + (colors != null ? colors.hashCode() : 0);
                return result;
            }
        }


        public static class TShirtSize {
            @NotNull
            @NotEmpty
            private List<String> ids;

            @NotNull
            @NotEmpty
            private String mainTShirtSizeFieldId;

            @NotNull
            @NotEmpty
            private String extraSmall = "XS";
            @NotNull
            @NotEmpty
            private String small = "S";
            @NotNull
            @NotEmpty
            private String medium = "M";
            @NotNull
            @NotEmpty
            private String large = "L";
            @NotNull
            @NotEmpty
            private String extraLarge = "XL";

            public List<String> getIds() {
                return ids;
            }

            public String getMainTShirtSizeFieldId() {
                return this.mainTShirtSizeFieldId;
            }

            public String getExtraSmall() {
                return this.extraSmall;
            }

            public String getSmall() {
                return this.small;
            }

            public String getMedium() {
                return this.medium;
            }

            public String getLarge() {
                return this.large;
            }

            public String getExtraLarge() {
                return this.extraLarge;
            }

            public void setIds(final List<String> ids) {
                this.ids = ids;
            }

            public void setMainTShirtSizeFieldId(final String mainTShirtSizeFieldId) {
                this.mainTShirtSizeFieldId = mainTShirtSizeFieldId;
            }

            public void setExtraSmall(final String extraSmall) {
                this.extraSmall = extraSmall;
            }

            public void setSmall(final String small) {
                this.small = small;
            }

            public void setMedium(final String medium) {
                this.medium = medium;
            }

            public void setLarge(final String large) {
                this.large = large;
            }

            public void setExtraLarge(final String extraLarge) {
                this.extraLarge = extraLarge;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                TShirtSize that = (TShirtSize) o;

                if (ids != null ? !ids.equals(that.ids) : that.ids != null) return false;
                if (mainTShirtSizeFieldId != null ? !mainTShirtSizeFieldId.equals(that.mainTShirtSizeFieldId) : that.mainTShirtSizeFieldId != null)
                    return false;
                if (extraSmall != null ? !extraSmall.equals(that.extraSmall) : that.extraSmall != null) return false;
                if (small != null ? !small.equals(that.small) : that.small != null) return false;
                if (medium != null ? !medium.equals(that.medium) : that.medium != null) return false;
                if (large != null ? !large.equals(that.large) : that.large != null) return false;
                return extraLarge != null ? extraLarge.equals(that.extraLarge) : that.extraLarge == null;
            }

            @Override
            public int hashCode() {
                int result = ids != null ? ids.hashCode() : 0;
                result = 31 * result + (mainTShirtSizeFieldId != null ? mainTShirtSizeFieldId.hashCode() : 0);
                result = 31 * result + (extraSmall != null ? extraSmall.hashCode() : 0);
                result = 31 * result + (small != null ? small.hashCode() : 0);
                result = 31 * result + (medium != null ? medium.hashCode() : 0);
                result = 31 * result + (large != null ? large.hashCode() : 0);
                result = 31 * result + (extraLarge != null ? extraLarge.hashCode() : 0);
                return result;
            }

            @Override
            public String toString() {
                return "TShirtSize{" +
                        "ids=" + ids +
                        ", mainTShirtSizeFieldId='" + mainTShirtSizeFieldId + '\'' +
                        ", extraSmall='" + extraSmall + '\'' +
                        ", small='" + small + '\'' +
                        ", medium='" + medium + '\'' +
                        ", large='" + large + '\'' +
                        ", extraLarge='" + extraLarge + '\'' +
                        '}';
            }
        }


        public static class Blocked extends CustomFieldDetails {
            @NotNull
            @DecimalMin("1")
            private Integer yesOptionId;

            public Integer getYesOptionId() {
                return this.yesOptionId;
            }

            public void setYesOptionId(final Integer yesOptionId) {
                this.yesOptionId = yesOptionId;
            }

            @Override
            public String toString() {
                return "Blocked{" +
                        "yesOptionId=" + yesOptionId +
                        '}';
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                if (!super.equals(o)) return false;

                Blocked blocked = (Blocked) o;

                return yesOptionId != null ? yesOptionId.equals(blocked.yesOptionId) : blocked.yesOptionId == null;
            }

            @Override
            public int hashCode() {
                int result = super.hashCode();
                result = 31 * result + (yesOptionId != null ? yesOptionId.hashCode() : 0);
                return result;
            }
        }

        public TShirtSize getTShirtSize() {
            return this.tShirtSize;
        }

        public ClassOfServiceDetails getClassOfService() {
            return this.classOfService;
        }

        public Blocked getBlocked() {
            return this.blocked;
        }

        public CustomFieldDetails getLastBlockReason() {
            return this.lastBlockReason;
        }

        public CustomFieldDetails getCoAssignees() {
            return this.coAssignees;
        }

        public CustomFieldDetails getRelease() {
            return this.release;
        }

        public CustomFieldDetails getAdditionalEstimatedHours() {
            return this.additionalEstimatedHours;
        }

        public void setTShirtSize(final TShirtSize tShirtSize) {
            this.tShirtSize = tShirtSize;
        }

        public void setClassOfService(final ClassOfServiceDetails classOfService) {
            this.classOfService = classOfService;
        }

        public void setBlocked(final Blocked blocked) {
            this.blocked = blocked;
        }

        public void setLastBlockReason(final CustomFieldDetails lastBlockReason) {
            this.lastBlockReason = lastBlockReason;
        }

        public void setCoAssignees(final CustomFieldDetails coAssignees) {
            this.coAssignees = coAssignees;
        }

        public void setRelease(final CustomFieldDetails release) {
            this.release = release;
        }

        public void setAdditionalEstimatedHours(final CustomFieldDetails additionalEstimatedHours) {
            this.additionalEstimatedHours = additionalEstimatedHours;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CustomField that = (CustomField) o;

            if (tShirtSize != null ? !tShirtSize.equals(that.tShirtSize) : that.tShirtSize != null) return false;
            if (classOfService != null ? !classOfService.equals(that.classOfService) : that.classOfService != null)
                return false;
            if (blocked != null ? !blocked.equals(that.blocked) : that.blocked != null) return false;
            if (lastBlockReason != null ? !lastBlockReason.equals(that.lastBlockReason) : that.lastBlockReason != null)
                return false;
            if (coAssignees != null ? !coAssignees.equals(that.coAssignees) : that.coAssignees != null) return false;
            if (release != null ? !release.equals(that.release) : that.release != null) return false;
            return additionalEstimatedHours != null ? additionalEstimatedHours.equals(that.additionalEstimatedHours) : that.additionalEstimatedHours == null;
        }

        @Override
        public int hashCode() {
            int result = tShirtSize != null ? tShirtSize.hashCode() : 0;
            result = 31 * result + (classOfService != null ? classOfService.hashCode() : 0);
            result = 31 * result + (blocked != null ? blocked.hashCode() : 0);
            result = 31 * result + (lastBlockReason != null ? lastBlockReason.hashCode() : 0);
            result = 31 * result + (coAssignees != null ? coAssignees.hashCode() : 0);
            result = 31 * result + (release != null ? release.hashCode() : 0);
            result = 31 * result + (additionalEstimatedHours != null ? additionalEstimatedHours.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "CustomField{" +
                    "tShirtSize=" + tShirtSize +
                    ", classOfService=" + classOfService +
                    ", blocked=" + blocked +
                    ", lastBlockReason=" + lastBlockReason +
                    ", coAssignees=" + coAssignees +
                    ", release=" + release +
                    ", additionalEstimatedHours=" + additionalEstimatedHours +
                    '}';
        }
    }

    public static class Wip {
        private List<Long> ignoreIssuetypesIds = new ArrayList<>();

        public List<Long> getIgnoreIssuetypesIds() {
            return this.ignoreIssuetypesIds;
        }

        public void setIgnoreIssuetypesIds(List<Long> ignoreIssuetypesIds) {
            this.ignoreIssuetypesIds = ignoreIssuetypesIds;
        }
    }

    public static class IssueLink {
        @NotNull
        private List<String> dependencies;

        @NotNull
        @DecimalMin("1")
        private Integer demandId;

        public List<String> getDependencies() {
            return this.dependencies;
        }

        public Integer getDemandId() {
            return this.demandId;
        }

        public void setDependencies(final List<String> dependencies) {
            this.dependencies = dependencies;
        }

        public void setDemandId(final Integer demandId) {
            this.demandId = demandId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            IssueLink issueLink = (IssueLink) o;

            if (dependencies != null ? !dependencies.equals(issueLink.dependencies) : issueLink.dependencies != null)
                return false;
            return demandId != null ? demandId.equals(issueLink.demandId) : issueLink.demandId == null;
        }

        @Override
        public int hashCode() {
            int result = dependencies != null ? dependencies.hashCode() : 0;
            result = 31 * result + (demandId != null ? demandId.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "IssueLink{" +
                    "dependencies=" + dependencies +
                    ", demandId=" + demandId +
                    '}';
        }
    }


    public static class IssueType {
        @NotNull
        @Valid
        private IssueTypeDetails demand;

        @NotNull
        @Valid
        private IssueTypeDetails defaultFeature;

        @NotNull
        private List<IssueTypeDetails> features;


        public static class IssueTypeDetails {
            public IssueTypeDetails(long taskissuetype) {
                id = taskissuetype;
            }
            public IssueTypeDetails() {}

            @NotNull
            @DecimalMin("1")
            private long id;

            public long getId() {
                return this.id;
            }

            public void setId(final long id) {
                this.id = id;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                IssueTypeDetails that = (IssueTypeDetails) o;

                return id == that.id;
            }

            @Override
            public int hashCode() {
                return (int) (id ^ (id >>> 32));
            }

            @Override
            public String toString() {
                return "IssueTypeDetails{" +
                        "id=" + id +
                        '}';
            }
        }

        public IssueTypeDetails getDemand() {
            return this.demand;
        }

        public IssueTypeDetails getDefaultFeature() {
            return this.defaultFeature;
        }

        public List<IssueTypeDetails> getFeatures() {
            return this.features;
        }

        public void setDemand(final IssueTypeDetails demand) {
            this.demand = demand;
        }

        public void setDefaultFeature(final IssueTypeDetails defaultFeature) {
            this.defaultFeature = defaultFeature;
        }

        public void setFeatures(final List<IssueTypeDetails> features) {
            this.features = features;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            IssueType issueType = (IssueType) o;

            if (demand != null ? !demand.equals(issueType.demand) : issueType.demand != null) return false;
            if (defaultFeature != null ? !defaultFeature.equals(issueType.defaultFeature) : issueType.defaultFeature != null)
                return false;
            return features != null ? features.equals(issueType.features) : issueType.features == null;
        }

        @Override
        public int hashCode() {
            int result = demand != null ? demand.hashCode() : 0;
            result = 31 * result + (defaultFeature != null ? defaultFeature.hashCode() : 0);
            result = 31 * result + (features != null ? features.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "IssueType{" +
                    "demand=" + demand +
                    ", defaultFeature=" + defaultFeature +
                    ", features=" + features +
                    '}';
        }
    }


    public static class Resolutions {
        @NotNull
        @Valid
        private Resolution done;
        @NotNull
        @Valid
        private Resolution canceled;


        public static class Resolution {
            @NotNull
            @NotEmpty
            private String name;

            public String getName() {
                return this.name;
            }

            public void setName(final String name) {
                this.name = name;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Resolution that = (Resolution) o;

                return name != null ? name.equals(that.name) : that.name == null;
            }

            @Override
            public int hashCode() {
                return name != null ? name.hashCode() : 0;
            }

            @Override
            public String toString() {
                return "Resolution{" +
                        "name='" + name + '\'' +
                        '}';
            }
        }

        public Resolution getDone() {
            return this.done;
        }

        public Resolution getCanceled() {
            return this.canceled;
        }

        public void setDone(final Resolution done) {
            this.done = done;
        }

        public void setCanceled(final Resolution canceled) {
            this.canceled = canceled;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Resolutions that = (Resolutions) o;

            if (done != null ? !done.equals(that.done) : that.done != null) return false;
            return canceled != null ? canceled.equals(that.canceled) : that.canceled == null;
        }

        @Override
        public int hashCode() {
            int result = done != null ? done.hashCode() : 0;
            result = 31 * result + (canceled != null ? canceled.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Resolutions{" +
                    "done=" + done +
                    ", canceled=" + canceled +
                    '}';
        }
    }


    public static class BallparkMapping {
        @NotNull
        private String issueType;

        @NotNull
        private List<Long> jiraIssueTypes;

        @NotNull
        private String tshirtCustomFieldId;

        @NotNull
        private Integer valueStreamOrder;
        
        private Integer lastApplicableStatusId;

        public String getIssueType() {
            return this.issueType;
        }

        public List<Long> getJiraIssueTypes() {
            return this.jiraIssueTypes;
        }

        public String getTshirtCustomFieldId() {
            return this.tshirtCustomFieldId;
        }

        public Integer getValueStreamOrder() {
            return this.valueStreamOrder;
        }

        public void setIssueType(final String issueType) {
            this.issueType = issueType;
        }

        public void setJiraIssueTypes(final List<Long> jiraIssueTypes) {
            this.jiraIssueTypes = jiraIssueTypes;
        }

        public void setTshirtCustomFieldId(final String tshirtCustomFieldId) {
            this.tshirtCustomFieldId = tshirtCustomFieldId;
        }

        public void setValueStreamOrder(final Integer valueStreamOrder) {
            this.valueStreamOrder = valueStreamOrder;
        }
        
        public Optional<Integer> getLastApplicableStatusId() {
            return Optional.ofNullable(lastApplicableStatusId);
        }
        
        public void setLastApplicableStatusId(Integer lastApplicableStatusId) {
            this.lastApplicableStatusId = lastApplicableStatusId;
        }

        @Override
        public String toString() {
            return "BallparkMapping{" +
                    "issueType='" + issueType + '\'' +
                    ", jiraIssueTypes=" + jiraIssueTypes +
                    ", tshirtCustomFieldId='" + tshirtCustomFieldId + '\'' +
                    ", valueStreamOrder=" + valueStreamOrder + 
                    ", lastApplicableStatusId=" + lastApplicableStatusId +
                    '}';
        }
    }


    public static class ExecutionDataHistoryGenerator {
        @NotEmpty
        private String cron;
        private String timezone;

        public String getCron() {
            return this.cron;
        }

        public String getTimezone() {
            return this.timezone;
        }

        public void setCron(final String cron) {
            this.cron = cron;
        }

        public void setTimezone(final String timezone) {
            this.timezone = timezone;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ExecutionDataHistoryGenerator that = (ExecutionDataHistoryGenerator) o;

            if (cron != null ? !cron.equals(that.cron) : that.cron != null) return false;
            return timezone != null ? timezone.equals(that.timezone) : that.timezone == null;
        }

        @Override
        public int hashCode() {
            int result = cron != null ? cron.hashCode() : 0;
            result = 31 * result + (timezone != null ? timezone.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "ExecutionDataHistoryGenerator{" +
                    "cron='" + cron + '\'' +
                    ", timezone='" + timezone + '\'' +
                    '}';
        }
    }


    public static class Followup {
        @NotNull
        private Map<Long, List<BallparkMapping>> ballparkMappings = new LinkedHashMap<Long, List<BallparkMapping>>();

        @NotNull
        private List<Long> featureStatusThatDontGenerateBallpark = new LinkedList<>();

        @NotNull
        private List<Long> subtaskStatusThatDontPreventBallparkGeneration = new LinkedList<>();

        private List<Long> statusExcludedFromFollowup = new LinkedList<Long>();

        @NotNull
        private Long ballparkDefaultStatus;

        @NotNull
        @Valid
        private ExecutionDataHistoryGenerator executionDataHistoryGenerator;

        public List<Long> getStatusExcludedFromFollowup() {
            return statusExcludedFromFollowup;
        }

        public Map<Long, List<BallparkMapping>> getBallparkMappings() {
            return this.ballparkMappings;
        }

        public List<Long> getFeatureStatusThatDontGenerateBallpark() {
            return this.featureStatusThatDontGenerateBallpark;
        }

        public List<Long> getSubtaskStatusThatDontPreventBallparkGeneration() {
            return this.subtaskStatusThatDontPreventBallparkGeneration;
        }

        public Long getBallparkDefaultStatus() {
            return this.ballparkDefaultStatus;
        }

        public ExecutionDataHistoryGenerator getExecutionDataHistoryGenerator() {
            return this.executionDataHistoryGenerator;
        }

        public void setBallparkMappings(final Map<Long, List<BallparkMapping>> ballparkMappings) {
            this.ballparkMappings = ballparkMappings;
        }

        public void setFeatureStatusThatDontGenerateBallpark(final List<Long> featureStatusThatDontGenerateBallpark) {
            this.featureStatusThatDontGenerateBallpark = featureStatusThatDontGenerateBallpark;
        }

        public void setSubtaskStatusThatDontPreventBallparkGeneration(final List<Long> subtaskStatusThatDontPreventBallparkGeneration) {
            this.subtaskStatusThatDontPreventBallparkGeneration = subtaskStatusThatDontPreventBallparkGeneration;
        }

        public void setStatusExcludedFromFollowup(final List<Long> statusExcludedFromFollowup) {
            this.statusExcludedFromFollowup = statusExcludedFromFollowup;
        }

        public void setBallparkDefaultStatus(final Long ballparkDefaultStatus) {
            this.ballparkDefaultStatus = ballparkDefaultStatus;
        }

        public void setExecutionDataHistoryGenerator(final ExecutionDataHistoryGenerator executionDataHistoryGenerator) {
            this.executionDataHistoryGenerator = executionDataHistoryGenerator;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Followup followup = (Followup) o;

            if (ballparkMappings != null ? !ballparkMappings.equals(followup.ballparkMappings) : followup.ballparkMappings != null)
                return false;
            if (featureStatusThatDontGenerateBallpark != null ? !featureStatusThatDontGenerateBallpark.equals(followup.featureStatusThatDontGenerateBallpark) : followup.featureStatusThatDontGenerateBallpark != null)
                return false;
            if (subtaskStatusThatDontPreventBallparkGeneration != null ? !subtaskStatusThatDontPreventBallparkGeneration.equals(followup.subtaskStatusThatDontPreventBallparkGeneration) : followup.subtaskStatusThatDontPreventBallparkGeneration != null)
                return false;
            if (statusExcludedFromFollowup != null ? !statusExcludedFromFollowup.equals(followup.statusExcludedFromFollowup) : followup.statusExcludedFromFollowup != null)
                return false;
            if (ballparkDefaultStatus != null ? !ballparkDefaultStatus.equals(followup.ballparkDefaultStatus) : followup.ballparkDefaultStatus != null)
                return false;
            return executionDataHistoryGenerator != null ? executionDataHistoryGenerator.equals(followup.executionDataHistoryGenerator) : followup.executionDataHistoryGenerator == null;
        }

        @Override
        public int hashCode() {
            int result = ballparkMappings != null ? ballparkMappings.hashCode() : 0;
            result = 31 * result + (featureStatusThatDontGenerateBallpark != null ? featureStatusThatDontGenerateBallpark.hashCode() : 0);
            result = 31 * result + (subtaskStatusThatDontPreventBallparkGeneration != null ? subtaskStatusThatDontPreventBallparkGeneration.hashCode() : 0);
            result = 31 * result + (statusExcludedFromFollowup != null ? statusExcludedFromFollowup.hashCode() : 0);
            result = 31 * result + (ballparkDefaultStatus != null ? ballparkDefaultStatus.hashCode() : 0);
            result = 31 * result + (executionDataHistoryGenerator != null ? executionDataHistoryGenerator.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Followup{" +
                    "ballparkMappings=" + ballparkMappings +
                    ", featureStatusThatDontGenerateBallpark=" + featureStatusThatDontGenerateBallpark +
                    ", subtaskStatusThatDontPreventBallparkGeneration=" + subtaskStatusThatDontPreventBallparkGeneration +
                    ", statusExcludedFromFollowup=" + statusExcludedFromFollowup +
                    ", ballparkDefaultStatus=" + ballparkDefaultStatus +
                    ", executionDataHistoryGenerator=" + executionDataHistoryGenerator +
                    '}';
        }
    }

    public static class SubtaskCreation {
        @NotNull
        private Long statusIdFrom;
        @NotNull
        private Long statusIdTo;
        @NotNull
        private Long issueTypeParentId;
        @NotNull
        private Long issueTypeId;
        @NotNull
        private String summaryPrefix;
        @NotNull
        private String tShirtSizeParentId;
        @NotNull
        private String tShirtSizeSubtaskId;
        @NotNull
        private String tShirtSizeDefaultValue = "M";
        private Optional<Long> transitionId;
        @Valid
        private CustomFieldCondition customFieldCondition;
        @NotNull
        private Boolean skipCreationWhenTShirtParentIsAbsent = false;

        public SubtaskCreation() {
        }

        public Long getStatusIdFrom() {
            return this.statusIdFrom;
        }

        public Long getStatusIdTo() {
            return this.statusIdTo;
        }

        public Long getIssueTypeParentId() {
            return this.issueTypeParentId;
        }

        public Long getIssueTypeId() {
            return this.issueTypeId;
        }

        public String getSummaryPrefix() {
            return this.summaryPrefix;
        }

        public String getTShirtSizeParentId() {
            return this.tShirtSizeParentId;
        }

        public String getTShirtSizeSubtaskId() {
            return this.tShirtSizeSubtaskId;
        }

        public String getTShirtSizeDefaultValue() {
            return this.tShirtSizeDefaultValue;
        }

        public Optional<Long> getTransitionId() {
            return this.transitionId;
        }

        public void setStatusIdFrom(final Long statusIdFrom) {
            this.statusIdFrom = statusIdFrom;
        }

        public void setStatusIdTo(final Long statusIdTo) {
            this.statusIdTo = statusIdTo;
        }

        public void setIssueTypeParentId(final Long issueTypeParentId) {
            this.issueTypeParentId = issueTypeParentId;
        }

        public void setIssueTypeId(final Long issueTypeId) {
            this.issueTypeId = issueTypeId;
        }

        public void setSummaryPrefix(final String summaryPrefix) {
            this.summaryPrefix = summaryPrefix;
        }

        public void setTShirtSizeParentId(final String tShirtSizeParentId) {
            this.tShirtSizeParentId = tShirtSizeParentId;
        }

        public void setTShirtSizeSubtaskId(final String tShirtSizeSubtaskId) {
            this.tShirtSizeSubtaskId = tShirtSizeSubtaskId;
        }

        public void setTShirtSizeDefaultValue(final String tShirtSizeDefaultValue) {
            this.tShirtSizeDefaultValue = tShirtSizeDefaultValue;
        }

        public void setTransitionId(final Optional<Long> transitionId) {
            this.transitionId = transitionId;
        }

        public Optional<CustomFieldCondition> getCustomFieldCondition() {
            return Optional.ofNullable(customFieldCondition);
        }

        public void setCustomFieldCondition(CustomFieldCondition customFieldCondition) {
            this.customFieldCondition = customFieldCondition;
        }
        
        public Boolean getSkipCreationWhenTShirtParentIsAbsent() {
            return this.skipCreationWhenTShirtParentIsAbsent;
        }
        
        public void setSkipCreationWhenTShirtParentIsAbsent(Boolean skipCreationWhenTShirtParentIsAbsent) {
            this.skipCreationWhenTShirtParentIsAbsent = skipCreationWhenTShirtParentIsAbsent;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SubtaskCreation that = (SubtaskCreation) o;

            if (statusIdFrom != null ? !statusIdFrom.equals(that.statusIdFrom) : that.statusIdFrom != null)
                return false;
            if (statusIdTo != null ? !statusIdTo.equals(that.statusIdTo) : that.statusIdTo != null)
                return false;
            if (issueTypeParentId != null ? !issueTypeParentId.equals(that.issueTypeParentId) : that.issueTypeParentId != null)
                return false;
            if (issueTypeId != null ? !issueTypeId.equals(that.issueTypeId) : that.issueTypeId != null)
                return false;
            if (summaryPrefix != null ? !summaryPrefix.equals(that.summaryPrefix) : that.summaryPrefix != null)
                return false;
            if (tShirtSizeParentId != null ? !tShirtSizeParentId.equals(that.tShirtSizeParentId) : that.tShirtSizeParentId != null)
                return false;
            if (tShirtSizeSubtaskId != null ? !tShirtSizeSubtaskId.equals(that.tShirtSizeSubtaskId) : that.tShirtSizeSubtaskId != null)
                return false;
            if (tShirtSizeDefaultValue != null ? !tShirtSizeDefaultValue.equals(that.tShirtSizeDefaultValue) : that.tShirtSizeDefaultValue != null)
                return false;
            if (transitionId != null ? !transitionId.equals(that.transitionId) : that.transitionId == null)//NOSONAR
                return false;
            if (customFieldCondition != null ? !customFieldCondition.equals(that.customFieldCondition) : that.customFieldCondition == null)
                return false;
            if (skipCreationWhenTShirtParentIsAbsent != null ? !skipCreationWhenTShirtParentIsAbsent.equals(that.skipCreationWhenTShirtParentIsAbsent) : that.skipCreationWhenTShirtParentIsAbsent == null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = statusIdFrom != null ? statusIdFrom.hashCode() : 0;
            result = 31 * result + (statusIdTo != null ? statusIdTo.hashCode() : 0);
            result = 31 * result + (issueTypeParentId != null ? issueTypeParentId.hashCode() : 0);
            result = 31 * result + (issueTypeId != null ? issueTypeId.hashCode() : 0);
            result = 31 * result + (summaryPrefix != null ? summaryPrefix.hashCode() : 0);
            result = 31 * result + (tShirtSizeParentId != null ? tShirtSizeParentId.hashCode() : 0);
            result = 31 * result + (tShirtSizeSubtaskId != null ? tShirtSizeSubtaskId.hashCode() : 0);
            result = 31 * result + (tShirtSizeDefaultValue != null ? tShirtSizeDefaultValue.hashCode() : 0);
            result = 31 * result + (transitionId != null ? transitionId.hashCode() : 0);//NOSONAR
            result = 31 * result + (customFieldCondition != null ? customFieldCondition.hashCode() : 0);
            result = 31 * result + (skipCreationWhenTShirtParentIsAbsent != null ? skipCreationWhenTShirtParentIsAbsent.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "SubtaskCreation{" +
                    "statusIdFrom=" + statusIdFrom +
                    ", statusIdTo=" + statusIdTo +
                    ", issueTypeParentId=" + issueTypeParentId +
                    ", issueTypeId=" + issueTypeId +
                    ", summaryPrefix='" + summaryPrefix + '\'' +
                    ", tShirtSizeParentId='" + tShirtSizeParentId + '\'' +
                    ", tShirtSizeSubtaskId='" + tShirtSizeSubtaskId + '\'' +
                    ", tShirtSizeDefaultValue='" + tShirtSizeDefaultValue + '\'' +
                    ", transitionId=" + transitionId +
                    ", customFieldCondition='" + customFieldCondition + '\'' +
                    ", skipCreationWhenTShirtParentIsAbsent='" + skipCreationWhenTShirtParentIsAbsent + '\'' +
                    '}';
        }

        public static class CustomFieldCondition {
            @NotEmpty
            private String id;
            @NotNull
            private String value;

            public String getId() {
                return id;
            }
            public void setId(String id) {
                this.id = id;
            }
            public String getValue() {
                return value;
            }
            public void setValue(String value) {
                this.value = value;
            }
        }

    }

    public StatusPriorityOrder getStatusPriorityOrder() {
        return statusPriorityOrder;
    }

    public boolean isDemand(Issue i) {
        return getIssuetype().getDemand().id == i.getType();
    }

    public String getUrl() {
        return this.url;
    }

    public Lousa getLousa() {
        return this.lousa;
    }

    public CustomField getCustomfield() {
        return this.customfield;
    }

    public IssueLink getIssuelink() {
        return this.issuelink;
    }

    public IssueType getIssuetype() {
        return this.issuetype;
    }

    public List<Long> getStatusesCompletedIds() {
        return this.statusesCompletedIds;
    }

    public List<Long> getStatusesCanceledIds() {
        return this.statusesCanceledIds;
    }

    public List<Long> getStatusesDeferredIds() {
        return this.statusesDeferredIds;
    }

    public List<String> getTransitionsWithRequiredCommentNames() {
        return this.transitionsWithRequiredCommentNames;
    }

    public List<String> getTransitionsDoneNames() {
        return this.transitionsDoneNames;
    }

    public List<String> getTransitionsCancelNames() {
        return this.transitionsCancelNames;
    }

    public Resolutions getResolutions() {
        return this.resolutions;
    }

    public Followup getFollowup() {
        return this.followup;
    }

    public Wip getWip() {
        return this.wip;
    }

    public void setWip(Wip wip) {
        this.wip = wip;
    }

    public List<SubtaskCreation> getSubtaskCreation() {
        return this.subtaskCreation;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public void setLousa(final Lousa lousa) {
        this.lousa = lousa;
    }

    public void setCustomfield(final CustomField customfield) {
        this.customfield = customfield;
    }

    public void setIssuelink(final IssueLink issuelink) {
        this.issuelink = issuelink;
    }

    public void setIssuetype(final IssueType issuetype) {
        this.issuetype = issuetype;
    }

    public void setStatusesCompletedIds(final List<Long> statusesCompletedIds) {
        this.statusesCompletedIds = statusesCompletedIds;
    }

    public void setStatusesCanceledIds(final List<Long> statusesCanceledIds) {
        this.statusesCanceledIds = statusesCanceledIds;
    }
    
    public void setStatusesDeferredIds(final List<Long> statusesDeferredIds) {
        this.statusesDeferredIds = statusesDeferredIds;
    }

    public void setTransitionsWithRequiredCommentNames(final List<String> transitionsWithRequiredCommentNames) {
        this.transitionsWithRequiredCommentNames = transitionsWithRequiredCommentNames;
    }

    public void setTransitionsDoneNames(final List<String> transitionsDoneNames) {
        this.transitionsDoneNames = transitionsDoneNames;
    }

    public void setTransitionsCancelNames(final List<String> transitionsCancelNames) {
        this.transitionsCancelNames = transitionsCancelNames;
    }

    public void setResolutions(final Resolutions resolutions) {
        this.resolutions = resolutions;
    }

    public void setFollowup(final Followup followup) {
        this.followup = followup;
    }

    public void setStatusPriorityOrder(final StatusPriorityOrder statusPriorityOrder) {
        this.statusPriorityOrder = statusPriorityOrder;
    }

    public void setSubtaskCreation(final List<SubtaskCreation> subtaskCreation) {
        this.subtaskCreation = subtaskCreation;
    }

    public Set<String> getSubtaskCreatorRequiredFieldsIds() {
        return subtaskCreation.stream()
                .filter(p -> p.getCustomFieldCondition().isPresent())
                .map(p -> p.getCustomFieldCondition().get().getId())
                .collect(toSet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JiraProperties that = (JiraProperties) o;

        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        if (lousa != null ? !lousa.equals(that.lousa) : that.lousa != null) return false;
        if (customfield != null ? !customfield.equals(that.customfield) : that.customfield != null) return false;
        if (issuelink != null ? !issuelink.equals(that.issuelink) : that.issuelink != null) return false;
        if (issuetype != null ? !issuetype.equals(that.issuetype) : that.issuetype != null) return false;
        if (statusesCompletedIds != null ? !statusesCompletedIds.equals(that.statusesCompletedIds) : that.statusesCompletedIds != null)
            return false;
        if (statusesCanceledIds != null ? !statusesCanceledIds.equals(that.statusesCanceledIds) : that.statusesCanceledIds != null)
            return false;
        if (transitionsWithRequiredCommentNames != null ? !transitionsWithRequiredCommentNames.equals(that.transitionsWithRequiredCommentNames) : that.transitionsWithRequiredCommentNames != null)
            return false;
        if (transitionsDoneNames != null ? !transitionsDoneNames.equals(that.transitionsDoneNames) : that.transitionsDoneNames != null)
            return false;
        if (transitionsCancelNames != null ? !transitionsCancelNames.equals(that.transitionsCancelNames) : that.transitionsCancelNames != null)
            return false;
        if (resolutions != null ? !resolutions.equals(that.resolutions) : that.resolutions != null) return false;
        if (followup != null ? !followup.equals(that.followup) : that.followup != null) return false;
        if (statusPriorityOrder != null ? !statusPriorityOrder.equals(that.statusPriorityOrder) : that.statusPriorityOrder != null)
            return false;
        return subtaskCreation != null ? subtaskCreation.equals(that.subtaskCreation) : that.subtaskCreation == null;
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (lousa != null ? lousa.hashCode() : 0);
        result = 31 * result + (customfield != null ? customfield.hashCode() : 0);
        result = 31 * result + (issuelink != null ? issuelink.hashCode() : 0);
        result = 31 * result + (issuetype != null ? issuetype.hashCode() : 0);
        result = 31 * result + (statusesCompletedIds != null ? statusesCompletedIds.hashCode() : 0);
        result = 31 * result + (statusesCanceledIds != null ? statusesCanceledIds.hashCode() : 0);
        result = 31 * result + (transitionsWithRequiredCommentNames != null ? transitionsWithRequiredCommentNames.hashCode() : 0);
        result = 31 * result + (transitionsDoneNames != null ? transitionsDoneNames.hashCode() : 0);
        result = 31 * result + (transitionsCancelNames != null ? transitionsCancelNames.hashCode() : 0);
        result = 31 * result + (resolutions != null ? resolutions.hashCode() : 0);
        result = 31 * result + (followup != null ? followup.hashCode() : 0);
        result = 31 * result + (statusPriorityOrder != null ? statusPriorityOrder.hashCode() : 0);
        result = 31 * result + (subtaskCreation != null ? subtaskCreation.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "JiraProperties{" +
                "url='" + url + '\'' +
                ", lousa=" + lousa +
                ", customfield=" + customfield +
                ", issuelink=" + issuelink +
                ", issuetype=" + issuetype +
                ", statusesCompletedIds=" + statusesCompletedIds +
                ", statusesCanceledIds=" + statusesCanceledIds +
                ", transitionsWithRequiredCommentNames=" + transitionsWithRequiredCommentNames +
                ", transitionsDoneNames=" + transitionsDoneNames +
                ", transitionsCancelNames=" + transitionsCancelNames +
                ", resolutions=" + resolutions +
                ", followup=" + followup +
                ", statusPriorityOrder=" + statusPriorityOrder +
                ", subtaskCreation=" + subtaskCreation +
                '}';
    }

}
