package com.lshzzz.mato.model.map;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.lshzzz.mato.model.BaseEntity;
import com.lshzzz.mato.model.mapsongs.MapSong;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "maps")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Map extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private String userId; // 유저 ID (방장 또는 생성자)

	@Column(name = "name", nullable = false, length = 50)
	private String name; // 맵 이름

	@Column(name = "description", columnDefinition = "TEXT")
	private String description; // 맵 설명

	@Column(name = "map_status", nullable = false)
	private Boolean isPublic;

	@OneToMany(mappedBy = "map", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<MapSong> mapSongs = new ArrayList<>();

	public void update(String name, String description, Boolean isPublic) {
		this.name = name;
		this.description = description;
		this.isPublic = isPublic;
	}
}

