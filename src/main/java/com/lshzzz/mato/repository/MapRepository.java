package com.lshzzz.mato.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lshzzz.mato.model.map.Map;

@Repository
public interface MapRepository extends JpaRepository<Map, Long> {
	List<Map> findByIsPublicTrue(); // 공개 맵 목록 조회

	boolean existsByName(String name);
	
	// 사용자별 맵 목록 조회
	List<Map> findByUserId(String userId);
	
	// 사용자별 공개 맵 목록 조회
	List<Map> findByUserIdAndIsPublicTrue(String userId);
}
