/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.oauth2.client.authentication;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.user.OAuth2UserService;
import org.springframework.security.oauth2.client.web.converter.AuthorizationCodeAuthorizationResponseAttributesConverter;
import org.springframework.security.oauth2.client.web.converter.ErrorResponseAttributesConverter;
import org.springframework.security.oauth2.core.AccessToken;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.*;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.springframework.security.oauth2.client.authentication.AuthorizationCodeRequestRedirectFilter.isDefaultRedirectUri;

/**
 * An implementation of an {@link AbstractAuthenticationProcessingFilter} that handles
 * the processing of an <i>OAuth 2.0 Authorization Response</i> for the authorization code grant flow.
 *
 * <p>
 * This <code>Filter</code> processes the <i>Authorization Response</i> in the following step sequence:
 *
 * <ol>
 * <li>
 *	Assuming the resource owner (end-user) has granted access to the client, the authorization server will append the
 *	{@link OAuth2Parameter#CODE} and {@link OAuth2Parameter#STATE} (if provided in the <i>Authorization Request</i>) parameters
 *	to the {@link OAuth2Parameter#REDIRECT_URI} (provided in the <i>Authorization Request</i>)
 *	and redirect the end-user's user-agent back to this <code>Filter</code> (the client).
 * </li>
 * <li>
 *  This <code>Filter</code> will then create an {@link AuthorizationCodeAuthenticationToken} with
 *  the {@link OAuth2Parameter#CODE} received in the previous step and pass it to
 *  {@link AuthorizationCodeAuthenticationProvider#authenticate(Authentication)} (indirectly via {@link AuthenticationManager}).
 *  The {@link AuthorizationCodeAuthenticationProvider} will use an {@link AuthorizationGrantTokenExchanger} to make a request
 *  to the authorization server's <i>Token Endpoint</i> for exchanging the {@link OAuth2Parameter#CODE} for an {@link AccessToken}.
 * </li>
 * <li>
 *  Upon receiving the <i>Access Token Request</i>, the authorization server will authenticate the client,
 *  verify the {@link OAuth2Parameter#CODE}, and ensure that the {@link OAuth2Parameter#REDIRECT_URI}
 *	received matches the <code>URI</code> originally provided in the <i>Authorization Request</i>.
 *	If the request is valid, the authorization server will respond back with a {@link TokenResponseAttributes}.
 * </li>
 * <li>
 *  The {@link AuthorizationCodeAuthenticationProvider} will then create a new {@link OAuth2AuthenticationToken}
 *  associating the {@link AccessToken} from the {@link TokenResponseAttributes} and pass it to
 *  {@link OAuth2UserService#loadUser(OAuth2AuthenticationToken)}. The {@link OAuth2UserService} will make a request
 *  to the authorization server's <i>UserInfo Endpoint</i> (using the {@link AccessToken})
 *  to obtain the end-user's (resource owner) attributes and return it in the form of an {@link OAuth2User}.
 * </li>
 * <li>
 *  The {@link AuthorizationCodeAuthenticationProvider} will create another new {@link OAuth2AuthenticationToken}
 *  but this time associating the {@link AccessToken} and {@link OAuth2User} returned from the {@link OAuth2UserService}.
 *  Finally, the {@link OAuth2AuthenticationToken} is returned to the {@link AuthenticationManager}
 *  and then back to this <code>Filter</code> at which point the session is considered <i>&quot;authenticated&quot;</i>.
 * </li>
 * </ol>
 *
 * <p>
 * <b>NOTE:</b> Steps 4-5 are <b>not</b> part of the authorization code grant flow and instead are
 * <i>&quot;authentication flow&quot;</i> steps that are required in order to authenticate the end-user with the system.
 *
 * @author Joe Grandja
 * @since 5.0
 * @see AbstractAuthenticationProcessingFilter
 * @see AuthorizationCodeAuthenticationToken
 * @see AuthorizationCodeAuthenticationProvider
 * @see AuthorizationGrantTokenExchanger
 * @see AuthorizationCodeAuthorizationResponseAttributes
 * @see AuthorizationRequestAttributes
 * @see AuthorizationRequestRepository
 * @see AuthorizationCodeRequestRedirectFilter
 * @see ClientRegistration
 * @see ClientRegistrationRepository
 * @see <a target="_blank" href="https://tools.ietf.org/html/rfc6749#section-4.1">Section 4.1 Authorization Code Grant Flow</a>
 * @see <a target="_blank" href="https://tools.ietf.org/html/rfc6749#section-4.1.2">Section 4.1.2 Authorization Response</a>
 */
public class AuthorizationCodeAuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter {
	public static final String AUTHORIZE_BASE_URI = "/oauth2/authorize/code";
	private static final String CLIENT_ALIAS_VARIABLE_NAME = "clientAlias";
	private static final String AUTHORIZE_URI = AUTHORIZE_BASE_URI + "/{" + CLIENT_ALIAS_VARIABLE_NAME + "}";
	private static final String AUTHORIZATION_REQUEST_NOT_FOUND_ERROR_CODE = "authorization_request_not_found";
	private static final String INVALID_STATE_PARAMETER_ERROR_CODE = "invalid_state_parameter";
	private static final String INVALID_REDIRECT_URI_PARAMETER_ERROR_CODE = "invalid_redirect_uri_parameter";
	private final ErrorResponseAttributesConverter errorResponseConverter = new ErrorResponseAttributesConverter();
	private final AuthorizationCodeAuthorizationResponseAttributesConverter authorizationCodeResponseConverter =
		new AuthorizationCodeAuthorizationResponseAttributesConverter();
	private final RequestMatcher authorizeRequestMatcher = new AntPathRequestMatcher(AUTHORIZE_URI);
	private ClientRegistrationRepository clientRegistrationRepository;
	private AuthorizationRequestRepository authorizationRequestRepository = new HttpSessionAuthorizationRequestRepository();

