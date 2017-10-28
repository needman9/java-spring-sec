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
package org.springframework.security.oauth2.client.oidc;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.util.Assert;

/**
 * A representation of an OpenID Connect 1.0 <i>&quot;Authorized Client&quot;</i>.
 * <p>
 * A client is considered <i>&quot;authorized&quot;</i> when the End-User (Resource Owner)
 * grants authorization to the Client to access its protected resources.
 * <p>
 * This class associates the {@link #getClientRegistration() Client}
 * to the {@link #getAccessToken() Access Token}
 * granted/authorized by the {@link #getPrincipalName() Resource Owner}, along with
 * the {@link #getIdToken() ID Token} which contains Claims about the authentication of the End-User.
 *
 * @author Joe Grandja
 * @since 5.0
 * @see OAuth2AuthorizedClient
 * @see OidcIdToken
 */
public class OidcAuthorizedClient extends OAuth2AuthorizedClient {
	private final OidcIdToken idToken;

	public OidcAuthorizedClient(ClientRegistration clientRegistration, String principalName,
								OAuth2AccessToken accessToken, OidcIdToken idToken) {

		super(clientRegistration, principalName, accessToken);
		Assert.notNull(idToken, "idToken cannot be null");
		this.idToken = idToken;
	}

	public OidcIdToken getIdToken() {
		return this.idToken;
	}
}
