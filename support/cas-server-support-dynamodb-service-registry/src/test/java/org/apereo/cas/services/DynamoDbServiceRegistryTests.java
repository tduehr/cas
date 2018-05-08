package org.apereo.cas.services;

import org.apereo.cas.category.DynamoDbCategory;
import org.apereo.cas.config.CasCoreAuthenticationMetadataConfiguration;
import org.apereo.cas.config.CasCoreServicesConfiguration;
import org.apereo.cas.config.CasCoreUtilConfiguration;
import org.apereo.cas.config.DynamoDbServiceRegistryConfiguration;
import org.apereo.cas.util.junit.ConditionalIgnore;
import org.apereo.cas.util.junit.RunningContinuousIntegrationCondition;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;

import static org.junit.Assert.*;

/**
 * This is {@link DynamoDbServiceRegistryTests}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
@SpringBootTest(classes = {DynamoDbServiceRegistryConfiguration.class,
    CasCoreServicesConfiguration.class,
    CasCoreUtilConfiguration.class,
    CasCoreAuthenticationMetadataConfiguration.class,
    RefreshAutoConfiguration.class})
@TestPropertySource(locations = "classpath:/dynamodb-serviceregistry.properties")
@Slf4j
@Category(DynamoDbCategory.class)
@ConditionalIgnore(condition = RunningContinuousIntegrationCondition.class)
<<<<<<< HEAD
public class DynamoDbServiceRegistryTests extends AbstractServiceRegistryTests {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    @Qualifier("serviceRegistry")
    private ServiceRegistry serviceRegistry;

    static {
        System.setProperty("aws.accessKeyId", "AKIAIPPIGGUNIO74C63Z");
        System.setProperty("aws.secretKey", "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");
    }

    @Override
    public ServiceRegistry getNewServiceRegistry() {
        return serviceRegistry;
    }
}
