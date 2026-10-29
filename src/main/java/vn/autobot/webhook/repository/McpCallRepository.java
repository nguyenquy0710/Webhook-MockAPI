package vn.autobot.webhook.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.autobot.webhook.model.McpCall;
import vn.autobot.webhook.model.User;

import java.time.LocalDateTime;
import java.util.Collection;

public interface McpCallRepository extends JpaRepository<McpCall, Long> {

    /** result: 0 any, 1 ok only, 2 errors only; empty tool/q mean "any" (empty strings keep the query portable). */
    @Query("select c from McpCall c where c.user = :user"
        + " and (:tool = '' or c.tool = :tool)"
        + " and (:result = 0 or (:result = 1 and c.resultOk = true) or (:result = 2 and c.resultOk = false))"
        + " and (:q = '' or lower(c.arguments) like lower(concat('%', :q, '%')) or lower(c.clientName) like lower(concat('%', :q, '%')))"
        + " order by c.createdAt desc, c.id desc")
    Page<McpCall> search(@Param("user") User user, @Param("tool") String tool, @Param("result") int result,
                         @Param("q") String q, Pageable pageable);

    long countByUserAndCreatedAtGreaterThanEqual(User user, LocalDateTime since);

    long countByUserAndResultOkFalseAndCreatedAtGreaterThanEqual(User user, LocalDateTime since);

    long countByUserAndResultOkTrueAndToolInAndCreatedAtGreaterThanEqual(User user, Collection<String> tools, LocalDateTime since);

    long countByCreatedAtGreaterThanEqual(LocalDateTime since);

    long countByResultOkFalseAndCreatedAtGreaterThanEqual(LocalDateTime since);

    @Modifying
    @Query("delete from McpCall c where c.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
