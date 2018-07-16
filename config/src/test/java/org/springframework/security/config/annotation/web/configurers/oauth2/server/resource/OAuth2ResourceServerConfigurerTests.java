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

package org.springframework.security.config.annotation.web.configurers.oauth2.server.resource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.StringContains;
import org.hamcrest.core.StringEndsWith;
import org.hamcrest.core.StringStartsWith;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.test.SpringTestRule;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Tests for {@link OAuth2ResourceServerConfigurer}
 *
 * @author Josh Cummings
 */
public class OAuth2ResourceServerConfigurerTests {

	@Autowired
	MockMvc mvc;

	@Autowired(required = false)
	MockWebServer authz;

	@Rule
	public final SpringTestRule spring = new SpringTestRule();

	@Test
	public void getWhenUsingDefaultsWithValidBearerTokenThenAcceptsRequest()
			throws Exception {

		this.spring.register(WebServerConfig.class, DefaultConfig.class, BasicController.class).autowire();
		this.authz.enqueue(this.jwks("Default"));
		String token = this.token("ValidNoScopes");

		this.mvc.perform(get("/").with(bearerToken(token)))
				.andExpect(status().isOk())
				.andExpect(content().string("ok"));
	}

	@Test
	public void getWhenUsingDefaultsWithExpiredBearerTokenThenInvalidToken()
			throws Exception {

		this.spring.register(WebServerConfig.class, DefaultConfig.class, BasicController.class).autowire();
		this.authz.enqueue(this.jwks("Default"));
		String token = this.token("Expired");

		this.mvc.perform(get("/").with(bearerToken(token)))
				.andExpect(status().isUnauthorized())
				.andExpect(invalidTokenHeader("An error occurred while attempting to decode the Jwt"));
	}

	@Test
	public void getWhenUsingDefaultsWithBadJwkEndpointThenInvalidToken()
		throws Exception {

		this.spring.register(WebServerConfig.class, DefaultConfig.class).autowire();
		this.authz.enqueue(new MockResponse().setBody("malformed"));
		String token = this.token("ValidNoScopes");

		this.mvc.perform(get("/").with(bearerToken(token)))
				.andExpect(status().isUnauthorized())
				.andExpect(invalidTokenHeader("An error occurred while attempting to decode the Jwt: Malformed Jwk set"));
	}

	@Test
	public void getWhenUsingDefaultsWithUnavailableJwkEndpointThenInvalidToken()
			throws Exception {

		this.spring.register(WebServerConfig.class, DefaultConfig.class).autowire();
		this.authz.shutdown();
		String token = this.token("ValidNoScopes");

		this.mvc.perform(get("/").with(bearerToken(token)))
				.andExpect(status().isUnauthorized())
				.andExpect(invalidTokenHeader("An error occurred while attempting to decode the Jwt"));
	}

	@Test
	public void getWhenUsingDefaultsWithMalformedBearerTokenThenInvalidToken()
			throws Exception {

		this.spring.register(DefaultConfig.class).autowire();

		this.mvc.perform(get("/").with(bearerToken("an\"invalid\"token")))
				.andExpect(status().isUnauthorized())
				.andExpect(invalidTokenHeader("Bearer token is malformed"));
	}

	@Test
	public void getWhenUsingDefaultsWithMalformedPayloadThenInvalidToken()
			throws Exception {

		this.spring.register(WebServerConfig.class, DefaultConfig.class).autowire();
		this.authz.enqueue(this.jwks("Default"));
		String token = this.token("MalformedPayload");

		this.mvc.perform(get("/").with(bearerToken(token)))
				.andExpect(status().isUnauthorized())
				.andExpect(invalidTokenHeader("An error occurred while attempting to decode the Jwt: Malformed payload"));
	}

	@Test
	public void getWhenUsingDefaultsWithUnsignedBearerTokenThenInvalidToken()
			throws Exception {

		this.spring.register(DefaultConfig.class).autowire();
		String token = this.token("Unsigned");

		this.mvc.perform(get("/").with(bearerToken(token)))
				.andExpect(status().isUnauthorized())
				.andExpect(invalidTokenHeader("Unsupported algorithm of none"));
	}

