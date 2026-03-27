package com.lshzzz.mato.service.v2;

import com.lshzzz.mato.config.JpaConfig;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = "com.lshzzz.mato.model")
@EnableJpaRepositories(basePackages = "com.lshzzz.mato.repository")
@Import({
    JpaConfig.class,
    V2MapCatalogService.class,
    V2RoomRuntimeService.class,
    V2MapAssetStorageService.class
})
class V2ServiceTestApplication {
}
