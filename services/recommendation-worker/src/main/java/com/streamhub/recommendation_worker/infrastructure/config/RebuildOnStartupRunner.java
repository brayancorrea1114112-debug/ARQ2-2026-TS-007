package com.streamhub.recommendation_worker.infrastructure.config;

import com.streamhub.recommendation_worker.application.usecases.RedisProjectionRebuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Optional safeguard: rebuild the read model on every start. Disabled by default because the
 * recommended flow is the manual {@code /admin/rebuild} endpoint.
 */
@Component
@ConditionalOnProperty(name = "rebuild.on-startup", havingValue = "true")
public class RebuildOnStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RebuildOnStartupRunner.class);

    private final RedisProjectionRebuilder rebuilder;

    public RebuildOnStartupRunner(RedisProjectionRebuilder rebuilder) {
        this.rebuilder = rebuilder;
    }

    @Override
    public void run(ApplicationArguments args) {
        int profiles = rebuilder.rebuildAll();
        log.info("Read model rebuilt from Cassandra for {} profile(s)", profiles);
    }
}
