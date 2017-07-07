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
package org.springframework.security.oauth2.oidc.core.user;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.oidc.core.IdToken;
import org.springframework.security.oauth2.oidc.core.UserInfo;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link GrantedAuthority} that is associated with an {@link OidcUser}.
 *
 * @author Joe Grandja
 * @since 5.0
 * @see OidcUser
 */
public class OidcUserAuthority extends OAuth2UserAuthority {
	private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;
	private final IdToken idToken;
	private final UserInfo userInfo;

	public OidcUserAuthority(IdToken idToken) {
		this(idToken, null);
	}

	public OidcUserAuthority(IdToken idToken, UserInfo userInfo) {
		this("ROLE_USER", idToken, userInfo);
	}

	public OidcUserAuthority(String authority, IdToken idToken, UserInfo userInfo) {
		super(authority, idToken.getClaims());
		this.idToken = idToken;
		this.userInfo = userInfo;
		if (userInfo != null) {
			this.setAttributes(
				Stream.of(this.getAttributes(), userInfo.getClaims())
					.flatMap(m -> m.entrySet().stream())
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> k1))
			);
		}
	}

	public IdToken getIdToken() {
		return this.idToken;
	}

	public UserInfo getUserInfo() {
		return this.userInfo;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		if (!super.equals(obj)) {
			return false;
		}

		OidcUserAuthority that = (OidcUserAuthority) obj;

		if (!this.getIdToken().equals(that.getIdToken())) {
			return false;
		}
		return this.getUserInfo() != null ? this.getUserInfo().equals(that.getUserInfo()) : that.getUserInfo() == null;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + this.getIdToken().hashCode();
		result = 31 * result + (this.getUserInfo() != null ? this.getUserInfo().hashCode() : 0);
		return result;
	}
}
