package com.sportsplatform.league.infrastructure.persistence;

import com.sportsplatform.league.application.Clock;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SystemClock implements Clock {
    @Override
    public Instant now() {
        return Instant.now();
    }
}
