package vn.autobot.webhook.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import vn.autobot.webhook.TestBrowser;
import vn.autobot.webhook.dto.RegisterRequestDto;
import vn.autobot.webhook.service.UserService;
import vn.autobot.webhook.service.WebSocketService;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

// Real-time request notifications reach only their user and administrators
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties =
    "spring.datasource.url=jdbc:h2:mem:topics;DB_CLOSE_DELAY=-1")
class TopicAccessTest {

  @LocalServerPort
  private int port;
  @Autowired
  private UserService users;
  @Autowired
  private WebSocketService notifications;
  private final BlockingQueue<Object> received = new LinkedBlockingQueue<>();

  @BeforeEach
  void createUsers() {
    for (String name : new String[] {"listener", "victim"}) {
      if (!users.existsByUsername(name)) {
        users.registerUser(new RegisterRequestDto(name, "secret123", "secret123", name + "@example.com"));
      }
    }
  }

  private StompSession connect(String username, String password) throws Exception {
    WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
    client.setMessageConverter(new MappingJackson2MessageConverter());
    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    headers.add("Cookie", new TestBrowser(port).login(username, password).cookieHeader());
    return client.connect("ws://localhost:" + port + "/ws/websocket", headers, new StompSessionHandlerAdapter() { }).get(5, TimeUnit.SECONDS);
  }

  private void subscribe(StompSession session, String destination) throws InterruptedException {
    session.subscribe(destination, new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return Map.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        received.add(payload);
      }
    });
    Thread.sleep(500); // subscriptions are not acknowledged
  }

  @Test
  void ownTopicIsDelivered() throws Exception {
    subscribe(connect("listener", "secret123"), "/topic/requests/listener");

    notifications.sendRequestUpdate("listener");

    assertNotNull(received.poll(5, TimeUnit.SECONDS));
  }

  @Test
  void administratorsMayListenToAnyUser() throws Exception {
    subscribe(connect("admin", "admin123"), "/topic/requests/victim");

    notifications.sendRequestUpdate("victim");

    assertNotNull(received.poll(5, TimeUnit.SECONDS));
  }

  @Test
  void otherTopicsAndPatternsAreRefused() throws Exception {
    for (String destination : new String[] {"/topic/requests/victim", "/topic/requests/*", "/topic/**", "/topic/requests/{x}", "/topic/other"}) {
      StompSession session = connect("listener", "secret123");
      subscribe(session, destination);

      notifications.sendRequestUpdate("victim");

      assertNull(received.poll(500, TimeUnit.MILLISECONDS), destination);
      assertFalse(session.isConnected(), destination);
    }
  }

  @Test
  void clientsCannotPublish() throws Exception {
    subscribe(connect("victim", "secret123"), "/topic/requests/victim");

    connect("listener", "secret123").send("/topic/requests/victim", Map.of("type", "REQUEST_UPDATE"));

    assertNull(received.poll(1, TimeUnit.SECONDS));
    notifications.sendRequestUpdate("victim"); // the subscription itself works
    assertNotNull(received.poll(5, TimeUnit.SECONDS));
  }
}
