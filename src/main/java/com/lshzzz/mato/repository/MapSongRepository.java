package com.lshzzz.mato.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lshzzz.mato.model.map.Map;
import com.lshzzz.mato.model.mapsongs.MapSong;

@Repository
public interface MapSongRepository extends JpaRepository<MapSong, Long> {
	List<MapSong> findByMapId(Long mapId);

	List<MapSong> findByMap(Map map);
}
