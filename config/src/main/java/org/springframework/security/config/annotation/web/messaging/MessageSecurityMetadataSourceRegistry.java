/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.security.config.annotation.web.messaging;

import org.springframework.messaging.Message;
import org.springframework.security.config.annotation.web.configurers.RememberMeConfigurer;
import org.springframework.security.messaging.access.expression.ExpressionBasedMessageSecurityMetadataSourceFactory;
import org.springframework.security.messaging.access.intercept.MessageSecurityMetadataSource;
import org.springframework.security.messaging.util.matcher.MessageMatcher;
import org.springframework.security.messaging.util.matcher.SimpDestinationMessageMatcher;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Allows mapping security constraints using {@link MessageMatcher} to the security expressions.
 *
 * @since 4.0
 * @author Rob Winch
 */
public class MessageSecurityMetadataSourceRegistry {
    private static final String permitAll = "permitAll";
    private static final String denyAll = "denyAll";
    private static final String anonymous = "anonymous";
    private static final String authenticated = "authenticated";
    private static final String fullyAuthenticated = "fullyAuthenticated";
    private static final String rememberMe = "rememberMe";

    private final LinkedHashMap<MatcherBuilder,String> matcherToExpression = new LinkedHashMap<MatcherBuilder,String>();

    private PathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Maps any {@link Message} to a security expression.
     *
     * @return the Expression to associate
     */
    public Constraint anyMessage() {
        return matchers(MessageMatcher.ANY_MESSAGE);
    }

    /**
     * Maps a {@link List} of {@link org.springframework.security.messaging.util.matcher.SimpDestinationMessageMatcher} instances.
     *
     * @param patterns the patterns to create {@link org.springframework.security.messaging.util.matcher.SimpDestinationMessageMatcher}
     *                    from. Uses {@link MessageSecurityMetadataSourceRegistry#pathMatcher(PathMatcher)}.
     *
     * @return the {@link Constraint}  that is associated to the {@link MessageMatcher}
     * @see {@link MessageSecurityMetadataSourceRegistry#pathMatcher(PathMatcher)}
     */
    public Constraint destinationMatchers(String... patterns) {
        List<MatcherBuilder> matchers = new ArrayList<MatcherBuilder>(patterns.length);
        for(String pattern : patterns) {
            matchers.add(new PathMatcherMessageMatcherBuilder(pattern));
        }
        return new Constraint(matchers);
    }

    /**
     * The {@link PathMatcher} to be used with the {@link MessageSecurityMetadataSourceRegistry#destinationMatchers(String...)}.
     * The default is to use the default constructor of {@link AntPathMatcher}.
     *
     * @param pathMatcher the {@link PathMatcher} to use. Cannot be null.
     * @return the {@link MessageSecurityMetadataSourceRegistry} for further customization.
     */
    public MessageSecurityMetadataSourceRegistry pathMatcher(PathMatcher pathMatcher) {
        Assert.notNull(pathMatcher, "pathMatcher cannot be null");
        this.pathMatcher = pathMatcher;
        return this;
    }

    /**
     * Maps a {@link List} of {@link MessageMatcher} instances to a security expression.
     *
     * @param matchers the {@link MessageMatcher} instances to map.
     * @return The {@link Constraint} that is associated to the {@link MessageMatcher} instances
     */
    public Constraint matchers(MessageMatcher<?>... matchers) {
        List<MatcherBuilder> builders = new ArrayList<MatcherBuilder>(matchers.length);
        for(MessageMatcher<?> matcher : matchers) {
            builders.add(new PreBuiltMatcherBuilder(matcher));
        }
        return new Constraint(builders);
    }

    /**
     * Allows subclasses to create creating a {@link MessageSecurityMetadataSource}.
     *
     * <p>This is not exposed so as not to confuse users of the API, which should never invoke this method.</p>
     *
     * @return the {@link MessageSecurityMetadataSource} to use
     */
    protected MessageSecurityMetadataSource createMetadataSource() {
        LinkedHashMap<MessageMatcher<?>,String> matcherToExpression = new LinkedHashMap<MessageMatcher<?>,String>();
        for(Map.Entry<MatcherBuilder,String> entry : this.matcherToExpression.entrySet()) {
            matcherToExpression.put(entry.getKey().build(), entry.getValue());
        }
        return ExpressionBasedMessageSecurityMetadataSourceFactory.createExpressionMessageMetadataSource(matcherToExpression);
    }

    /**
     * Represents the security constraint to be applied to the {@link MessageMatcher} instances.
     */
    public class Constraint {
        private final List<MatcherBuilder> messageMatchers;

        /**
         * Creates a new instance
         *
         * @param messageMatchers the {@link MessageMatcher} instances to map to this constraint
         */
        private Constraint(List<MatcherBuilder> messageMatchers) {
            Assert.notEmpty(messageMatchers, "messageMatchers cannot be null or empty");
            this.messageMatchers = messageMatchers;
        }

        /**
         * Shortcut for specifying {@link Message} instances require a particular role. If you do not want to have "ROLE_" automatically
         * inserted see {@link #hasAuthority(String)}.
         *
         * @param role the role to require (i.e. USER, ADMIN, etc). Note, it should not start with "ROLE_" as
         *             this is automatically inserted.
         * @return the {@link MessageSecurityMetadataSourceRegistry} for further customization
         */
        public MessageSecurityMetadataSourceRegistry hasRole(String role) {
            return access(MessageSecurityMetadataSourceRegistry.hasRole(role));
        }

