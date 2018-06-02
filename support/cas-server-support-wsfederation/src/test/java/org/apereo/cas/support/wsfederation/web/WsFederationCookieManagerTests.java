package org.apereo.cas.support.wsfederation.web;

import org.apereo.cas.CasProtocolConstants;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.support.wsfederation.AbstractWsFederationTests;
import org.apereo.cas.util.HttpRequestUtils;
import org.apereo.inspektr.common.web.ClientInfo;
import org.apereo.inspektr.common.web.ClientInfoHolder;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.test.MockRequestContext;

import static org.junit.Assert.*;

/**
 * This is {@link WsFederationCookieManagerTests}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
public class WsFederationCookieManagerTests extends AbstractWsFederationTests {
    @Test
    public void verifyOperation() {
        final var context = new MockRequestContext();
        final var request = new MockHttpServletRequest();
        final var response = new MockHttpServletResponse();
        context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, response));

        request.setRemoteAddr("185.86.151.11");
        request.setLocalAddr("185.88.151.11");
        request.addHeader(HttpRequestUtils.USER_AGENT_HEADER, "MSIE");
        ClientInfoHolder.setClientInfo(new ClientInfo(request));

        request.addParameter(CasProtocolConstants.PARAMETER_METHOD, "POST");
        request.setAttribute("locale", "en");
        request.setAttribute("theme", "custom");

        final var config = wsFederationConfigurations.iterator().next();
        final var wctx = config.getId();
        final var original = RegisteredServiceTestUtils.getService();
        wsFederationCookieManager.store(request, response, wctx, original, config);

        request.addParameter(WsFederationCookieManager.WCTX, wctx);
        request.setCookies(response.getCookies());
        final var service = wsFederationCookieManager.retrieve(context);
        assertNotNull(service);
        assertEquals(original.getId(), service.getId());
    }
}