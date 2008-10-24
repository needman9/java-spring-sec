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
package org.springframework.expression.spel.internal;

import org.antlr.runtime.Token;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.ast.SpelNode;

public class EmptySpelNode extends SpelNode {

	public EmptySpelNode(Token payload) {
		super(payload);
	}

	@Override
	public Object getValue(ExpressionState state) throws SpelException {
		throw new RuntimeException("?");
	}

	@Override
	public String toStringAST() {
		return "<no string form node '" + getTokenName() + "'>";
	}

}
