package objective.taskboard.followup.cluster;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import objective.taskboard.followup.data.Template;
import objective.taskboard.repository.TemplateRepository;

@Component
public class Test {

    @Autowired
    private FollowUpClusterItemRepository repo;
    
    @Autowired
    private TemplateRepository configRepo;
    
    @PostConstruct
    public void test() {
        Template config = configRepo.findById(2L);
        FollowUpClusterItem item = new FollowUpClusterItem(config, "Test TSize", "Feature", "M", 15.3f, 0.25f);
        repo.save(item);
        
        System.out.println("###################################### ok");
    }
}
