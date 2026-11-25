package vn.autobot.webhook.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

/** Clients may only listen, each to their own request topic (administrators to any), and never publish. */
class TopicAccessInterceptor implements ChannelInterceptor {

    private static final String TOPIC = "/topic/requests/";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
        if (StompCommand.SEND.equals(headers.getCommand())
                || StompCommand.SUBSCRIBE.equals(headers.getCommand()) && !maySubscribe(headers)) {
            throw new AccessDeniedException("Not allowed: " + headers.getCommand());
        }
        return message;
    }

    private static boolean maySubscribe(StompHeaderAccessor headers) {
        String destination = headers.getDestination();
        if (!(headers.getUser() instanceof Authentication) || destination == null || !destination.startsWith(TOPIC)) {
            return false;
        }
        Authentication user = (Authentication) headers.getUser();
        String name = destination.substring(TOPIC.length());
        // The broker treats *, ? and {} as patterns; usernames never contain them
        return name.equals(user.getName())
                || name.matches("[A-Za-z0-9_-]+") && user.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
