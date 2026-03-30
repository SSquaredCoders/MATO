package com.lshzzz.mato.repository;

import com.lshzzz.mato.model.v2.persistence.V2MapSongEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface V2MapSongEntityRepository extends JpaRepository<V2MapSongEntity, Long> {

    Page<V2MapSongEntity> findByMapIdAndMapCreatedByOrderBySongOrderAsc(
        long mapId,
        String createdBy,
        Pageable pageable
    );

    @Query("""
        select song
        from V2MapSongEntity song
        where song.map.id = :mapId
          and song.map.createdBy = :createdBy
          and (
            lower(song.title) like lower(concat('%', :query, '%'))
            or lower(song.artist) like lower(concat('%', :query, '%'))
            or lower(song.clue) like lower(concat('%', :query, '%'))
          )
        order by song.songOrder asc
        """)
    Page<V2MapSongEntity> searchByMapIdAndCreator(
        @Param("mapId") long mapId,
        @Param("createdBy") String createdBy,
        @Param("query") String query,
        Pageable pageable
    );
}