	@Test
	public void getWhenUsingDefaultsWithBearerTokenBeforeNotBeforeThenInvalidToken()
			throws Exception {

		this.spring.register(WebServerConfig.class, DefaultConfig.class).autowire();
		this.authz.enqueue(this.jwks("Default"));
		String token = this.token("TooEarly");

		this.mvc.perform(get("/").with(bearerToken(token)))
				.andExpect(status().isUnauthorized())
				.andExpect(invalidTokenHeader("An error occurred while attempting to decode the Jwt"));
	}

	@Test
	public void getWhenUsingDefaultsWithBearerTokenInTwoPlacesThenInvalidRequest()
			throws Exception {

		this.spring.register(DefaultConfig.class).autowire();

		this.mvc.perform(get("/")
						.with(bearerToken("token"))
						.with(bearerToken("token").asParam()))
				.andExpect(status().isBadRequest())
				.andExpect(invalidRequestHeader("Found multiple bearer tokens in the request"));
	}

	@Test
	public void getWhenUsingDefaultsWithBearerTokenInTwoParametersThenInvalidRequest()
			throws Exception {

		this.spring.register(DefaultConfig.class).autowire();

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("access_token", "token1");
		params.add("access_token", "token2");

		this.mvc.perform(get("/")
				.params(params))
				.andExpect(status().isBadRequest())
				.andExpect(invalidRequestHeader("Found multiple bearer tokens in the request"));
	}

	@Test
	public void postWhenUsingDefaultsWithBearerTokenAsFormParameterThenIgnoresToken()
			throws Exception {

		this.spring.register(DefaultConfig.class).autowire();

		this.mvc.perform(post("/") // engage csrf
				.with(bearerToken("token").asParam()))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
	}

	@Test
	public void postWhenCsrfDisabledWithBearerTokenAsFormParameterThenIgnoresToken()
			throws Exception {

		this.spring.register(CsrfDisabledConfig.class).autowire();

		this.mvc.perform(post("/")
				.with(bearerToken("token").asParam()))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
	}

