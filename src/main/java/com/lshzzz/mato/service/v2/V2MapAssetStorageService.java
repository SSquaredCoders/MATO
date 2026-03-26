package com.lshzzz.mato.service.v2;

import com.lshzzz.mato.model.v2.V2MapAudioAsset;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class V2MapAssetStorageService {

    private final Path storageRoot = Path.of(System.getProperty("user.dir"), "build", "v2-map-assets");
    private final Map<String, StoredAsset> assets = new ConcurrentHashMap<>();

    public V2MapAudioAsset store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "업로드할 파일이 필요합니다.");
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일을 저장하지 못했습니다.");
        }

        StoredAsset asset = new StoredAsset(
            assetId,
            originalFileName,
            target,
            Objects.requireNonNullElse(file.getContentType(), "application/octet-stream"),
            file.getSize()
        );
        assets.put(assetId, asset);
        return toResponse(asset);
    }

    public V2MapAudioAsset getAsset(String assetId) {
        return toResponse(getStoredAsset(assetId));
    }

    public Resource loadResource(String assetId) {
        return new FileSystemResource(getStoredAsset(assetId).path());
    }

    public String getContentType(String assetId) {
        return getStoredAsset(assetId).contentType();
    }

    private StoredAsset getStoredAsset(String assetId) {
        StoredAsset asset = assets.get(assetId);
        if (asset == null || !Files.exists(asset.path())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "오디오 파일을 찾을 수 없습니다.");
        }
        return asset;
    }

    private V2MapAudioAsset toResponse(StoredAsset asset) {
        return new V2MapAudioAsset(
            asset.assetId(),
            asset.originalFileName(),
            "/api/v2/maps/assets/" + asset.assetId(),
            asset.contentType(),
            asset.size()
        );
    }

    private String sanitizeFileName(String originalFileName) {
        String candidate = Objects.requireNonNullElse(originalFileName, "").trim();
        if (candidate.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 이름이 올바르지 않습니다.");
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

    private record StoredAsset(
        String assetId,
        String originalFileName,
        Path path,
        String contentType,
        long size
    ) {
    }
}
