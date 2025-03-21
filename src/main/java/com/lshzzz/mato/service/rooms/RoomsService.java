package com.lshzzz.mato.service.rooms;

import com.lshzzz.mato.exception.CustomException;
import com.lshzzz.mato.exception.ErrorCode;
import com.lshzzz.mato.model.room.Rooms;
import com.lshzzz.mato.model.room.dto.RoomsCreateRequest;
import com.lshzzz.mato.model.room.dto.RoomsResponse;
import com.lshzzz.mato.model.room.dto.RoomsUpdateRequest;
import com.lshzzz.mato.repository.RoomsRepository;
import com.lshzzz.mato.utils.rooms.RoomsMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomsService {

    private final RoomsRepository roomsRepository;

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
                guestNick = "게스트" + (int)(Math.random() * 9000 + 1000);
                session.setAttribute("guestNickname", guestNick);
            }
            return guestNick;
        }
    }

    // 전체 방 조회
    public List<RoomsResponse> findAllRooms() {
        return roomsRepository.findAll().stream()
            .map(RoomsMapper::toResponse)
            .collect(Collectors.toList());
    }

    // 방 제목을 통한 방 조회
    public RoomsResponse findByName(String name) {
        return roomsRepository.findByName(name)
            .map(RoomsMapper::toResponse)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
    }

    // 방 생성
    public RoomsResponse createRoom(RoomsCreateRequest request, String hostNickname) {
        Rooms room = Rooms.builder()
            .name(request.name())
            .password(request.password())
            .host(hostNickname)
            .participants(1)
            .gameStatus(request.gameStatus())
            .build();
        return RoomsMapper.toResponse(roomsRepository.save(room));
    }

    // 방 수정
    @Transactional
    public RoomsResponse updateRoom(Long roomId, RoomsUpdateRequest request, String nickname) {
        Rooms room = roomsRepository.findById(roomId)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.getHost().equals(nickname)) {
            throw new IllegalArgumentException("방장만 수정할 수 있습니다.");
        }

        room.updateName(request.name());
        room.updatePassword(request.password());

        return RoomsMapper.toResponse(room);
    }

    // 방 삭제
    @Transactional
    public void deleteRoom(Long roomId, String nickname) {
        Rooms room = roomsRepository.findById(roomId)
            .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.getHost().equals(nickname)) {
            throw new IllegalArgumentException("방장만 삭제할 수 있습니다.");
        }

        roomsRepository.delete(room);
    }
}