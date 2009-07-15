/* Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
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

package org.springframework.security.authentication;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.concurrent.ConcurrentLoginException;
import org.springframework.security.authentication.concurrent.ConcurrentSessionController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.util.Assert;


/**
 * Iterates an {@link Authentication} request through a list of {@link AuthenticationProvider}s.
 *
 * Can optionally be configured with a {@link ConcurrentSessionController} to limit the number of sessions a user can
 * have.
 * <p>
 * <tt>AuthenticationProvider</tt>s are usually tried in order until one provides a non-null response.
 * A non-null response indicates the provider had authority to decide on the authentication request and no further
 * providers are tried.
 * If a subsequent provider successfully authenticates the request, the earlier authentication exception is disregarded
 * and the successful authentication will be used. If no subsequent provider provides a non-null response, or a new
 * <code>AuthenticationException</code>, the last <code>AuthenticationException</code> received will be used.
 * If no provider returns a non-null response, or indicates it can even process an <code>Authentication</code>,
 * the <code>ProviderManager</code> will throw a <code>ProviderNotFoundException</code>.
 * <p>
 * The exception to this process is when a provider throws an {@link AccountStatusException} or if the configured
 * concurrent session controller throws a {@link ConcurrentLoginException}. In both these cases, no further providers
 * in the list will be queried.
 *
 * <h2>Event Publishing</h2>
 * <p>
 * Authentication event publishing is delegated to the configured {@link AuthenticationEventPublisher} which defaults
 * to a null implementation which doesn't publish events, so if you are configuring the bean yourself you must inject
 * a publisher bean if you want to receive events. The standard implementation is {@link DefaultAuthenticationEventPublisher}
 * which maps common exceptions to events (in the case of authentication failure) and publishes an
 * {@link org.springframework.security.authentication.event.AuthenticationSuccessEvent AuthenticationSuccessEvent} if
 * authentication succeeds. If you are using the namespace then an instance of this bean will be used automatically by
 * the <tt>&lt;http&gt;</tt> configuration, so you will receive events from the web part of your application automatically.
 * <p>
 * Note that the implementation also publishes authentication failure events when it obtains an authentication result
 * (or an exception) from the "parent" <tt>AuthenticationManager</tt> if one has been set. So in this situation, the
 * parent should not generally be configured to publish events or there will be duplicates.
 *
 * @author Ben Alex
 * @author Luke Taylor
 * @version $Id$
 * @see ConcurrentSessionController
 * @see DefaultAuthenticationEventPublisher
 */
public class ProviderManager extends AbstractAuthenticationManager implements MessageSourceAware {
    //~ Static fields/initializers =====================================================================================

    private static final Log logger = LogFactory.getLog(ProviderManager.class);

    //~ Instance fields ================================================================================================

    private AuthenticationEventPublisher eventPublisher = new NullEventPublisher();
    private ConcurrentSessionController sessionController = new NullConcurrentSessionController();
    private List<AuthenticationProvider> providers;
    protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
    private AuthenticationManager parent;

    //~ Methods ========================================================================================================

