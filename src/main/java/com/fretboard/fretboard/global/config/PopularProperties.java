package com.fretboard.fretboard.global.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "popular")
public record PopularProperties(Weights weights, int likeThreshold, int todayLimit, Duration todayTtl) {

    public record Weights(double view, double like, double comment) {
    }
}
