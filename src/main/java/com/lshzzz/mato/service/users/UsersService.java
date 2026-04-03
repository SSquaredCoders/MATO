package com.lshzzz.mato.service.users;

import com.lshzzz.mato.exception.CustomException;
import com.lshzzz.mato.exception.ErrorCode;
import com.lshzzz.mato.model.users.Users;
import com.lshzzz.mato.model.users.dto.UsersLoginResponse;
import com.lshzzz.mato.model.users.dto.UsersRegisterRequest;
import com.lshzzz.mato.model.users.dto.UsersRegisterResponse;
import com.lshzzz.mato.model.users.dto.UsersUpdateRequest;
import com.lshzzz.mato.model.users.dto.UsersUpdateResponse;
import com.lshzzz.mato.repository.UsersRepository;
import com.lshzzz.mato.utils.users.UsersMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsersService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    // 로그인 로직
    @Transactional
    public UsersLoginResponse login(String userId, String rawPassword) {
        Users user = usersRepository.findByUserIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 검증
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        return UsersMapper.toLoginResponse(user);
    }

    public UsersLoginResponse getCurrentUser(String userId) {
        Users user = usersRepository.findByUserIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return UsersMapper.toLoginResponse(user);
    }

    @Transactional
    public UsersLoginResponse upsertGoogleUser(String subject, String name, String email) {
        String socialUserId = "google:" + subject;
        Users existingUser = usersRepository.findByUserId(socialUserId).orElse(null);

        if (existingUser != null) {
            if (existingUser.getDeletedAt() != null) {
                throw new CustomException(ErrorCode.USER_ALREADY_DELETED);
            }
            return UsersMapper.toLoginResponse(existingUser);
        }

        String nickname = buildGoogleNickname(name, email, subject);
        Users newUser = Users.builder()
            .userId(socialUserId)
            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
            .nickname(nickname)
            .role(com.lshzzz.mato.model.users.Role.USER)
            .build();

        usersRepository.save(newUser);
        return UsersMapper.toLoginResponse(newUser);
    }

    // 아이디 중복 확인 메서드
    @Transactional(readOnly = true)
    public boolean isUserIdAvailable(String userId) {
        return !usersRepository.existsByUserId(userId);
    }

    // 회원 가입 로직
    @Transactional
    public UsersRegisterResponse register(UsersRegisterRequest request) {
        // 이메일 중복 확인
        if (usersRepository.existsByUserId(request.userId())) {
            throw new CustomException(ErrorCode.ID_ALREADY_EXISTS);
        }

        // 비밀번호 & 비밀번호 확인 검증
        if (!request.password().equals(request.confirmPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD_CONFIRMATION);
        }

        // 비밀번호 암호화
        String encryptedPassword = passwordEncoder.encode(request.password());

        // 사용자 저장
        Users user = UsersMapper.toEntity(request, encryptedPassword);
        usersRepository.save(user);

        return UsersMapper.toRegisterResponse(user);
    }

    // 회원 수정 로직
    @Transactional
    public UsersUpdateResponse updateUser(String userId, UsersUpdateRequest request) {
        Users user = usersRepository.findByUserIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 닉네임과 새 비밀번호가 모두 null인 경우 예외 처리
        if (request.nickname() == null && request.newPassword() == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        // 현재 비밀번호 검증
        if (request.currentPassword() != null && !passwordEncoder.matches(request.currentPassword(),
            user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 닉네임 업데이트
        if (request.nickname() != null) {
            user.updateNickname(request.nickname());
        }

        // 새 비밀번호 업데이트
        if (request.newPassword() != null) {
            String encodedPassword = passwordEncoder.encode(request.newPassword());
            user.updatePassword(encodedPassword);
        }

        Users updatedUser = usersRepository.save(user);

        return UsersMapper.toUpdateResponse(updatedUser);
    }

    // 회원 탈퇴 로직
    @Transactional
    public void deleteUser(String userId) {
        Users user = usersRepository.findByUserIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getDeletedAt() != null) {
            throw new CustomException(ErrorCode.USER_ALREADY_DELETED);
        }

        user.delete();
    }

    private String buildGoogleNickname(String name, String email, String subject) {
        String base = firstNonBlank(name, emailPrefix(email), "google-" + tail(subject, 6));
        String trimmed = base.trim();
        return trimmed.length() > 20 ? trimmed.substring(0, 20) : trimmed;
    }

    private String emailPrefix(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return "";
        }
        return email.substring(0, email.indexOf('@'));
    }

    private String tail(String value, int length) {
        if (value == null || value.isBlank()) {
            return "user";
        }
        return value.length() <= length ? value : value.substring(value.length() - length);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "google-user";
    }
}
