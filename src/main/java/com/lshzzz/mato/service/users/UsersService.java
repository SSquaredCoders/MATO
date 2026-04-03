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
}