        /**
         * Shortcut for specifying {@link Message} instances require any of a number of roles. If you
         * do not want to have "ROLE_" automatically inserted see
         * {@link #hasAnyAuthority(String...)}
         *
         * @param roles
         *            the roles to require (i.e. USER, ADMIN, etc). Note, it
         *            should not start with "ROLE_" as this is automatically
         *            inserted.
         * @return the {@link MessageSecurityMetadataSourceRegistry} for further
         *         customization
         */
        public MessageSecurityMetadataSourceRegistry hasAnyRole(String... roles) {
            return access(MessageSecurityMetadataSourceRegistry.hasAnyRole(roles));
        }

        /**
         * Specify that {@link Message} instances require a particular authority.
         *
         * @param authority the authority to require (i.e. ROLE_USER, ROLE_ADMIN, etc).
         * @return the {@link MessageSecurityMetadataSourceRegistry} for further customization
         */
        public MessageSecurityMetadataSourceRegistry hasAuthority(String authority) {
            return access(MessageSecurityMetadataSourceRegistry.hasAuthority(authority));
        }

        /**
         * Specify that {@link Message} instances requires any of a number authorities.
         *
         * @param authorities the requests require at least one of the authorities (i.e. "ROLE_USER","ROLE_ADMIN" would
         *                    mean either "ROLE_USER" or "ROLE_ADMIN" is required).
         * @return the {@link MessageSecurityMetadataSourceRegistry} for further customization
         */
        public MessageSecurityMetadataSourceRegistry hasAnyAuthority(String... authorities) {
            return access(MessageSecurityMetadataSourceRegistry.hasAnyAuthority(authorities));
        }

        /**
         * Specify that Messages are allowed by anyone.
         *
         * @return the {@link MessageSecurityMetadataSourceRegistry} for further customization
         */
        public MessageSecurityMetadataSourceRegistry permitAll() {
            return access(permitAll);
        }

        /**
         * Specify that Messages are allowed by anonymous users.
         *
         * @return the {@link MessageSecurityMetadataSourceRegistry} for further customization
         */
        public MessageSecurityMetadataSourceRegistry anonymous() {
            return access(anonymous);
        }

        /**
         * Specify that Messages are allowed by users that have been remembered.
         *
         * @return the {@link MessageSecurityMetadataSourceRegistry} for further customization
         * @see {@link RememberMeConfigurer}
         */
        public MessageSecurityMetadataSourceRegistry rememberMe() {
            return access(rememberMe);
        }

        /**
         * Specify that Messages are not allowed by anyone.
         *
         * @return the {@link MessageSecurityMetadataSourceRegistry} for further customization
         */
        public MessageSecurityMetadataSourceRegistry denyAll() {
            return access(denyAll);
        }

        /**
         * Specify that Messages are allowed by any authenticated user.
         *
         * @return the {@link MessageSecurityMetadataSourceRegistry} for further customization
         */
        public MessageSecurityMetadataSourceRegistry authenticated() {
            return access(authenticated);
        }

        /**
         * Specify that Messages are allowed by users who have authenticated and were not "remembered".
         *
         * @return the {@link MessageSecurityMetadataSourceRegistry} for further customization
         * @see {@link RememberMeConfigurer}
         */
        public MessageSecurityMetadataSourceRegistry fullyAuthenticated() {
            return access(fullyAuthenticated);
        }

        /**
         * Allows specifying that Messages are secured by an arbitrary expression
         *
         * @param attribute the expression to secure the URLs (i.e. "hasRole('ROLE_USER') and hasRole('ROLE_SUPER')")
         * @return the {@link MessageSecurityMetadataSourceRegistry} for further customization
         */
        public MessageSecurityMetadataSourceRegistry access(String attribute) {
            for(MatcherBuilder messageMatcher : messageMatchers) {
                matcherToExpression.put(messageMatcher, attribute);
            }
            return MessageSecurityMetadataSourceRegistry.this;
        }
    }

    private static String hasAnyRole(String... authorities) {
        String anyAuthorities = StringUtils.arrayToDelimitedString(authorities, "','ROLE_");
        return "hasAnyRole('ROLE_" + anyAuthorities + "')";
    }

    private static String hasRole(String role) {
        Assert.notNull(role, "role cannot be null");
        if (role.startsWith("ROLE_")) {
            throw new IllegalArgumentException("role should not start with 'ROLE_' since it is automatically inserted. Got '" + role + "'");
        }
        return "hasRole('ROLE_" + role + "')";
    }

    private static String hasAuthority(String authority) {
        return "hasAuthority('" + authority + "')";
    }

    private static String hasAnyAuthority(String... authorities) {
        String anyAuthorities = StringUtils.arrayToDelimitedString(authorities, "','");
        return "hasAnyAuthority('" + anyAuthorities + "')";
    }

    private class PreBuiltMatcherBuilder implements MatcherBuilder {
        private MessageMatcher<?> matcher;

        private PreBuiltMatcherBuilder(MessageMatcher<?> matcher) {
            this.matcher = matcher;
        }

        public MessageMatcher<?> build() {
            return matcher;
        }
    }

    private class PathMatcherMessageMatcherBuilder implements MatcherBuilder {
        private final String pattern;

        private PathMatcherMessageMatcherBuilder(String pattern) {
            this.pattern = pattern;
        }

        public MessageMatcher<?> build() {
            return new SimpDestinationMessageMatcher(pattern, pathMatcher);
        }
    }

    private interface MatcherBuilder {
        MessageMatcher<?> build();
    }
}
