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

import org.springframework.security.oauth2.core.endpoint.AuthorizationRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Implementations of this interface are responsible for the persistence
 * of {@link AuthorizationRequestAttributes} between requests.
 *
 * <p>
 * Used by the {@link AuthorizationCodeRequestRedirectFilter} for persisting the <i>Authorization Request</i>
 * before it initiates the authorization code grant flow.
 * As well, used by the {@link AuthorizationCodeAuthenticationProcessingFilter} when resolving
 * the associated <i>Authorization Request</i> during the handling of the <i>Authorization Response</i>.
 *
 * @author Joe Grandja
 * @since 5.0
 * @see AuthorizationRequestAttributes
 * @see HttpSessionAuthorizationRequestRepository
 */
public interface AuthorizationRequestRepository {

	AuthorizationRequestAttributes loadAuthorizationRequest(HttpServletRequest request);

	void saveAuthorizationRequest(AuthorizationRequestAttributes authorizationRequest, HttpServletRequest request);

	AuthorizationRequestAttributes removeAuthorizationRequest(HttpServletRequest request);

}
