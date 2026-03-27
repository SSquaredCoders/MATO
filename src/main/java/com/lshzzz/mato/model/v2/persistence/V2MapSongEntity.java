package com.lshzzz.mato.model.v2.persistence;

import com.lshzzz.mato.model.BaseEntity;
import com.lshzzz.mato.model.v2.V2MapSongDefinition;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "v2_map_songs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class V2MapSongEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "map_id", nullable = false)
    private V2MapEntity map;

    @Column(nullable = false)
    private Integer songOrder;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String clue;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 200)
    private String artist;

    @Column(length = 24)
    private String audioSourceType;

    @Column(length = 1000)
    private String audioSourceValue;

    @Column(length = 255)
    private String audioSourceLabel;

    @ElementCollection
    @CollectionTable(
        name = "v2_map_song_answers",
        joinColumns = @JoinColumn(name = "map_song_id")
    )
    @OrderColumn(name = "answer_order")
    @Column(name = "answer_value", nullable = false, length = 255)
    private List<String> answers = new ArrayList<>();

    private V2MapSongEntity(
        int songOrder,
        String clue,
        String title,
        String artist,
        List<String> answers,
        String audioSourceType,
        String audioSourceValue,
        String audioSourceLabel
    ) {
        this.songOrder = songOrder;
        this.clue = clue;
        this.title = title;
        this.artist = artist;
        this.answers = new ArrayList<>(answers);
        this.audioSourceType = audioSourceType;
        this.audioSourceValue = audioSourceValue;
        this.audioSourceLabel = audioSourceLabel;
    }

    public static V2MapSongEntity create(int songOrder, V2MapSongDefinition song) {
        return new V2MapSongEntity(
            songOrder,
            song.clue(),
            song.title(),
            song.artist(),
            song.answers(),
            song.audioSourceType(),
            song.audioSourceValue(),
            song.audioSourceLabel()
        );
    }

    void attachTo(V2MapEntity map) {
        this.map = map;
    }
}
