/* Copyright 2004, 2005 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sf.acegisecurity.ui.session;

import junit.framework.TestCase;

import net.sf.acegisecurity.MockHttpSession;

import org.springframework.mock.web.MockServletContext;

import org.springframework.web.context.support.StaticWebApplicationContext;

import javax.servlet.ServletContextEvent;
import javax.servlet.http.HttpSessionEvent;


/**
 * The HttpSessionEventPublisher tests
 *
 * @author Ray Krueger
 */
public class HttpSessionEventPublisherTests extends TestCase {
    //~ Methods ================================================================

    /**
     * It's not that complicated so we'll just run it straight through here.
     * @throws Exception
     */
    public void testPublisher() throws Exception {
        HttpSessionEventPublisher publisher = new HttpSessionEventPublisher();

        StaticWebApplicationContext context = new StaticWebApplicationContext();

        MockServletContext servletContext = new MockServletContext();
        servletContext.setAttribute(StaticWebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
            context);

        context.setServletContext(servletContext);
        context.registerSingleton("listener", TestListener.class, null);
        context.refresh();

        publisher.contextInitialized(new ServletContextEvent(servletContext));

        MockHttpSession session = new MockHttpSession();
        TestListener listener = (TestListener) context.getBean("listener");

        HttpSessionEvent event = new HttpSessionEvent(session);
        publisher.sessionCreated(event);

        assertNotNull(listener.getCreatedEvent());
        assertNull(listener.getDestroyedEvent());
        assertEquals(session, listener.getCreatedEvent().getSession());

        listener.setCreatedEvent(null);
        listener.setDestroyedEvent(null);

        publisher.sessionDestroyed(event);
        assertNotNull(listener.getDestroyedEvent());
        assertNull(listener.getCreatedEvent());
        assertEquals(session, listener.getDestroyedEvent().getSession());

        publisher.contextDestroyed(new ServletContextEvent(servletContext));
    }
}
