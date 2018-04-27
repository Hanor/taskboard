package objective.taskboard.project.config;

import static java.util.stream.Collectors.toList;
import static objective.taskboard.repository.PermissionRepository.ADMINISTRATIVE;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.jira.ProjectService;

@RestController
@RequestMapping("/ws/project/config")
public class ProjectConfigController {

    @Autowired
    private ProjectService projectService;

    @GetMapping("items")
    public List<ProjectListItemDto> getItems() {
        return projectService.getTaskboardProjects(ADMINISTRATIVE).stream()
                .map(ProjectListItemDto::from)
                .collect(toList());
    }
    
    @GetMapping("crud/{projectKey}/init-data")
    public ResponseEntity<?> getCrudInitData(@PathVariable("projectKey") String projectKey) {
        Optional<ProjectFilterConfiguration> configuration = projectService.getTaskboardProject(projectKey, ADMINISTRATIVE);
        
        if (!configuration.isPresent())
            return ResponseEntity.notFound().build();
        
        List<LocalDate> availableBaselineDates = projectService.getAvailableBaselineDates(projectKey);
        ProjectConfigurationDto config = ProjectConfigurationDto.from(configuration.get());

        return ResponseEntity.ok(new ProjectConfigurationDataDto(availableBaselineDates, config));
    }

    @PostMapping("crud/{projectKey}")
    public ResponseEntity<?> updateConfig(@PathVariable("projectKey") String projectKey, @RequestBody ProjectConfigurationDto dto) {
        Optional<ProjectFilterConfiguration> optConfiguration = projectService.getTaskboardProject(projectKey, ADMINISTRATIVE);
        if (!optConfiguration.isPresent())
            return ResponseEntity.notFound().build();

        ProjectFilterConfiguration configuration = optConfiguration.get();

        configuration.setStartDate(dto.startDate);
        configuration.setDeliveryDate(dto.deliveryDate);
        configuration.setArchived(dto.isArchived);
        configuration.setRiskPercentage(dto.risk == null ? null : dto.risk.divide(BigDecimal.valueOf(100)));
        configuration.setProjectionTimespan(dto.projectionTimespan);
        configuration.setBaselineDate(dto.baselineDate);

        projectService.saveTaskboardProject(configuration);

        return ResponseEntity.ok().build();
    }
    
    public static class ProjectListItemDto {
        public String projectKey;
        public Boolean isArchived;
        
        public static ProjectListItemDto from(ProjectFilterConfiguration configuration) {
            ProjectListItemDto dto = new ProjectListItemDto();
            dto.projectKey = configuration.getProjectKey();
            dto.isArchived = configuration.isArchived();
            return dto;
        }
    }
    
    public static class ProjectConfigurationDataDto {
        public List<LocalDate> availableBaselineDates;
        public ProjectConfigurationDto config;
        
        public ProjectConfigurationDataDto(List<LocalDate> availableBaselineDates, ProjectConfigurationDto config) {
            this.availableBaselineDates = availableBaselineDates;
            this.config = config;
        }
    }

    public static class ProjectConfigurationDto {
        public String projectKey;
        public LocalDate startDate;
        public LocalDate deliveryDate;
        public Boolean isArchived;
        public BigDecimal risk;
        public Integer projectionTimespan;
        public LocalDate baselineDate;

        public static ProjectConfigurationDto from(ProjectFilterConfiguration configuration) {
            ProjectConfigurationDto dto = new ProjectConfigurationDto();
            dto.projectKey = configuration.getProjectKey();
            dto.startDate = configuration.getStartDate().orElse(null);
            dto.deliveryDate = configuration.getDeliveryDate().orElse(null);
            dto.isArchived = configuration.isArchived();
            dto.risk = configuration.getRiskPercentage().multiply(BigDecimal.valueOf(100));
            dto.projectionTimespan = configuration.getProjectionTimespan();
            dto.baselineDate = configuration.getBaselineDate().orElse(null);

            return dto;
        }
    }
}
