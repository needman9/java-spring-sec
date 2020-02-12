/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.config.http;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Elements;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationProvider;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.w3c.dom.Element;

/**
 * @author Ruby Hartono
 * @since 5.3
 */
final class OAuth2LoginBeanDefinitionParser implements BeanDefinitionParser {

	private static final String DEFAULT_AUTHORIZATION_REQUEST_BASE_URI = "/oauth2/authorization";
	private static final String DEFAULT_LOGIN_URI = DefaultLoginPageGeneratingFilter.DEFAULT_LOGIN_PAGE_URL;

	private static final String ELT_CLIENT_REGISTRATION = "client-registration";
	private static final String ATT_REGISTRATION_ID = "registration-id";
	private static final String ATT_CLIENT_REGISTRATION_REPOSITORY_REF = "client-registration-repository-ref";
	private static final String ATT_AUTHORIZED_CLIENT_REPOSITORY_REF = "authorized-client-repository-ref";
	private static final String ATT_AUTHORIZED_CLIENT_SERVICE_REF = "authorized-client-service-ref";
	private static final String ATT_AUTHORIZATION_REQUEST_REPOSITORY_REF = "authorization-request-repository-ref";
	private static final String ATT_AUTHORIZATION_REQUEST_RESOLVER_REF = "authorization-request-resolver-ref";
	private static final String ATT_ACCESS_TOKEN_RESPONSE_CLIENT_REF = "access-token-response-client-ref";
	private static final String ATT_USER_AUTHORITIES_MAPPER_REF = "user-authorities-mapper-ref";
	private static final String ATT_USER_SERVICE_REF = "user-service-ref";
	private static final String ATT_OIDC_USER_SERVICE_REF = "oidc-user-service-ref";
	private static final String ATT_LOGIN_PROCESSING_URL = "login-processing-url";
	private static final String ATT_LOGIN_PAGE = "login-page";
	private static final String ATT_AUTHENTICATION_SUCCESS_HANDLER_REF = "authentication-success-handler-ref";
	private static final String ATT_AUTHENTICATION_FAILURE_HANDLER_REF = "authentication-failure-handler-ref";
	private static final String ATT_JWT_DECODER_FACTORY_REF = "jwt-decoder-factory-ref";

	private BeanReference requestCache;
	private final BeanReference portMapper;
	private final BeanReference portResolver;
	private final BeanReference sessionStrategy;
	private final boolean allowSessionCreation;

	private BeanDefinition oauth2AuthorizationRequestRedirectFilter;

	private BeanDefinition oauth2LoginAuthenticationEntryPoint;

	private BeanDefinition oauth2LoginAuthenticationProvider;

	private BeanDefinition oauth2LoginOidcAuthenticationProvider;

	private BeanDefinition oauth2LoginLinks;

	OAuth2LoginBeanDefinitionParser(BeanReference requestCache, BeanReference portMapper, BeanReference portResolver,
			BeanReference sessionStrategy, boolean allowSessionCreation) {
		this.requestCache = requestCache;
		this.portMapper = portMapper;
		this.portResolver = portResolver;
		this.sessionStrategy = sessionStrategy;
		this.allowSessionCreation = allowSessionCreation;
	}

	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		// register magic bean
		BeanDefinition oauth2LoginBeanConfig = BeanDefinitionBuilder.rootBeanDefinition(OAuth2LoginBeanConfig.class)
				.getBeanDefinition();
		String oauth2LoginBeanConfigId = parserContext.getReaderContext().generateBeanName(oauth2LoginBeanConfig);
		parserContext
				.registerBeanComponent(new BeanComponentDefinition(oauth2LoginBeanConfig, oauth2LoginBeanConfigId));

		// configure filter
		BeanMetadataElement clientRegistrationRepository = getClientRegistrationRepository(element);
		BeanMetadataElement authorizedClientRepository = getAuthorizedClientRepository(element,
				clientRegistrationRepository);
		BeanMetadataElement accessTokenResponseClient = getAccessTokenResponseClient(element);
		BeanMetadataElement oauth2UserService = getOAuth2UserService(element);
		BeanMetadataElement oauth2AuthRequestRepository = getOAuth2AuthorizationRequestRepository(element);

		BeanDefinitionBuilder oauth2LoginAuthenticationFilterBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(OAuth2LoginAuthenticationFilter.class)
				.addConstructorArgValue(clientRegistrationRepository).addConstructorArgValue(authorizedClientRepository)
				.addPropertyValue("authorizationRequestRepository", oauth2AuthRequestRepository);

