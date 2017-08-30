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

package org.springframework.security.test.context.support;

/**
 * @author Rob Winch
 * @since 5.0
 */

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.OrderComparator;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.TestContext;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ReactorContextTestExecutionListenerTests {

	@Mock
	private TestContext testContext;

	private ReactorContextTestExecutionListener listener =
		new ReactorContextTestExecutionListener();

	@After
	public void cleanup() {
		TestSecurityContextHolder.clearContext();
		Hooks.resetOnLastOperator();
	}

	@Test
	public void beforeTestMethodWhenSecurityContextEmptyThenReactorContextNull() throws Exception {
		listener.beforeTestMethod(testContext);

		assertThat(Mono.currentContext().block().isEmpty()).isTrue();
	}

	@Test
	public void beforeTestMethodWhenNullAuthenticationThenReactorContextNull() throws Exception {
		TestSecurityContextHolder.setContext(new SecurityContextImpl());

		listener.beforeTestMethod(testContext);

		assertThat(Mono.currentContext().block().isEmpty()).isTrue();
	}


	@Test
	public void beforeTestMethodWhenAuthenticationThenReactorContextHasAuthentication() throws Exception {
		TestingAuthenticationToken expectedAuthentication = new TestingAuthenticationToken("user", "password", "ROLE_USER");
		SecurityContextImpl context = new SecurityContextImpl();
		context.setAuthentication(expectedAuthentication);
		TestSecurityContextHolder.setContext(context);

		listener.beforeTestMethod(testContext);

		assertAuthentication(expectedAuthentication);
	}

	@Test
	public void afterTestMethodWhenSecurityContextEmptyThenNoError() throws Exception {
		listener.beforeTestMethod(testContext);

		listener.afterTestMethod(testContext);
	}

	@Test
	public void afterTestMethodWhenSetupThenReactorContextNull() throws Exception {
		beforeTestMethodWhenAuthenticationThenReactorContextHasAuthentication();

		listener.afterTestMethod(testContext);

		assertThat(Mono.currentContext().block().isEmpty()).isTrue();
	}

	@Test
	public void orderWhenComparedToWithSecurityContextTestExecutionListenerIsAfter() {
		OrderComparator comparator = new OrderComparator();
		WithSecurityContextTestExecutionListener withSecurity = new WithSecurityContextTestExecutionListener();
		ReactorContextTestExecutionListener reactorContext = new ReactorContextTestExecutionListener();
		assertThat(comparator.compare(withSecurity, reactorContext)).isLessThan(0);
	}

	public void assertAuthentication(Authentication expected) {
		Mono<Authentication> authentication = Mono.currentContext()
			.flatMap( context -> context.<Mono<Authentication>>get(Authentication.class));

		StepVerifier.create(authentication)
			.expectNext(expected)
			.verifyComplete();
	}
}
