package org.apereo.cas;

import org.apereo.cas.support.saml.SamlAttributeEncoderTests;
import org.apereo.cas.support.saml.SamlIdPConfigurationTests;
import org.apereo.cas.support.saml.SamlRegisteredServiceJpaMicrosoftSqlServerTests;
import org.apereo.cas.support.saml.SamlRegisteredServiceJpaTests;
import org.apereo.cas.support.saml.SamlRegisteredServiceTests;
import org.apereo.cas.support.saml.services.GroovySamlRegisteredServiceAttributeReleasePolicyTests;
import org.apereo.cas.support.saml.services.PatternMatchingEntityIdAttributeReleasePolicyTests;
import org.apereo.cas.support.saml.web.idp.profile.builders.nameid.SamlProfileSamlNameIdBuilderTests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test suite to run all SAML tests.
 *
 * @author Misagh Moayyed
 * @since 4.2.0
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    SamlRegisteredServiceTests.class,
    SamlIdPConfigurationTests.class,
    SamlAttributeEncoderTests.class,
    SamlRegisteredServiceJpaTests.class,
    SamlProfileSamlNameIdBuilderTests.class,
    SamlRegisteredServiceJpaMicrosoftSqlServerTests.class,
    PatternMatchingEntityIdAttributeReleasePolicyTests.class,
    GroovySamlRegisteredServiceAttributeReleasePolicyTests.class
})
public class AllTestsSuite {
}

