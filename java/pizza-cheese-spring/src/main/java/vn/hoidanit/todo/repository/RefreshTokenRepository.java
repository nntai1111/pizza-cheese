package vn.hoidanit.todo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.hoidanit.todo.domain.RefreshToken;
import vn.hoidanit.todo.domain.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);
}
