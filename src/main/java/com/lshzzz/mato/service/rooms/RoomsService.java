package com.lshzzz.mato.service.rooms;

import com.lshzzz.mato.exception.CustomException;
import com.lshzzz.mato.exception.ErrorCode;
import com.lshzzz.mato.model.map.Map;
import com.lshzzz.mato.model.room.Rooms;
import com.lshzzz.mato.model.room.dto.RoomsCreateRequest;
import com.lshzzz.mato.model.room.dto.RoomsResponse;
import com.lshzzz.mato.model.room.dto.RoomsUpdateRequest;
import com.lshzzz.mato.repository.RoomsRepository;
import com.lshzzz.mato.repository.MapRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomsService {

    private final RoomsRepository roomsRepository;
    private final MapRepository mapRepository;

    // 로그인 여부 확인 뒤 비회원 닉네임 랜덤 부여
    public String resolveNickname(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken)) {
            return authentication.getName();
        } else {
            HttpSession session = request.getSession();
            String guestNick = (String) session.getAttribute("guestNickname");
            if (guestNick == null) {
                guestNick = "게스트" + (int) (Math.random() * 9000 + 1000);
                session.setAttribute("guestNickname", guestNick);
            }
            return guestNick;
        }
    }

    @Transactional(readOnly = true)
    public List<RoomsResponse> findAllRooms() {
        return roomsRepository.findAll().stream()
            .map(RoomsResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoomsResponse findByName(String name) {
        return roomsRepository.findByName(name)
            .map(RoomsResponse::from)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<HashMap<String, Object>> getRoomParticipants(String name) {
        try {
            Rooms room = roomsRepository.findByName(name)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

            List<String> nicknames = room.getParticipantNicknames();
            List<Boolean> readyStatus = room.getParticipantReadyStatus();

            if (nicknames.size() != readyStatus.size()) {
                log.error("참가자 닉네임과 준비 상태 목록의 길이가 일치하지 않습니다: {} vs {}",
                    nicknames.size(), readyStatus.size());
                int minSize = Math.min(nicknames.size(), readyStatus.size());
                nicknames = nicknames.subList(0, minSize);
                readyStatus = readyStatus.subList(0, minSize);
            }

            List<HashMap<String, Object>> result = new ArrayList<>();
            for (int i = 0; i < nicknames.size(); i++) {
                HashMap<String, Object> participant = new HashMap<>();
                participant.put("nickname", nicknames.get(i));
                participant.put("ready", readyStatus.get(i));
                result.add(participant);
            }

            return result;
        } catch (Exception e) {
            log.error("참가자 목록 조회 중 오류 발생", e);
            throw new CustomException(ErrorCode.SERVER_ERROR);
        }
    }

    @Transactional
    public RoomsResponse createRoom(RoomsCreateRequest request, String hostNickname) {
        Map map = mapRepository.findById(request.mapId())
            .orElseThrow(() -> new CustomException(ErrorCode.MAP_NOT_FOUND));

        Rooms room = Rooms.builder()
            .name(request.name())
            .password(request.password())
            .maxParticipants(request.maxParticipants())
            .host(hostNickname)
            .map(map)
            .build();
        room.addParticipant(hostNickname);

        return RoomsResponse.from(roomsRepository.save(room));
    }

    @Transactional
    public RoomsResponse updateRoom(Long roomId, RoomsUpdateRequest request, String nickname) {
        Rooms room = roomsRepository.findById(roomId)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.getHost().equals(nickname)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Map map = mapRepository.findById(request.mapId())
            .orElseThrow(() -> new CustomException(ErrorCode.MAP_NOT_FOUND));

        room.updateName(request.name());
        room.updatePassword(request.password());
        room.updateMap(map);

        return RoomsResponse.from(room);
    }

    @Transactional
    public void deleteRoom(Long roomId, String nickname) {
        Rooms room = roomsRepository.findById(roomId)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.getHost().equals(nickname)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        roomsRepository.delete(room);
    }

    @Transactional(readOnly = true)
    public boolean validatePassword(String roomName, String inputPassword) {
        Rooms room = roomsRepository.findByName(roomName)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (room.getPassword() == null || room.getPassword().isBlank()) {
            return true;
        }

        return room.getPassword().equals(inputPassword);
    }

    @Transactional
    public void addParticipant(String roomName, String nickname) {
        Rooms room = roomsRepository.findByName(roomName)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        room.addParticipant(nickname);
    }

    @Transactional
    public void removeParticipant(String roomName, String nickname) {
        Rooms room = roomsRepository.findByName(roomName)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        room.removeParticipant(nickname);
    }

    @Transactional
    public void setParticipantReady(String roomName, String nickname, boolean ready) {
        Rooms room = roomsRepository.findByName(roomName)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        room.setParticipantReady(nickname, ready);
    }
}
