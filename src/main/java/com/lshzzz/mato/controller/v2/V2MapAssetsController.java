package com.lshzzz.mato.controller.v2;

import com.lshzzz.mato.model.v2.V2MapAudioAsset;
import com.lshzzz.mato.service.v2.V2MapAssetStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v2/maps/assets")
@RequiredArgsConstructor
public class V2MapAssetsController {

    private final V2MapAssetStorageService mapAssetStorageService;

    @PostMapping
    public V2MapAudioAsset uploadAsset(@RequestParam("file") MultipartFile file) {
        return mapAssetStorageService.store(file);
    }

    @GetMapping("/{assetId}")
    public ResponseEntity<Resource> getAsset(@PathVariable String assetId) {
        V2MapAudioAsset asset = mapAssetStorageService.getAsset(assetId);
        MediaType mediaType = MediaType.parseMediaType(asset.contentType());
        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + asset.originalFileName() + "\"")
            .body(mapAssetStorageService.loadResource(assetId));
    }
}
