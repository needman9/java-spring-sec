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
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtDecoder;
import org.springframework.security.oauth2.client.authentication.jwt.JwtDecoderRegistry;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.token.SecurityTokenRepository;
import org.springframework.security.oauth2.client.user.OAuth2UserService;
import org.springframework.security.oauth2.client.web.AuthorizationGrantTokenExchanger;
import org.springframework.security.oauth2.core.AccessToken;
import org.springframework.security.oauth2.core.endpoint.TokenResponseAttributes;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.oidc.client.authentication.OidcClientAuthenticationToken;
import org.springframework.security.oauth2.oidc.client.authentication.OidcUserAuthenticationToken;
import org.springframework.security.oauth2.oidc.core.IdToken;
import org.springframework.security.oauth2.oidc.core.endpoint.OidcParameter;
import org.springframework.security.oauth2.oidc.core.user.OidcUser;
import org.springframework.util.Assert;

import java.util.Collection;

/**
 * An implementation of an {@link AuthenticationProvider} that is responsible for authenticating
 * an <i>authorization code</i> credential with the authorization server's <i>Token Endpoint</i>
 * and if valid, exchanging it for an <i>access token</i> credential and optionally an
 * <i>id token</i> credential (for OpenID Connect Authorization Code Flow).
 * Additionally, it will also obtain the end-user's (resource owner) attributes from the <i>UserInfo Endpoint</i>
 * (using the <i>access token</i>) and create a <code>Principal</code> in the form of an {@link OAuth2User}
 * associating it with the returned {@link OAuth2UserAuthenticationToken}.
 *
 * <p>
 * The {@link AuthorizationCodeAuthenticationProvider} uses an {@link AuthorizationGrantTokenExchanger}
 * to make a request to the authorization server's <i>Token Endpoint</i>
 * to verify the {@link AuthorizationCodeAuthenticationToken#getAuthorizationCode()}.
 * If the request is valid, the authorization server will respond back with a {@link TokenResponseAttributes}.
 *
 * <p>
 * It will then create an {@link OAuth2ClientAuthenticationToken} associating the {@link AccessToken} and optionally
 * the {@link IdToken} from the {@link TokenResponseAttributes} and pass it to
 * {@link OAuth2UserService#loadUser(OAuth2ClientAuthenticationToken)} to obtain the end-user's (resource owner) attributes
 * in the form of an {@link OAuth2User}.
 *
 * <p>
 * Finally, it will create an {@link OAuth2UserAuthenticationToken}, associating the {@link OAuth2User}
 * and {@link OAuth2ClientAuthenticationToken} and return it to the {@link AuthenticationManager},
 * at which point the {@link OAuth2UserAuthenticationToken} is considered <i>&quot;authenticated&quot;</i>.
 *
 * @author Joe Grandja
 * @since 5.0
 * @see AuthorizationCodeAuthenticationToken
 * @see OAuth2ClientAuthenticationToken
 * @see OidcClientAuthenticationToken
 * @see OAuth2UserAuthenticationToken
 * @see AuthorizationGrantTokenExchanger
 * @see TokenResponseAttributes
 * @see AccessToken
 * @see IdToken
 * @see OAuth2UserService
 * @see OAuth2User
 * @see OidcUser
 * @see <a target="_blank" href="https://tools.ietf.org/html/rfc6749#section-4.1">Section 4.1 Authorization Code Grant Flow</a>
 * @see <a target="_blank" href="http://openid.net/specs/openid-connect-core-1_0.html#CodeFlowAuth">Section 3.1 OpenID Connect Authorization Code Flow</a>
 * @see <a target="_blank" href="https://tools.ietf.org/html/rfc6749#section-4.1.3">Section 4.1.3 Access Token Request</a>
 * @see <a target="_blank" href="https://tools.ietf.org/html/rfc6749#section-4.1.4">Section 4.1.4 Access Token Response</a>
 * @see <a target="_blank" href="http://openid.net/specs/openid-connect-core-1_0.html#TokenResponse">Section 3.1.3.3 OpenID Connect Token Response</a>
 */
