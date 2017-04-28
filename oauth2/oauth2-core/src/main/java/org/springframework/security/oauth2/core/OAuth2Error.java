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
package org.springframework.security.oauth2.core;

import org.springframework.util.Assert;

/**
 * A representation of an <i>OAuth 2.0 Error</i>.
 *
 * <p>
 * At a minimum, an error response will contain an error code.
 * The error code may be one of the standard codes defined by the specification,
 * or a <i>new</i> code defined in the <i>OAuth Extensions Error Registry</i>,
 * for cases where protocol extensions require additional error code(s) above the standard codes.
 *
 * @author Joe Grandja
 * @since 5.0
 * @see <a target="_blank" href="https://tools.ietf.org/html/rfc6749#section-11.4">Section 11.4 OAuth Extensions Error Registry</a>
 */
public class OAuth2Error {
	// Standard error codes
	public static final String INVALID_REQUEST_ERROR_CODE = "invalid_request";
	public static final String INVALID_CLIENT_ERROR_CODE = "invalid_client";
	public static final String INVALID_GRANT_ERROR_CODE = "invalid_grant";
	public static final String UNAUTHORIZED_CLIENT_ERROR_CODE = "unauthorized_client";
	public static final String UNSUPPORTED_GRANT_TYPE_ERROR_CODE = "unsupported_grant_type";
	public static final String INVALID_SCOPE_ERROR_CODE = "invalid_scope";

	private final String errorCode;
	private final String description;
	private final String uri;

	public OAuth2Error(String errorCode) {
		this(errorCode, null, null);
	}

	public OAuth2Error(String errorCode, String description, String uri) {
		Assert.hasText(errorCode, "errorCode cannot be empty");
		this.errorCode = errorCode;
		this.description = description;
		this.uri = uri;
	}

	public String getErrorCode() {
		return this.errorCode;
	}

	public String getDescription() {
		return this.description;
	}

	public String getUri() {
		return this.uri;
	}

	@Override
	public String toString() {
		return "[" + this.getErrorCode() + "] " +
				(this.getDescription() != null ? this.getDescription() : "");
	}
}
