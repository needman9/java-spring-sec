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

package org.springframework.security.oauth2.client.endpoint;

import org.junit.Test;
import org.springframework.security.oauth2.core.endpoint.AuthorizationRequest;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Winch
 * @since 5.0
 */
public class DefaultAuthorizationRequestUriBuilderTests {
	private DefaultAuthorizationRequestUriBuilder builder = new DefaultAuthorizationRequestUriBuilder();

	@Test
	public void buildWhenScopeMultiThenSeparatedByEncodedSpace() {
		AuthorizationRequest request = AuthorizationRequest.implicit()
			.additionalParameters(Collections.singletonMap("foo","bar"))
			.authorizationUri("https://idp.example.com/oauth2/v2/auth")
			.clientId("client-id")
			.state("thestate")
			.redirectUri("https://client.example.com/login/oauth2")
			.scopes(new HashSet<>(Arrays.asList("openid", "user")))
			.build();

		URI result = this.builder.build(request);

		assertThat(result.toASCIIString()).isEqualTo("https://idp.example.com/oauth2/v2/auth?response_type=token&client_id=client-id&scope=openid%20user&state=thestate&redirect_uri=https://client.example.com/login/oauth2");
	}
}
