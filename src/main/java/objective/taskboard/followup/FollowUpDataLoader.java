package objective.taskboard.followup;

import static java.util.Arrays.asList;
import static objective.taskboard.followup.FollowUpTransitionsDataProvider.HEADER_ISSUE_TYPE_COLUMN_NAME;

import java.lang.reflect.Type;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import objective.taskboard.Constants;

public class FollowUpDataLoader {
    
    private final Gson gson;
    private final V2_Loader internalLoader;
    
    public FollowUpDataLoader(Gson gson, ZoneId timezone) {
        this.gson = gson;
        this.internalLoader = new V2_Loader(timezone);
    }
    
    public void load(JsonElement element) {
        internalLoader.load(element);
    }
    
    public FollowUpData create() {
        return internalLoader.create();
    }

    private class V2_Loader extends BaseLoader {

        public V2_Loader(ZoneId timezone) {
            super(timezone);
        }

        public V2_Loader load(JsonElement element) {
            if(matchVersion(element, FollowUpData.Version.VERSION_2)) {
                doLoad(gson.fromJson(element, FollowUpData.class));
            } else {
                doLoad(new V1_Loader(timezone).load(element).upgrade());
            }
            return this;
        }
    }

    private class V1_Loader extends BaseLoader {

        public V1_Loader(ZoneId timezone) {
            super(timezone);
        }

        public V1_Loader load(JsonElement element) {
            if(matchVersion(element, FollowUpData.Version.VERSION_1)) {
                doLoad(gson.fromJson(element, FollowUpData.class));
            } else {
                doLoad(new V0_Loader().load(element).upgrade());
            }
            return this;
        }

        private FollowUpData upgrade() {
            for(String issueType : syntheticsHeaders.keySet()) {
                syntheticsHeaders.get(issueType).add(1, HEADER_ISSUE_TYPE_COLUMN_NAME);
            }
            return create();
        }
    }

    private abstract class BaseLoader {

        protected final ZoneId timezone;
        protected List<FromJiraDataRow> fromJira = new ArrayList<>();
        protected Set<String> types = new LinkedHashSet<>();
        protected ListMultimap<String, String> analyticsHeaders = LinkedListMultimap.create();
        protected ListMultimap<String, AnalyticsTransitionsDataRow> analytics = LinkedListMultimap.create();
        protected ListMultimap<String, String> syntheticsHeaders = LinkedListMultimap.create();

        protected BaseLoader(ZoneId timezone) {
            this.timezone = timezone;
        }

        protected void doLoad(FollowUpData data) {
            if (data.fromJiraDs != null)
                fromJira.addAll(data.fromJiraDs.rows);

            if(data.analyticsTransitionsDsList != null) {
                for (AnalyticsTransitionsDataSet analyticsDs : data.analyticsTransitionsDsList) {
                    final String type = analyticsDs.issueType;
                    types.add(type);
                    addHeaders(analyticsHeaders, type, analyticsDs.headers);
                    analyticsDs.rows.stream()
                            .map(this::convertDates)
                            .forEach(row -> analytics.put(type, row));
                }
            }
            if(data.syntheticsTransitionsDsList != null) {
                for (SyntheticTransitionsDataSet syntheticsDs : data.syntheticsTransitionsDsList) {
                    String type = syntheticsDs.issueType;
                    addHeaders(syntheticsHeaders, type, syntheticsDs.headers);
                }
            }
        }

        /**
         * Adds unique headers at most likely index.
         * E.g.:
         *   headerMap contains: Done, QA, Doing, To Do
         *   newHeaders contains: Done, Doing, To Do, Open
         *
         *   expected result: Done, QA, Doing, To Do, Open
         * @param headerMap where headers will be aggregated by type
         * @param type issue type to be used as key in headerMap
         * @param newHeaders new headers to be added
         */
        protected void addHeaders(ListMultimap<String, String> headerMap, String type, List<String> newHeaders) {
            int insertIndex = 0;
            for(String header : newHeaders) {
                if(headerMap.containsEntry(type, header)) {
                    insertIndex = headerMap.get(type).indexOf(header) + 1;
                } else {
                    headerMap.get(type).add(insertIndex++, header);
                }
            }
        }

        private AnalyticsTransitionsDataRow convertDates(AnalyticsTransitionsDataRow row) {
            List<ZonedDateTime> dates = row.transitionsDates.stream()
                    .map(date -> date != null ? date.withZoneSameInstant(timezone) : null)
                    .collect(Collectors.toList());
            return new AnalyticsTransitionsDataRow(row.issueKey, row.issueType, dates);
        }

        public FollowUpData create() {
            FromJiraDataSet fromJiraDs = new FromJiraDataSet(Constants.FROMJIRA_HEADERS, fromJira);
            List<AnalyticsTransitionsDataSet> analyticsDsList = new LinkedList<>();
            for(String type : types) {
                List<String> headers = new LinkedList<>(analyticsHeaders.get(type));
                List<AnalyticsTransitionsDataRow> rows = analytics.get(type);
                analyticsDsList.add(new AnalyticsTransitionsDataSet(type, headers, rows));
            }
            List<SyntheticTransitionsDataSet> syntheticDsList = new LinkedList<>();
            for (AnalyticsTransitionsDataSet dataset : analyticsDsList) {
                String type = dataset.issueType;
                List<String> headers = new LinkedList<>(syntheticsHeaders.get(type));
                String[] statuses = dataset.headers.subList(2, dataset.headers.size()).toArray(new String[0]);
                List<String> doneStatuses = headers.stream()
                        .filter(h -> h.contains("/"))
                        .flatMap(h -> asList(h.split("/")).stream())
                        .collect(Collectors.toList());
                syntheticDsList.add(FollowUpTransitionsDataProvider.getSyntheticTransitionsDs(headers, statuses, doneStatuses, dataset));
            }
            return new FollowUpData(fromJiraDs, analyticsDsList, syntheticDsList);
        }
    }

    private class V0_Loader {

        private List<FromJiraDataRow> data = new ArrayList<>();

        @SuppressWarnings("serial")
        private V0_Loader load(JsonElement element) {
            Type type = new TypeToken<List<FromJiraDataRow>>(){}.getType();
            data.addAll(gson.fromJson(element, type));
            return this;
        }

        private FollowUpData upgrade() {
            return new FollowUpData(new FromJiraDataSet(Constants.FROMJIRA_HEADERS, data), null, null);
        }
    }

    private static boolean matchVersion(JsonElement element, FollowUpData.Version expectedVersion) {
        if(!element.isJsonObject())
            return false;
        JsonObject obj = element.getAsJsonObject();
        if(!obj.has("followupDataVersion"))
            return false;
        JsonElement version = obj.get("followupDataVersion");
        if(!version.isJsonPrimitive() || !version.getAsJsonPrimitive().isString())
            return false;
        if(!Objects.equals(expectedVersion.value, version.getAsString()))
            return false;
        return true;
    }
}
