package com.example.socialmedia.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
public class StompHeaderInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public StompHeaderInterceptor(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(@org.springframework.lang.NonNull Message<?> message,
            @org.springframework.lang.NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        // Validate token on CONNECT frame - initial WebSocket handshake
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            validateAndSetAuthentication(accessor);
        }
        // Validate token on SUBSCRIBE frame - allow user channel subscription
        else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            if (accessor.getUser() != null) {
                SecurityContextHolder.getContext().setAuthentication((org.springframework.security.core.Authentication) accessor.getUser());
            } else {
                validateAndSetAuthentication(accessor);
            }
        }
        // Validate token on SEND frame
        else if (StompCommand.SEND.equals(accessor.getCommand())) {
            if (accessor.getUser() != null) {
                SecurityContextHolder.getContext().setAuthentication((org.springframework.security.core.Authentication) accessor.getUser());
            } else {
                validateAndSetAuthentication(accessor);
            }
        }

        return message;
    }

    private void validateAndSetAuthentication(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        accessor.setUser(authToken);
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                } catch (Exception e) {
                    // Log auth failure but don't crash - will be handled as unauthenticated
                    org.slf4j.LoggerFactory.getLogger(this.getClass())
                            .warn("WebSocket authentication failed: {}", e.getMessage());
                }
            }
        }
    }
}
