package com.example.socialmedia.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KeepAliveScheduler {

    @Scheduled(fixedDelay = 840000)
    public void keepAlive() {
        log.debug("Keep-alive ping - preventing Render cold start");
    }
}
