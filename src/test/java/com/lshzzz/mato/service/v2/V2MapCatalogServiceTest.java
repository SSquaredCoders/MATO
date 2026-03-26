package com.lshzzz.mato.service.v2;

import static org.assertj.core.api.Assertions.assertThat;

import com.lshzzz.mato.model.v2.V2CreateMapRequest;
import com.lshzzz.mato.model.v2.V2MapSongDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

class V2MapCatalogServiceTest {

    private final V2MapCatalogService mapCatalogService = new V2MapCatalogService();

    @Test
    void hidesMapsThatBelongToOtherCreators() {
        var maps = mapCatalogService.getMaps("host-01");

        assertThat(maps).isEmpty();
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
                7,
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
        assertThat(created.hintRevealDelaySeconds()).isEqualTo(7);
        assertThat(created.songs()).hasSize(1);
        assertThat(mapCatalogService.getMap(created.id(), "host-01").songs())
            .singleElement()
            .extracting("title")
            .isEqualTo("Blue Bird");
    }

    @Test
    void hidesMapsCreatedByOtherUsers() {
        mapCatalogService.createMap(
            new V2CreateMapRequest(
                "Private Queue",
                "다른 유저 맵",
                "guest-77",
                "normal",
                "private",
                25,
                5,
                List.of(
                    new V2MapSongDefinition(
                        "힌트: 테스트 곡",
                        "Blue Bird",
                        "Ikimono-gakari",
                        List.of("blue bird"),
                        "youtube",
                        "https://youtu.be/example-blue-bird",
                        "Blue Bird demo"
                    )
                )
            )
        );

        assertThat(mapCatalogService.getMaps("host-01"))
            .extracting("name")
            .doesNotContain("Private Queue");
    }

    @Test
    void exposesOnlyMapsCreatedByViewer() {
        mapCatalogService.createMap(
            new V2CreateMapRequest(
                "My Queue",
                "viewer owned map",
                "host-01",
                "normal",
                "public",
                25,
                5,
                List.of(
                    new V2MapSongDefinition(
                        "Hint: my song",
                        "Blue Bird",
                        "Ikimono-gakari",
                        List.of("blue bird")
                    )
                )
            )
        );

        assertThat(mapCatalogService.getMaps("host-01"))
            .extracting("name")
            .containsExactly("My Queue");
    }
}
