package vn.autobot.webhook.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.autobot.webhook.model.ApiConfig;
import vn.autobot.webhook.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiConfigRepository extends JpaRepository<ApiConfig, Long> {

    List<ApiConfig> findByUser(User user);

    List<ApiConfig> findByUserOrderByCreatedAtDesc(User user);

    Page<ApiConfig> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Optional<ApiConfig> findByUserAndPathAndMethod(User user, String path, String method);

    boolean existsByUserAndPathAndMethod(User user, String path, String method);
}