	@Test
	public void getWhenUsingDefaultsWithNoBearerTokenThenUnauthorized()
			throws Exception {

		this.spring.register(DefaultConfig.class).autowire();

		this.mvc.perform(get("/"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
	}

	@Test
	public void getWhenUsingDefaultsWithSufficientlyScopedBearerTokenThenAcceptsRequest()
			throws Exception {

		this.spring.register(WebServerConfig.class, DefaultConfig.class, BasicController.class).autowire();
		this.authz.enqueue(this.jwks("Default"));
		String token = this.token("ValidMessageReadScope");

		this.mvc.perform(get("/requires-read-scope")
				.with(bearerToken(token)))
				.andExpect(status().isOk())
				.andExpect(content().string("SCOPE_message:read"));
	}

	@Test
	public void getWhenUsingDefaultsWithInsufficientScopeThenInsufficientScopeError()
			throws Exception {

		this.spring.register(WebServerConfig.class, DefaultConfig.class, BasicController.class).autowire();
		this.authz.enqueue(this.jwks("Default"));
		String token = this.token("ValidNoScopes");

		this.mvc.perform(get("/requires-read-scope")
				.with(bearerToken(token)))
				.andExpect(status().isForbidden())
				.andExpect(insufficientScopeHeader(""));
	}

	@Test
	public void getWhenUsingDefaultsWithInsufficientScpThenInsufficientScopeError()
			throws Exception {

		this.spring.register(WebServerConfig.class, DefaultConfig.class, BasicController.class).autowire();
		this.authz.enqueue(this.jwks("Default"));
		String token = this.token("ValidMessageWriteScp");

		this.mvc.perform(get("/requires-read-scope")
				.with(bearerToken(token)))
				.andExpect(status().isForbidden())
				.andExpect(insufficientScopeHeader("message:write"));
	}

	@Test
	public void getWhenUsingDefaultsAndAuthorizationServerHasNoMatchingKeyThenInvalidToken()
			throws Exception {

		this.spring.register(WebServerConfig.class, DefaultConfig.class).autowire();
		this.authz.enqueue(this.jwks("Empty"));
		String token = this.token("ValidNoScopes");

		this.mvc.perform(get("/")
				.with(bearerToken(token)))
				.andExpect(status().isUnauthorized())
				.andExpect(invalidTokenHeader("An error occurred while attempting to decode the Jwt"));
	}

	@Test
	public void getWhenUsingDefaultsAndAuthorizationServerHasMultipleMatchingKeysThenOk()
			throws Exception {

		this.spring.register(WebServerConfig.class, DefaultConfig.class, BasicController.class).autowire();
		this.authz.enqueue(this.jwks("TwoKeys"));
		String token = this.token("ValidNoScopes");

		this.mvc.perform(get("/authenticated")
				.with(bearerToken(token)))
				.andExpect(status().isOk())
				.andExpect(content().string("test-subject"));
	}

	@Test
	public void getWhenUsingDefaultsAndKeyMatchesByKidThenOk()
			throws Exception {

		this.spring.register(WebServerConfig.class, DefaultConfig.class, BasicController.class).autowire();
		this.authz.enqueue(this.jwks("TwoKeys"));
		String token = this.token("Kid");

		this.mvc.perform(get("/authenticated")
				.with(bearerToken(token)))
				.andExpect(status().isOk())
				.andExpect(content().string("test-subject"));
	}

	// -- Method Security

	@Test
	public void getWhenUsingMethodSecurityWithValidBearerTokenThenAcceptsRequest()
			throws Exception {

		this.spring.register(WebServerConfig.class, MethodSecurityConfig.class, BasicController.class).autowire();
		this.authz.enqueue(this.jwks("Default"));
		String token = this.token("ValidMessageReadScope");

		this.mvc.perform(get("/ms-requires-read-scope")
				.with(bearerToken(token)))
				.andExpect(status().isOk())
				.andExpect(content().string("SCOPE_message:read"));
	}

	@Test
	public void getWhenUsingMethodSecurityWithValidBearerTokenHavingScpAttributeThenAcceptsRequest()
			throws Exception {

		this.spring.register(WebServerConfig.class, MethodSecurityConfig.class, BasicController.class).autowire();
		this.authz.enqueue(this.jwks("Default"));
		String token = this.token("ValidMessageReadScp");

		this.mvc.perform(get("/ms-requires-read-scope")
				.with(bearerToken(token)))
				.andExpect(status().isOk())
				.andExpect(content().string("SCOPE_message:read"));
	}

	@Test
	public void getWhenUsingMethodSecurityWithInsufficientScopeThenInsufficientScopeError()
			throws Exception {

		this.spring.register(WebServerConfig.class, MethodSecurityConfig.class, BasicController.class).autowire();
		this.authz.enqueue(this.jwks("Default"));
		String token = this.token("ValidNoScopes");

		this.mvc.perform(get("/ms-requires-read-scope")
				.with(bearerToken(token)))
				.andExpect(status().isForbidden())
				.andExpect(insufficientScopeHeader(""));

	}

	@Test
	public void getWhenUsingMethodSecurityWithInsufficientScpThenInsufficientScopeError()
			throws Exception {

		this.spring.register(WebServerConfig.class, MethodSecurityConfig.class, BasicController.class).autowire();
		this.authz.enqueue(this.jwks("Default"));
		String token = this.token("ValidMessageWriteScp");

		this.mvc.perform(get("/ms-requires-read-scope")
				.with(bearerToken(token)))
				.andExpect(status().isForbidden())
				.andExpect(insufficientScopeHeader("message:write"));
	}

	@Test
	public void getWhenUsingMethodSecurityWithDenyAllThenInsufficientScopeError()
			throws Exception {

		this.spring.register(WebServerConfig.class, MethodSecurityConfig.class, BasicController.class).autowire();
		this.authz.enqueue(this.jwks("Default"));
		String token = this.token("ValidMessageReadScope");

		this.mvc.perform(get("/ms-deny")
				.with(bearerToken(token)))
				.andExpect(status().isForbidden())
				.andExpect(insufficientScopeHeader("message:read"));
	}

	// -- Resource Server should not engage csrf

	@Test
	public void postWhenUsingDefaultsWithValidBearerTokenAndNoCsrfTokenThenOk()
			throws Exception {

		this.spring.register(WebServerConfig.class, DefaultConfig.class, BasicController.class).autowire();
		this.authz.enqueue(this.jwks("Default"));
		String token = this.token("ValidNoScopes");

		this.mvc.perform(post("/authenticated")
				.with(bearerToken(token)))
				.andExpect(status().isOk())
				.andExpect(content().string("test-subject"));
	}

	@Test
	public void postWhenUsingDefaultsWithNoBearerTokenThenCsrfDenies()
			throws Exception {

		this.spring.register(DefaultConfig.class).autowire();

		this.mvc.perform(post("/authenticated"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
	}

	@Test
	public void postWhenUsingDefaultsWithExpiredBearerTokenAndNoCsrfThenInvalidToken()
			throws Exception {

		this.spring.register(WebServerConfig.class, DefaultConfig.class).autowire();
		this.authz.enqueue(this.jwks("Default"));
		String token = this.token("Expired");

		this.mvc.perform(post("/authenticated")
				.with(bearerToken(token)))
				.andExpect(status().isUnauthorized())
				.andExpect(invalidTokenHeader("An error occurred while attempting to decode the Jwt"));
	}

	// -- Resource Server should not create sessions

	@Test
	public void requestWhenDefaultConfiguredThenSessionIsNotCreated()
			throws Exception {

		this.spring.register(WebServerConfig.class, DefaultConfig.class, BasicController.class).autowire();
		this.authz.enqueue(this.jwks("Default"));
		String token = this.token("ValidNoScopes");

		MvcResult result = this.mvc.perform(get("/")
				.with(bearerToken(token)))
				.andExpect(status().isOk())
				.andReturn();

		assertThat(result.getRequest().getSession(false)).isNull();
	}

	@Test
	public void requestWhenUsingDefaultsAndNoBearerTokenThenSessionIsNotCreated()
			throws Exception {

		this.spring.register(DefaultConfig.class, BasicController.class).autowire();

		MvcResult result = this.mvc.perform(get("/"))
				.andExpect(status().isUnauthorized())
				.andReturn();

		assertThat(result.getRequest().getSession(false)).isNull();
	}

	@Test
	public void requestWhenSessionManagementConfiguredThenUserConfigurationOverrides()
			throws Exception {

		this.spring.register(WebServerConfig.class, AlwaysSessionCreationConfig.class, BasicController.class).autowire();
		this.authz.enqueue(this.jwks("Default"));
		String token = this.token("ValidNoScopes");

		MvcResult result = this.mvc.perform(get("/")
				.with(bearerToken(token)))
				.andExpect(status().isOk())
				.andReturn();

		assertThat(result.getRequest().getSession(false)).isNotNull();
	}

	// -- In combination with other authentication providers

	@Test
	public void getWhenAlsoUsingHttpBasicThenCorrectProviderEngages()
			throws Exception {

		this.spring.register(WebServerConfig.class, BasicAndResourceServerConfig.class, BasicController.class).autowire();
		this.authz.enqueue(this.jwks("Default"));
		String token = this.token("ValidNoScopes");

		this.mvc.perform(get("/authenticated")
				.with(bearerToken(token)))
				.andExpect(status().isOk())
				.andExpect(content().string("test-subject"));

		this.mvc.perform(get("/authenticated")
				.with(httpBasic("basic-user", "basic-password")))
				.andExpect(status().isOk())
				.andExpect(content().string("basic-user"));
	}

	// -- Incorrect Configuration

	@Test
	public void configuredWhenMissingJwtAuthenticationProviderThenWiringException() {

		assertThatCode(() -> this.spring.register(JwtlessConfig.class).autowire())
				.isInstanceOf(BeanCreationException.class)
				.hasMessageContaining("no instance of JwtDecoder");
	}

	@Test
	public void configureWhenMissingJwkSetUriThenWiringException() {

		assertThatCode(() -> this.spring.register(JwtHalfConfiguredConfig.class).autowire())
				.isInstanceOf(BeanCreationException.class)
				.hasMessageContaining("no instance of JwtDecoder");
	}

	// -- support

	@EnableWebSecurity
	static class DefaultConfig extends WebSecurityConfigurerAdapter {
		@Value("${mock.jwk-set-uri:https://example.org}") String uri;

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			// @formatter:off
			http
				.authorizeRequests()
					.antMatchers("/requires-read-scope").access("hasAuthority('SCOPE_message:read')")
					.anyRequest().authenticated()
					.and()
				.oauth2()
					.resourceServer()
						.jwt()
							.jwkSetUri(this.uri);
			// @formatter:on
		}
	}

	@EnableWebSecurity
	static class CsrfDisabledConfig extends WebSecurityConfigurerAdapter {
		@Value("${mock.jwk-set-uri:https://example.org}") String uri;

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			// @formatter:off
			http
				.authorizeRequests()
					.antMatchers("/requires-read-scope").access("hasAuthority('SCOPE_message:read')")
					.anyRequest().authenticated()
					.and()
				.csrf().disable()
				.oauth2()
					.resourceServer()
						.jwt()
							.jwkSetUri(this.uri);
			// @formatter:on
		}
	}

	@EnableWebSecurity
	@EnableGlobalMethodSecurity(prePostEnabled = true)
	static class MethodSecurityConfig extends WebSecurityConfigurerAdapter {
		@Value("${mock.jwk-set-uri:https://example.org}") String uri;

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			// @formatter:off
			http
				.authorizeRequests()
					.anyRequest().authenticated()
					.and()
				.oauth2()
					.resourceServer()
						.jwt()
							.jwkSetUri(this.uri);
			// @formatter:on
		}
	}

	@EnableWebSecurity
	static class JwtlessConfig extends WebSecurityConfigurerAdapter {
		@Override
		protected void configure(HttpSecurity http) throws Exception {
			// @formatter:off
			http
				.authorizeRequests()
					.anyRequest().authenticated()
					.and()
				.oauth2()
					.resourceServer();
			// @formatter:on
		}
	}

	@EnableWebSecurity
	static class BasicAndResourceServerConfig extends WebSecurityConfigurerAdapter {
		@Value("${mock.jwk-set-uri:https://example.org}") String uri;

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			// @formatter:off
			http
				.authorizeRequests()
					.anyRequest().authenticated()
					.and()
				.httpBasic()
					.and()
				.oauth2()
					.resourceServer()
						.jwt()
							.jwkSetUri(this.uri);
			// @formatter:on
		}

		@Bean
		public UserDetailsService userDetailsService() {
			return new InMemoryUserDetailsManager(
					org.springframework.security.core.userdetails.User.withDefaultPasswordEncoder()
							.username("basic-user")
							.password("basic-password")
							.roles("USER")
							.build());
		}
	}

