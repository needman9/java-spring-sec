/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.security.samples.cas.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

/**
 * The CAS login page.
 *
 * @author Rob Winch
 * @author Josh Cummings
 */
public class LoginPage extends Page<LoginPage> {
	private final Content content;

	public LoginPage(WebDriver driver, String baseUrl) {
		super(driver, baseUrl + "/login");
		this.content = PageFactory.initElements(driver, Content.class);
	}

	public void login(String user) {
		login(user, user);
	}

	public void login(String user, String password) {
		this.content.username(user).password(password).submit();
	}

	public static class Content {
		private WebElement username;
		private WebElement password;
		@FindBy(css = "input[type=submit]")
		private WebElement submit;

		public Content username(String username) {
			this.username.sendKeys(username);
			return this;
		}

		public Content password(String password) {
			this.password.sendKeys(password);
			return this;
		}

		public void submit() {
			this.submit.click();
		}
	}
}
