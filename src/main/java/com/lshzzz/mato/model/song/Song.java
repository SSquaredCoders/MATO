package com.lshzzz.mato.model.song;

import com.lshzzz.mato.model.BaseEntity;
import com.lshzzz.mato.model.mapsongs.MapSong;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "songs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Song extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String title; // 노래 제목

	@Column(nullable = false, length = 50)
	private String artist; // 가수

	@Column(length = 50)
	private String composer; // 작곡가

	@Column(nullable = false)
	private String youtubeUrl; // 오디오 파일 URL (유튜브 OR 직접 업로드)

	// 업데이트 메서드
	public void update(String title, String artist, String composer, String youtubeUrl) {
		this.title = title;
		this.artist = artist;
		this.composer = composer;
		this.youtubeUrl = youtubeUrl;
	}
}
