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
package org.springframework.security.oauth2.client.web;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.client.endpoint.AuthorizationRequestUriBuilder;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;

/**
 * Tests {@link OAuth2AuthorizationRequestRedirectFilter}.
 *
 * @author Joe Grandja
 */
public class OAuth2AuthorizationRequestRedirectFilterTests {

	@Test(expected = IllegalArgumentException.class)
	public void constructorWhenClientRegistrationRepositoryIsNullThenThrowIllegalArgumentException() {
		new OAuth2AuthorizationRequestRedirectFilter(null);
	}

	@Test
	public void doFilterWhenRequestDoesNotMatchClientThenContinueChain() throws Exception {
		ClientRegistration clientRegistration = TestUtil.googleClientRegistration();
		String authorizationUri = clientRegistration.getProviderDetails().getAuthorizationUri().toString();
		OAuth2AuthorizationRequestRedirectFilter filter =
				setupFilter(authorizationUri, clientRegistration);

		String requestURI = "/path";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestURI);
		request.setServletPath(requestURI);
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = Mockito.mock(FilterChain.class);

		filter.doFilter(request, response, filterChain);

		Mockito.verify(filterChain).doFilter(Matchers.any(HttpServletRequest.class), Matchers.any(HttpServletResponse.class));
	}

	@Test
	public void doFilterWhenRequestMatchesClientThenRedirectForAuthorization() throws Exception {
		ClientRegistration clientRegistration = TestUtil.googleClientRegistration();
		String authorizationUri = clientRegistration.getProviderDetails().getAuthorizationUri().toString();
		OAuth2AuthorizationRequestRedirectFilter filter =
				setupFilter(authorizationUri, clientRegistration);

		String requestUri = TestUtil.AUTHORIZATION_BASE_URI + "/" + clientRegistration.getRegistrationId();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = Mockito.mock(FilterChain.class);

		filter.doFilter(request, response, filterChain);

		Mockito.verifyZeroInteractions(filterChain);        // Request should not proceed up the chain

		Assertions.assertThat(response.getRedirectedUrl()).isEqualTo(authorizationUri);
	}

	@Test
	public void doFilterWhenRequestMatchesClientThenAuthorizationRequestSavedInSession() throws Exception {
		ClientRegistration clientRegistration = TestUtil.githubClientRegistration();
		String authorizationUri = clientRegistration.getProviderDetails().getAuthorizationUri().toString();
		OAuth2AuthorizationRequestRedirectFilter filter =
				setupFilter(authorizationUri, clientRegistration);
		AuthorizationRequestRepository authorizationRequestRepository = new HttpSessionAuthorizationRequestRepository();
		filter.setAuthorizationRequestRepository(authorizationRequestRepository);

		String requestUri = TestUtil.AUTHORIZATION_BASE_URI + "/" + clientRegistration.getRegistrationId();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setServletPath(requestUri);
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = Mockito.mock(FilterChain.class);

		filter.doFilter(request, response, filterChain);

		Mockito.verifyZeroInteractions(filterChain);        // Request should not proceed up the chain

		// The authorization request attributes are saved in the session before the redirect happens
		OAuth2AuthorizationRequest authorizationRequest =
				authorizationRequestRepository.loadAuthorizationRequest(request);
		Assertions.assertThat(authorizationRequest).isNotNull();

		Assertions.assertThat(authorizationRequest.getAuthorizationUri()).isNotNull();
		Assertions.assertThat(authorizationRequest.getGrantType()).isNotNull();
		Assertions.assertThat(authorizationRequest.getResponseType()).isNotNull();
		Assertions.assertThat(authorizationRequest.getClientId()).isNotNull();
		Assertions.assertThat(authorizationRequest.getRedirectUri()).isNotNull();
		Assertions.assertThat(authorizationRequest.getScopes()).isNotNull();
		Assertions.assertThat(authorizationRequest.getState()).isNotNull();
	}

	private OAuth2AuthorizationRequestRedirectFilter setupFilter(String authorizationUri,
																	ClientRegistration... clientRegistrations) throws Exception {

		AuthorizationRequestUriBuilder authorizationUriBuilder = Mockito.mock(AuthorizationRequestUriBuilder.class);
		URI authorizationURI = new URI(authorizationUri);
		Mockito.when(authorizationUriBuilder.build(Matchers.any(OAuth2AuthorizationRequest.class))).thenReturn(authorizationURI);

		return setupFilter(authorizationUriBuilder, clientRegistrations);
	}

	private OAuth2AuthorizationRequestRedirectFilter setupFilter(AuthorizationRequestUriBuilder authorizationUriBuilder,
																	ClientRegistration... clientRegistrations) throws Exception {

		ClientRegistrationRepository clientRegistrationRepository = TestUtil.clientRegistrationRepository(clientRegistrations);
		OAuth2AuthorizationRequestRedirectFilter filter = new OAuth2AuthorizationRequestRedirectFilter(clientRegistrationRepository);
		filter.setAuthorizationRequestUriBuilder(authorizationUriBuilder);

		return filter;
	}
}
