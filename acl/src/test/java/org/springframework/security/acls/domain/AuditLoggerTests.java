/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.security.acls.domain;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.AuditableAccessControlEntry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link ConsoleAuditLogger}.
 *
 * @author Andrei Stefan
 */
public class AuditLoggerTests {

	private PrintStream console;

	private ByteArrayOutputStream bytes = new ByteArrayOutputStream();

	private ConsoleAuditLogger logger;

	private AuditableAccessControlEntry ace;

	@Before
	public void setUp() {
		logger = new ConsoleAuditLogger();
		ace = mock(AuditableAccessControlEntry.class);
		console = System.out;
		System.setOut(new PrintStream(bytes));
	}

	@After
	public void tearDown() {
		System.setOut(console);
		bytes.reset();
	}

	@Test
	public void nonAuditableAceIsIgnored() {
		AccessControlEntry ace = mock(AccessControlEntry.class);
		logger.logIfNeeded(true, ace);
		assertThat(bytes.size()).isZero();
	}

	@Test
	public void successIsNotLoggedIfAceDoesntRequireSuccessAudit() {
		when(ace.isAuditSuccess()).thenReturn(false);
		logger.logIfNeeded(true, ace);
		assertThat(bytes.size()).isZero();
	}

	@Test
	public void successIsLoggedIfAceRequiresSuccessAudit() {
		when(ace.isAuditSuccess()).thenReturn(true);

		logger.logIfNeeded(true, ace);
		assertThat(bytes.toString()).startsWith("GRANTED due to ACE");
	}

	@Test
	public void failureIsntLoggedIfAceDoesntRequireFailureAudit() {
		when(ace.isAuditFailure()).thenReturn(false);
		logger.logIfNeeded(false, ace);
		assertThat(bytes.size()).isZero();
	}

	@Test
	public void failureIsLoggedIfAceRequiresFailureAudit() {
		when(ace.isAuditFailure()).thenReturn(true);
		logger.logIfNeeded(false, ace);
		assertThat(bytes.toString()).startsWith("DENIED due to ACE");
	}

}
