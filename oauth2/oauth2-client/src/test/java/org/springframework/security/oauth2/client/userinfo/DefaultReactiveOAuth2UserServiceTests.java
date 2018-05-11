/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.security.oauth2.client.userinfo;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Rob Winch
 * @since 5.1
 */
public class DefaultReactiveOAuth2UserServiceTests {
	private ClientRegistration.Builder clientRegistration;

	private DefaultReactiveOAuth2UserService userService = new DefaultReactiveOAuth2UserService();

	private OAuth2AccessToken accessToken = new OAuth2AccessToken(
			OAuth2AccessToken.TokenType.BEARER, "access-token", Instant.now(), Instant.now().plus(Duration.ofDays(1)));

	private MockWebServer server;

	@Before
	public void setup() throws Exception {
		this.server = new MockWebServer();
		this.server.start();

		String userInfoUri = this.server.url("/user").toString();

		this.clientRegistration = ClientRegistration.withRegistrationId("github")
				.redirectUriTemplate("{baseUrl}/{action}/oauth2/code/{registrationId}")
				.clientAuthenticationMethod(ClientAuthenticationMethod.BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.scope("read:user")
				.authorizationUri("https://github.com/login/oauth/authorize")
				.tokenUri("https://github.com/login/oauth/access_token")
				.userInfoUri(userInfoUri)
				.userNameAttributeName("user-name")
				.clientName("GitHub")
				.clientId("clientId")
				.clientSecret("clientSecret");
	}

	@After
	public void cleanup() throws Exception {
		this.server.shutdown();
	}

	@Test
	public void loadUserWhenUserRequestIsNullThenThrowIllegalArgumentException() {
		OAuth2UserRequest request = null;
		StepVerifier.create(this.userService.loadUser(request))
			.expectError(IllegalArgumentException.class)
			.verify();
	}

	@Test
	public void loadUserWhenUserInfoUriIsNullThenThrowOAuth2AuthenticationException() {
		this.clientRegistration.userInfoUri(null);

		StepVerifier.create(this.userService.loadUser(oauth2UserRequest()))
				.expectErrorSatisfies(t -> assertThat(t)
						.isInstanceOf(OAuth2AuthenticationException.class)
						.hasMessageContaining("missing_user_info_uri")
				)
				.verify();
	}

	@Test
	public void loadUserWhenUserNameAttributeNameIsNullThenThrowOAuth2AuthenticationException() {
		this.clientRegistration.userNameAttributeName(null);

		StepVerifier.create(this.userService.loadUser(oauth2UserRequest()))
				.expectErrorSatisfies(t -> assertThat(t)
						.isInstanceOf(OAuth2AuthenticationException.class)
						.hasMessageContaining("missing_user_name_attribute")
				)
				.verify();
	}

	@Test
	public void loadUserWhenUserInfoSuccessResponseThenReturnUser() throws Exception {
		String userInfoResponse = "{\n" +
				"	\"user-name\": \"user1\",\n" +
				"   \"first-name\": \"first\",\n" +
				"   \"last-name\": \"last\",\n" +
				"   \"middle-name\": \"middle\",\n" +
				"   \"address\": \"address\",\n" +
				"   \"email\": \"user1@example.com\"\n" +
				"}\n";
		enqueueApplicationJsonBody(userInfoResponse);

		OAuth2User user = this.userService.loadUser(oauth2UserRequest()).block();

		assertThat(user.getName()).isEqualTo("user1");
		assertThat(user.getAttributes().size()).isEqualTo(6);
		assertThat(user.getAttributes().get("user-name")).isEqualTo("user1");
		assertThat(user.getAttributes().get("first-name")).isEqualTo("first");
		assertThat(user.getAttributes().get("last-name")).isEqualTo("last");
		assertThat(user.getAttributes().get("middle-name")).isEqualTo("middle");
		assertThat(user.getAttributes().get("address")).isEqualTo("address");
		assertThat(user.getAttributes().get("email")).isEqualTo("user1@example.com");

		assertThat(user.getAuthorities().size()).isEqualTo(1);
		assertThat(user.getAuthorities().iterator().next()).isInstanceOf(OAuth2UserAuthority.class);
		OAuth2UserAuthority userAuthority = (OAuth2UserAuthority) user.getAuthorities().iterator().next();
		assertThat(userAuthority.getAuthority()).isEqualTo("ROLE_USER");
		assertThat(userAuthority.getAttributes()).isEqualTo(user.getAttributes());
	}

	@Test
	public void loadUserWhenUserInfoSuccessResponseInvalidThenThrowOAuth2AuthenticationException() throws Exception {
		String userInfoResponse = "{\n" +
				"	\"user-name\": \"user1\",\n" +
				"   \"first-name\": \"first\",\n" +
				"   \"last-name\": \"last\",\n" +
				"   \"middle-name\": \"middle\",\n" +
				"   \"address\": \"address\",\n" +
				"   \"email\": \"user1@example.com\"\n";
		//			"}\n";		// Make the JSON invalid/malformed
		enqueueApplicationJsonBody(userInfoResponse);

		assertThatThrownBy(() -> this.userService.loadUser(oauth2UserRequest()).block())
			.isInstanceOf(OAuth2AuthenticationException.class)
			.hasMessageContaining("invalid_user_info_response");
	}

	@Test
	public void loadUserWhenUserInfoErrorResponseThenThrowOAuth2AuthenticationException() throws Exception {
		this.server.enqueue(new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).setResponseCode(500).setBody("{}"));

		assertThatThrownBy(() -> this.userService.loadUser(oauth2UserRequest()).block())
				.isInstanceOf(OAuth2AuthenticationException.class)
				.hasMessageContaining("invalid_user_info_response");
	}

	@Test
	public void loadUserWhenUserInfoUriInvalidThenThrowAuthenticationServiceException() throws Exception {
		this.clientRegistration.userInfoUri("http://invalid-provider.com/user");
		assertThatThrownBy(() -> this.userService.loadUser(oauth2UserRequest()).block())
				.isInstanceOf(AuthenticationServiceException.class);
	}

	private OAuth2UserRequest oauth2UserRequest() {
		return new OAuth2UserRequest(this.clientRegistration.build(), this.accessToken);
	}

	private void enqueueApplicationJsonBody(String json) {

		this.server.enqueue(new MockResponse()
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody(json));
	}
}
