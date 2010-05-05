package org.springframework.security.config.http;

import static org.springframework.security.config.http.HttpSecurityBeanDefinitionParser.*;
import static org.springframework.security.config.http.SecurityFilters.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.config.Elements;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.PortResolverImpl;
import org.springframework.security.web.access.DefaultWebInvocationPrivilegeEvaluator;
import org.springframework.security.web.access.channel.ChannelDecisionManagerImpl;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.access.channel.InsecureChannelProcessor;
import org.springframework.security.web.access.channel.RetryWithHttpEntryPoint;
import org.springframework.security.web.access.channel.RetryWithHttpsEntryPoint;
import org.springframework.security.web.access.channel.SecureChannelProcessor;
import org.springframework.security.web.access.expression.WebExpressionVoter;
import org.springframework.security.web.access.intercept.DefaultFilterInvocationSecurityMetadataSource;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlStrategy;
import org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.savedrequest.RequestCacheAwareFilter;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import org.springframework.security.web.session.ConcurrentSessionFilter;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Stateful class which helps HttpSecurityBDP to create the configuration for the &lt;http&gt; element.
 *
 * @author Luke Taylor
 * @since 3.0
 */
class HttpConfigurationBuilder {
    private static final String ATT_CREATE_SESSION = "create-session";

    private static final String ATT_SESSION_FIXATION_PROTECTION = "session-fixation-protection";
    private static final String OPT_SESSION_FIXATION_NO_PROTECTION = "none";
    private static final String OPT_SESSION_FIXATION_MIGRATE_SESSION = "migrateSession";

    private static final String ATT_INVALID_SESSION_URL = "invalid-session-url";
    private static final String ATT_SESSION_AUTH_STRATEGY_REF = "session-authentication-strategy-ref";
    private static final String ATT_SESSION_AUTH_ERROR_URL = "session-authentication-error-url";
    private static final String ATT_SECURITY_CONTEXT_REPOSITORY = "security-context-repository-ref";

    private static final String ATT_DISABLE_URL_REWRITING = "disable-url-rewriting";

    private static final String ATT_ACCESS_MGR = "access-decision-manager-ref";
    private static final String ATT_ONCE_PER_REQUEST = "once-per-request";

    private static final String ATT_REF = "ref";

    private final Element httpElt;
    private final ParserContext pc;
    private final SessionCreationPolicy sessionPolicy;
    private final List<Element> interceptUrls;
    private final MatcherType matcherType;

    private BeanDefinition cpf;
    private BeanDefinition securityContextPersistenceFilter;
    private BeanReference contextRepoRef;
    private BeanReference sessionRegistryRef;
    private BeanDefinition concurrentSessionFilter;
    private BeanDefinition requestCacheAwareFilter;
    private BeanReference sessionStrategyRef;
    private RootBeanDefinition sfpf;
    private BeanDefinition servApiFilter;
    private String portMapperName;
    private BeanReference fsi;
    private BeanReference requestCache;

    public HttpConfigurationBuilder(Element element, ParserContext pc, MatcherType matcherType,
            String portMapperName, BeanReference authenticationManager) {
        this.httpElt = element;
        this.pc = pc;
        this.portMapperName = portMapperName;
        this.matcherType = matcherType;
        interceptUrls = DomUtils.getChildElementsByTagName(element, Elements.INTERCEPT_URL);

        for (Element urlElt : interceptUrls) {
            if (StringUtils.hasText(urlElt.getAttribute(ATT_FILTERS))) {
                pc.getReaderContext().error("The use of \"filters='none'\" is no longer supported. Please define a" +
                        " separate <http> element for the pattern you want to exclude and use the attribute" +
                        " \"secured='false'\".", pc.extractSource(urlElt));
            }
        }

        String createSession = element.getAttribute(ATT_CREATE_SESSION);

        if (StringUtils.hasText(createSession)) {
            sessionPolicy = SessionCreationPolicy.valueOf(createSession);
        } else {
            sessionPolicy = SessionCreationPolicy.ifRequired;
        }

        createSecurityContextPersistenceFilter();
        createSessionManagementFilters();
        createRequestCacheFilter();
        createServletApiFilter();
        createChannelProcessingFilter();
        createFilterSecurityInterceptor(authenticationManager);
    }

