package org.apereo.cas.config;

import org.apereo.cas.CipherExecutor;
import org.apereo.cas.authentication.AuthenticationServiceSelectionPlan;
import org.apereo.cas.authentication.AuthenticationServiceSelectionStrategy;
import org.apereo.cas.authentication.SecurityTokenServiceClientBuilder;
import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.config.support.authentication.AuthenticationServiceSelectionStrategyConfigurer;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.support.wsfed.WsFederationProperties;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.ticket.SecurityTokenTicketFactory;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.ticket.registry.TicketRegistrySupport;
import org.apereo.cas.util.http.HttpClient;
import org.apereo.cas.web.flow.CasWebflowConfigurer;
import org.apereo.cas.web.support.CookieRetrievingCookieGenerator;
import org.apereo.cas.ws.idp.IdentityProviderConfigurationService;
import org.apereo.cas.ws.idp.RealmAwareIdentityProvider;
import org.apereo.cas.ws.idp.authentication.WSFederationAuthenticationServiceSelectionStrategy;
import org.apereo.cas.ws.idp.impl.DefaultIdentityProviderConfigurationService;
import org.apereo.cas.ws.idp.impl.DefaultRealmAwareIdentityProvider;
import org.apereo.cas.ws.idp.metadata.WSFederationMetadataServlet;
import org.apereo.cas.ws.idp.services.DefaultRelyingPartyTokenProducer;
import org.apereo.cas.ws.idp.services.WSFederationRelyingPartyTokenProducer;
import org.apereo.cas.ws.idp.web.WSFederationValidateRequestCallbackController;
import org.apereo.cas.ws.idp.web.WSFederationValidateRequestController;
import org.apereo.cas.ws.idp.web.flow.WSFederationMetadataUIAction;
import org.apereo.cas.ws.idp.web.flow.WSFederationWebflowConfigurer;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Lazy;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;
import org.springframework.webflow.execution.Action;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This is {@link CoreWsSecurityIdentityProviderConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
@Configuration("coreWsSecurityIdentityProviderConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ImportResource(locations = {"classpath:META-INF/cxf/cxf.xml"})
public class CoreWsSecurityIdentityProviderConfiguration implements AuthenticationServiceSelectionStrategyConfigurer {

    @Autowired
    @Qualifier("ticketGrantingTicketCookieGenerator")
    private CookieRetrievingCookieGenerator ticketGrantingTicketCookieGenerator;

    @Autowired
    @Qualifier("noRedirectHttpClient")
    private HttpClient httpClient;

    @Autowired
    @Qualifier("loginFlowRegistry")
    private FlowDefinitionRegistry loginFlowDefinitionRegistry;

    @Autowired
    @Qualifier("defaultTicketRegistrySupport")
    private TicketRegistrySupport ticketRegistrySupport;

    @Autowired
    private FlowBuilderServices flowBuilderServices;

    @Autowired
    @Qualifier("servicesManager")
    private ServicesManager servicesManager;

    @Autowired
    @Qualifier("webApplicationServiceFactory")
    private ServiceFactory webApplicationServiceFactory;


    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    @Qualifier("securityTokenTicketFactory")
    private SecurityTokenTicketFactory securityTokenTicketFactory;

    @Autowired
    @Qualifier("ticketRegistry")
    private TicketRegistry ticketRegistry;

    @Lazy
    @Bean
    public WSFederationValidateRequestController federationValidateRequestController() {
        return new WSFederationValidateRequestController(idpConfigService(), servicesManager,
                webApplicationServiceFactory, casProperties, wsFederationAuthenticationServiceSelectionStrategy(),
                httpClient, securityTokenTicketFactory, ticketRegistry, ticketGrantingTicketCookieGenerator,
                ticketRegistrySupport);
    }

    @Lazy
    @Autowired
    @Bean
    public WSFederationValidateRequestCallbackController federationValidateRequestCallbackController(
            @Qualifier("wsFederationRelyingPartyTokenProducer")
            final WSFederationRelyingPartyTokenProducer wsFederationRelyingPartyTokenProducer) {
        return new WSFederationValidateRequestCallbackController(idpConfigService(), servicesManager,
                webApplicationServiceFactory, casProperties, wsFederationRelyingPartyTokenProducer,
                wsFederationAuthenticationServiceSelectionStrategy(),
                httpClient, securityTokenTicketFactory, ticketRegistry, ticketGrantingTicketCookieGenerator,
                ticketRegistrySupport);
    }

    @Lazy
    @Bean
    public ServletRegistrationBean wsIdpMetadataServlet() {
        final WsFederationProperties wsfed = casProperties.getAuthn().getWsfedIdP();
        final ServletRegistrationBean bean = new ServletRegistrationBean();
        bean.setEnabled(true);
        bean.setName("federationServletIdentityProvider");
        bean.setServlet(new WSFederationMetadataServlet(wsfed.getIdp().getRealm()));
        bean.setUrlMappings(Collections.singleton("/ws/idp/metadata"));
        bean.setAsyncSupported(true);
        return bean;
    }

    @Lazy
    @Autowired
    @Bean
    public WSFederationRelyingPartyTokenProducer wsFederationRelyingPartyTokenProducer(
            @Qualifier("securityTokenServiceCredentialCipherExecutor") final CipherExecutor securityTokenServiceCredentialCipherExecutor,
            @Qualifier("securityTokenServiceClientBuilder") final SecurityTokenServiceClientBuilder securityTokenServiceClientBuilder) {
        return new DefaultRelyingPartyTokenProducer(securityTokenServiceClientBuilder,
                securityTokenServiceCredentialCipherExecutor,
                idpConfigService());
    }

    @Bean
    public IdentityProviderConfigurationService idpConfigService() {
        return new DefaultIdentityProviderConfigurationService(identityProviders());
    }

    @Bean
    public List<RealmAwareIdentityProvider> identityProviders() {
        try {
            final WsFederationProperties wsfed = casProperties.getAuthn().getWsfedIdP();
            final DefaultRealmAwareIdentityProvider idp = new DefaultRealmAwareIdentityProvider();
            idp.setRealm(wsfed.getIdp().getRealm());
            idp.setUri(wsfed.getIdp().getRealmUri());
            idp.setStsUrl(new URL(casProperties.getServer().getPrefix().concat("/ws/sts/").concat(wsfed.getIdp().getRealmUri())));
            idp.setIdpUrl(new URL(casProperties.getServer().getPrefix().concat("/ws/idp/federation")));
            idp.setCertificate(wsfed.getIdp().getCertificate());
            idp.setCertificatePassword(wsfed.getIdp().getCertificatePassword());
            idp.setSupportedProtocols(Arrays.asList("http://docs.oasis-open.org/wsfed/federation/200706", "http://docs.oasis-open.org/ws-sx/ws-trust/200512"));
            idp.setAuthenticationURIs(Collections.singletonMap("default", "federation"));
            idp.setDescription("WsFederation Identity Provider");
            idp.setDisplayName("WsFederation");
            return Arrays.asList(idp);
        } catch (final Exception e) {
            throw new BeanCreationException(e.getMessage(), e);
        }
    }

    @Bean
    public AuthenticationServiceSelectionStrategy wsFederationAuthenticationServiceSelectionStrategy() {
        return new WSFederationAuthenticationServiceSelectionStrategy(webApplicationServiceFactory);
    }

    @Bean
    public Action wsFederationMetadataUIAction() {
        return new WSFederationMetadataUIAction(servicesManager, wsFederationAuthenticationServiceSelectionStrategy());
    }

    @Bean
    public CasWebflowConfigurer wsFederationWebflowConfigurer() {
        return new WSFederationWebflowConfigurer(flowBuilderServices, loginFlowDefinitionRegistry, wsFederationMetadataUIAction());
    }

    @Override
    public void configureAuthenticationServiceSelectionStrategy(final AuthenticationServiceSelectionPlan plan) {
        plan.registerStrategy(wsFederationAuthenticationServiceSelectionStrategy());
    }
}