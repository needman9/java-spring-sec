/*
 *
 *  * Copyright 2002-2017 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.security.config.annotation.web.reactive;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.web.server.HttpSecurity;
import org.springframework.security.core.userdetails.MapUserDetailsRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsRepository;
import org.springframework.security.test.web.reactive.server.WebTestClientBuilder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainFilter;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.Mockito.mock;

/**
 * @author Rob Winch
 * @since 5.0
 */
@RunWith(Enclosed.class)
public class EnableWebFluxSecurityTests {
	@RunWith(SpringRunner.class)
	public static class Defaults {
		@Autowired
		WebFilterChainFilter springSecurityFilterChain;

		@Test
		public void defaultRequiresAuthentication() {
			WebTestClient client = WebTestClientBuilder.bindToWebFilters(springSecurityFilterChain).build();

			client.get()
				.uri("/")
				.exchange()
				.expectStatus().isUnauthorized()
				.expectBody().isEmpty();
		}

		@EnableWebFluxSecurity
		static class Config {
			@Bean
			public UserDetailsRepository userDetailsRepository() {
				return new MapUserDetailsRepository(User.withUsername("user")
					.password("password")
					.roles("USER")
					.build()
				);
			}
		}
	}

	@RunWith(SpringRunner.class)
	public static class MultiHttpSecurity {
		@Autowired
		WebFilterChainFilter springSecurityFilterChain;

		@Test
		public void multiWorks() {
			WebTestClient client = WebTestClientBuilder.bindToWebFilters(springSecurityFilterChain).build();

			client.get()
				.uri("/api/test")
				.exchange()
				.expectStatus().isUnauthorized()
				.expectBody().isEmpty();

			client.get()
				.uri("/test")
				.exchange()
				.expectStatus().isOk();
		}

		@EnableWebFluxSecurity
		static class Config {
			@Order(Ordered.HIGHEST_PRECEDENCE)
			@Bean
			public SecurityWebFilterChain apiHttpSecurity(HttpSecurity http) {
				http
					.securityMatcher(new PathPatternParserServerWebExchangeMatcher("/api/**"))
					.authorizeExchange()
						.anyExchange().denyAll();
				return http.build();
			}

			@Bean
			public SecurityWebFilterChain httpSecurity(HttpSecurity http) {
				return http.build();
			}

			@Bean
			public UserDetailsRepository userDetailsRepository() {
				return new MapUserDetailsRepository(User.withUsername("user")
					.password("password")
					.roles("USER")
					.build()
				);
			}
		}
	}
}
