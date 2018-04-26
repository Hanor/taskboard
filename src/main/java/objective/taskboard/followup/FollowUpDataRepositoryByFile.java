package objective.taskboard.followup;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createTempDirectory;
import static java.util.stream.Collectors.toList;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import objective.taskboard.database.directory.DataBaseDirectory;
import objective.taskboard.utils.DateTimeUtils;

@Repository
public class FollowUpDataRepositoryByFile implements FollowUpDataRepository {
    public static final String PATH_FOLLOWUP_HISTORY = "followup-history";
    public static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final String EXTENSION_JSON = ".json";
    public static final String EXTENSION_ZIP = ".zip";

    private final DataBaseDirectory dataBaseDirectory;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ZonedDateTime.class, new DateTimeUtils.ZonedDateTimeAdapter())
            .setPrettyPrinting()
            .create();
    
    @Autowired
    public FollowUpDataRepositoryByFile(DataBaseDirectory dataBaseDirectory) {
        this.dataBaseDirectory = dataBaseDirectory;
    }

    private FollowUpData sanitizeData(FollowUpData data) {
        return new FollowUpData(
                nullIfEmpty(data.fromJiraDs), 
                nullIfEmpty(data.analyticsTransitionsDsList),
                headerOnly(data.syntheticsTransitionsDsList));
    }

    @Override
    public FollowUpData get(LocalDate date, ZoneId timezone, String projectKey) {
        FollowUpDataLoader loader = new FollowUpDataLoader(gson, timezone);
        String dateString = date.format(FILE_NAME_FORMATTER);

        String fileZipName = dateString + EXTENSION_JSON + EXTENSION_ZIP;
        File fileZip = dataBaseDirectory.path(PATH_FOLLOWUP_HISTORY).resolve(projectKey).resolve(fileZipName).toFile();
        if (!fileZip.exists())
            throw new IllegalStateException(fileZip.toString() + " not found");

        Path temp = null;
        try {
            temp = createTempDirectory(getClass().getSimpleName());
            unzip(fileZip, temp);

            File fileJSON = temp.resolve(dateString + EXTENSION_JSON).toFile();
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

        return loader.create();
    }

    @Override
    public List<LocalDate> getHistoryByProject(String projectKey) {
        Path pathProject = dataBaseDirectory.path(PATH_FOLLOWUP_HISTORY).resolve(projectKey);
        if (!pathProject.toFile().exists())
            return Collections.emptyList();

        String fileExtension = EXTENSION_JSON + EXTENSION_ZIP;
        LocalDate today = LocalDate.now();
        
        try {
            return Files.walk(pathProject)
                    .map(Path::toFile)
                    .filter(file -> file.isFile())
                    .filter(file -> file.getName().toLowerCase().endsWith(fileExtension))
                    .map(file -> parseLocalDate(file.getName().replace(fileExtension, "")))
                    .filter(date -> !date.equals(today))
                    .sorted()
                    .collect(toList());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    private static LocalDate parseLocalDate(String date) {
        return LocalDate.parse(date, FILE_NAME_FORMATTER);
    }

    @Override
    public void save(String projectKey, LocalDate date, FollowUpData data) {
        Path pathProject = dataBaseDirectory.path(PATH_FOLLOWUP_HISTORY).resolve(projectKey);
        if (!pathProject.toFile().exists()) {
            try {
                createDirectories(pathProject);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        String dateString = date.format(FILE_NAME_FORMATTER);
        Path pathJSON = pathProject.resolve(dateString + EXTENSION_JSON);

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
}
