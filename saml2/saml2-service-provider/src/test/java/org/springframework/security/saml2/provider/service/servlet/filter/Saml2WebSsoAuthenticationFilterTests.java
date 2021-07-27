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

package org.springframework.security.saml2.provider.service.servlet.filter;

import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.saml2.provider.service.authentication.AbstractSaml2AuthenticationRequest;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.TestSaml2AuthenticationTokens;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.servlet.Saml2AuthenticationRequestRepository;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationTokenConverter;
import org.springframework.security.web.authentication.AuthenticationConverter;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class Saml2WebSsoAuthenticationFilterTests {

	private Saml2WebSsoAuthenticationFilter filter;

	private RelyingPartyRegistrationRepository repository = mock(RelyingPartyRegistrationRepository.class);

	private MockHttpServletRequest request = new MockHttpServletRequest();

	private HttpServletResponse response = new MockHttpServletResponse();

	@BeforeEach
	public void setup() {
		this.filter = new Saml2WebSsoAuthenticationFilter(this.repository);
		this.request.setPathInfo("/login/saml2/sso/idp-registration-id");
		this.request.setParameter("SAMLResponse", "xml-data-goes-here");
	}

	@Test
	public void constructingFilterWithMissingRegistrationIdVariableThenThrowsException() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
				() -> this.filter = new Saml2WebSsoAuthenticationFilter(this.repository, "/url/missing/variable"))
				.withMessage("filterProcessesUrl must contain a {registrationId} match variable");
	}

	@Test
	public void constructingFilterWithValidRegistrationIdVariableThenSucceeds() {
		this.filter = new Saml2WebSsoAuthenticationFilter(this.repository, "/url/variable/is/present/{registrationId}");
	}

	@Test
	public void requiresAuthenticationWhenHappyPathThenReturnsTrue() {
		Assertions.assertTrue(this.filter.requiresAuthentication(this.request, this.response));
	}

	@Test
	public void requiresAuthenticationWhenCustomProcessingUrlThenReturnsTrue() {
		this.filter = new Saml2WebSsoAuthenticationFilter(this.repository, "/some/other/path/{registrationId}");
		this.request.setPathInfo("/some/other/path/idp-registration-id");
		this.request.setParameter("SAMLResponse", "xml-data-goes-here");
		Assertions.assertTrue(this.filter.requiresAuthentication(this.request, this.response));
	}

	@Test
	public void attemptAuthenticationWhenRegistrationIdDoesNotExistThenThrowsException() {
		given(this.repository.findByRegistrationId("non-existent-id")).willReturn(null);
		this.filter = new Saml2WebSsoAuthenticationFilter(this.repository, "/some/other/path/{registrationId}");
		this.request.setPathInfo("/some/other/path/non-existent-id");
		this.request.setParameter("SAMLResponse", "response");
		assertThatExceptionOfType(Saml2AuthenticationException.class)
				.isThrownBy(() -> this.filter.attemptAuthentication(this.request, this.response))
				.withMessage("No relying party registration found");
	}

	@Test
	public void attemptAuthenticationWhenSavedAuthnRequestThenRemovesAuthnRequest() {
		Saml2AuthenticationRequestRepository<AbstractSaml2AuthenticationRequest> authenticationRequestRepository = mock(
				Saml2AuthenticationRequestRepository.class);
		AuthenticationConverter authenticationConverter = mock(AuthenticationConverter.class);
		given(authenticationConverter.convert(this.request)).willReturn(TestSaml2AuthenticationTokens.token());
		this.filter = new Saml2WebSsoAuthenticationFilter(authenticationConverter, "/some/other/path/{registrationId}");
		this.filter.setAuthenticationManager((authentication) -> null);
		this.request.setPathInfo("/some/other/path/idp-registration-id");
		this.filter.setAuthenticationRequestRepository(authenticationRequestRepository);
		this.filter.attemptAuthentication(this.request, this.response);
		verify(authenticationRequestRepository).removeAuthenticationRequest(this.request, this.response);
	}

	@Test
	public void setAuthenticationRequestRepositoryWhenNullThenThrowsIllegalArgument() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.filter.setAuthenticationRequestRepository(null))
				.withMessage("authenticationRequestRepository cannot be null");
	}

	@Test
	public void setAuthenticationRequestRepositoryWhenExpectedAuthenticationConverterTypeThenSetLoaderIntoConverter() {
		Saml2AuthenticationTokenConverter authenticationConverterMock = mock(Saml2AuthenticationTokenConverter.class);
		Saml2AuthenticationRequestRepository<AbstractSaml2AuthenticationRequest> authenticationRequestRepository = mock(
				Saml2AuthenticationRequestRepository.class);
		this.filter = new Saml2WebSsoAuthenticationFilter(authenticationConverterMock,
				"/some/other/path/{registrationId}");
		this.filter.setAuthenticationRequestRepository(authenticationRequestRepository);
		verify(authenticationConverterMock).setAuthenticationRequestRepository(authenticationRequestRepository);
	}

	@Test
	public void setAuthenticationRequestRepositoryWhenNotExpectedAuthenticationConverterTypeThenDontSet() {
		AuthenticationConverter authenticationConverter = mock(AuthenticationConverter.class);
		Saml2AuthenticationRequestRepository<AbstractSaml2AuthenticationRequest> authenticationRequestRepository = mock(
				Saml2AuthenticationRequestRepository.class);
		this.filter = new Saml2WebSsoAuthenticationFilter(authenticationConverter, "/some/other/path/{registrationId}");
		this.filter.setAuthenticationRequestRepository(authenticationRequestRepository);
		verifyNoInteractions(authenticationConverter);
	}

}