		if (sessionStrategy != null) {
			oauth2LoginAuthenticationFilterBuilder.addPropertyValue("sessionAuthenticationStrategy", sessionStrategy);
		}

		Object source = parserContext.extractSource(element);
		String loginProcessingUrl = element.getAttribute(ATT_LOGIN_PROCESSING_URL);
		WebConfigUtils.validateHttpRedirect(loginProcessingUrl, parserContext, source);
		if (!StringUtils.isEmpty(loginProcessingUrl)) {
			oauth2LoginAuthenticationFilterBuilder.addConstructorArgValue(loginProcessingUrl);
		} else {
			oauth2LoginAuthenticationFilterBuilder
					.addConstructorArgValue(OAuth2LoginAuthenticationFilter.DEFAULT_FILTER_PROCESSES_URI);
		}

		BeanDefinitionBuilder oauth2LoginAuthenticationProviderBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(OAuth2LoginAuthenticationProvider.class)
				.addConstructorArgValue(accessTokenResponseClient).addConstructorArgValue(oauth2UserService);

		String oauth2UserAuthMapperRef = element.getAttribute(ATT_USER_AUTHORITIES_MAPPER_REF);
		if (!StringUtils.isEmpty(oauth2UserAuthMapperRef)) {
			oauth2LoginAuthenticationProviderBuilder.addPropertyReference("authoritiesMapper", oauth2UserAuthMapperRef);
		}

		oauth2LoginAuthenticationProvider = oauth2LoginAuthenticationProviderBuilder.getBeanDefinition();

		oauth2LoginOidcAuthenticationProvider = getOAuth2OidcAuthProvider(element, accessTokenResponseClient,
				oauth2UserAuthMapperRef, parserContext);

		BeanDefinitionBuilder oauth2AuthorizationRequestRedirectFilterBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(OAuth2AuthorizationRequestRedirectFilter.class);

		String oauth2AuthorizationRequestResolverRef = element.getAttribute(ATT_AUTHORIZATION_REQUEST_RESOLVER_REF);
		if (!StringUtils.isEmpty(oauth2AuthorizationRequestResolverRef)) {
			oauth2AuthorizationRequestRedirectFilterBuilder
					.addConstructorArgReference(oauth2AuthorizationRequestResolverRef);
		} else {
			oauth2AuthorizationRequestRedirectFilterBuilder.addConstructorArgValue(clientRegistrationRepository);
		}

		oauth2AuthorizationRequestRedirectFilterBuilder
				.addPropertyValue("authorizationRequestRepository", oauth2AuthRequestRepository)
				.addPropertyValue("requestCache", requestCache);
		oauth2AuthorizationRequestRedirectFilter = oauth2AuthorizationRequestRedirectFilterBuilder.getBeanDefinition();

		String authenticationSuccessHandlerRef = element.getAttribute(ATT_AUTHENTICATION_SUCCESS_HANDLER_REF);
		if (!StringUtils.isEmpty(authenticationSuccessHandlerRef)) {
			oauth2LoginAuthenticationFilterBuilder.addPropertyReference("authenticationSuccessHandler",
					authenticationSuccessHandlerRef);
		}

		String loginPage = element.getAttribute(ATT_LOGIN_PAGE);
		WebConfigUtils.validateHttpRedirect(loginPage, parserContext, source);
		if (!StringUtils.isEmpty(loginPage)) {
			oauth2LoginAuthenticationEntryPoint = BeanDefinitionBuilder
					.rootBeanDefinition(LoginUrlAuthenticationEntryPoint.class).addConstructorArgValue(loginPage)
					.addPropertyValue("portMapper", portMapper).addPropertyValue("portResolver", portResolver)
					.getBeanDefinition();
		} else {
			Map<RequestMatcher, AuthenticationEntryPoint> entryPoint = getLoginEntryPoint(element, parserContext);

			if (entryPoint != null) {
				oauth2LoginAuthenticationEntryPoint = BeanDefinitionBuilder
						.rootBeanDefinition(DelegatingAuthenticationEntryPoint.class).addConstructorArgValue(entryPoint)
						.addPropertyValue("defaultEntryPoint", new LoginUrlAuthenticationEntryPoint(DEFAULT_LOGIN_URI))
						.getBeanDefinition();
			}
		}