	@EnableWebSecurity
	static class JwtHalfConfiguredConfig extends WebSecurityConfigurerAdapter {
		@Override
		protected void configure(HttpSecurity http) throws Exception {
			// @formatter:off
			http
				.authorizeRequests()
					.anyRequest().authenticated()
					.and()
				.oauth2()
					.resourceServer()
						.jwt(); // missing key configuration, e.g. jwkSetUri
			// @formatter:on
		}
	}

	@EnableWebSecurity
	static class AlwaysSessionCreationConfig extends WebSecurityConfigurerAdapter {
		@Value("${mock.jwk-set-uri}") String uri;

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			// @formatter:off
			http
				.sessionManagement()
					.sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
					.and()
				.oauth2()
					.resourceServer()
						.jwt()
							.jwkSetUri(this.uri);
			// @formatter:on
		}
	}

	@RestController
	static class BasicController {
		@GetMapping("/")
		public String get() {
			return "ok";
		}

		@PostMapping("/post")
		public String post() {
			return "post";
		}

		@RequestMapping(value = "/authenticated", method = { GET, POST })
		public String authenticated(@AuthenticationPrincipal Authentication authentication) {
			return authentication.getName();
		}

		@GetMapping("/requires-read-scope")
		public String requiresReadScope(@AuthenticationPrincipal JwtAuthenticationToken token) {
			return token.getAuthorities().stream()
					.map(GrantedAuthority::getAuthority)
					.filter(auth -> auth.endsWith("message:read"))
					.findFirst().orElse(null);
		}

		@GetMapping("/ms-requires-read-scope")
		@PreAuthorize("hasAuthority('SCOPE_message:read')")
		public String msRequiresReadScope(@AuthenticationPrincipal JwtAuthenticationToken token) {
			return requiresReadScope(token);
		}

		@GetMapping("/ms-deny")
		@PreAuthorize("denyAll")
		public String deny() {
			return "hmm, that's odd";
		}
	}

	@Configuration
	static class WebServerConfig implements BeanPostProcessor {
		private final MockWebServer server = new MockWebServer();

		@PreDestroy
		public void shutdown() throws IOException {
			this.server.shutdown();
		}

		@Bean
		public MockWebServer authz() {
			return this.server;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof WebSecurityConfigurerAdapter) {
				Field f = ReflectionUtils.findField(bean.getClass(), field ->
						field.getAnnotation(Value.class) != null);
				if (f != null) {
					ReflectionUtils.setField(f, bean, this.server.url("/.well-known/jwks.json").toString());
				}
			}
			return null;
		}
	}

	private static class BearerTokenRequestPostProcessor implements RequestPostProcessor {
		private boolean asRequestParameter;

		private String token;

		public BearerTokenRequestPostProcessor(String token) {
			this.token = token;
		}

		public BearerTokenRequestPostProcessor asParam() {
			this.asRequestParameter = true;
			return this;
		}

		@Override
		public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
			if (this.asRequestParameter) {
				request.setParameter("access_token", this.token);
			} else {
				request.addHeader("Authorization", "Bearer " + this.token);
			}

			return request;
		}
	}

	private static BearerTokenRequestPostProcessor bearerToken(String token) {
		return new BearerTokenRequestPostProcessor(token);
	}

	private static ResultMatcher invalidRequestHeader(String message) {
		return header().string(HttpHeaders.WWW_AUTHENTICATE,
				AllOf.allOf(
						new StringStartsWith("Bearer " +
								"error=\"invalid_request\", " +
								"error_description=\""),
						new StringContains(message),
						new StringEndsWith(", " +
								"error_uri=\"https://tools.ietf.org/html/rfc6750#section-3.1\"")
						)
				);
	}

	private static ResultMatcher invalidTokenHeader(String message) {
		return header().string(HttpHeaders.WWW_AUTHENTICATE,
				AllOf.allOf(
						new StringStartsWith("Bearer " +
								"error=\"invalid_token\", " +
								"error_description=\""),
						new StringContains(message),
						new StringEndsWith(", " +
								"error_uri=\"https://tools.ietf.org/html/rfc6750#section-3.1\"")
				)
		);
	}

	private static ResultMatcher insufficientScopeHeader(String scope) {
		return header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer " +
				"error=\"insufficient_scope\"" +
				", error_description=\"The token provided has insufficient scope [" + scope + "] for this request\"" +
				", error_uri=\"https://tools.ietf.org/html/rfc6750#section-3.1\"" +
				(StringUtils.hasText(scope) ? ", scope=\"" + scope + "\"" : ""));
	}

	private String token(String name) throws IOException {
		return resource(name + ".token");
	}

	private MockResponse jwks(String name) throws IOException {
		String response = resource(name + ".jwks");
		return new MockResponse()
				.setResponseCode(200)
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody(response);
	}

	private String resource(String suffix) throws IOException {
		String name = this.getClass().getSimpleName() + "-" + suffix;
		ClassPathResource resource = new ClassPathResource(name, this.getClass());
		try ( BufferedReader reader = new BufferedReader(new FileReader(resource.getFile())) ) {
			return reader.lines().collect(Collectors.joining());
		}
	}
}
