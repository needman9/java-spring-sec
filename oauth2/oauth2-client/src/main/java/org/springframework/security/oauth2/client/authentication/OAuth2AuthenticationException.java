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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.util.Assert;

/**
 * This exception is thrown for all <i>OAuth 2.0</i> related {@link Authentication} errors.
 *
 * <p>
 * There are a number of scenarios where an error may occur, for example:
 * <ul>
 *  <li>The authorization request or token request is missing a required parameter</li>
 *	<li>Missing or invalid client identifier</li>
 *	<li>Invalid or mismatching redirection URI</li>
 *	<li>The requested scope is invalid, unknown, or malformed</li>
 *	<li>The resource owner or authorization server denied the access request</li>
 *	<li>Client authentication failed</li>
 *	<li>The provided authorization grant (authorization code, resource owner credentials) is invalid, expired, or revoked</li>
 * </ul>
 *
 * @author Joe Grandja
 * @since 5.0
 */
public class OAuth2AuthenticationException extends AuthenticationException {
	private OAuth2Error errorObject;

	public OAuth2AuthenticationException(OAuth2Error errorObject, Throwable cause) {
		this(errorObject, cause.getMessage(), cause);
	}

	public OAuth2AuthenticationException(OAuth2Error errorObject, String message) {
		super(message);
		this.setErrorObject(errorObject);
	}

	public OAuth2AuthenticationException(OAuth2Error errorObject, String message, Throwable cause) {
		super(message, cause);
		this.setErrorObject(errorObject);
	}

	public OAuth2Error getErrorObject() {
		return errorObject;
	}

	private void setErrorObject(OAuth2Error errorObject) {
		Assert.notNull(errorObject, "OAuth2 Error object cannot be null");
		this.errorObject = errorObject;
	}
}
