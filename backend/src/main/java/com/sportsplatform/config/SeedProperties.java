package com.sportsplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sports-platform.seed")
public class SeedProperties {

    /**
     * Whether to apply a prepared-structure seed at application startup. Off by default; nothing
     * in production enables it at this point.
     */
    private boolean applyOnStartup = false;

    /**
     * Spring resource location of the seed file (for example
     * {@code classpath:seed/prepared-structure.yaml} or
     * {@code file:/etc/sports-platform/seed.yaml}). Only consulted when {@link #isApplyOnStartup()}
     * is {@code true}.
     */
    private String path;

    public boolean isApplyOnStartup() { return applyOnStartup; }
    public void setApplyOnStartup(boolean applyOnStartup) { this.applyOnStartup = applyOnStartup; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}