		String authenticationFailureHandlerRef = element.getAttribute(ATT_AUTHENTICATION_FAILURE_HANDLER_REF);
		if (!StringUtils.isEmpty(authenticationFailureHandlerRef)) {
			oauth2LoginAuthenticationFilterBuilder.addPropertyReference("authenticationFailureHandler",
					authenticationFailureHandlerRef);
		} else {
			BeanDefinitionBuilder failureHandlerBuilder = BeanDefinitionBuilder.rootBeanDefinition(
					"org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler");
			failureHandlerBuilder.addConstructorArgValue(
					DEFAULT_LOGIN_URI + "?" + DefaultLoginPageGeneratingFilter.ERROR_PARAMETER_NAME);
			failureHandlerBuilder.addPropertyValue("allowSessionCreation", allowSessionCreation);

			oauth2LoginAuthenticationFilterBuilder.addPropertyValue("authenticationFailureHandler",
					failureHandlerBuilder.getBeanDefinition());
		}

		// prepare loginlinks
		oauth2LoginLinks = BeanDefinitionBuilder.rootBeanDefinition(Map.class)
				.setFactoryMethodOnBean("getLoginLinks", oauth2LoginBeanConfigId).getBeanDefinition();

		return oauth2LoginAuthenticationFilterBuilder.getBeanDefinition();
	}

	private BeanMetadataElement getOAuth2AuthorizationRequestRepository(Element element) {
		BeanMetadataElement oauth2AuthRequestRepository = null;
		String oauth2AuthRequestRepositoryRef = element.getAttribute(ATT_AUTHORIZATION_REQUEST_REPOSITORY_REF);
		if (!StringUtils.isEmpty(oauth2AuthRequestRepositoryRef)) {
			oauth2AuthRequestRepository = new RuntimeBeanReference(oauth2AuthRequestRepositoryRef);
		} else {
			oauth2AuthRequestRepository = BeanDefinitionBuilder.rootBeanDefinition(
					"org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository")
					.getBeanDefinition();
		}
		return oauth2AuthRequestRepository;
	}

	private BeanMetadataElement getAuthorizedClientRepository(Element element,
			BeanMetadataElement clientRegistrationRepository) {
		BeanMetadataElement authorizedClientRepository = null;

		String authorizedClientRepositoryRef = element.getAttribute(ATT_AUTHORIZED_CLIENT_REPOSITORY_REF);
		if (!StringUtils.isEmpty(authorizedClientRepositoryRef)) {
			authorizedClientRepository = new RuntimeBeanReference(authorizedClientRepositoryRef);
		} else {
			BeanMetadataElement oauth2AuthorizedClientService = null;
			String authorizedClientServiceRef = element.getAttribute(ATT_AUTHORIZED_CLIENT_SERVICE_REF);
			if (!StringUtils.isEmpty(authorizedClientServiceRef)) {
				oauth2AuthorizedClientService = new RuntimeBeanReference(authorizedClientServiceRef);
			} else {
				oauth2AuthorizedClientService = BeanDefinitionBuilder
						.rootBeanDefinition(
								"org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService")
						.addConstructorArgValue(clientRegistrationRepository).getBeanDefinition();
			}

			authorizedClientRepository = BeanDefinitionBuilder.rootBeanDefinition(
					"org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository")
					.addConstructorArgValue(oauth2AuthorizedClientService).getBeanDefinition();
		}

		return authorizedClientRepository;
	}

	private BeanMetadataElement getClientRegistrationRepository(Element element) {
		BeanMetadataElement clientRegistrationRepository = null;

		String clientRegistrationRepositoryRef = element.getAttribute(ATT_CLIENT_REGISTRATION_REPOSITORY_REF);
		if (!StringUtils.isEmpty(clientRegistrationRepositoryRef)) {
			clientRegistrationRepository = new RuntimeBeanReference(clientRegistrationRepositoryRef);
		} else {
			clientRegistrationRepository = new RuntimeBeanReference(ClientRegistrationRepository.class);
		}
		return clientRegistrationRepository;
	}

	private BeanDefinition getOAuth2OidcAuthProvider(Element element, BeanMetadataElement accessTokenResponseClient,
			String oauth2UserAuthMapperRef, ParserContext parserContext) {
		BeanDefinition oauth2OidcAuthProvider = null;
		boolean oidcAuthenticationProviderEnabled = ClassUtils
				.isPresent("org.springframework.security.oauth2.jwt.JwtDecoder", this.getClass().getClassLoader());

		if (oidcAuthenticationProviderEnabled) {
			BeanMetadataElement oidcUserService = getOAuth2OidcUserService(element);

			BeanDefinitionBuilder oauth2OidcAuthProviderBuilder = BeanDefinitionBuilder.rootBeanDefinition(
					"org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider")
					.addConstructorArgValue(accessTokenResponseClient).addConstructorArgValue(oidcUserService);

			if (!StringUtils.isEmpty(oauth2UserAuthMapperRef)) {
				oauth2OidcAuthProviderBuilder.addPropertyReference("authoritiesMapper", oauth2UserAuthMapperRef);
			}

			String jwtDecoderFactoryRef = element.getAttribute(ATT_JWT_DECODER_FACTORY_REF);
			if (!StringUtils.isEmpty(jwtDecoderFactoryRef)) {
				oauth2OidcAuthProviderBuilder.addPropertyReference("jwtDecoderFactory", jwtDecoderFactoryRef);
			}

			oauth2OidcAuthProvider = oauth2OidcAuthProviderBuilder.getBeanDefinition();
		} else {
			oauth2OidcAuthProvider = BeanDefinitionBuilder.rootBeanDefinition(OidcAuthenticationRequestChecker.class)
					.getBeanDefinition();
		}

		return oauth2OidcAuthProvider;
	}

	private BeanMetadataElement getOAuth2OidcUserService(Element element) {
		BeanMetadataElement oauth2OidcUserService = null;
		String oauth2UserServiceRef = element.getAttribute(ATT_OIDC_USER_SERVICE_REF);
		if (!StringUtils.isEmpty(oauth2UserServiceRef)) {
			oauth2OidcUserService = new RuntimeBeanReference(oauth2UserServiceRef);
		} else {
			oauth2OidcUserService = BeanDefinitionBuilder
					.rootBeanDefinition("org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService")
					.getBeanDefinition();
		}
		return oauth2OidcUserService;
	}

	private BeanMetadataElement getOAuth2UserService(Element element) {
		BeanMetadataElement oauth2UserService = null;
		String oauth2UserServiceRef = element.getAttribute(ATT_USER_SERVICE_REF);
		if (!StringUtils.isEmpty(oauth2UserServiceRef)) {
			oauth2UserService = new RuntimeBeanReference(oauth2UserServiceRef);
		} else {
			oauth2UserService = BeanDefinitionBuilder
					.rootBeanDefinition("org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService")
					.getBeanDefinition();
		}
		return oauth2UserService;
	}

	private BeanMetadataElement getAccessTokenResponseClient(Element element) {
		BeanMetadataElement accessTokenResponseClient = null;

		String accessTokenResponseClientRef = element.getAttribute(ATT_ACCESS_TOKEN_RESPONSE_CLIENT_REF);
		if (!StringUtils.isEmpty(accessTokenResponseClientRef)) {
			accessTokenResponseClient = new RuntimeBeanReference(accessTokenResponseClientRef);
		} else {
			accessTokenResponseClient = BeanDefinitionBuilder.rootBeanDefinition(
					"org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient")
					.getBeanDefinition();
		}
		return accessTokenResponseClient;
	}

	BeanDefinition getOAuth2AuthorizationRequestRedirectFilter() {
		return oauth2AuthorizationRequestRedirectFilter;
	}

	BeanDefinition getOAuth2LoginAuthenticationEntryPoint() {
		return oauth2LoginAuthenticationEntryPoint;
	}

	BeanDefinition getOAuth2LoginAuthenticationProvider() {
		return oauth2LoginAuthenticationProvider;
	}

	BeanDefinition getOAuth2LoginOidcAuthenticationProvider() {
		return oauth2LoginOidcAuthenticationProvider;
	}

	BeanDefinition getOAuth2LoginLinks() {
		return oauth2LoginLinks;
	}

	private Map<RequestMatcher, AuthenticationEntryPoint> getLoginEntryPoint(Element element,
			ParserContext parserContext) {
		Map<RequestMatcher, AuthenticationEntryPoint> entryPoints = null;
		Element clientRegsElt = DomUtils.getChildElementByTagName(element.getOwnerDocument().getDocumentElement(),
				Elements.CLIENT_REGISTRATIONS);

		if (clientRegsElt != null) {
			List<Element> clientRegList = DomUtils.getChildElementsByTagName(clientRegsElt, ELT_CLIENT_REGISTRATION);

			if (clientRegList.size() == 1) {
				RequestMatcher loginPageMatcher = new AntPathRequestMatcher(DEFAULT_LOGIN_URI);
				RequestMatcher faviconMatcher = new AntPathRequestMatcher("/favicon.ico");
				RequestMatcher defaultEntryPointMatcher = this.getAuthenticationEntryPointMatcher();
				RequestMatcher defaultLoginPageMatcher = new AndRequestMatcher(
						new OrRequestMatcher(loginPageMatcher, faviconMatcher), defaultEntryPointMatcher);

				RequestMatcher notXRequestedWith = new NegatedRequestMatcher(
						new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest"));

				Element clientRegElt = clientRegList.get(0);
				entryPoints = new LinkedHashMap<>();
				entryPoints.put(
						new AndRequestMatcher(notXRequestedWith, new NegatedRequestMatcher(defaultLoginPageMatcher)),
						new LoginUrlAuthenticationEntryPoint(DEFAULT_AUTHORIZATION_REQUEST_BASE_URI + "/"
								+ clientRegElt.getAttribute(ATT_REGISTRATION_ID)));

			}
		}

		return entryPoints;
	}

	private RequestMatcher getAuthenticationEntryPointMatcher() {
		ContentNegotiationStrategy contentNegotiationStrategy = new HeaderContentNegotiationStrategy();

		MediaTypeRequestMatcher mediaMatcher = new MediaTypeRequestMatcher(contentNegotiationStrategy,
				MediaType.APPLICATION_XHTML_XML, new MediaType("image", "*"), MediaType.TEXT_HTML,
				MediaType.TEXT_PLAIN);
		mediaMatcher.setIgnoredMediaTypes(Collections.singleton(MediaType.ALL));

		RequestMatcher notXRequestedWith = new NegatedRequestMatcher(
				new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest"));

		return new AndRequestMatcher(Arrays.asList(notXRequestedWith, mediaMatcher));
	}

	private static class OidcAuthenticationRequestChecker implements AuthenticationProvider {

		@Override
		public Authentication authenticate(Authentication authentication) throws AuthenticationException {
			OAuth2LoginAuthenticationToken authorizationCodeAuthentication = (OAuth2LoginAuthenticationToken) authentication;

			// Section 3.1.2.1 Authentication Request -
			// https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest
			// scope
			// REQUIRED. OpenID Connect requests MUST contain the "openid" scope value.
			if (authorizationCodeAuthentication.getAuthorizationExchange().getAuthorizationRequest().getScopes()
					.contains(OidcScopes.OPENID)) {

				OAuth2Error oauth2Error = new OAuth2Error("oidc_provider_not_configured",
						"An OpenID Connect Authentication Provider has not been configured. "
								+ "Check to ensure you include the dependency 'spring-security-oauth2-jose'.",
						null);
				throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
			}

			return null;
		}

		@Override
		public boolean supports(Class<?> authentication) {
			return OAuth2LoginAuthenticationToken.class.isAssignableFrom(authentication);
		}
	}

	/**
	 * Wrapper bean class to provide configuration from applicationContext
	 */
	private static class OAuth2LoginBeanConfig implements ApplicationContextAware {

		private ApplicationContext appContext;

		@Override
		public void setApplicationContext(ApplicationContext appContext) throws BeansException {
			this.appContext = appContext;
		}

		@SuppressWarnings({ "unchecked", "unused" })
		public Map<String, String> getLoginLinks() {
			Iterable<ClientRegistration> clientRegistrations = null;
			ClientRegistrationRepository clientRegistrationRepository = appContext
					.getBean(ClientRegistrationRepository.class);
			ResolvableType type = ResolvableType.forInstance(clientRegistrationRepository).as(Iterable.class);
			if (type != ResolvableType.NONE && ClientRegistration.class.isAssignableFrom(type.resolveGenerics()[0])) {
				clientRegistrations = (Iterable<ClientRegistration>) clientRegistrationRepository;
			}
			if (clientRegistrations == null) {
				return Collections.emptyMap();
			}

			String authorizationRequestBaseUri = DEFAULT_AUTHORIZATION_REQUEST_BASE_URI;
			Map<String, String> loginUrlToClientName = new HashMap<>();
			clientRegistrations.forEach(registration -> loginUrlToClientName.put(
					authorizationRequestBaseUri + "/" + registration.getRegistrationId(),
					registration.getClientName()));

			return loginUrlToClientName;
		}

	}
}
