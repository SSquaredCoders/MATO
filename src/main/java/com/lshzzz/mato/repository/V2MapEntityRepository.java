package com.lshzzz.mato.repository;

import com.lshzzz.mato.model.v2.persistence.V2MapEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface V2MapEntityRepository extends JpaRepository<V2MapEntity, Long> {

    List<V2MapEntity> findByCreatedByOrderByIdAsc(String createdBy);

    Optional<V2MapEntity> findByIdAndCreatedBy(long id, String createdBy);
}
