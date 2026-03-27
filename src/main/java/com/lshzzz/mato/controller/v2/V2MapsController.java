package com.lshzzz.mato.controller.v2;

import com.lshzzz.mato.model.v2.V2CreateMapRequest;
import com.lshzzz.mato.model.v2.V2MapDetail;
import com.lshzzz.mato.model.v2.V2MapSummary;
import com.lshzzz.mato.service.v2.V2MapCatalogService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/maps")
@RequiredArgsConstructor
public class V2MapsController {

    private final V2MapCatalogService mapCatalogService;

    @GetMapping
    public List<V2MapSummary> getMaps(@RequestParam(required = false) String viewer) {
        return mapCatalogService.getMaps(viewer);
    }

    @GetMapping("/{mapId}")
    public V2MapDetail getMap(@PathVariable long mapId, @RequestParam(required = false) String viewer) {
        return mapCatalogService.getMap(mapId, viewer);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public V2MapDetail createMap(@Valid @RequestBody V2CreateMapRequest request) {
        return mapCatalogService.createMap(request);
    }

    @PutMapping("/{mapId}")
    public V2MapDetail updateMap(
        @PathVariable long mapId,
        @Valid @RequestBody V2CreateMapRequest request
    ) {
        return mapCatalogService.updateMap(mapId, request);
    }
}
