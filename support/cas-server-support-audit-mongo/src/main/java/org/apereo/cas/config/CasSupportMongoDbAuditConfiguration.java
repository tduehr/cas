package org.apereo.cas.config;

import org.apereo.cas.audit.AuditTrailExecutionPlanConfigurer;
import org.apereo.cas.audit.MongoDbAuditTrailManager;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.mongo.MongoDbConnectionFactory;

import lombok.extern.slf4j.Slf4j;
import org.apereo.inspektr.audit.AuditTrailManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This is {@link CasSupportMongoDbAuditConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Configuration("casSupportMongoDbAuditConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
public class CasSupportMongoDbAuditConfiguration {

    @Autowired
    private CasConfigurationProperties casProperties;

    @Bean
    public AuditTrailManager mongoDbAuditTrailManager() {
        final var mongo = casProperties.getAudit().getMongo();
        final var factory = new MongoDbConnectionFactory();
        final var mongoTemplate = factory.buildMongoTemplate(mongo);
        factory.createCollection(mongoTemplate, mongo.getCollection(), mongo.isDropCollection());
        return new MongoDbAuditTrailManager(mongoTemplate, mongo.getCollection(), mongo.isAsynchronous());
    }

    @Bean
    public AuditTrailExecutionPlanConfigurer mongoDbAuditTrailExecutionPlanConfigurer() {
        return plan -> plan.registerAuditTrailManager(mongoDbAuditTrailManager());
    }
}
