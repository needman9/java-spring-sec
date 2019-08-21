/*
 * Copyright 2002-2019 the original author or authors.
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
package org.springframework.security.oauth2.client.web.server;

import org.junit.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.TestClientRegistrations;
import org.springframework.security.oauth2.core.TestOAuth2AccessTokens;
import org.springframework.security.oauth2.core.TestOAuth2RefreshTokens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ServerOAuth2AuthorizeRequest}.
 *
 * @author Joe Grandja
 */
public class ServerOAuth2AuthorizeRequestTests {
	private ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration().build();
	private Authentication principal = new TestingAuthenticationToken("principal", "password");
	private OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
			this.clientRegistration, this.principal.getName(),
			TestOAuth2AccessTokens.scopes("read", "write"), TestOAuth2RefreshTokens.refreshToken());
	private MockServerWebExchange serverWebExchange = MockServerWebExchange.builder(MockServerHttpRequest.get("/")).build();

	@Test
	public void constructorWhenClientRegistrationIdIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> new ServerOAuth2AuthorizeRequest((String) null, this.principal, this.serverWebExchange))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("clientRegistrationId cannot be empty");
	}

	@Test
	public void constructorWhenAuthorizedClientIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> new ServerOAuth2AuthorizeRequest((OAuth2AuthorizedClient) null, this.principal, this.serverWebExchange))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("authorizedClient cannot be null");
	}

	@Test
	public void constructorWhenPrincipalIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> new ServerOAuth2AuthorizeRequest(this.clientRegistration.getRegistrationId(), null, this.serverWebExchange))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("principal cannot be null");
	}

	@Test
	public void constructorWhenServerWebExchangeIsNullThenThrowIllegalArgumentException() {
		assertThatThrownBy(() -> new ServerOAuth2AuthorizeRequest(this.clientRegistration.getRegistrationId(), this.principal, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("serverWebExchange cannot be null");
	}

	@Test
	public void constructorClientRegistrationIdWhenAllValuesProvidedThenAllValuesAreSet() {
		ServerOAuth2AuthorizeRequest authorizeRequest = new ServerOAuth2AuthorizeRequest(
				this.clientRegistration.getRegistrationId(), this.principal, this.serverWebExchange);

		assertThat(authorizeRequest.getClientRegistrationId()).isEqualTo(this.clientRegistration.getRegistrationId());
		assertThat(authorizeRequest.getPrincipal()).isEqualTo(this.principal);
		assertThat(authorizeRequest.getServerWebExchange()).isEqualTo(this.serverWebExchange);
	}

	@Test
	public void constructorAuthorizedClientWhenAllValuesProvidedThenAllValuesAreSet() {
		ServerOAuth2AuthorizeRequest authorizeRequest = new ServerOAuth2AuthorizeRequest(
				this.authorizedClient, this.principal, this.serverWebExchange);

		assertThat(authorizeRequest.getClientRegistrationId()).isEqualTo(this.authorizedClient.getClientRegistration().getRegistrationId());
		assertThat(authorizeRequest.getAuthorizedClient()).isEqualTo(this.authorizedClient);
		assertThat(authorizeRequest.getPrincipal()).isEqualTo(this.principal);
		assertThat(authorizeRequest.getServerWebExchange()).isEqualTo(this.serverWebExchange);
	}
}
