package vn.autobot.webhook.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.autobot.webhook.model.RequestLog;
import vn.autobot.webhook.model.User;

import java.util.List;

@Repository
public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {

    Page<RequestLog> findByUserOrderByTimestampDesc(User user, Pageable pageable);

    List<RequestLog> findByUserOrderByTimestampDesc(User user);

    long countByUser(User user);

    void deleteByUser(User user);
}