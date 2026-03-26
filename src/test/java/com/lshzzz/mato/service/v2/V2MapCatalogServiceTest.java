package com.lshzzz.mato.service.v2;

import static org.assertj.core.api.Assertions.assertThat;

import com.lshzzz.mato.model.v2.V2CreateMapRequest;
import com.lshzzz.mato.model.v2.V2MapSongDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

class V2MapCatalogServiceTest {

    private final V2MapCatalogService mapCatalogService = new V2MapCatalogService();

    @Test
    void exposesSeededMaps() {
        var maps = mapCatalogService.getMaps();

        assertThat(maps).isNotEmpty();
        assertThat(maps)
            .extracting("name")
            .contains("Anime Rush", "Boss Battle");
    }

    @Test
    void createsMapWithSongs() {
        var created = mapCatalogService.createMap(
            new V2CreateMapRequest(
                "Night Drive",
                "테스트용 신곡 맵",
                "host-01",
                "normal",
                "public",
                35,
                List.of(
                    new V2MapSongDefinition(
                        "문제: 테스트용 곡입니다.",
                        "Blue Bird",
                        "Ikimono-gakari",
                        List.of("blue bird")
                    )
                )
            )
        );

        assertThat(created.name()).isEqualTo("Night Drive");
        assertThat(created.songs()).hasSize(1);
        assertThat(mapCatalogService.getMap(created.id()).songs())
            .singleElement()
            .extracting("title")
            .isEqualTo("Blue Bird");
    }
}
