package org.springframework.security.web.session;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.authentication.concurrent.SessionRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.savedrequest.SavedRequest;

/**
 * The default implementation of {@link AuthenticatedSessionStrategy}.
 * <p>
 * Creates a new session for the newly authenticated user if they already have a session, and copies their
 * session attributes across to the new session (can be disabled by setting <tt>migrateSessionAttributes</tt> to
 * <tt>false</tt>).
 * <p>
 * If concurrent session control is in use, then a <tt>SessionRegistry</tt> must be injected.
 *
 * @author Luke Taylor
 * @version $Id$
 * @since 3.0
 */
public class DefaultAuthenticatedSessionStrategy implements AuthenticatedSessionStrategy{
    protected final Log logger = LogFactory.getLog(this.getClass());

    private SessionRegistry sessionRegistry;

    /**
     * Indicates that the session attributes of an existing session
     * should be migrated to the new session. Defaults to <code>true</code>.
     */
    private boolean migrateSessionAttributes = true;

    /**
     * In the case where the attributes will not be migrated, this field allows a list of named attributes
     * which should <em>not</em> be discarded.
     */
    private List<String> retainedAttributes = Arrays.asList(SavedRequest.SPRING_SECURITY_SAVED_REQUEST_KEY);

    /**
     * If set to <tt>true</tt>, a session will always be created, even if one didn't exist at the start of the request.
     * Defaults to <tt>false</tt>.
     */
    private boolean alwaysCreateSession;

    /**
     * Called when a user is newly authenticated.
     * <p>
     * If a session already exists, a new session will be created, the session attributes copied to it (if
     * <tt>migrateSessionAttributes</tt> is set) and the sessionRegistry updated with the new session information.
     * <p>
     * If there is no session, no action is taken unless the <tt>alwaysCreateSession</tt> property is set, in which
     * case a session will be created if one doesn't already exist.
     */
    public void onAuthenticationSuccess(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) == null) {
            // Session fixation isn't a problem if there's no session
            if (alwaysCreateSession) {
                request.getSession();
            }

            return;
        }

        // Create new session
        HttpSession session = request.getSession();

        String originalSessionId = session.getId();

        if (logger.isDebugEnabled()) {
            logger.debug("Invalidating session with Id '" + originalSessionId +"' " + (migrateSessionAttributes ?
                    "and" : "without") +  " migrating attributes.");
        }

        HashMap<String, Object> attributesToMigrate = createMigratedAttributeMap(session);

        session.invalidate();
        session = request.getSession(true); // we now have a new session

        if (logger.isDebugEnabled()) {
            logger.debug("Started new session: " + session.getId());
        }

        // Copy attributes to new session
        if (attributesToMigrate != null) {
            for (Map.Entry<String, Object> entry : attributesToMigrate.entrySet()) {
                session.setAttribute(entry.getKey(), entry.getValue());
            }
        }

        // Update the session registry
        if (sessionRegistry != null) {
            sessionRegistry.removeSessionInformation(originalSessionId);
            sessionRegistry.registerNewSession(session.getId(),
                    SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        }
    }

    @SuppressWarnings("unchecked")
    private HashMap<String, Object> createMigratedAttributeMap(HttpSession session) {
        HashMap<String, Object> attributesToMigrate = null;

        if (migrateSessionAttributes) {
            attributesToMigrate = new HashMap<String, Object>();

            Enumeration enumer = session.getAttributeNames();

            while (enumer.hasMoreElements()) {
                String key = (String) enumer.nextElement();
                attributesToMigrate.put(key, session.getAttribute(key));
            }
        } else {
            // Only retain the attributes which have been specified in the retainAttributes list
            if (!retainedAttributes.isEmpty()) {
                attributesToMigrate = new HashMap<String, Object>();
                for (String name : retainedAttributes) {
                    Object value = session.getAttribute(name);

                    if (value != null) {
                        attributesToMigrate.put(name, value);
                    }
                }
            }
        }
        return attributesToMigrate;
    }

    public void setMigrateSessionAttributes(boolean migrateSessionAttributes) {
        this.migrateSessionAttributes = migrateSessionAttributes;
    }

    /**
     * Sets the session registry which should be updated when the authenticated session is changed.
     * This must be set if you are using concurrent session control.
     *
     * @param sessionRegistry
     */
    public void setSessionRegistry(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public void setRetainedAttributes(List<String> retainedAttributes) {
        this.retainedAttributes = retainedAttributes;
    }
}
