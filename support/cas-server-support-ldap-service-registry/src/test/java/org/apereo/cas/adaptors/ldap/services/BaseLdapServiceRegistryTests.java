package org.apereo.cas.adaptors.ldap.services;

import org.apereo.cas.adaptors.ldap.services.config.LdapServiceRegistryConfiguration;
import org.apereo.cas.authentication.principal.ShibbolethCompatiblePersistentIdGenerator;
import org.apereo.cas.category.LdapCategory;
import org.apereo.cas.services.AbstractRegisteredService;
import org.apereo.cas.services.AbstractServiceRegistryTests;
import org.apereo.cas.services.AnonymousRegisteredServiceUsernameAttributeProvider;
import org.apereo.cas.services.DefaultRegisteredServiceProperty;
import org.apereo.cas.services.RefuseRegisteredServiceProxyPolicy;
import org.apereo.cas.services.RegexRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceProperty;
import org.apereo.cas.services.ServiceRegistry;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.junit.ConditionalIgnoreRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

import static org.junit.Assert.*;

/**
 * This is {@link BaseLdapServiceRegistryTests}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@EnableScheduling
@DirtiesContext
@Slf4j
@Category(LdapCategory.class)
@SpringBootTest(classes = {LdapServiceRegistryConfiguration.class, RefreshAutoConfiguration.class})
public class BaseLdapServiceRegistryTests extends AbstractServiceRegistryTests {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Rule
    public final ConditionalIgnoreRule conditionalIgnoreRule = new ConditionalIgnoreRule();

    @Autowired
    @Qualifier("ldapServiceRegistry")
    private ServiceRegistry serviceRegistry;
    
    @Override
    public ServiceRegistry getNewServiceRegistry() {
        return serviceRegistry;
    }

    @Test
    public void verifySavingServiceChangesDn() {
        this.serviceRegistry.save(getRegexRegisteredService());
        final List<RegisteredService> services = this.serviceRegistry.load();

        final AbstractRegisteredService rs = (AbstractRegisteredService) this.serviceRegistry.findServiceById(services.get(0).getId());
        final long originalId = rs.getId();
        assertNotNull(rs);
        rs.setId(666);
        assertNotNull(this.serviceRegistry.save(rs));
        assertNotEquals(rs.getId(), originalId);
    }

    private static RegisteredService getRegexRegisteredService() {
        final AbstractRegisteredService rs = new RegexRegisteredService();
        rs.setName("Service Name Regex");
        rs.setProxyPolicy(new RefuseRegisteredServiceProxyPolicy());
        rs.setUsernameAttributeProvider(new AnonymousRegisteredServiceUsernameAttributeProvider(
            new ShibbolethCompatiblePersistentIdGenerator("hello")
        ));
        rs.setDescription("Service description");
        rs.setServiceId("^http?://.+");
        rs.setTheme("the theme name");
        rs.setEvaluationOrder(123);
        rs.setDescription("Here is another description");
        rs.setRequiredHandlers(CollectionUtils.wrapHashSet("handler1", "handler2"));

        final Map<String, RegisteredServiceProperty> propertyMap = new HashMap<>();
        final DefaultRegisteredServiceProperty property = new DefaultRegisteredServiceProperty();

        final Set<String> values = new HashSet<>();
        values.add("value1");
        values.add("value2");
        property.setValues(values);
        propertyMap.put("field1", property);
        rs.setProperties(propertyMap);

        return rs;
    }
}