    /**
     * Attempts to authenticate the passed {@link Authentication} object.
     * <p>
     * The list of {@link AuthenticationProvider}s will be successively tried until an
     * <code>AuthenticationProvider</code> indicates it is  capable of authenticating the type of
     * <code>Authentication</code> object passed. Authentication will then be attempted with that
     * <code>AuthenticationProvider</code>.
     * <p>
     * If more than one <code>AuthenticationProvider</code> supports the passed <code>Authentication</code>
     * object, only the first <code>AuthenticationProvider</code> tried will determine the result. No subsequent
     * <code>AuthenticationProvider</code>s will be tried.
     *
     * @param authentication the authentication request object.
     *
     * @return a fully authenticated object including credentials.
     *
     * @throws AuthenticationException if authentication fails.
     */
    public Authentication doAuthentication(Authentication authentication) throws AuthenticationException {
        Class<? extends Authentication> toTest = authentication.getClass();
        AuthenticationException lastException = null;
        Authentication result = null;

        for (AuthenticationProvider provider : getProviders()) {
            if (!provider.supports(toTest)) {
                continue;
            }

            logger.debug("Authentication attempt using " + provider.getClass().getName());

            try {
                result = provider.authenticate(authentication);

                if (result != null) {
                    copyDetails(authentication, result);
                    break;
                }
            } catch (AccountStatusException e) {
                // SEC-546: Avoid polling additional providers if auth failure is due to invalid account status
                eventPublisher.publishAuthenticationFailure(e, authentication);
                throw e;
            } catch (AuthenticationException e) {
                lastException = e;
            }
        }

        if (result == null && parent != null) {
            // Allow the parent to try.
            try {
                result = parent.authenticate(authentication);
            } catch (ProviderNotFoundException e) {
                // ignore as we will throw below if no other exception occurred prior to calling parent and the parent
                // may throw ProviderNotFound even though a provider in the child already handled the request
            } catch (AuthenticationException e) {
                lastException = e;
            }
        }

        // Finally check if the concurrent session controller will allow authentication
        try {
            if (result != null) {
                sessionController.checkAuthenticationAllowed(result);
                sessionController.registerSuccessfulAuthentication(result);
                eventPublisher.publishAuthenticationSuccess(result);

                return result;
            }
        } catch (AuthenticationException e) {
            lastException = e;
        }

        // Session control failed, parent was null, or didn't authenticate (or throw an exception).

        if (lastException == null) {
            lastException = new ProviderNotFoundException(messages.getMessage("ProviderManager.providerNotFound",
                        new Object[] {toTest.getName()}, "No AuthenticationProvider found for {0}"));
        }

        eventPublisher.publishAuthenticationFailure(lastException, authentication);

        throw lastException;
    }

    /**
     * Copies the authentication details from a source Authentication object to a destination one, provided the
     * latter does not already have one set.
     *
     * @param source source authentication
     * @param dest the destination authentication object
     */
    private void copyDetails(Authentication source, Authentication dest) {
        if ((dest instanceof AbstractAuthenticationToken) && (dest.getDetails() == null)) {
            AbstractAuthenticationToken token = (AbstractAuthenticationToken) dest;

            token.setDetails(source.getDetails());
        }
    }

    public List<AuthenticationProvider> getProviders() {
        if (providers == null || providers.size() == 0) {
            throw new IllegalArgumentException("A list of AuthenticationProviders is required");
        }

        return providers;
    }

    /**
     * The configured {@link ConcurrentSessionController} is returned or the {@link
     * NullConcurrentSessionController} if a specific one has not been set.
     *
     * @return {@link ConcurrentSessionController} instance
     */
    public ConcurrentSessionController getSessionController() {
        return sessionController;
    }

    public void setMessageSource(MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }

    public void setParent(AuthenticationManager parent) {
        this.parent = parent;
    }

    public void setAuthenticationEventPublisher(AuthenticationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Sets the {@link AuthenticationProvider} objects to be used for authentication.
     *
     * @param providers the list of authentication providers which will be used to process authentication requests.
     *
     * @throws IllegalArgumentException if the list is empty or null, or any of the elements in the list is not an
     * AuthenticationProvider instance.
     */
    @SuppressWarnings("unchecked")
    public void setProviders(List providers) {
        Assert.notEmpty(providers, "A list of AuthenticationProviders is required");

        for(Object currentObject : providers) {
            Assert.isInstanceOf(AuthenticationProvider.class, currentObject, "Can only provide AuthenticationProvider instances");
        }

        this.providers = providers;
    }

    /**
     * Set the {@link ConcurrentSessionController} to be used for limiting users' sessions.
     *
     * @param sessionController {@link ConcurrentSessionController}
     */
    public void setSessionController(ConcurrentSessionController sessionController) {
        this.sessionController = sessionController;
    }

    private static final class NullEventPublisher implements AuthenticationEventPublisher {
        public void publishAuthenticationFailure(AuthenticationException exception, Authentication authentication) {}
        public void publishAuthenticationSuccess(Authentication authentication) {}
    }

    private static final class NullConcurrentSessionController implements ConcurrentSessionController {
        public void checkAuthenticationAllowed(Authentication request) {}
        public void registerSuccessfulAuthentication(Authentication authentication) {}
    }
}
