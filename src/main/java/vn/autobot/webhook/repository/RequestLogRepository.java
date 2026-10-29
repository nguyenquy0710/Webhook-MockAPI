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

  /** Logs of one exact request path (e.g. /api/@user/orders); method empty means any; status range is [statusMin, statusMax). */
  @Query("select r from RequestLog r where r.user = :user and r.path = :path"
      + " and (:method = '' or r.method = :method)"
      + " and r.responseStatus >= :statusMin and r.responseStatus < :statusMax"
      + " order by r.timestamp desc")
  Page<RequestLog> searchByPath(@Param("user") User user, @Param("path") String path, @Param("method") String method,
                                @Param("statusMin") int statusMin, @Param("statusMax") int statusMax, Pageable pageable);

  // Statistics over [from, to); an empty path means all paths
  @Query("select count(r) from RequestLog r where r.user = :user and r.timestamp >= :from and r.timestamp < :to"
      + " and (:path = '' or r.path = :path)")
  long countInRange(@Param("user") User user, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                    @Param("path") String path);

  @Query("select r.method, count(r) from RequestLog r where r.user = :user and r.timestamp >= :from and r.timestamp < :to"
      + " and (:path = '' or r.path = :path) group by r.method order by count(r) desc")
  List<Object[]> countByMethodInRange(@Param("user") User user, @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to, @Param("path") String path);

  @Query("select r.responseStatus / 100, count(r) from RequestLog r where r.user = :user and r.timestamp >= :from"
      + " and r.timestamp < :to and (:path = '' or r.path = :path) and r.responseStatus is not null"
      + " group by r.responseStatus / 100 order by r.responseStatus / 100")
  List<Object[]> countByStatusClassInRange(@Param("user") User user, @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to, @Param("path") String path);

  // Year, month, day and count per day (not a date value, which would be shifted by the JVM time zone)
  @Query("select year(r.timestamp), month(r.timestamp), day(r.timestamp), count(r) from RequestLog r where r.user = :user"
      + " and r.timestamp >= :from and r.timestamp < :to and (:path = '' or r.path = :path)"
      + " group by year(r.timestamp), month(r.timestamp), day(r.timestamp)"
      + " order by year(r.timestamp), month(r.timestamp), day(r.timestamp)")
  List<Object[]> countByDayInRange(@Param("user") User user, @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to, @Param("path") String path);

  @Query("select r.path, count(r) from RequestLog r where r.user = :user and r.timestamp >= :from and r.timestamp < :to"
      + " and (:path = '' or r.path = :path) group by r.path order by count(r) desc")
  List<Object[]> topPathsInRange(@Param("user") User user, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                 @Param("path") String path, Pageable pageable);

  @Modifying
  @Query("delete from RequestLog r where r.timestamp < :before")
  int deleteOlderThan(@Param("before") LocalDateTime before);
}