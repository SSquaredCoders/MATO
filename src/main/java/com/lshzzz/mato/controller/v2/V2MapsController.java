package com.lshzzz.mato.controller.v2;

import com.lshzzz.mato.model.v2.V2CreateMapRequest;
import com.lshzzz.mato.model.v2.V2MapDetail;
import com.lshzzz.mato.model.v2.V2MapSongPage;
import com.lshzzz.mato.model.v2.V2MapSummary;
import com.lshzzz.mato.model.users.CustomUserDetails;
import com.lshzzz.mato.service.v2.V2MapCatalogService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    public List<V2MapSummary> getMaps(
        @RequestParam(required = false) String viewer,
        Authentication authentication
    ) {
        return mapCatalogService.getMaps(resolveViewer(viewer, authentication));
    }

    @GetMapping("/{mapId}")
    public V2MapDetail getMap(
        @PathVariable long mapId,
        @RequestParam(required = false) String viewer,
        @RequestParam(defaultValue = "true") boolean includeSongs,
        Authentication authentication
    ) {
        return mapCatalogService.getMap(
            mapId,
            resolveViewer(viewer, authentication),
            includeSongs
        );
    }

    @GetMapping("/{mapId}/songs")
    public V2MapSongPage getMapSongs(
        @PathVariable long mapId,
        @RequestParam(required = false) String viewer,
        @RequestParam(required = false) String query,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        Authentication authentication
    ) {
        return mapCatalogService.getMapSongs(
            mapId,
            resolveViewer(viewer, authentication),
            query,
            page,
            size
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public V2MapDetail createMap(
        @Valid @RequestBody V2CreateMapRequest request,
        Authentication authentication
    ) {
        return mapCatalogService.createMap(withCreator(request, requireNickname(authentication)));
    }

    @PutMapping("/{mapId}")
    public V2MapDetail updateMap(
        @PathVariable long mapId,
        @Valid @RequestBody V2CreateMapRequest request,
        Authentication authentication
    ) {
        return mapCatalogService.updateMap(mapId, withCreator(request, requireNickname(authentication)));
    }

    @DeleteMapping("/{mapId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMap(
        @PathVariable long mapId,
        @RequestParam(required = false) String viewer,
        Authentication authentication
    ) {
        mapCatalogService.deleteMap(mapId, resolveViewer(viewer, authentication));
    }

    private String resolveViewer(String viewer, Authentication authentication) {
        String authenticationNickname = extractNickname(authentication);
        if (authenticationNickname != null) {
            return authenticationNickname;
        }
        return viewer;
    }

    private String requireNickname(Authentication authentication) {
        String nickname = extractNickname(authentication);
        if (nickname == null || nickname.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Login required."
            );
        }
        return nickname;
    }

    private String extractNickname(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getNickname();
        }
        return null;
    }

    private V2CreateMapRequest withCreator(V2CreateMapRequest request, String createdBy) {
        return new V2CreateMapRequest(
            request.name(),
            request.description(),
            createdBy,
            request.difficulty(),
            request.visibility(),
            request.showMediaControls(),
            request.songOrderMode(),
            request.answerMode(),
            request.roundFlowMode(),
            request.roundTimeLimitSeconds(),
            request.skipVotesRequired(),
            request.hintRevealDelaySeconds(),
            request.songs()
        );
    }
}
