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

package org.springframework.security.oauth2.server.resource.introspection;

import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * A Spring implementation of {@link OpaqueTokenIntrospector} that verifies and
 * introspects a token using the configured
 * <a href="https://tools.ietf.org/html/rfc7662" target="_blank">OAuth 2.0 Introspection
 * Endpoint</a>.
 *
 * @author Josh Cummings
 * @since 5.6
 */
public class SpringOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

	private final Log logger = LogFactory.getLog(getClass());

	private static final ParameterizedTypeReference<Map<String, Object>> STRING_OBJECT_MAP = new ParameterizedTypeReference<Map<String, Object>>() {
	};

	private Converter<String, RequestEntity<?>> requestEntityConverter;

	private RestOperations restOperations;

	private final String authorityPrefix = "SCOPE_";

	/**
	 * Creates a {@code OpaqueTokenAuthenticationProvider} with the provided parameters
	 * @param introspectionUri The introspection endpoint uri
	 * @param clientId The client id authorized to introspect
	 * @param clientSecret The client's secret
	 */
	public SpringOpaqueTokenIntrospector(String introspectionUri, String clientId, String clientSecret) {
		Assert.notNull(introspectionUri, "introspectionUri cannot be null");
		Assert.notNull(clientId, "clientId cannot be null");
		Assert.notNull(clientSecret, "clientSecret cannot be null");
		this.requestEntityConverter = this.defaultRequestEntityConverter(URI.create(introspectionUri));
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(clientId, clientSecret));
		this.restOperations = restTemplate;
	}

	/**
	 * Creates a {@code OpaqueTokenAuthenticationProvider} with the provided parameters
	 *
	 * The given {@link RestOperations} should perform its own client authentication
	 * against the introspection endpoint.
	 * @param introspectionUri The introspection endpoint uri
	 * @param restOperations The client for performing the introspection request
	 */
	public SpringOpaqueTokenIntrospector(String introspectionUri, RestOperations restOperations) {
		Assert.notNull(introspectionUri, "introspectionUri cannot be null");
		Assert.notNull(restOperations, "restOperations cannot be null");
		this.requestEntityConverter = this.defaultRequestEntityConverter(URI.create(introspectionUri));
		this.restOperations = restOperations;
	}

	private Converter<String, RequestEntity<?>> defaultRequestEntityConverter(URI introspectionUri) {
		return (token) -> {
			HttpHeaders headers = requestHeaders();
			MultiValueMap<String, String> body = requestBody(token);
			return new RequestEntity<>(body, headers, HttpMethod.POST, introspectionUri);
		};
	}

	private HttpHeaders requestHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		return headers;
	}

	private MultiValueMap<String, String> requestBody(String token) {
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("token", token);
		return body;
	}

	@Override
	public OAuth2AuthenticatedPrincipal introspect(String token) {
		RequestEntity<?> requestEntity = this.requestEntityConverter.convert(token);
		if (requestEntity == null) {
			throw new OAuth2IntrospectionException("requestEntityConverter returned a null entity");
		}
		ResponseEntity<Map<String, Object>> responseEntity = makeRequest(requestEntity);
		Map<String, Object> claims = adaptToNimbusResponse(responseEntity);
		return convertClaimsSet(claims);
	}

	/**
	 * Sets the {@link Converter} used for converting the OAuth 2.0 access token to a
	 * {@link RequestEntity} representation of the OAuth 2.0 token introspection request.
	 * @param requestEntityConverter the {@link Converter} used for converting to a
	 * {@link RequestEntity} representation of the token introspection request
	 */
	public void setRequestEntityConverter(Converter<String, RequestEntity<?>> requestEntityConverter) {
		Assert.notNull(requestEntityConverter, "requestEntityConverter cannot be null");
		this.requestEntityConverter = requestEntityConverter;
	}

	private ResponseEntity<Map<String, Object>> makeRequest(RequestEntity<?> requestEntity) {
		try {
			return this.restOperations.exchange(requestEntity, STRING_OBJECT_MAP);
		}
		catch (Exception ex) {
			throw new OAuth2IntrospectionException(ex.getMessage(), ex);
		}
	}

	private Map<String, Object> adaptToNimbusResponse(ResponseEntity<Map<String, Object>> responseEntity) {
		if (responseEntity.getStatusCode() != HttpStatus.OK) {
			throw new OAuth2IntrospectionException(
					"Introspection endpoint responded with " + responseEntity.getStatusCode());
		}
		Map<String, Object> claims = responseEntity.getBody();
		// relying solely on the authorization server to validate this token (not checking
		// 'exp', for example)
		boolean active = (boolean) claims.compute(OAuth2IntrospectionClaimNames.ACTIVE, (k, v) -> {
			if (v instanceof String) {
				return Boolean.parseBoolean((String) v);
			}
			if (v instanceof Boolean) {
				return v;
			}
			return false;
		});
		if (!active) {
			this.logger.trace("Did not validate token since it is inactive");
			throw new BadOpaqueTokenException("Provided token isn't active");
		}
		return claims;
	}

	private OAuth2AuthenticatedPrincipal convertClaimsSet(Map<String, Object> claims) {
		claims.computeIfPresent(OAuth2IntrospectionClaimNames.AUDIENCE, (k, v) -> {
			if (v instanceof String) {
				return Collections.singletonList(v);
			}
			return v;
		});
		claims.computeIfPresent(OAuth2IntrospectionClaimNames.CLIENT_ID, (k, v) -> v.toString());
		claims.computeIfPresent(OAuth2IntrospectionClaimNames.EXPIRES_AT,
				(k, v) -> Instant.ofEpochSecond(((Number) v).longValue()));
		claims.computeIfPresent(OAuth2IntrospectionClaimNames.ISSUED_AT,
				(k, v) -> Instant.ofEpochSecond(((Number) v).longValue()));
		claims.computeIfPresent(OAuth2IntrospectionClaimNames.ISSUER, (k, v) -> issuer(v.toString()));
		claims.computeIfPresent(OAuth2IntrospectionClaimNames.NOT_BEFORE,
				(k, v) -> Instant.ofEpochSecond(((Number) v).longValue()));
		Collection<GrantedAuthority> authorities = new ArrayList<>();
		claims.computeIfPresent(OAuth2IntrospectionClaimNames.SCOPE, (k, v) -> {
			if (v instanceof String) {
				Collection<String> scopes = Arrays.asList(((String) v).split(" "));
				for (String scope : scopes) {
					authorities.add(new SimpleGrantedAuthority(this.authorityPrefix + scope));
				}
				return scopes;
			}
			return v;
		});
		return new OAuth2IntrospectionAuthenticatedPrincipal(claims, authorities);
	}

	private URL issuer(String uri) {
		try {
			return new URL(uri);
		}
		catch (Exception ex) {
			throw new OAuth2IntrospectionException(
					"Invalid " + OAuth2IntrospectionClaimNames.ISSUER + " value: " + uri);
		}
	}

}
