package com.lshzzz.mato.service.v2;

import com.lshzzz.mato.model.v2.V2MapAudioAsset;
import com.lshzzz.mato.model.v2.persistence.V2MapAssetEntity;
import com.lshzzz.mato.repository.V2MapAssetEntityRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class V2MapAssetStorageService {

    private final Path storageRoot = Path.of(System.getProperty("user.dir"), "build", "v2-map-assets");
    private final V2MapAssetEntityRepository mapAssetRepository;

    @Transactional
    public V2MapAudioAsset store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A file is required.");
        }

        String originalFileName = sanitizeFileName(file.getOriginalFilename());
        String extension = getExtension(originalFileName);
        String assetId = UUID.randomUUID().toString();
        String storedFileName = extension == null ? assetId : assetId + "." + extension;
        Path target = storageRoot.resolve(storedFileName);

        try {
            Files.createDirectories(storageRoot);
            file.transferTo(target);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store the uploaded file.");
        }

        V2MapAssetEntity asset = V2MapAssetEntity.create(
            assetId,
            originalFileName,
            storedFileName,
            Objects.requireNonNullElse(file.getContentType(), "application/octet-stream"),
            file.getSize()
        );
        return toResponse(mapAssetRepository.save(asset));
    }

    public V2MapAudioAsset getAsset(String assetId) {
        return toResponse(getStoredAsset(assetId));
    }

    public Resource loadResource(String assetId) {
        return new FileSystemResource(resolveAssetPath(getStoredAsset(assetId)));
    }

    public String getContentType(String assetId) {
        return getStoredAsset(assetId).getContentType();
    }

    @Transactional
    void resetForTests() {
        mapAssetRepository.deleteAll();
        try {
            if (!Files.exists(storageRoot)) {
                return;
            }
            Files.walk(storageRoot)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException exception) {
                        throw new IllegalStateException("Failed to clean asset storage.", exception);
                    }
                });
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to clean asset storage.", exception);
        }
    }

    private V2MapAssetEntity getStoredAsset(String assetId) {
        V2MapAssetEntity asset = mapAssetRepository.findById(assetId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found."));
        if (!Files.exists(resolveAssetPath(asset))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found.");
        }
        return asset;
    }

    private V2MapAudioAsset toResponse(V2MapAssetEntity asset) {
        return new V2MapAudioAsset(
            asset.getAssetId(),
            asset.getOriginalFileName(),
            "/api/v2/maps/assets/" + asset.getAssetId(),
            asset.getContentType(),
            asset.getSize()
        );
    }

    private String sanitizeFileName(String originalFileName) {
        String candidate = Objects.requireNonNullElse(originalFileName, "").trim();
        if (candidate.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name.");
        }
        return candidate.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String getExtension(String fileName) {
        int separatorIndex = fileName.lastIndexOf('.');
        if (separatorIndex < 0 || separatorIndex == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(separatorIndex + 1).toLowerCase();
    }

    private Path resolveAssetPath(V2MapAssetEntity asset) {
        return storageRoot.resolve(asset.getStoredFileName());
    }
}
