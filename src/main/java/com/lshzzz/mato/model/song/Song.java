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

	@Column(nullable = false)
	private String youtubeUrl; // 오디오 파일 URL (유튜브 OR 직접 업로드)

	// 업데이트 메서드
	public void update(String youtubeUrl) {
		this.youtubeUrl = youtubeUrl;
	}
}
