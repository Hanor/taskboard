package objective.taskboard.testUtils;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class AbstractJpaRepositoryMock <T,E extends Serializable> implements JpaRepository<T, E> {
    @Override
    public Page<T> findAll(Pageable pageable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends T> S findOne(Example<S> example) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends T> long count(Example<S> example) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends T> boolean exists(Example<S> example) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<T> findAll(Sort sort) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends T> List<S> findAll(Example<S> example) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends T> List<S> findAll(Example<S> example, Sort sort) {
        throw new UnsupportedOperationException();
    }
    
    
    @Override
    public List<T> findAll() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public List<T> findAll(Iterable<E> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends T> S saveAndFlush(S entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteInBatch(Iterable<T> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAllInBatch() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T getOne(E id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends T> S save(S entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T findOne(E id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists(E id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long count() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(E id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(T entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Iterable<? extends T> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends T> List<S> save(Iterable<S> entities) {
        throw new UnsupportedOperationException();
    }
}
