/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.security.oauth2.core;

/**
 * The names of the &quot;Introspection Claims&quot; defined by an
 * <a target="_blank" href="https://tools.ietf.org/html/rfc7662#section-2.2">Introspection
 * Response</a>.
 *
 * @author Josh Cummings
 * @since 5.6
 * @see <a target="_blank" href="https://tools.ietf.org/html/rfc7662#section-2.2">OAuth
 * 2.0 Token Introspection (RFC7662)</a>
 * @see <a target="_blank" href=
 * "https://www.iana.org/assignments/oauth-parameters/oauth-parameters.xhtml#token-introspection-response">OAuth
 * Parameters (IANA)</a>
 */
public interface OAuth2TokenIntrospectionClaimNames {

	/**
	 * {@code active} - Indicator whether or not the token is currently active
	 */
	String ACTIVE = "active";

	/**
	 * {@code username} - A human-readable identifier for the resource owner that
	 * authorized the token
	 */
	String USERNAME = "username";

	/**
	 * {@code client_id} - The Client identifier for the token
	 */
	String CLIENT_ID = "client_id";

	/**
	 * {@code scope} - The scopes for the token
	 */
	String SCOPE = "scope";

	/**
	 * {@code token_type} - The type of the token, for example {@code bearer}.
	 */
	String TOKEN_TYPE = "token_type";

	/**
	 * {@code exp} - A timestamp indicating when the token expires
	 */
	String EXP = "exp";

	/**
	 * {@code iat} - A timestamp indicating when the token was issued
	 */
	String IAT = "iat";

	/**
	 * {@code nbf} - A timestamp indicating when the token is not to be used before
	 */
	String NBF = "nbf";

	/**
	 * {@code sub} - Usually a machine-readable identifier of the resource owner who
	 * authorized the token
	 */
	String SUB = "sub";

	/**
	 * {@code aud} - The intended audience for the token
	 */
	String AUD = "aud";

	/**
	 * {@code iss} - The issuer of the token
	 */
	String ISS = "iss";

	/**
	 * {@code jti} - The identifier for the token
	 */
	String JTI = "jti";

}