    // Needed to account for placeholders
    static String createPath(String path, boolean lowerCase) {
        return lowerCase ? path.toLowerCase() : path;
    }

    private void createSecurityContextPersistenceFilter() {
        BeanDefinitionBuilder scpf = BeanDefinitionBuilder.rootBeanDefinition(SecurityContextPersistenceFilter.class);

        String repoRef = httpElt.getAttribute(ATT_SECURITY_CONTEXT_REPOSITORY);
        String disableUrlRewriting = httpElt.getAttribute(ATT_DISABLE_URL_REWRITING);

        if (StringUtils.hasText(repoRef)) {
            if (sessionPolicy == SessionCreationPolicy.always) {
                scpf.addPropertyValue("forceEagerSessionCreation", Boolean.TRUE);
            }
        } else {
            BeanDefinitionBuilder contextRepo;
            if (sessionPolicy == SessionCreationPolicy.stateless) {
                contextRepo = BeanDefinitionBuilder.rootBeanDefinition(NullSecurityContextRepository.class);
            } else {
                contextRepo = BeanDefinitionBuilder.rootBeanDefinition(HttpSessionSecurityContextRepository.class);
                switch (sessionPolicy) {
                    case always:
                        contextRepo.addPropertyValue("allowSessionCreation", Boolean.TRUE);
                        scpf.addPropertyValue("forceEagerSessionCreation", Boolean.TRUE);
                        break;
                    case never:
                        contextRepo.addPropertyValue("allowSessionCreation", Boolean.FALSE);
                        scpf.addPropertyValue("forceEagerSessionCreation", Boolean.FALSE);
                        break;
                    default:
                        contextRepo.addPropertyValue("allowSessionCreation", Boolean.TRUE);
                        scpf.addPropertyValue("forceEagerSessionCreation", Boolean.FALSE);
                }

                if ("true".equals(disableUrlRewriting)) {
                    contextRepo.addPropertyValue("disableUrlRewriting", Boolean.TRUE);
                }
            }

            BeanDefinition repoBean = contextRepo.getBeanDefinition();
            repoRef = pc.getReaderContext().generateBeanName(repoBean);
            pc.registerBeanComponent(new BeanComponentDefinition(repoBean, repoRef));
        }

        contextRepoRef = new RuntimeBeanReference(repoRef);
        scpf.addPropertyValue("securityContextRepository", contextRepoRef);

        securityContextPersistenceFilter = scpf.getBeanDefinition();
    }

