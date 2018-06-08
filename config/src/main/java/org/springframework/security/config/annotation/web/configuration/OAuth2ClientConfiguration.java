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
package org.springframework.security.config.annotation.web.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.web.method.annotation.OAuth2AuthorizedClientArgumentResolver;
import org.springframework.util.ClassUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * {@link Configuration} for OAuth 2.0 Client support.
 *
 * <p>
 * This {@code Configuration} is conditionally imported by {@link OAuth2ImportSelector}
 * when the {@code spring-security-oauth2-client} module is present on the classpath.
 *
 * @author Joe Grandja
 * @since 5.1
 * @see OAuth2ImportSelector
 */
@Import(OAuth2ClientConfiguration.OAuth2ClientWebMvcImportSelector.class)
final class OAuth2ClientConfiguration {

	static class OAuth2ClientWebMvcImportSelector implements ImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			boolean webmvcPresent = ClassUtils.isPresent(
				"org.springframework.web.servlet.DispatcherServlet", getClass().getClassLoader());

			return webmvcPresent ?
				new String[] { "org.springframework.security.config.annotation.web.configuration.OAuth2ClientConfiguration.OAuth2ClientWebMvcSecurityConfiguration" } :
				new String[] {};
		}
	}

	@Configuration
	static class OAuth2ClientWebMvcSecurityConfiguration implements WebMvcConfigurer {
		@Autowired(required = false)
		private OAuth2AuthorizedClientService authorizedClientService;

		@Override
		public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
			if (this.authorizedClientService != null) {
				OAuth2AuthorizedClientArgumentResolver authorizedClientArgumentResolver =
						new OAuth2AuthorizedClientArgumentResolver(this.authorizedClientService);
				argumentResolvers.add(authorizedClientArgumentResolver);
			}
		}
	}
}
