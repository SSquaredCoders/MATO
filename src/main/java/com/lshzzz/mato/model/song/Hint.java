package com.lshzzz.mato.model.song;

import com.lshzzz.mato.model.BaseEntity;
import com.lshzzz.mato.model.mapsongs.MapSong;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hints")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Hint extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "map_song_id", nullable = false)
	private MapSong mapSong;

	@Column(nullable = false)
	private String hintText; // 🎯 힌트 내용 (ex. "OST", "3글자")

	@Column(nullable = false)
	private int revealTime; // 🎯 몇 초 후에 힌트를 보여줄 것인지 설정 (ex. 10초 후)
}
