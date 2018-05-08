package org.apereo.cas.services;

import org.apereo.cas.config.CasCoreServicesConfiguration;
import org.apereo.cas.config.CasCoreUtilConfiguration;
import org.apereo.cas.config.JpaServiceRegistryConfiguration;
import org.joda.time.DateTimeUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;

import static org.junit.Assert.*;


/**
 * Handles tests for {@link JpaServiceRegistry}
 *
 * @author battags
 * @since 3.1.0
 */
@RunWith(ConditionalSpringRunner.class)
@SpringBootTest(classes = {
    RefreshAutoConfiguration.class,
    AopAutoConfiguration.class,
    CasCoreUtilConfiguration.class,
    JpaServiceRegistryConfiguration.class,
    JpaServiceRegistryTests.TimeAwareServicesManagerConfiguration.class,
    CasCoreServicesConfiguration.class})
@DirtiesContext
@Slf4j
public class JpaServiceRegistryTests extends AbstractServiceRegistryTests {

    @Autowired
    @Qualifier("jpaServiceRegistry")
    private ServiceRegistry serviceRegistry;

    @Autowired
    @Qualifier("servicesManager")
    private ServicesManager servicesManager;

    @Override
    public ServiceRegistry getNewServiceRegistry() {
        return serviceRegistry;
    }

    @Test
    public void verifyExpiredServiceDeleted() {
        final RegexRegisteredService r = new RegexRegisteredService();
        r.setServiceId("testExpired");
        r.setName("expired");
        r.setExpirationPolicy(new DefaultRegisteredServiceExpirationPolicy(true, LocalDateTime.now().minusSeconds(1)));
        final RegisteredService r2 = this.servicesManager.save(r);
        DateTimeUtils.setCurrentMillisFixed(System.currentTimeMillis() + 2000);
        this.servicesManager.load();
        final RegisteredService svc = this.servicesManager.findServiceBy(r2.getServiceId());
        assertNull(svc);
    }

    @Test
    public void verifyExpiredServiceDisabled() {
        final RegexRegisteredService r = new RegexRegisteredService();
        r.setServiceId("testExpired1");
        r.setName("expired1");
        final LocalDateTime expirationDate = LocalDateTime.now().plusSeconds(1);
        r.setExpirationPolicy(new DefaultRegisteredServiceExpirationPolicy(false, expirationDate));
        final RegisteredService r2 = this.servicesManager.save(r);
        RegisteredService svc = this.servicesManager.findServiceBy(r2.getServiceId());
        assertNotNull(svc);
        DateTimeUtils.setCurrentMillisFixed(System.currentTimeMillis() + 2000);
        svc = this.servicesManager.findServiceBy(r2.getServiceId());
        assertNotNull(svc);
        assertFalse(svc.getAccessStrategy().isServiceAccessAllowed());
    }

    @TestConfiguration("timeAwareServicesManagerConfiguration")
    public static class TimeAwareServicesManagerConfiguration {

        @Autowired
        @Qualifier("serviceRegistry")
        private ServiceRegistry serviceRegistry;

        @Bean
        public ServicesManager servicesManager() {
            return new TimeAwareServicesManager(serviceRegistry);
        }

        public static class TimeAwareServicesManager extends DefaultServicesManager {
            public TimeAwareServicesManager(final ServiceRegistry serviceRegistry) {
                super(serviceRegistry, null);
            }

            @Override
            protected LocalDateTime getCurrentSystemTime() {
                return org.apereo.cas.util.DateTimeUtils.localDateTimeOf(DateTimeUtils.currentTimeMillis());
            }
        }
    }
}
