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
package org.springframework.security.oauth2.client.web.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;

/**
 * An implementation of a {@link HandlerMethodArgumentResolver} that is capable
 * of resolving a method parameter to an argument value of type {@link OAuth2AuthorizedClient}.
 *
 * <p>
 * For example:
 * <pre>
 * &#64;Controller
 * public class MyController {
 *     &#64;GetMapping("/authorized-client")
 *     public String authorizedClient(@RegisteredOAuth2AuthorizedClient("login-client") OAuth2AuthorizedClient authorizedClient) {
 *         // do something with authorizedClient
 *     }
 * }
 * </pre>
 *
 * @author Joe Grandja
 * @since 5.1
 * @see RegisteredOAuth2AuthorizedClient
 */
public final class OAuth2AuthorizedClientArgumentResolver implements HandlerMethodArgumentResolver {
	private final OAuth2AuthorizedClientRepository authorizedClientRepository;

	/**
	 * Constructs an {@code OAuth2AuthorizedClientArgumentResolver} using the provided parameters.
	 *
	 * @param authorizedClientRepository the authorized client repository
	 */
	public OAuth2AuthorizedClientArgumentResolver(OAuth2AuthorizedClientRepository authorizedClientRepository) {
		Assert.notNull(authorizedClientRepository, "authorizedClientRepository cannot be null");
		this.authorizedClientRepository = authorizedClientRepository;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> parameterType = parameter.getParameterType();
		return (OAuth2AuthorizedClient.class.isAssignableFrom(parameterType) &&
				(AnnotatedElementUtils.findMergedAnnotation(
						parameter.getParameter(), RegisteredOAuth2AuthorizedClient.class) != null));
	}

	@NonNull
	@Override
	public Object resolveArgument(MethodParameter parameter,
									@Nullable ModelAndViewContainer mavContainer,
									NativeWebRequest webRequest,
									@Nullable WebDataBinderFactory binderFactory) throws Exception {

		RegisteredOAuth2AuthorizedClient authorizedClientAnnotation = AnnotatedElementUtils.findMergedAnnotation(
				parameter.getParameter(), RegisteredOAuth2AuthorizedClient.class);
		Authentication principal = SecurityContextHolder.getContext().getAuthentication();

		String clientRegistrationId = null;
		if (!StringUtils.isEmpty(authorizedClientAnnotation.registrationId())) {
			clientRegistrationId = authorizedClientAnnotation.registrationId();
		} else if (!StringUtils.isEmpty(authorizedClientAnnotation.value())) {
			clientRegistrationId = authorizedClientAnnotation.value();
		} else if (principal != null && OAuth2AuthenticationToken.class.isAssignableFrom(principal.getClass())) {
			clientRegistrationId = ((OAuth2AuthenticationToken) principal).getAuthorizedClientRegistrationId();
		}
		if (StringUtils.isEmpty(clientRegistrationId)) {
			throw new IllegalArgumentException("Unable to resolve the Client Registration Identifier. " +
					"It must be provided via @RegisteredOAuth2AuthorizedClient(\"client1\") or @RegisteredOAuth2AuthorizedClient(registrationId = \"client1\").");
		}

		OAuth2AuthorizedClient authorizedClient = this.authorizedClientRepository.loadAuthorizedClient(
			clientRegistrationId, principal, webRequest.getNativeRequest(HttpServletRequest.class));
		if (authorizedClient == null) {
			throw new ClientAuthorizationRequiredException(clientRegistrationId);
		}

		return authorizedClient;
	}
}
