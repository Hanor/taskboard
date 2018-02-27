package objective.taskboard.testUtils;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import objective.taskboard.domain.TaskboardEntity;

public class JpaRepositoryMock<T extends TaskboardEntity> extends AbstractJpaRepositoryMock<T, Long> {
    
    private final Map<Long, T> data = new LinkedHashMap<>();
    private Long currentId = 0L;

    @Override
    public <S extends T> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(currentId++);
        } else if (currentId < entity.getId()) {
            currentId = entity.getId();
        }
        
        data.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public T findOne(Long id) {
        return data.get(id);
    }

    @Override
    public boolean exists(Long id) {
        return data.containsKey(id);
    }

    @Override
    public long count() {
        return data.size();
    }

    @Override
    public void delete(Long id) {
        data.remove(id);
    }

    @Override
    public void delete(T entity) {
        delete(entity.getId());
    }

    @Override
    public void delete(Iterable<? extends T> entities) {
        for (T e : entities)
            delete(e);
    }

    @Override
    public void deleteAll() {
        data.clear();
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(data.values());
    }

    @Override
    public List<T> findAll(Iterable<Long> ids) {
        Set<Long> idsSet = Sets.newHashSet(ids);
        return data.values().stream().filter(e -> idsSet.contains(e.getId())).collect(toList());
    }

    @Override
    public <S extends T> List<S> save(Iterable<S> entities) {
        for (T e : entities)
            save(e);

        return Lists.newArrayList(entities);
    }

    @Override
    public <S extends T> S saveAndFlush(S entity) {
        return save(entity);
    }

    @Override
    public void deleteInBatch(Iterable<T> entities) {
        delete(entities);
    }

    @Override
    public void deleteAllInBatch() {
        deleteAll();
    }

    @Override
    public T getOne(Long id) {
        return data.get(id);
    }

    @Override
    public void flush() {
    }
}