    private void createSessionManagementFilters() {
        Element sessionMgmtElt = DomUtils.getChildElementByTagName(httpElt, Elements.SESSION_MANAGEMENT);
        Element sessionCtrlElt = null;

        String sessionFixationAttribute = null;
        String invalidSessionUrl = null;
        String sessionAuthStratRef = null;
        String errorUrl = null;

        if (sessionMgmtElt != null) {
            if (sessionPolicy == SessionCreationPolicy.stateless) {
                pc.getReaderContext().error(Elements.SESSION_MANAGEMENT + "  cannot be used" +
                        " in combination with " + ATT_CREATE_SESSION + "='"+ SessionCreationPolicy.stateless +"'",
                        pc.extractSource(sessionMgmtElt));
            }
            sessionFixationAttribute = sessionMgmtElt.getAttribute(ATT_SESSION_FIXATION_PROTECTION);
            invalidSessionUrl = sessionMgmtElt.getAttribute(ATT_INVALID_SESSION_URL);
            sessionAuthStratRef = sessionMgmtElt.getAttribute(ATT_SESSION_AUTH_STRATEGY_REF);
            errorUrl = sessionMgmtElt.getAttribute(ATT_SESSION_AUTH_ERROR_URL);
            sessionCtrlElt = DomUtils.getChildElementByTagName(sessionMgmtElt, Elements.CONCURRENT_SESSIONS);

            if (sessionCtrlElt != null) {
                if (StringUtils.hasText(sessionAuthStratRef)) {
                    pc.getReaderContext().error(ATT_SESSION_AUTH_STRATEGY_REF + " attribute cannot be used" +
                            " in combination with <" + Elements.CONCURRENT_SESSIONS + ">", pc.extractSource(sessionCtrlElt));
                }
                createConcurrencyControlFilterAndSessionRegistry(sessionCtrlElt);
            }
        }

        if (!StringUtils.hasText(sessionFixationAttribute)) {
            sessionFixationAttribute = OPT_SESSION_FIXATION_MIGRATE_SESSION;
        } else if (StringUtils.hasText(sessionAuthStratRef)) {
            pc.getReaderContext().error(ATT_SESSION_FIXATION_PROTECTION + " attribute cannot be used" +
                    " in combination with " + ATT_SESSION_AUTH_STRATEGY_REF, pc.extractSource(sessionMgmtElt));
        }

        if (sessionPolicy == SessionCreationPolicy.stateless) {
            // SEC-1424: do nothing
            return;
        }

        boolean sessionFixationProtectionRequired = !sessionFixationAttribute.equals(OPT_SESSION_FIXATION_NO_PROTECTION);

        BeanDefinitionBuilder sessionStrategy;

        if (sessionCtrlElt != null) {
            assert sessionRegistryRef != null;
            sessionStrategy = BeanDefinitionBuilder.rootBeanDefinition(ConcurrentSessionControlStrategy.class);
            sessionStrategy.addConstructorArgValue(sessionRegistryRef);

            String maxSessions = sessionCtrlElt.getAttribute("max-sessions");

            if (StringUtils.hasText(maxSessions)) {
                sessionStrategy.addPropertyValue("maximumSessions", maxSessions);
            }

            String exceptionIfMaximumExceeded = sessionCtrlElt.getAttribute("error-if-maximum-exceeded");

            if (StringUtils.hasText(exceptionIfMaximumExceeded)) {
                sessionStrategy.addPropertyValue("exceptionIfMaximumExceeded", exceptionIfMaximumExceeded);
            }
        } else if (sessionFixationProtectionRequired || StringUtils.hasText(invalidSessionUrl)
                || StringUtils.hasText(sessionAuthStratRef)) {
            sessionStrategy = BeanDefinitionBuilder.rootBeanDefinition(SessionFixationProtectionStrategy.class);
        } else {
            sfpf = null;
            return;
        }

        BeanDefinitionBuilder sessionMgmtFilter = BeanDefinitionBuilder.rootBeanDefinition(SessionManagementFilter.class);
        RootBeanDefinition failureHandler = new RootBeanDefinition(SimpleUrlAuthenticationFailureHandler.class);
        if (StringUtils.hasText(errorUrl)) {
            failureHandler.getPropertyValues().addPropertyValue("defaultFailureUrl", errorUrl);
        }
        sessionMgmtFilter.addPropertyValue("authenticationFailureHandler", failureHandler);
        sessionMgmtFilter.addConstructorArgValue(contextRepoRef);

        if (!StringUtils.hasText(sessionAuthStratRef)) {
            BeanDefinition strategyBean = sessionStrategy.getBeanDefinition();

            if (sessionFixationProtectionRequired) {
                sessionStrategy.addPropertyValue("migrateSessionAttributes",
                        Boolean.valueOf(sessionFixationAttribute.equals(OPT_SESSION_FIXATION_MIGRATE_SESSION)));
            }
            sessionAuthStratRef = pc.getReaderContext().generateBeanName(strategyBean);
            pc.registerBeanComponent(new BeanComponentDefinition(strategyBean, sessionAuthStratRef));
        }

        if (StringUtils.hasText(invalidSessionUrl)) {
            sessionMgmtFilter.addPropertyValue("invalidSessionUrl", invalidSessionUrl);
        }

        sessionMgmtFilter.addPropertyReference("sessionAuthenticationStrategy", sessionAuthStratRef);

        sfpf = (RootBeanDefinition) sessionMgmtFilter.getBeanDefinition();
        sessionStrategyRef = new RuntimeBeanReference(sessionAuthStratRef);
    }

