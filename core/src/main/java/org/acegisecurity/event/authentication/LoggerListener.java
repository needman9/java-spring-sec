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

package org.acegisecurity.event.authentication;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import org.springframework.util.ClassUtils;


/**
 * Outputs authentication-related application events to Commons Logging.
 * 
 * <P>
 * All authentication failures are logged at the warning level, whilst
 * authentication successes are logged at the information level.
 * </p>
 *
 * @author Ben Alex
 * @version $Id$
 */
public class LoggerListener implements ApplicationListener {
    //~ Static fields/initializers =============================================

    private static final Log logger = LogFactory.getLog(LoggerListener.class);

    //~ Methods ================================================================

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof AbstractAuthenticationEvent) {
            AbstractAuthenticationEvent authEvent = (AbstractAuthenticationEvent) event;

            if (logger.isWarnEnabled()) {
                String message = "Authentication event "
                    + ClassUtils.getShortName(authEvent.getClass()) + ": "
                    + authEvent.getAuthentication().getName() + "; details: "
                    + authEvent.getAuthentication().getDetails();

                if (event instanceof AbstractAuthenticationFailureEvent) {
                    message = message + "; exception: "
                        + ((AbstractAuthenticationFailureEvent) event).getException()
                           .getMessage();
                }

                logger.warn(message);
            }
        }
    }
}
