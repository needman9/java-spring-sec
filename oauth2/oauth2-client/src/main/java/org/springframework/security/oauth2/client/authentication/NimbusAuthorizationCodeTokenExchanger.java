/*
 * Copyright 2002-2017 the original author or authors.
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


import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.ClientID;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of an {@link AuthorizationGrantTokenExchanger} that <i>&quot;exchanges&quot;</i>
 * an <i>authorization code</i> credential for an <i>access token</i> credential
 * at the authorization server's <i>Token Endpoint</i>.
 *
 * <p>
 * <b>NOTE:</b> This implementation uses the <b>Nimbus OAuth 2.0 SDK</b> internally.
 *
 * @author Joe Grandja
 * @since 5.0
 * @see AuthorizationGrantTokenExchanger
 * @see OAuth2AuthorizationCodeAuthenticationToken
 * @see OAuth2AccessTokenResponse
 * @see <a target="_blank" href="https://connect2id.com/products/nimbus-oauth-openid-connect-sdk">Nimbus OAuth 2.0 SDK</a>
 * @see <a target="_blank" href="https://tools.ietf.org/html/rfc6749#section-4.1.3">Section 4.1.3 Access Token Request (Authorization Code Grant)</a>
 * @see <a target="_blank" href="https://tools.ietf.org/html/rfc6749#section-4.1.4">Section 4.1.4 Access Token Response (Authorization Code Grant)</a>
 */
public class NimbusAuthorizationCodeTokenExchanger implements AuthorizationGrantTokenExchanger<OAuth2AuthorizationCodeAuthenticationToken> {
	private static final String INVALID_TOKEN_RESPONSE_ERROR_CODE = "invalid_token_response";

	@Override
	public OAuth2AccessTokenResponse exchange(OAuth2AuthorizationCodeAuthenticationToken authorizationCodeAuthentication)
			throws OAuth2AuthenticationException {

		ClientRegistration clientRegistration = authorizationCodeAuthentication.getClientRegistration();

		// Build the authorization code grant request for the token endpoint
		AuthorizationCode authorizationCode = new AuthorizationCode(
			authorizationCodeAuthentication.getAuthorizationExchange().getAuthorizationResponse().getCode());
		URI redirectUri = toURI(clientRegistration.getRedirectUri());
		AuthorizationGrant authorizationCodeGrant = new AuthorizationCodeGrant(authorizationCode, redirectUri);
		URI tokenUri = toURI(clientRegistration.getProviderDetails().getTokenUri());

		// Set the credentials to authenticate the client at the token endpoint
		ClientID clientId = new ClientID(clientRegistration.getClientId());
		Secret clientSecret = new Secret(clientRegistration.getClientSecret());
		ClientAuthentication clientAuthentication;
		if (ClientAuthenticationMethod.POST.equals(clientRegistration.getClientAuthenticationMethod())) {
			clientAuthentication = new ClientSecretPost(clientId, clientSecret);
		} else {
			clientAuthentication = new ClientSecretBasic(clientId, clientSecret);
		}

		com.nimbusds.oauth2.sdk.TokenResponse tokenResponse;
		try {
			// Send the Access Token request
			TokenRequest tokenRequest = new TokenRequest(tokenUri, clientAuthentication, authorizationCodeGrant);
			HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
			httpRequest.setAccept(MediaType.APPLICATION_JSON_VALUE);
			httpRequest.setConnectTimeout(30000);
			httpRequest.setReadTimeout(30000);
			tokenResponse = com.nimbusds.oauth2.sdk.TokenResponse.parse(httpRequest.send());
		} catch (ParseException pe) {
			throw new OAuth2AuthenticationException(new OAuth2Error(INVALID_TOKEN_RESPONSE_ERROR_CODE), pe);
		} catch (IOException ioe) {
			throw new AuthenticationServiceException("An error occurred while sending the Access Token Request: " +
					ioe.getMessage(), ioe);
		}

		if (!tokenResponse.indicatesSuccess()) {
			TokenErrorResponse tokenErrorResponse = (TokenErrorResponse) tokenResponse;
			ErrorObject errorObject = tokenErrorResponse.getErrorObject();
			OAuth2Error oauth2Error = new OAuth2Error(errorObject.getCode(), errorObject.getDescription(),
				(errorObject.getURI() != null ? errorObject.getURI().toString() : null));
			throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
		}

		AccessTokenResponse accessTokenResponse = (AccessTokenResponse) tokenResponse;

		String accessToken = accessTokenResponse.getTokens().getAccessToken().getValue();
		OAuth2AccessToken.TokenType accessTokenType = null;
		if (OAuth2AccessToken.TokenType.BEARER.getValue().equalsIgnoreCase(accessTokenResponse.getTokens().getAccessToken().getType().getValue())) {
			accessTokenType = OAuth2AccessToken.TokenType.BEARER;
		}
		long expiresIn = accessTokenResponse.getTokens().getAccessToken().getLifetime();

		// As per spec, in section 5.1 Successful Access Token Response
		// https://tools.ietf.org/html/rfc6749#section-5.1
		// If AccessTokenResponse.scope is empty, then default to the scope
		// originally requested by the client in the Authorization Request
		Set<String> scopes;
		if (CollectionUtils.isEmpty(accessTokenResponse.getTokens().getAccessToken().getScope())) {
			scopes = new LinkedHashSet<>(
				authorizationCodeAuthentication.getAuthorizationExchange().getAuthorizationRequest().getScopes());
		} else {
			scopes = new LinkedHashSet<>(
				accessTokenResponse.getTokens().getAccessToken().getScope().toStringList());
		}

		Map<String, Object> additionalParameters = new LinkedHashMap<>(accessTokenResponse.getCustomParameters());

		return OAuth2AccessTokenResponse.withToken(accessToken)
			.tokenType(accessTokenType)
			.expiresIn(expiresIn)
			.scopes(scopes)
			.additionalParameters(additionalParameters)
			.build();
	}

	private static URI toURI(String uriStr) {
		try {
			return new URI(uriStr);
		} catch (Exception ex) {
			throw new IllegalArgumentException("An error occurred parsing URI: " + uriStr, ex);
		}
	}
}
