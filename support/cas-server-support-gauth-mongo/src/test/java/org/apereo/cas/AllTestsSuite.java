package org.apereo.cas;

import org.apereo.cas.adaptors.gauth.GoogleAuthenticatorMongoDbTokenRepositoryTests;
import org.apereo.cas.adaptors.gauth.MongoDbGoogleAuthenticatorTokenCredentialRepositoryTests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * This is {@link AllTestsSuite}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({MongoDbGoogleAuthenticatorTokenCredentialRepositoryTests.class,
    GoogleAuthenticatorMongoDbTokenRepositoryTests.class})
public class AllTestsSuite {
}