    private void createConcurrencyControlFilterAndSessionRegistry(Element element) {
        final String ATT_EXPIRY_URL = "expired-url";
        final String ATT_SESSION_REGISTRY_ALIAS = "session-registry-alias";
        final String ATT_SESSION_REGISTRY_REF = "session-registry-ref";

        CompositeComponentDefinition compositeDef =
            new CompositeComponentDefinition(element.getTagName(), pc.extractSource(element));
        pc.pushContainingComponent(compositeDef);

        BeanDefinitionRegistry beanRegistry = pc.getRegistry();

        String sessionRegistryId = element.getAttribute(ATT_SESSION_REGISTRY_REF);

        if (!StringUtils.hasText(sessionRegistryId)) {
            // Register an internal SessionRegistryImpl if no external reference supplied.
            RootBeanDefinition sessionRegistry = new RootBeanDefinition(SessionRegistryImpl.class);
            sessionRegistryId = pc.getReaderContext().registerWithGeneratedName(sessionRegistry);
            pc.registerComponent(new BeanComponentDefinition(sessionRegistry, sessionRegistryId));
        }

        String registryAlias = element.getAttribute(ATT_SESSION_REGISTRY_ALIAS);
        if (StringUtils.hasText(registryAlias)) {
            beanRegistry.registerAlias(sessionRegistryId, registryAlias);
        }

        BeanDefinitionBuilder filterBuilder =
                BeanDefinitionBuilder.rootBeanDefinition(ConcurrentSessionFilter.class);
        filterBuilder.addPropertyReference("sessionRegistry", sessionRegistryId);

        Object source = pc.extractSource(element);
        filterBuilder.getRawBeanDefinition().setSource(source);
        filterBuilder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

        String expiryUrl = element.getAttribute(ATT_EXPIRY_URL);

        if (StringUtils.hasText(expiryUrl)) {
            WebConfigUtils.validateHttpRedirect(expiryUrl, pc, source);
            filterBuilder.addPropertyValue("expiredUrl", expiryUrl);
        }

        pc.popAndRegisterContainingComponent();

        concurrentSessionFilter = filterBuilder.getBeanDefinition();
        sessionRegistryRef = new RuntimeBeanReference(sessionRegistryId);
    }

    // Adds the servlet-api integration filter if required
    private void createServletApiFilter() {
        final String ATT_SERVLET_API_PROVISION = "servlet-api-provision";
        final String DEF_SERVLET_API_PROVISION = "true";

        String provideServletApi = httpElt.getAttribute(ATT_SERVLET_API_PROVISION);
        if (!StringUtils.hasText(provideServletApi)) {
            provideServletApi = DEF_SERVLET_API_PROVISION;
        }

        if ("true".equals(provideServletApi)) {
            servApiFilter = new RootBeanDefinition(SecurityContextHolderAwareRequestFilter.class);
        }
    }

    private void createChannelProcessingFilter() {
        ManagedMap<BeanDefinition,BeanDefinition> channelRequestMap = parseInterceptUrlsForChannelSecurity();

        if (channelRequestMap.isEmpty()) {
            return;
        }

        RootBeanDefinition channelFilter = new RootBeanDefinition(ChannelProcessingFilter.class);
        BeanDefinitionBuilder metadataSourceBldr = BeanDefinitionBuilder.rootBeanDefinition(DefaultFilterInvocationSecurityMetadataSource.class);
        metadataSourceBldr.addConstructorArgValue(channelRequestMap);
//        metadataSourceBldr.addPropertyValue("stripQueryStringFromUrls", matcher instanceof AntUrlPathMatcher);

        channelFilter.getPropertyValues().addPropertyValue("securityMetadataSource", metadataSourceBldr.getBeanDefinition());
        RootBeanDefinition channelDecisionManager = new RootBeanDefinition(ChannelDecisionManagerImpl.class);
        ManagedList<RootBeanDefinition> channelProcessors = new ManagedList<RootBeanDefinition>(3);
        RootBeanDefinition secureChannelProcessor = new RootBeanDefinition(SecureChannelProcessor.class);
        RootBeanDefinition retryWithHttp = new RootBeanDefinition(RetryWithHttpEntryPoint.class);
        RootBeanDefinition retryWithHttps = new RootBeanDefinition(RetryWithHttpsEntryPoint.class);
        RuntimeBeanReference portMapper = new RuntimeBeanReference(portMapperName);
        retryWithHttp.getPropertyValues().addPropertyValue("portMapper", portMapper);
        retryWithHttps.getPropertyValues().addPropertyValue("portMapper", portMapper);
        secureChannelProcessor.getPropertyValues().addPropertyValue("entryPoint", retryWithHttps);
        RootBeanDefinition inSecureChannelProcessor = new RootBeanDefinition(InsecureChannelProcessor.class);
        inSecureChannelProcessor.getPropertyValues().addPropertyValue("entryPoint", retryWithHttp);
        channelProcessors.add(secureChannelProcessor);
        channelProcessors.add(inSecureChannelProcessor);
        channelDecisionManager.getPropertyValues().addPropertyValue("channelProcessors", channelProcessors);

        String id = pc.getReaderContext().registerWithGeneratedName(channelDecisionManager);
        channelFilter.getPropertyValues().addPropertyValue("channelDecisionManager", new RuntimeBeanReference(id));
        cpf = channelFilter;
    }

