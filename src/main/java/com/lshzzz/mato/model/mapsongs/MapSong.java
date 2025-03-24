package com.lshzzz.mato.model.mapsongs;

import java.util.ArrayList;
import java.util.List;

import com.lshzzz.mato.model.BaseEntity;
import com.lshzzz.mato.model.map.Map;
import com.lshzzz.mato.model.song.Answer;
import com.lshzzz.mato.model.song.Hint;
import com.lshzzz.mato.model.song.Song;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "map_songs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MapSong extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "map_id", nullable = false)
	private Map map;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "song_id", nullable = false)
	private Song song;

	@Column(nullable = false)
	private Integer startTime; // 시작 시점 (초)

	@Column(nullable = false)
	private Integer endTime; // 종료 시점 (초)

	@Column(nullable = false)
	private Integer repeatCount; // 반복 횟수

	@OneToMany(mappedBy = "mapSong", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Answer> answers = new ArrayList<>();

	@OneToMany(mappedBy = "mapSong", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Hint> hints = new ArrayList<>();

	public void updateSong(Song newSong) {
		this.song = newSong;
	}

	public void updateTiming(Integer start, Integer end, Integer repeat) {
		this.startTime = start;
		this.endTime = end;
		this.repeatCount = repeat;
	}

}
