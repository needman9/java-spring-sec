/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.security.oauth2.core;

import org.springframework.util.Assert;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An &quot;accessor&quot; for a set of claims that may be used for assertions.
 *
 * @author Joe Grandja
 * @since 5.0
 */
public interface ClaimAccessor {

	/**
	 * Returns a set of claims that may be used for assertions.
	 *
	 * @return a {@code Map} of claims
	 */
	Map<String, Object> getClaims();

	/**
	 * Returns {@code true} if the claim exists in {@link #getClaims()}, otherwise {@code false}.
	 *
	 * @param claim the name of the claim
	 * @return {@code true} if the claim exists, otherwise {@code false}
	 */
	default Boolean containsClaim(String claim) {
		Assert.notNull(claim, "claim cannot be null");
		return this.getClaims().containsKey(claim);
	}

	/**
	 * Returns the claim value as a {@code String} or {@code null} if it does not exist.
	 *
	 * @param claim the name of the claim
	 * @return the claim value or {@code null} if it does not exist
	 */
	default String getClaimAsString(String claim) {
		return (this.containsClaim(claim) ? this.getClaims().get(claim).toString() : null);
	}

	/**
	 * Returns the claim value as a {@code Boolean} or {@code null} if it does not exist.
	 *
	 * @param claim the name of the claim
	 * @return the claim value or {@code null} if it does not exist
	 */
	default Boolean getClaimAsBoolean(String claim) {
		return (this.containsClaim(claim) ? Boolean.valueOf(this.getClaimAsString(claim)) : null);
	}

	/**
	 * Returns the claim value as an {@code Instant} or {@code null} if it does not exist.
	 *
	 * @param claim the name of the claim
	 * @return the claim value or {@code null} if it does not exist
	 */
	default Instant getClaimAsInstant(String claim) {
		if (!this.containsClaim(claim)) {
			return null;
		}
		try {
			return Instant.ofEpochMilli(Long.valueOf(this.getClaimAsString(claim)));
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Unable to convert claim '" + claim + "' to Instant: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Returns the claim value as an {@code URL} or {@code null} if it does not exist.
	 *
	 * @param claim the name of the claim
	 * @return the claim value or {@code null} if it does not exist
	 */
	default URL getClaimAsURL(String claim) {
		if (!this.containsClaim(claim)) {
			return null;
		}
		try {
			return new URL(this.getClaimAsString(claim));
		} catch (MalformedURLException ex) {
			throw new IllegalArgumentException("Unable to convert claim '" + claim + "' to URL: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Returns the claim value as a {@code Map<String, Object>}
	 * or {@code null} if it does not exist or cannot be assigned to a {@code Map}.
	 *
	 * @param claim the name of the claim
	 * @return the claim value or {@code null} if it does not exist or cannot be assigned to a {@code Map}
	 */
	default Map<String, Object> getClaimAsMap(String claim) {
		if (!this.containsClaim(claim) || !Map.class.isAssignableFrom(this.getClaims().get(claim).getClass())) {
			return null;
		}
		Map<String, Object> claimValues = new HashMap<>();
		((Map<?, ?>) this.getClaims().get(claim)).forEach((k, v) -> claimValues.put(k.toString(), v));
		return claimValues;
	}

	/**
	 * Returns the claim value as a {@code List<String>}
	 * or {@code null} if it does not exist or cannot be assigned to a {@code List}.
	 *
	 * @param claim the name of the claim
	 * @return the claim value or {@code null} if it does not exist or cannot be assigned to a {@code List}
	 */
	default List<String> getClaimAsStringList(String claim) {
		if (!this.containsClaim(claim) || !List.class.isAssignableFrom(this.getClaims().get(claim).getClass())) {
			return null;
		}
		List<String> claimValues = new ArrayList<>();
		((List<?>) this.getClaims().get(claim)).forEach(e -> claimValues.add(e.toString()));
		return claimValues;
	}
}
