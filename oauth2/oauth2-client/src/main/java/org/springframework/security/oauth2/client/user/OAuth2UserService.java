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
package org.springframework.security.oauth2.client.user;

import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.oidc.core.UserInfo;
import org.springframework.security.oauth2.oidc.core.user.OidcUser;

/**
 * Implementations of this interface are responsible for obtaining
 * the end-user's (resource owner) attributes from the <i>UserInfo Endpoint</i>
 * using the provided {@link OAuth2ClientAuthenticationToken#getAccessToken()}
 * and returning an {@link AuthenticatedPrincipal} in the form of an {@link OAuth2User}.
 *
 * @author Joe Grandja
 * @since 5.0
 * @see OAuth2ClientAuthenticationToken
 * @see AuthenticatedPrincipal
 * @see OAuth2User
 * @see OidcUser
 * @see UserInfo
 */
public interface OAuth2UserService {

	OAuth2User loadUser(OAuth2ClientAuthenticationToken token) throws OAuth2AuthenticationException;

}
