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

package org.springframework.security.authorization.method;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import org.springframework.aop.Pointcut;
import org.springframework.security.authorization.AuthorizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link AuthorizationManagerAfterMethodInterceptor}.
 *
 * @author Evgeniy Cheban
 */
public class AuthorizationManagerAfterMethodInterceptorTests {

	@Test
	public void instantiateWhenMethodMatcherNullThenException() {
		AuthorizationManager<MethodInvocationResult> mockAuthorizationManager = mock(AuthorizationManager.class);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new AuthorizationManagerAfterMethodInterceptor(null, mockAuthorizationManager))
				.withMessage("pointcut cannot be null");
	}

	@Test
	public void instantiateWhenAuthorizationManagerNullThenException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new AuthorizationManagerAfterMethodInterceptor(mock(Pointcut.class), null))
				.withMessage("authorizationManager cannot be null");
	}

	@Test
	public void beforeWhenMockAuthorizationManagerThenCheckAndReturnedObject() throws Throwable {
		MethodInvocation mockMethodInvocation = mock(MethodInvocation.class);
		MethodInvocationResult result = new MethodInvocationResult(mockMethodInvocation, new Object());
		given(mockMethodInvocation.proceed()).willReturn(result.getResult());
		AuthorizationManager<MethodInvocationResult> mockAuthorizationManager = mock(AuthorizationManager.class);
		AuthorizationManagerAfterMethodInterceptor advice = new AuthorizationManagerAfterMethodInterceptor(
				Pointcut.TRUE, mockAuthorizationManager);
		Object returnedObject = advice.invoke(mockMethodInvocation);
		assertThat(returnedObject).isEqualTo(result.getResult());
		verify(mockAuthorizationManager).check(eq(AuthorizationManagerAfterMethodInterceptor.AUTHENTICATION_SUPPLIER),
				any(MethodInvocationResult.class));
	}

}
