package vn.autobot.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.repository.RequestLogRepository;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Request log page filters: path text, HTTP method and status class
class RequestLogSearchTest {

  private RequestLogRepository repository;
  private RequestLogService service;
  private User user;
  private final Pageable pageable = PageRequest.of(0, 10);

  @BeforeEach
  void setUp() {
    repository = mock(RequestLogRepository.class);
    UserService userService = mock(UserService.class);
    user = new User();
    user.setId(1L);
    when(userService.findByUsername("demo")).thenReturn(user);
    when(repository.search(any(), anyString(), anyString(), anyInt(), anyInt(), any())).thenReturn(new PageImpl<>(Collections.emptyList()));
    service = new RequestLogService(repository, userService, mock(WebSocketService.class), new ObjectMapper());
  }

  @Test
  void statusClassBecomesHundredRange() {
    service.searchRequestLogs("demo", "orders", "post", "5", pageable);

    verify(repository).search(eq(user), eq("orders"), eq("POST"), eq(500), eq(600), eq(pageable));
  }

  @Test
  void emptyFiltersMeanAny() {
    service.searchRequestLogs("demo", null, null, "", pageable);

    verify(repository).search(eq(user), eq(""), eq(""), eq(0), eq(Integer.MAX_VALUE), eq(pageable));
  }

  @Test
  void invalidStatusClassIsIgnored() {
    service.searchRequestLogs("demo", "  /hello ", "GET", "abc", pageable);

    verify(repository).search(eq(user), eq("/hello"), eq("GET"), eq(0), eq(Integer.MAX_VALUE), eq(pageable));
  }
}
