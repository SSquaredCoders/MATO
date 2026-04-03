package com.lshzzz.mato.repository;

import com.lshzzz.mato.model.users.Users;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsersRepository extends JpaRepository<Users, Long> {

    Optional<Users> findByUserIdAndDeletedAtIsNull(String userId);

    Optional<Users> findByUserId(String userId);

    boolean existsByUserId(String userId);
}