public class AuthorizationCodeAuthenticationProvider implements AuthenticationProvider {
	private final AuthorizationGrantTokenExchanger<AuthorizationCodeAuthenticationToken> authorizationCodeTokenExchanger;
	private final SecurityTokenRepository<AccessToken> accessTokenRepository;
	private final JwtDecoderRegistry jwtDecoderRegistry;
	private final OAuth2UserService userInfoService;
	private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

	public AuthorizationCodeAuthenticationProvider(
			AuthorizationGrantTokenExchanger<AuthorizationCodeAuthenticationToken> authorizationCodeTokenExchanger,
			SecurityTokenRepository<AccessToken> accessTokenRepository,
			JwtDecoderRegistry jwtDecoderRegistry,
			OAuth2UserService userInfoService) {

		Assert.notNull(authorizationCodeTokenExchanger, "authorizationCodeTokenExchanger cannot be null");
		Assert.notNull(accessTokenRepository, "accessTokenRepository cannot be null");
		Assert.notNull(jwtDecoderRegistry, "jwtDecoderRegistry cannot be null");
		Assert.notNull(userInfoService, "userInfoService cannot be null");
		this.authorizationCodeTokenExchanger = authorizationCodeTokenExchanger;
		this.accessTokenRepository = accessTokenRepository;
		this.jwtDecoderRegistry = jwtDecoderRegistry;
		this.userInfoService = userInfoService;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		AuthorizationCodeAuthenticationToken authorizationCodeAuthentication =
				(AuthorizationCodeAuthenticationToken) authentication;
		ClientRegistration clientRegistration = authorizationCodeAuthentication.getClientRegistration();

		TokenResponseAttributes tokenResponse =
				this.authorizationCodeTokenExchanger.exchange(authorizationCodeAuthentication);

		AccessToken accessToken = new AccessToken(tokenResponse.getTokenType(),
				tokenResponse.getTokenValue(), tokenResponse.getIssuedAt(),
				tokenResponse.getExpiresAt(), tokenResponse.getScope());

		IdToken idToken = null;
		if (tokenResponse.getAdditionalParameters().containsKey(OidcParameter.ID_TOKEN)) {
			JwtDecoder jwtDecoder = this.jwtDecoderRegistry.getJwtDecoder(clientRegistration);
			if (jwtDecoder == null) {
				throw new IllegalArgumentException("Unable to find a registered JwtDecoder for Client Registration: '" + clientRegistration.getRegistrationId() +
					"'. Check to ensure you have configured the JwkSet URI property.");
			}
			Jwt jwt = jwtDecoder.decode((String)tokenResponse.getAdditionalParameters().get(OidcParameter.ID_TOKEN));
			idToken = new IdToken(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(), jwt.getClaims());
		}

		OAuth2ClientAuthenticationToken oauth2ClientAuthentication;
		if (idToken != null) {
			oauth2ClientAuthentication = new OidcClientAuthenticationToken(clientRegistration, accessToken, idToken);
		} else {
			oauth2ClientAuthentication = new OAuth2ClientAuthenticationToken(clientRegistration, accessToken);
		}
		oauth2ClientAuthentication.setDetails(authorizationCodeAuthentication.getDetails());

		OAuth2User user = this.userInfoService.loadUser(oauth2ClientAuthentication);

		Collection<? extends GrantedAuthority> authorities =
				this.authoritiesMapper.mapAuthorities(user.getAuthorities());

		OAuth2UserAuthenticationToken oauth2UserAuthentication;
		if (OidcUser.class.isAssignableFrom(user.getClass())) {
			oauth2UserAuthentication = new OidcUserAuthenticationToken(
				(OidcUser)user, authorities, (OidcClientAuthenticationToken)oauth2ClientAuthentication);
		} else {
			oauth2UserAuthentication = new OAuth2UserAuthenticationToken(user, authorities, oauth2ClientAuthentication);
		}
		oauth2UserAuthentication.setDetails(oauth2ClientAuthentication.getDetails());

		this.accessTokenRepository.saveSecurityToken(accessToken, clientRegistration);

		return oauth2UserAuthentication;
	}

	public final void setAuthoritiesMapper(GrantedAuthoritiesMapper authoritiesMapper) {
		Assert.notNull(authoritiesMapper, "authoritiesMapper cannot be null");
		this.authoritiesMapper = authoritiesMapper;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return AuthorizationCodeAuthenticationToken.class.isAssignableFrom(authentication);
	}
}
