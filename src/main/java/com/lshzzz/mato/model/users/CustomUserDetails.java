package com.lshzzz.mato.model.users;

import java.util.Collection;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final String username;
    private final String nickname;
    private final Collection<? extends GrantedAuthority> authorities;

    // Users 엔티티 기반 생성자 (로그인 시 사용)
    public CustomUserDetails(Users user) {
        this.username = user.getUserId();
        this.nickname = user.getNickname();
        this.authorities = user.getRole().getGrantedAuthorities();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null; // JWT 기반 인증이므로 비밀번호는 null 처리
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}