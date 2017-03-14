package org.apereo.cas.ws.idp.web;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apereo.cas.CasProtocolConstants;
import org.apereo.cas.authentication.AuthenticationServiceSelectionStrategy;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.support.wsfed.WsFederationProperties;
import org.apereo.cas.services.RegisteredServiceAccessStrategyUtils;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.services.UnauthorizedServiceException;
import org.apereo.cas.ticket.SecurityTokenTicketFactory;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.ticket.registry.TicketRegistrySupport;
import org.apereo.cas.util.EncodingUtils;
import org.apereo.cas.util.http.HttpClient;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.support.CookieRetrievingCookieGenerator;
import org.apereo.cas.ws.idp.IdentityProviderConfigurationService;
import org.apereo.cas.ws.idp.RealmAwareIdentityProvider;
import org.apereo.cas.ws.idp.WSFederationConstants;
import org.apereo.cas.ws.idp.services.WSFederationRegisteredService;
import org.apereo.cas.ws.idp.services.WSFederationRelyingPartyTokenProducer;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.util.CommonUtils;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas30ServiceTicketValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * This is {@link WSFederationValidateRequestCallbackController}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
public class WSFederationValidateRequestCallbackController extends BaseWSFederationRequestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WSFederationValidateRequestCallbackController.class);
    private final WSFederationRelyingPartyTokenProducer relyingPartyTokenProducer;

    public WSFederationValidateRequestCallbackController(final IdentityProviderConfigurationService identityProviderConfigurationService,
                                                         final ServicesManager servicesManager,
                                                         final ServiceFactory<WebApplicationService> webApplicationServiceFactory,
                                                         final CasConfigurationProperties casProperties,
                                                         final WSFederationRelyingPartyTokenProducer relyingPartyTokenProducer,
                                                         final AuthenticationServiceSelectionStrategy serviceSelectionStrategy,
                                                         final HttpClient httpClient,
                                                         final SecurityTokenTicketFactory securityTokenTicketFactory,
                                                         final TicketRegistry ticketRegistry,
                                                         final CookieRetrievingCookieGenerator ticketGrantingTicketCookieGenerator,
                                                         final TicketRegistrySupport ticketRegistrySupport) {
        super(identityProviderConfigurationService, servicesManager,
                webApplicationServiceFactory, casProperties,
                serviceSelectionStrategy, httpClient, securityTokenTicketFactory,
                ticketRegistry, ticketGrantingTicketCookieGenerator,
                ticketRegistrySupport);
        this.relyingPartyTokenProducer = relyingPartyTokenProducer;
    }

    /**
     * Handle federation request.
     *
     * @param response the response
     * @param request  the request
     * @return the model and view
     * @throws Exception the exception
     */
    @GetMapping(path = WSFederationConstants.ENDPOINT_FEDERATION_REQUEST_CALLBACK)
    protected ModelAndView handleFederationRequest(final HttpServletResponse response, final HttpServletRequest request) throws Exception {
        final WSFederationRequest fedRequest = WSFederationRequest.of(request);
        final WsFederationProperties wsfed = casProperties.getAuthn().getWsfedIdP();
        final RealmAwareIdentityProvider idp = this.identityProviderConfigurationService.getIdentityProvider(wsfed.getIdp().getRealm());
        LOGGER.info("Received callback profile request [{}]", request.getRequestURI());

        final String ticket = CommonUtils.safeGetParameter(request, CasProtocolConstants.PARAMETER_TICKET);
        if (StringUtils.isBlank(ticket)) {
            LOGGER.error("Can not validate the request because no [{}] is provided via the request", CasProtocolConstants.PARAMETER_TICKET);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return new ModelAndView(CasWebflowConstants.VIEW_ID_ERROR, new HashMap<>(), HttpStatus.FORBIDDEN);
        }

        final Assertion assertion = validateRequestAndBuildCasAssertion(response, request, fedRequest);
        final SecurityToken securityToken = validateSecurityTokenInAssertion(assertion, request, response);
        addSecurityTokenTicketToRegistry(request, securityToken);
        final String rpToken = produceRelyingPartyToken(response, request, fedRequest, securityToken, assertion);
        return postResponseBackToRelyingParty(rpToken, securityToken, assertion, fedRequest, request, response, idp);
    }

    private void addSecurityTokenTicketToRegistry(final HttpServletRequest request, final SecurityToken securityToken) {
        LOGGER.debug("Adding security token as a ticket to CAS ticket registry...");
        final TicketGrantingTicket tgt = getTicketGrantingTicketFromRequest(request);
        this.ticketRegistry.addTicket(securityTokenTicketFactory.create(tgt, securityToken));
    }

    private ModelAndView postResponseBackToRelyingParty(final String rpToken,
                                                        final SecurityToken securityToken,
                                                        final Assertion assertion,
                                                        final WSFederationRequest fedRequest,
                                                        final HttpServletRequest request,
                                                        final HttpServletResponse response,
                                                        final RealmAwareIdentityProvider idp) throws Exception {
        final WSFederationRegisteredService service = getWsFederationRegisteredService(response, request, fedRequest);
        final String postUrl = StringUtils.isNotBlank(fedRequest.getWreply()) ? fedRequest.getWreply() : fedRequest.getWtrealm();

        final Map model = new HashMap<>();
        model.put("originalUrl", postUrl);

        final Map parameters = new HashMap<>();
        parameters.put(WSFederationConstants.WA, WSFederationConstants.WSIGNIN10);
        parameters.put(WSFederationConstants.WRESULT, StringEscapeUtils.unescapeHtml4(rpToken));
        parameters.put(WSFederationConstants.WTREALM, fedRequest.getWtrealm());

        if (StringUtils.isNotBlank(fedRequest.getWctx())) {
            parameters.put(WSFederationConstants.WCTX, fedRequest.getWctx());
        }
        model.put("parameters", parameters);

        LOGGER.debug("Posting relying party token to [{}]", postUrl);
        return new ModelAndView(CasWebflowConstants.VIEW_ID_POST_RESPONSE, model);
    }

    private String produceRelyingPartyToken(final HttpServletResponse response, final HttpServletRequest request,
                                            final WSFederationRequest fedRequest, final SecurityToken securityToken,
                                            final Assertion assertion) {
        final WSFederationRegisteredService service = getWsFederationRegisteredService(response, request, fedRequest);
        return relyingPartyTokenProducer.produce(securityToken, service, fedRequest, request, assertion);
    }

    private WSFederationRegisteredService getWsFederationRegisteredService(final HttpServletResponse response, final HttpServletRequest request,
                                                                           final WSFederationRequest fedRequest) {
        final String serviceUrl = constructServiceUrl(request, response, fedRequest);
        final Service targetService = this.serviceSelectionStrategy.resolveServiceFrom(this.webApplicationServiceFactory.createService(serviceUrl));
        final WSFederationRegisteredService svc = this.servicesManager.findServiceBy(targetService, WSFederationRegisteredService.class);
        RegisteredServiceAccessStrategyUtils.ensureServiceAccessIsAllowed(targetService, svc);
        return svc;
    }

    private SecurityToken validateSecurityTokenInAssertion(final Assertion assertion, final HttpServletRequest request,
                                                           final HttpServletResponse response) {
        LOGGER.debug("Validating security token in CAS assertion...");

        final AttributePrincipal principal = assertion.getPrincipal();
        if (!principal.getAttributes().containsKey(WSFederationConstants.SECURITY_TOKEN_ATTRIBUTE)) {
            throw new UnauthorizedServiceException(UnauthorizedServiceException.CODE_UNAUTHZ_SERVICE);
        }
        final String token = (String) principal.getAttributes().get(WSFederationConstants.SECURITY_TOKEN_ATTRIBUTE);
        final byte[] securityTokenBin = EncodingUtils.decodeBase64(token);
        return SerializationUtils.deserialize(securityTokenBin);
    }

    private Assertion validateRequestAndBuildCasAssertion(final HttpServletResponse response,
                                                          final HttpServletRequest request,
                                                          final WSFederationRequest fedRequest) throws Exception {
        final String ticket = CommonUtils.safeGetParameter(request, CasProtocolConstants.PARAMETER_TICKET);
        final Cas30ServiceTicketValidator validator = new Cas30ServiceTicketValidator(casProperties.getServer().getPrefix());
        final String serviceUrl = constructServiceUrl(request, response, fedRequest);
        LOGGER.debug("Created service url for validation: [{}]", serviceUrl);
        final Assertion assertion = validator.validate(ticket, serviceUrl);
        LOGGER.debug("Located CAS assertion [{}]", assertion);
        return assertion;
    }

}