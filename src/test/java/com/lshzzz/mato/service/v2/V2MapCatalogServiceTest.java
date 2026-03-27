package com.lshzzz.mato.service.v2;

import static org.assertj.core.api.Assertions.assertThat;

import com.lshzzz.mato.model.v2.V2CreateMapRequest;
import com.lshzzz.mato.model.v2.V2MapSongDefinition;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = V2ServiceTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class V2MapCatalogServiceTest {

    @Autowired
    private V2MapCatalogService mapCatalogService;

    @BeforeEach
    void setUp() {
        mapCatalogService.resetForTests();
    }

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
                "test-only queue",
                "host-01",
                "normal",
                "public",
                false,
                "single-lock",
                "advance-on-correct",
                35,
                7,
                List.of(
                    new V2MapSongDefinition(
                        "Hint: test song",
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
                "other user map",
                "guest-77",
                "normal",
                "private",
                false,
                "single-lock",
                "advance-on-correct",
                25,
                5,
                List.of(
                    new V2MapSongDefinition(
                        "Hint: hidden song",
                        "Blue Bird",
                        "Ikimono-gakari",
                        List.of("blue bird"),
                        "youtube",
                        "https://youtu.be/example-blue-bird",
                        "Blue Bird demo",
                        0,
                        null
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
                false,
                "single-lock",
                "advance-on-correct",
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
