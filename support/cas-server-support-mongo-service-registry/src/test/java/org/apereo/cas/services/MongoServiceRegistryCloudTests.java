package org.apereo.cas.services;


import org.apereo.cas.config.MongoDbServiceRegistryConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import lombok.extern.slf4j.Slf4j;

import static org.junit.Assert.*;


/**
 * This is {@link MongoServiceRegistryCloudTests}.
 *
 * @author Misagh Moayyed
 * @since 4.2.0
 */
@SpringBootTest(classes = {MongoDbServiceRegistryConfiguration.class, RefreshAutoConfiguration.class})
@TestPropertySource(locations = {"classpath:/mongoservices.properties"})
@Slf4j
public class MongoServiceRegistryCloudTests extends AbstractServiceRegistryTests {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    @Qualifier("mongoDbServiceRegistry")
    private ServiceRegistry serviceRegistry;


    @Override
    public ServiceRegistry getNewServiceRegistry() {
        return serviceRegistry;
    }
}