    /**
     * Parses the intercept-url elements to obtain the map used by channel security.
     * This will be empty unless the <tt>requires-channel</tt> attribute has been used on a URL path.
     */
    private ManagedMap<BeanDefinition,BeanDefinition> parseInterceptUrlsForChannelSecurity() {

        ManagedMap<BeanDefinition, BeanDefinition> channelRequestMap = new ManagedMap<BeanDefinition, BeanDefinition>();

        for (Element urlElt : interceptUrls) {
            String path = urlElt.getAttribute(ATT_PATH_PATTERN);
            String method = urlElt.getAttribute(ATT_HTTP_METHOD);

            if(!StringUtils.hasText(path)) {
                pc.getReaderContext().error("pattern attribute cannot be empty or null", urlElt);
            }

            String requiredChannel = urlElt.getAttribute(ATT_REQUIRES_CHANNEL);

            if (StringUtils.hasText(requiredChannel)) {
                BeanDefinition matcher = matcherType.createMatcher(path, method);

                RootBeanDefinition channelAttributes = new RootBeanDefinition(ChannelAttributeFactory.class);
                channelAttributes.getConstructorArgumentValues().addGenericArgumentValue(requiredChannel);
                channelAttributes.setFactoryMethodName("createChannelAttributes");

                channelRequestMap.put(matcher, channelAttributes);
            }
        }

        return channelRequestMap;
    }

    private void createRequestCacheFilter() {
        Element requestCacheElt = DomUtils.getChildElementByTagName(httpElt, Elements.REQUEST_CACHE);

        if (requestCacheElt != null) {
            requestCache = new RuntimeBeanReference(requestCacheElt.getAttribute(ATT_REF));
        } else {
            BeanDefinitionBuilder requestCacheBldr;

            if (sessionPolicy == SessionCreationPolicy.stateless) {
                requestCacheBldr = BeanDefinitionBuilder.rootBeanDefinition(NullRequestCache.class);
            } else {
                requestCacheBldr = BeanDefinitionBuilder.rootBeanDefinition(HttpSessionRequestCache.class);
                BeanDefinitionBuilder portResolver = BeanDefinitionBuilder.rootBeanDefinition(PortResolverImpl.class);
                portResolver.addPropertyReference("portMapper", portMapperName);
                requestCacheBldr.addPropertyValue("createSessionAllowed", sessionPolicy == SessionCreationPolicy.ifRequired);
                requestCacheBldr.addPropertyValue("portResolver", portResolver.getBeanDefinition());
            }

            BeanDefinition bean = requestCacheBldr.getBeanDefinition();
            String id = pc.getReaderContext().generateBeanName(bean);
            pc.registerBeanComponent(new BeanComponentDefinition(bean, id));

            this.requestCache = new RuntimeBeanReference(id);
        }

        requestCacheAwareFilter = new RootBeanDefinition(RequestCacheAwareFilter.class);
        requestCacheAwareFilter.getPropertyValues().addPropertyValue("requestCache", requestCache);
    }