	public AuthorizationCodeAuthenticationProcessingFilter() {
		super(AUTHORIZE_URI);
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException, IOException, ServletException {

		ErrorResponseAttributes authorizationError = this.errorResponseConverter.apply(request);
		if (authorizationError != null) {
			OAuth2Error oauth2Error = new OAuth2Error(authorizationError.getErrorCode(),
					authorizationError.getDescription(), authorizationError.getUri());
			this.getAuthorizationRequestRepository().removeAuthorizationRequest(request);
			throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
		}

		AuthorizationRequestAttributes matchingAuthorizationRequest = this.resolveAuthorizationRequest(request);

		ClientRegistration clientRegistration = this.getClientRegistrationRepository().getRegistrationByClientId(
				matchingAuthorizationRequest.getClientId());

		// If clientRegistration.redirectUri is the default one (with Uri template variables)
		// then use matchingAuthorizationRequest.redirectUri instead
		if (isDefaultRedirectUri(clientRegistration)) {
			clientRegistration = new ClientRegistrationBuilderWithUriOverrides(
				clientRegistration, matchingAuthorizationRequest.getRedirectUri()).build();
		}

		AuthorizationCodeAuthorizationResponseAttributes authorizationCodeResponseAttributes =
				this.authorizationCodeResponseConverter.apply(request);

		AuthorizationCodeAuthenticationToken authRequest = new AuthorizationCodeAuthenticationToken(
				authorizationCodeResponseAttributes.getCode(), clientRegistration);

		authRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));

		Authentication authenticated = this.getAuthenticationManager().authenticate(authRequest);

		return authenticated;
	}

	public RequestMatcher getAuthorizeRequestMatcher() {
		return this.authorizeRequestMatcher;
	}

	protected ClientRegistrationRepository getClientRegistrationRepository() {
		return this.clientRegistrationRepository;
	}

	public final void setClientRegistrationRepository(ClientRegistrationRepository clientRegistrationRepository) {
		Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
		Assert.notEmpty(clientRegistrationRepository.getRegistrations(), "clientRegistrationRepository cannot be empty");
		this.clientRegistrationRepository = clientRegistrationRepository;
	}

	protected AuthorizationRequestRepository getAuthorizationRequestRepository() {
		return this.authorizationRequestRepository;
	}

	public final void setAuthorizationRequestRepository(AuthorizationRequestRepository authorizationRequestRepository) {
		Assert.notNull(authorizationRequestRepository, "authorizationRequestRepository cannot be null");
		this.authorizationRequestRepository = authorizationRequestRepository;
	}

	private AuthorizationRequestAttributes resolveAuthorizationRequest(HttpServletRequest request) {
		AuthorizationRequestAttributes authorizationRequest =
				this.getAuthorizationRequestRepository().loadAuthorizationRequest(request);
		if (authorizationRequest == null) {
			OAuth2Error oauth2Error = new OAuth2Error(AUTHORIZATION_REQUEST_NOT_FOUND_ERROR_CODE);
			throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
		}
		this.getAuthorizationRequestRepository().removeAuthorizationRequest(request);
		this.assertMatchingAuthorizationRequest(request, authorizationRequest);
		return authorizationRequest;
	}

	private void assertMatchingAuthorizationRequest(HttpServletRequest request, AuthorizationRequestAttributes authorizationRequest) {
		String state = request.getParameter(OAuth2Parameter.STATE);
		if (!authorizationRequest.getState().equals(state)) {
			OAuth2Error oauth2Error = new OAuth2Error(INVALID_STATE_PARAMETER_ERROR_CODE);
			throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
		}

		if (!request.getRequestURL().toString().equals(authorizationRequest.getRedirectUri())) {
			OAuth2Error oauth2Error = new OAuth2Error(INVALID_REDIRECT_URI_PARAMETER_ERROR_CODE);
			throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
		}
	}

	private static class ClientRegistrationBuilderWithUriOverrides extends ClientRegistration.Builder {

		private ClientRegistrationBuilderWithUriOverrides(ClientRegistration clientRegistration, String redirectUri) {
			super(clientRegistration.getClientId());
			this.clientSecret(clientRegistration.getClientSecret());
			this.clientAuthenticationMethod(clientRegistration.getClientAuthenticationMethod());
			this.authorizedGrantType(clientRegistration.getAuthorizedGrantType());
			this.redirectUri(redirectUri);
			if (!CollectionUtils.isEmpty(clientRegistration.getScopes())) {
				this.scopes(clientRegistration.getScopes().stream().toArray(String[]::new));
			}
			this.authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri());
			this.tokenUri(clientRegistration.getProviderDetails().getTokenUri());
			this.userInfoUri(clientRegistration.getProviderDetails().getUserInfoUri());
			this.jwkSetUri(clientRegistration.getProviderDetails().getJwkSetUri());
			this.clientName(clientRegistration.getClientName());
			this.clientAlias(clientRegistration.getClientAlias());
		}
	}
}
