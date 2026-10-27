package vn.autobot.webhook.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

  Page<RequestLog> findByUserAndPathContainingIgnoreCaseOrderByTimestampDesc(User user, String path, Pageable pageable);

  /**
   * Filtered search used by the request log page. Empty path/method mean "any"; the status range is [statusMin, statusMax).
   * Empty strings and numeric bounds are used instead of nulls to keep the query portable across databases.
   */
  @Query("select r from RequestLog r where r.user = :user"
      + " and (:path = '' or lower(r.path) like lower(concat('%', :path, '%')))"
      + " and (:method = '' or r.method = :method)"
      + " and r.responseStatus >= :statusMin and r.responseStatus < :statusMax"
      + " order by r.timestamp desc")
  Page<RequestLog> search(@Param("user") User user, @Param("path") String path, @Param("method") String method,
                          @Param("statusMin") int statusMin, @Param("statusMax") int statusMax, Pageable pageable);

  long countByUser(User user);

  long countByUserAndTimestampGreaterThanEqual(User user, LocalDateTime since);

  long countByUserAndPathContainingIgnoreCase(User user, String path);

  void deleteByUser(User user);
}