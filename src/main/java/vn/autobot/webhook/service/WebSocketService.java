package vn.autobot.webhook.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.repository.RequestLogRepository;

import java.util.HashMap;
import java.util.Map;

@Service
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RequestLogRepository requestLogRepository;
    private final UserService userService;

    public WebSocketService(SimpMessagingTemplate messagingTemplate,
                            RequestLogRepository requestLogRepository,
                            UserService userService) {
        this.messagingTemplate = messagingTemplate;
        this.requestLogRepository = requestLogRepository;
        this.userService = userService;
    }

    public void sendRequestUpdate(String username) {
        User user = userService.findByUsername(username);
        long count = requestLogRepository.countByUser(user);

        Map<String, Object> message = new HashMap<>();
        message.put("type", "REQUEST_UPDATE");
        message.put("count", count);
        message.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/requests/" + username, message);
    }
}