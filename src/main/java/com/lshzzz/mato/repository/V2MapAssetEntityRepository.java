package com.lshzzz.mato.repository;

import com.lshzzz.mato.model.v2.persistence.V2MapAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface V2MapAssetEntityRepository extends JpaRepository<V2MapAssetEntity, String> {
}
