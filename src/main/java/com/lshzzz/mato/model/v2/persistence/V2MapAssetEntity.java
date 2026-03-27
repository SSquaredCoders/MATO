package com.lshzzz.mato.model.v2.persistence;

import com.lshzzz.mato.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "v2_map_audio_assets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class V2MapAssetEntity extends BaseEntity {

    @Id
    @Column(name = "asset_id", nullable = false, length = 64)
    private String assetId;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 255)
    private String storedFileName;

    @Column(nullable = false, length = 150)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    private V2MapAssetEntity(
        String assetId,
        String originalFileName,
        String storedFileName,
        String contentType,
        long size
    ) {
        this.assetId = assetId;
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.contentType = contentType;
        this.size = size;
    }

    public static V2MapAssetEntity create(
        String assetId,
        String originalFileName,
        String storedFileName,
        String contentType,
        long size
    ) {
        return new V2MapAssetEntity(
            assetId,
            originalFileName,
            storedFileName,
            contentType,
            size
        );
    }
}
