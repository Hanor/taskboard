package objective.taskboard.repository;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import objective.taskboard.domain.FollowupDailySynthesis;

@Repository
@Transactional(readOnly = true)
class FollowupDailySynthesisJpaRepository implements FollowupDailySynthesisRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean exists(Integer projectId, LocalDate date) {
        return entityManager.createNamedQuery("FollowupDailySynthesis.count", Long.class)
                .setParameter("projectId", projectId)
                .setParameter("date", date)
                .getSingleResult() > 0;
    }
    
    @Override
    public List<FollowupDailySynthesis> listAllBefore(Integer projectId, LocalDate maxDateExclusive) {
        return entityManager.createNamedQuery("FollowupDailySynthesis.findByMaxDate", FollowupDailySynthesis.class)
                .setParameter("projectId", projectId)
                .setParameter("maxDate", maxDateExclusive)
                .getResultList();
    }

    @Override
    @Transactional
    public void add(FollowupDailySynthesis followupDailySynthesis) {
        entityManager.persist(followupDailySynthesis);
    }

    @Override
    @Transactional
    public void remove(Integer projectId, LocalDate date) {
        entityManager.createNamedQuery("FollowupDailySynthesis.delete")
            .setParameter("projectId", projectId)
            .setParameter("date", date)
            .executeUpdate();
    }
}
