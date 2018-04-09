package objective.taskboard.followup;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createTempDirectory;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static objective.taskboard.utils.IOUtilities.ENCODE_UTF_8;
import static objective.taskboard.utils.IOUtilities.asResource;
import static objective.taskboard.utils.IOUtilities.write;
import static objective.taskboard.utils.ZipUtils.unzip;
import static objective.taskboard.utils.ZipUtils.zip;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import objective.taskboard.database.directory.DataBaseDirectory;
import objective.taskboard.domain.FollowupDailySynthesis;
import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.repository.FollowupDailySynthesisRepository;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;
import objective.taskboard.utils.DateTimeUtils;

@Component
public class FollowUpDataHistoryRepository {
    public static final String PATH_FOLLOWUP_HISTORY = "followup-history";
    public static final String FILE_NAME_FORMAT = "yyyyMMdd";
    public static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern(FILE_NAME_FORMAT);
    public static final String EXTENSION_JSON = ".json";
    public static final String EXTENSION_ZIP = ".zip";

    private final DataBaseDirectory dataBaseDirectory;
    private final FollowupClusterProvider clusterProvider;
    private final FollowupDailySynthesisRepository followupDailySynthesisRepo;
    private final ProjectFilterConfigurationCachedRepository projectRepo;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ZonedDateTime.class, new DateTimeUtils.ZonedDateTimeAdapter())
            .setPrettyPrinting()
            .create();
    
    @Autowired
    public FollowUpDataHistoryRepository(
            DataBaseDirectory dataBaseDirectory, 
            FollowupClusterProvider clusterProvider,
            FollowupDailySynthesisRepository followupDailySynthesisRepo,
            ProjectFilterConfigurationCachedRepository projectRepo
            ) 
    {
        this.dataBaseDirectory = dataBaseDirectory;
        this.clusterProvider = clusterProvider;
        this.followupDailySynthesisRepo = followupDailySynthesisRepo;
        this.projectRepo = projectRepo;
    }

    private FollowupData sanitizeData(FollowupData data) {
        return new FollowupData(
                nullIfEmpty(data.fromJiraDs), 
                nullIfEmpty(data.analyticsTransitionsDsList),
                headerOnly(data.syntheticsTransitionsDsList));
    }

    public FollowUpDataSnapshot get(String date, ZoneId timezone, String... projectsKey) {
        return load(date, timezone, asList(projectsKey));
    }

    private FollowUpDataSnapshot load(String date, ZoneId timezone, List<String> projectsKey) {
        FollowUpDataLoader loader = new FollowUpDataLoader(gson, timezone);

        for (String project : projectsKey) {
            String fileZipName = date + EXTENSION_JSON + EXTENSION_ZIP;
            File fileZip = dataBaseDirectory.path(PATH_FOLLOWUP_HISTORY).resolve(project).resolve(fileZipName).toFile();
            if (!fileZip.exists())
                throw new IllegalStateException(fileZip.toString() + " not found");

            Path temp = null;
            try {
                temp = createTempDirectory(getClass().getSimpleName());
                unzip(fileZip, temp);

                File fileJSON = temp.resolve(date + EXTENSION_JSON).toFile();
                if (!fileJSON.exists())
                    throw new IllegalStateException(fileJSON.toString() + " not found");

                String json = IOUtils.toString(asResource(fileJSON).getInputStream(), ENCODE_UTF_8);
                JsonElement jsonElement = new JsonParser().parse(json);

                loader.load(jsonElement);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            } finally {
                if (temp != null)
                    deleteQuietly(temp.toFile());
            }
        }

        FollowupCluster cluster = clusterProvider.getForProject(projectsKey.get(0));
        FollowUpTimeline timeline = FollowUpTimeline.getTimeline(parseLocalDate(date), projectRepo.getProjectByKey(projectsKey.get(0)));
        return new FollowUpDataSnapshot(timeline, loader.create(), cluster);
    }

    private FollowUpDataSnapshot load(String date, ZoneId timezone, String project) {
        FollowUpDataLoader loader = new FollowUpDataLoader(gson, timezone);

        String fileZipName = date + EXTENSION_JSON + EXTENSION_ZIP;
        File fileZip = dataBaseDirectory.path(PATH_FOLLOWUP_HISTORY).resolve(project).resolve(fileZipName).toFile();
        if (!fileZip.exists())
            throw new IllegalStateException(fileZip.toString() + " not found");

        Path temp = null;
        try {
            temp = createTempDirectory(getClass().getSimpleName());
            unzip(fileZip, temp);

            File fileJSON = temp.resolve(date + EXTENSION_JSON).toFile();
            if (!fileJSON.exists())
                throw new IllegalStateException(fileJSON.toString() + " not found");

            String json = IOUtils.toString(asResource(fileJSON).getInputStream(), ENCODE_UTF_8);
            JsonElement jsonElement = new JsonParser().parse(json);

            loader.load(jsonElement);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        } finally {
            if (temp != null)
                deleteQuietly(temp.toFile());
        }

        FollowupCluster cluster = clusterProvider.getForProject(project);
        FollowUpTimeline timeline = FollowUpTimeline.getTimeline(parseLocalDate(date), projectRepo.getProjectByKey(project));
        return new FollowUpDataSnapshot(timeline, loader.create(), cluster);
    }
    
    public List<String> getHistoryGivenProjects(String... projectsKey) {
        return getHistoryGivenProjects(false, projectsKey);
    }

    public List<String> getHistoryGivenProjects(boolean includeToday, String... projectsKey) {
        return getHistoryGivenProjectsInFilesystem(asList(projectsKey), includeToday).stream().map(d->d.format(FILE_NAME_FORMATTER)).collect(Collectors.toList());
    }
    
    private List<LocalDate> getHistoryGivenProjectsInFilesystem(List<String> projectsKey, boolean includeToday) {
        return getHistoryGivenProjects(projectsKey, new FileSystemHistoryProvider(includeToday));
    }
    
    private List<LocalDate> getHistoryGivenProjectsFromDatabase(List<String> projectsKey) {
        return getHistoryGivenProjects(projectsKey, new DatabaseHistoryProvider());
    }
    
    private List<LocalDate> getHistoryGivenProjects(List<String> projectsKey, HistoryProvider provider) {
        if (projectsKey.isEmpty())
            return Collections.emptyList();

        Set<LocalDate> history = null;
        
        for (String projectKey : projectsKey) {
            Set<LocalDate> projectHistory = provider.getHistoryByProject(projectKey);

            if (history == null) {
                history = new HashSet<>(projectHistory);
            } else {
                history.retainAll(projectHistory);
            }
            
            if (history.isEmpty())
                return Collections.emptyList();
        }
        if (history == null)
            return Collections.emptyList();
        
        return history.stream().sorted().collect(toList());
    }

    private static FromJiraDataSet nullIfEmpty(FromJiraDataSet fromJiraDs) {
        if(isEmpty(fromJiraDs.rows)) {
            return null;
        }
        return new FromJiraDataSet(null, fromJiraDs.rows);
    }

    private static <T> List<T> nullIfEmpty(List<T> list) {//NOSONAR - sonar has bug and thinks this method is not used
        return isEmpty(list) ? null : list;
    }

    /**
     * Should not persist data, only headers
     * @param syntheticsDsList
     * @return
     */
    private static List<SyntheticTransitionsDataSet> headerOnly(List<SyntheticTransitionsDataSet> syntheticsDsList) {
        if(isEmpty(syntheticsDsList))
            return null;
        return syntheticsDsList.stream()
                .map(s -> new SyntheticTransitionsDataSet(s.issueType, s.headers, null))
                .collect(Collectors.toList());
    }

    public List<EffortHistoryRow> getHistoryRows(
            List<String> includeProjects, 
            Optional<String> optionalEndDate,
            FollowUpDataSnapshot lastSnapshot) 
    {
        Map<LocalDate, EffortHistoryRow> res = new LinkedHashMap<>();
        
        Optional<LocalDate> endDate = optionalEndDate.map(this::parseLocalDate);

        getHistoryGivenProjectsFromDatabase(includeProjects).stream()
                .filter(d -> endDate.map(end->d.isBefore(end)).orElse(true))
                .forEach(d -> {
                    includeProjects
                        .stream()
                        .map(p -> getEffortForProject(d,p))
                        .forEach(row -> res.put(row.date, row));
                });
        
        if (lastSnapshot != null) {
            EffortHistoryRow historyRow = lastSnapshot.getEffortHistoryRow();
            if (endDate.map(end->historyRow.date.isBefore(end)).orElse(true))
                res.put(historyRow.date, historyRow);
        }
        
        return new LinkedList<>(res.values());
    }
    
    private void calculateAndPersistEffortForProject(LocalDate localDate, String projectKey) {
        calculateAndPersistEffortForProject(localDate, projectKey, Optional.empty());
    }
    
    private void calculateAndPersistEffortForProject(LocalDate localDate, String projectKey, Optional<FollowUpDataSnapshot> snapshot) {
        Optional<ProjectFilterConfiguration> project = projectRepo.getProjectByKey(projectKey);
        if (!project.isPresent())
            throw new IllegalArgumentException("Project key " + project + " not registered");
        
        Optional<FollowupDailySynthesis> dailySynthesis = followupDailySynthesisRepo.findByFollowupDateAndProjectId(localDate, project.get().getId());
        if (dailySynthesis.isPresent())
            return;
        
        EffortHistoryRow effortHistoryRow =
                snapshot.orElseGet(()->load(localDate.format(FILE_NAME_FORMATTER), ZoneId.systemDefault(), projectKey))
                    .getEffortHistoryRow();
        
        followupDailySynthesisRepo.save(new FollowupDailySynthesis(
                project.get().getId(), 
                localDate, 
                effortHistoryRow.sumEffortDone, 
                effortHistoryRow.sumEffortBacklog));
    }
    
    private EffortHistoryRow getEffortForProject(LocalDate date, String projectKey) {
        Optional<ProjectFilterConfiguration> project = projectRepo.getProjectByKey(projectKey);
        if (!project.isPresent())
            throw new IllegalArgumentException("Project key " + project + " not registered");
        
        Optional<FollowupDailySynthesis> dailySynthesis = followupDailySynthesisRepo.findByFollowupDateAndProjectId(date, project.get().getId());
        if (!dailySynthesis.isPresent())
            throw new IllegalArgumentException(projectKey + " has no history for date " + date);
        return EffortHistoryRow.from(dailySynthesis.get());
    }

    private LocalDate parseLocalDate(String date) {
        return LocalDate.parse(date, FILE_NAME_FORMATTER);
    }

    public void save(String projectKey, FollowUpDataSnapshot followUpDataEntry) throws IOException {
        save(projectKey, followUpDataEntry, false);
    }
    
    public void save(String projectKey, FollowUpDataSnapshot followUpDataEntry, boolean migration) throws IOException {
        saveSnapshotInFile(projectKey, followUpDataEntry.getTimeline().getReference(), followUpDataEntry.getData(), migration);
        calculateAndPersistEffortForProject(
                followUpDataEntry.getTimeline().getReference(),
                projectKey,
                Optional.of(followUpDataEntry));
    }
    
    void saveSnapshotInFile(String projectKey, LocalDate date, FollowupData data) throws IOException {
        saveSnapshotInFile(projectKey, date, data, false);
    }
    
    void saveSnapshotInFile(String projectKey, LocalDate date, FollowupData data, boolean migration) throws IOException {
        Path pathProject = dataBaseDirectory.path(PATH_FOLLOWUP_HISTORY).resolve(projectKey);
        if (!pathProject.toFile().exists())
            createDirectories(pathProject);

        String dateString = date.format(FILE_NAME_FORMATTER);
        Path pathJSON = pathProject.resolve(dateString + EXTENSION_JSON + (migration ? ".migration" : ""));

        try {
            String json = gson.toJson(sanitizeData(data));
            write(pathJSON.toFile(), json);

            Path pathZIP = Paths.get(pathJSON.toString() + EXTENSION_ZIP);
            deleteQuietly(pathZIP.toFile());
            zip(pathJSON, pathZIP);
        } finally {
            deleteQuietly(pathJSON.toFile());
        }
    }

    public void syncEffortHistory(String projectKey) {
        List<String> includeProjects = Arrays.asList(projectKey);
        getHistoryGivenProjectsInFilesystem(includeProjects, false).stream()
                .forEach(date ->calculateAndPersistEffortForProject(date, projectKey));
    }
    
    private static interface HistoryProvider {
        Set<LocalDate> getHistoryByProject(String projectKey);
    }
    
    private class DatabaseHistoryProvider implements HistoryProvider {
        @Override
        public Set<LocalDate> getHistoryByProject(String projectKey) {
            Optional<ProjectFilterConfiguration> project = projectRepo.getProjectByKey(projectKey);
            if (!project.isPresent())
                throw new IllegalArgumentException("Project key " + project + " not registered");
            
            List<FollowupDailySynthesis> synthesis = followupDailySynthesisRepo.findByFollowupProjectId(project.get().getId());
            
            return synthesis.stream().map(p->p.getFollowupDate()).collect(Collectors.toSet());
        }
    }
    
    private class FileSystemHistoryProvider implements HistoryProvider {
        private final boolean includeToday;
        
        public FileSystemHistoryProvider(boolean includeToday) {
            this.includeToday = includeToday;
        }

        @Override
        public Set<LocalDate> getHistoryByProject(String projectKey) {
            Path pathProject = dataBaseDirectory.path(PATH_FOLLOWUP_HISTORY).resolve(projectKey);
            if (!pathProject.toFile().exists())
                return Collections.emptySet();

            String fileExtension = EXTENSION_JSON + EXTENSION_ZIP;
            String today = DateTime.now().toString(FILE_NAME_FORMAT);
            
            try {
                return Files.walk(pathProject)
                        .map(Path::toFile)
                        .filter(file -> file.isFile())
                        .filter(file -> file.getName().toLowerCase().endsWith(fileExtension))
                        .map(file -> file.getName().replace(fileExtension, ""))
                        .filter(fileName -> includeToday ? true : !today.equals(fileName))
                        .map(d->parseLocalDate(d))
                        .collect(toSet());

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
