package com.lshzzz.mato.model.v2.persistence;

import com.lshzzz.mato.model.BaseEntity;
import com.lshzzz.mato.model.v2.V2MapSongDefinition;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "v2_maps")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class V2MapEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 80)
    private String createdBy;

    @Column(nullable = false, length = 16)
    private String difficulty;

    @Column(nullable = false, length = 16)
    private String visibility;

    @Column
    private Boolean showMediaControls;

    @Column(length = 32)
    private String answerMode;

    @Column(length = 32)
    private String roundFlowMode;

    @Column(nullable = false)
    private Integer roundTimeLimitSeconds;

    @Column(nullable = false)
    private Integer hintRevealDelaySeconds;

    @OneToMany(mappedBy = "map", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("songOrder ASC, id ASC")
    private final List<V2MapSongEntity> songs = new ArrayList<>();

    private V2MapEntity(
        String name,
        String description,
        String createdBy,
        String difficulty,
        String visibility,
        boolean showMediaControls,
        String answerMode,
        String roundFlowMode,
        int roundTimeLimitSeconds,
        int hintRevealDelaySeconds
    ) {
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.difficulty = difficulty;
        this.visibility = visibility;
        this.showMediaControls = showMediaControls;
        this.answerMode = answerMode;
        this.roundFlowMode = roundFlowMode;
        this.roundTimeLimitSeconds = roundTimeLimitSeconds;
        this.hintRevealDelaySeconds = hintRevealDelaySeconds;
    }

    public static V2MapEntity create(
        String name,
        String description,
        String createdBy,
        String difficulty,
        String visibility,
        boolean showMediaControls,
        String answerMode,
        String roundFlowMode,
        int roundTimeLimitSeconds,
        int hintRevealDelaySeconds,
        List<V2MapSongDefinition> songs
    ) {
        V2MapEntity entity = new V2MapEntity(
            name,
            description,
            createdBy,
            difficulty,
            visibility,
            showMediaControls,
            answerMode,
            roundFlowMode,
            roundTimeLimitSeconds,
            hintRevealDelaySeconds
        );
        for (int index = 0; index < songs.size(); index += 1) {
            entity.addSong(V2MapSongEntity.create(index, songs.get(index)));
        }
        return entity;
    }

    public void update(
        String name,
        String description,
        String difficulty,
        String visibility,
        boolean showMediaControls,
        String answerMode,
        String roundFlowMode,
        int roundTimeLimitSeconds,
        int hintRevealDelaySeconds,
        List<V2MapSongDefinition> songs
    ) {
        this.name = name;
        this.description = description;
        this.difficulty = difficulty;
        this.visibility = visibility;
        this.showMediaControls = showMediaControls;
        this.answerMode = answerMode;
        this.roundFlowMode = roundFlowMode;
        this.roundTimeLimitSeconds = roundTimeLimitSeconds;
        this.hintRevealDelaySeconds = hintRevealDelaySeconds;
        this.songs.clear();
        for (int index = 0; index < songs.size(); index += 1) {
            addSong(V2MapSongEntity.create(index, songs.get(index)));
        }
    }

    private void addSong(V2MapSongEntity song) {
        songs.add(song);
        song.attachTo(this);
    }
}
