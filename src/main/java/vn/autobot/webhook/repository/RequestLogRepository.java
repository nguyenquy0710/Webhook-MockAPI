package vn.autobot.webhook.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.autobot.webhook.model.RequestLog;
import vn.autobot.webhook.model.User;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {

  List<RequestLog> findByUser(User user);

  List<RequestLog> findByUserOrderByTimestampDesc(User user);

  Page<RequestLog> findByUserOrderByTimestampDesc(User user, Pageable pageable);

  long countByUser(User user);

  void deleteByUser(User user);

  @Modifying
  @Query("DELETE FROM RequestLog r WHERE r.timestamp < :cutoffDate")
  int deleteByTimestampBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}