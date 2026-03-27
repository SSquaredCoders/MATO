package com.lshzzz.mato.service.v2;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest(
    classes = V2ServiceTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class V2MapAssetStorageServiceTest {

    @Autowired
    private V2MapAssetStorageService mapAssetStorageService;

    @BeforeEach
    void setUp() {
        mapAssetStorageService.resetForTests();
    }

    @Test
    void storesAssetMetadataAndFile() {
        var file = new MockMultipartFile(
            "file",
            "sample-audio.mp3",
            "audio/mpeg",
            "fake audio bytes".getBytes(StandardCharsets.UTF_8)
        );

        var stored = mapAssetStorageService.store(file);

        assertThat(stored.originalFileName()).isEqualTo("sample-audio.mp3");
        assertThat(stored.assetUrl()).isEqualTo("/api/v2/maps/assets/" + stored.assetId());
        assertThat(mapAssetStorageService.getAsset(stored.assetId()).contentType()).isEqualTo("audio/mpeg");
        assertThat(mapAssetStorageService.loadResource(stored.assetId()).exists()).isTrue();
    }
}
