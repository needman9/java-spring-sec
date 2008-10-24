/*
 * Copyright 2004-2008 the original author or authors.
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
package org.springframework.expression.spel.ast;

import org.antlr.runtime.Token;

/**
 * Expression language AST node that represents an integer literal.
 * 
 * @author Andy Clement
 */
public class IntLiteral extends Literal {

	private final Integer value;

	IntLiteral(Token payload, int value) {
		super(payload);
		this.value = value;
	}

	@Override
	public Integer getLiteralValue() {
		return value;
	}

}
