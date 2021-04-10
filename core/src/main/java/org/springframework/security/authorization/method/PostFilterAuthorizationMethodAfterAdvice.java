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

import java.lang.reflect.Method;
import java.util.function.Supplier;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.lang.NonNull;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;

/**
 * An {@link AuthorizationMethodAfterAdvice} which filters a <code>returnedObject</code>
 * from the {@link MethodInvocation} by evaluating an expression from the
 * {@link PostFilter} annotation.
 *
 * @author Evgeniy Cheban
 * @author Josh Cummings
 * @since 5.5
 */
public final class PostFilterAuthorizationMethodAfterAdvice
		implements AuthorizationMethodAfterAdvice<MethodAuthorizationContext> {

	private final PostFilterExpressionAttributeRegistry registry = new PostFilterExpressionAttributeRegistry();

	private final Pointcut pointcut;

	private MethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();

	/**
	 * Create a {@link PostFilterAuthorizationMethodAfterAdvice} using the provided
	 * parameters
	 * @param pointcut the {@link Pointcut} for when this advice applies
	 */
	public PostFilterAuthorizationMethodAfterAdvice(Pointcut pointcut) {
		this.pointcut = pointcut;
	}

	/**
	 * Sets the {@link MethodSecurityExpressionHandler}.
	 * @param expressionHandler the {@link MethodSecurityExpressionHandler} to use
	 */
	public void setExpressionHandler(MethodSecurityExpressionHandler expressionHandler) {
		Assert.notNull(expressionHandler, "expressionHandler cannot be null");
		this.expressionHandler = expressionHandler;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

	/**
	 * Filters a <code>returnedObject</code> from the {@link MethodInvocation} by
	 * evaluating an expression from the {@link PostFilter} annotation.
	 * @param authentication the {@link Supplier} of the {@link Authentication} to check
	 * @param methodAuthorizationContext the {@link MethodAuthorizationContext} to check
	 * check
	 * @return filtered <code>returnedObject</code> from the {@link MethodInvocation}
	 */
	@Override
	public Object after(Supplier<Authentication> authentication, MethodAuthorizationContext methodAuthorizationContext,
			Object returnedObject) {
		if (returnedObject == null) {
			return null;
		}
		ExpressionAttribute attribute = this.registry.getAttribute(methodAuthorizationContext);
		if (attribute == ExpressionAttribute.NULL_ATTRIBUTE) {
			return returnedObject;
		}
		EvaluationContext ctx = this.expressionHandler.createEvaluationContext(authentication.get(),
				methodAuthorizationContext.getMethodInvocation());
		return this.expressionHandler.filter(returnedObject, attribute.getExpression(), ctx);
	}

	private final class PostFilterExpressionAttributeRegistry
			extends AbstractExpressionAttributeRegistry<ExpressionAttribute> {

		@NonNull
		@Override
		ExpressionAttribute resolveAttribute(Method method, Class<?> targetClass) {
			Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
			PostFilter postFilter = findPostFilterAnnotation(specificMethod);
			if (postFilter == null) {
				return ExpressionAttribute.NULL_ATTRIBUTE;
			}
			Expression postFilterExpression = PostFilterAuthorizationMethodAfterAdvice.this.expressionHandler
					.getExpressionParser().parseExpression(postFilter.value());
			return new ExpressionAttribute(postFilterExpression);
		}

		private PostFilter findPostFilterAnnotation(Method method) {
			PostFilter postFilter = AnnotationUtils.findAnnotation(method, PostFilter.class);
			return (postFilter != null) ? postFilter
					: AnnotationUtils.findAnnotation(method.getDeclaringClass(), PostFilter.class);
		}

	}

}
