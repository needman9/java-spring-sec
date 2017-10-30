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
package org.springframework.security.oauth2.core.oidc.user;

import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimAccessor;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.StandardClaimAccessor;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

/**
 * A representation of a user <code>Principal</code>
 * that is registered with an <i>OpenID Connect 1.0 Provider</i>.
 *
 * <p>
 * An <code>OidcUser</code> contains &quot;Claims&quot; about the Authentication of the End-User.
 * The claims are aggregated from the <code>OidcIdToken</code> and optionally the <code>OidcUserInfo</code>.
 *
 * <p>
 * Implementation instances of this interface represent an {@link AuthenticatedPrincipal}
 * which is associated to an {@link Authentication} object
 * and may be accessed via {@link Authentication#getPrincipal()}.
 *
 * @author Joe Grandja
 * @since 5.0
 * @see DefaultOidcUser
 * @see OAuth2User
 * @see OidcIdToken
 * @see OidcUserInfo
 * @see IdTokenClaimAccessor
 * @see StandardClaimAccessor
 * @see <a target="_blank" href="http://openid.net/specs/openid-connect-core-1_0.html#IDToken">ID Token</a>
 * @see <a target="_blank" href="http://openid.net/specs/openid-connect-core-1_0.html#StandardClaims">Standard Claims</a>
 */
public interface OidcUser extends OAuth2User, IdTokenClaimAccessor {

	Map<String, Object> getClaims();

	OidcUserInfo getUserInfo();

	OidcIdToken getIdToken();
}