    private void createFilterSecurityInterceptor(BeanReference authManager) {
        boolean useExpressions = FilterInvocationSecurityMetadataSourceParser.isUseExpressions(httpElt);
        BeanDefinition securityMds = FilterInvocationSecurityMetadataSourceParser.createSecurityMetadataSource(interceptUrls, httpElt, pc);

        RootBeanDefinition accessDecisionMgr;
        ManagedList<BeanDefinition> voters =  new ManagedList<BeanDefinition>(2);

        if (useExpressions) {
            BeanDefinitionBuilder expressionVoter = BeanDefinitionBuilder.rootBeanDefinition(WebExpressionVoter.class);
            RuntimeBeanReference expressionHandler = new RuntimeBeanReference(
                    FilterInvocationSecurityMetadataSourceParser.registerDefaultExpressionHandler(pc));
            expressionVoter.addPropertyValue("expressionHandler", expressionHandler);

            voters.add(expressionVoter.getBeanDefinition());
        } else {
            voters.add(new RootBeanDefinition(RoleVoter.class));
            voters.add(new RootBeanDefinition(AuthenticatedVoter.class));
        }
        accessDecisionMgr = new RootBeanDefinition(AffirmativeBased.class);
        accessDecisionMgr.getPropertyValues().addPropertyValue("decisionVoters", voters);
        accessDecisionMgr.setSource(pc.extractSource(httpElt));

        // Set up the access manager reference for http
        String accessManagerId = httpElt.getAttribute(ATT_ACCESS_MGR);

        if (!StringUtils.hasText(accessManagerId)) {
            accessManagerId = pc.getReaderContext().generateBeanName(accessDecisionMgr);
            pc.registerBeanComponent(new BeanComponentDefinition(accessDecisionMgr, accessManagerId));
        }

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(FilterSecurityInterceptor.class);

        builder.addPropertyReference("accessDecisionManager", accessManagerId);
        builder.addPropertyValue("authenticationManager", authManager);

        if ("false".equals(httpElt.getAttribute(ATT_ONCE_PER_REQUEST))) {
            builder.addPropertyValue("observeOncePerRequest", Boolean.FALSE);
        }

        builder.addPropertyValue("securityMetadataSource", securityMds);
        BeanDefinition fsiBean = builder.getBeanDefinition();
        String fsiId = pc.getReaderContext().generateBeanName(fsiBean);
        pc.registerBeanComponent(new BeanComponentDefinition(fsiBean,fsiId));

        // Create and register a DefaultWebInvocationPrivilegeEvaluator for use with taglibs etc.
        BeanDefinition wipe = new RootBeanDefinition(DefaultWebInvocationPrivilegeEvaluator.class);
        wipe.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference(fsiId));

        pc.registerBeanComponent(new BeanComponentDefinition(wipe, pc.getReaderContext().generateBeanName(wipe)));

        this.fsi = new RuntimeBeanReference(fsiId);
    }

    BeanReference getSessionStrategy() {
        return sessionStrategyRef;
    }

    SessionCreationPolicy getSessionCreationPolicy() {
        return sessionPolicy;
    }

    BeanReference getRequestCache() {
        return requestCache;
    }

    List<OrderDecorator> getFilters() {
        List<OrderDecorator> filters = new ArrayList<OrderDecorator>();

        if (cpf != null) {
            filters.add(new OrderDecorator(cpf, CHANNEL_FILTER));
        }

        if (concurrentSessionFilter != null) {
            filters.add(new OrderDecorator(concurrentSessionFilter, CONCURRENT_SESSION_FILTER));
        }

        filters.add(new OrderDecorator(securityContextPersistenceFilter, SECURITY_CONTEXT_FILTER));

        if (servApiFilter != null) {
            filters.add(new OrderDecorator(servApiFilter, SERVLET_API_SUPPORT_FILTER));
        }

        if (sfpf != null) {
            filters.add(new OrderDecorator(sfpf, SESSION_MANAGEMENT_FILTER));
        }

        filters.add(new OrderDecorator(fsi, FILTER_SECURITY_INTERCEPTOR));

        if (sessionPolicy != SessionCreationPolicy.stateless) {
            filters.add(new OrderDecorator(requestCacheAwareFilter, REQUEST_CACHE_FILTER));
        }

        return filters;
    }
}
