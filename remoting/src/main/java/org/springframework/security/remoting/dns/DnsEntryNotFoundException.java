/*
 * Copyright 2009-2016 the original author or authors.
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

package org.springframework.security.remoting.dns;

/**
 * This will be thrown if no entry matches the specified DNS query.
 *
 * @author Mike Wiesner
 * @since 3.0
 * @deprecated as of 5.6.0 with no replacement
 */
@Deprecated
public class DnsEntryNotFoundException extends DnsLookupException {

	private static final long serialVersionUID = -947232730426775162L;

	public DnsEntryNotFoundException(String msg) {
		super(msg);
	}

	public DnsEntryNotFoundException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
