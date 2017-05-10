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

package org.springframework.security.authentication;

import org.springframework.security.core.Authentication;

import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

/**
 * @author Rob Winch
 * @since 5.0
 */
public class UserDetailsRepositoryAuthenticationManager implements ReactiveAuthenticationManager {
	private final UserDetailsRepository repository;

	public UserDetailsRepositoryAuthenticationManager(UserDetailsRepository userDetailsRepository) {
		Assert.notNull(userDetailsRepository, "userDetailsRepository cannot be null");
		this.repository = userDetailsRepository;
	}

	@Override
	public Mono<Authentication> authenticate(Authentication authentication) {
		final String username = authentication.getName();
		return repository
				.findByUsername(username)
				.filter( u -> u.getPassword().equals(authentication.getCredentials()))
				.switchIfEmpty(  Mono.error(new BadCredentialsException("Invalid Credentials")) )
				.map( u -> new UsernamePasswordAuthenticationToken(u, u.getPassword(), u.getAuthorities()) );
	}
}
