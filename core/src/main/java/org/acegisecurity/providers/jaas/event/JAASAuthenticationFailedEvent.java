package net.sf.acegisecurity.providers.jaas.event;

import net.sf.acegisecurity.Authentication;

/**
 * Fired when LoginContext.login throws a LoginException, or if any other exception is thrown during that time.
 * <br>
 *
 * @author Ray Krueger
 * @version $Id$
 */
public class JAASAuthenticationFailedEvent extends JAASAuthenticationEvent {

    private Exception exception;

    public JAASAuthenticationFailedEvent(Authentication auth, Exception exception) {
        super(auth);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

}
