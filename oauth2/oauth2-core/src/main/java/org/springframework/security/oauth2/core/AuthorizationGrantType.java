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

/**
 * An authorization grant is a credential representing the resource owner's authorization
 * (to access it's protected resources) to the client and used by the client to obtain an access token.
 *
 * <p>
 * The <i>OAuth 2.0 Authorization Framework</i> defines four standard grant types:
 * authorization code, implicit, resource owner password credentials, and client credentials.
 * It also provides an extensibility mechanism for defining additional grant types.
 *
 * <p>
 * <b>NOTE:</b> &quot;authorization code&quot; is currently the only supported grant type.
 *
 * @author Joe Grandja
 * @since 5.0
 * @see <a target="_blank" href="https://tools.ietf.org/html/rfc6749#section-1.3">Section 1.3 Authorization Grant</a>
 */
public enum AuthorizationGrantType {
	AUTHORIZATION_CODE("authorization_code");

	private final String value;

	AuthorizationGrantType(String value) {
		this.value = value;
	}

	public String value() {
		return this.value;
	}
}
