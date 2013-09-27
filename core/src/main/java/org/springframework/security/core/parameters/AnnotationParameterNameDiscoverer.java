/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.security.core.parameters;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.PrioritizedParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.access.method.P;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Allows finding parameter names using the value attribute of any number of
 * {@link Annotation} instances. This is useful when needing to discover the
 * parameter names of interfaces with Spring Security's method level security.
 * For example, consider the following:
 *
 * <pre>
 * import org.springframework.security.access.method.P;
 *
 * @PostAuthorize("#to == returnObject.to")
 * public Message findMessageByTo(@P("to") String to);
 * </pre>
 *
 * We can make this possible using the following
 * {@link AnnotationParameterNameDiscoverer}:
 *
 * <pre>
 * ParameterAnnotationsNameDiscoverer discoverer = new ParameterAnnotationsNameDiscoverer(
 * 		&quot;org.springframework.security.access.method.P&quot;);
 * </pre>
 *
 * <p>
 * It is common for users to use {@link AnnotationParameterNameDiscoverer} in
 * conjuction with {@link PrioritizedParameterNameDiscoverer}. In fact, Spring
 * Security's {@link DefaultSecurityParameterNameDiscoverer} (which is used by
 * default with method level security) extends
 * {@link PrioritizedParameterNameDiscoverer} and will automatically support
 * both {@link P} and Spring Data's Param annotation if it is found on the
 * classpath.
 * </p>
 *
 * <p>
 * It is important that all the parameter names have a supported annotation on
 * them. Otherwise, the result will be null. For example, consider the
 * following:
 * </p>
 *
 * <pre>
 * import org.springframework.security.access.method.P;
 *
 * @PostAuthorize("#to == returnObject.to")
 * public Message findMessageByToAndFrom(@P("to") User to, User from);
 * </pre>
 *
 * <p>
 * The result of finding parameters on the previous sample will be a null
 * String[] since only a single parameter contains an annotation. This is mostly
 * due to the fact that the fallbacks for
 * {@link PrioritizedParameterNameDiscoverer} are an all or nothing operation.
 * </p>
 *
 * @see DefaultSecurityParameterNameDiscoverer
 *
 * @author Rob Winch
 * @since 3.2
 */
public class AnnotationParameterNameDiscoverer implements
        ParameterNameDiscoverer {

    private final Set<String> annotationClassesToUse;

    public AnnotationParameterNameDiscoverer(String... annotationClassToUse) {
        this(new HashSet<String>(Arrays.asList(annotationClassToUse)));
    }

    public AnnotationParameterNameDiscoverer(Set<String> annotationClassesToUse) {
        Assert.notEmpty(annotationClassesToUse,
                "annotationClassesToUse cannot be null or empty");
        this.annotationClassesToUse = annotationClassesToUse;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.core.ParameterNameDiscoverer#getParameterNames(java
     * .lang.reflect.Method)
     */
    public String[] getParameterNames(Method method) {
        Method originalMethod = BridgeMethodResolver.findBridgedMethod(method);
        String[] paramNames = lookupParameterNames(METHOD_METHODPARAM_FACTORY, originalMethod);
        if(paramNames != null) {
            return paramNames;
        }
        Class<?> declaringClass = method.getDeclaringClass();
        Class<?>[] interfaces = declaringClass.getInterfaces();
        for(Class<?> intrfc : interfaces) {
            Method intrfcMethod = ReflectionUtils.findMethod(intrfc, method.getName(), method.getParameterTypes());
            if(intrfcMethod != null) {
                return lookupParameterNames(METHOD_METHODPARAM_FACTORY, intrfcMethod);
            }
        }
        return paramNames;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.core.ParameterNameDiscoverer#getParameterNames(java
     * .lang.reflect.Constructor)
     */
    public String[] getParameterNames(Constructor<?> constructor) {
        return lookupParameterNames(CONSTRUCTOR_METHODPARAM_FACTORY,
                constructor);
    }

    /**
     * Gets the parameter names or null if not found.
     *
     * @param parameterNameFactory the {@link ParameterNameFactory} to use
     * @param t the {@link AccessibleObject} to find the parameter names on (i.e. Method or Constructor)
     * @return the parameter names or null
     */
    private <T extends AccessibleObject> String[] lookupParameterNames(
            ParameterNameFactory<T> parameterNameFactory, T t) {
        int parameterCount = parameterNameFactory.getParamCount(t);
        String[] paramNames = new String[parameterCount];
        for (int i = 0; i < parameterCount; i++) {
            Annotation[] annotations = parameterNameFactory.findAnnotationsAt(t, i);
            String parameterName = findParameterName(annotations);
            if (parameterName == null) {
                return null;
            } else {
                paramNames[i] = parameterName;
            }
        }
        return paramNames;
    }

    /**
     * Finds the parameter name from the provided {@link Annotation}s or null if
     * it could not find it. The search is done by looking at the value property
     * of the {@link #annotationClassesToUse}.
     *
     * @param parameterAnnotations
     *            the {@link Annotation}'s to search.
     * @return
     */
    private String findParameterName(Annotation[] parameterAnnotations) {
        for (Annotation paramAnnotation : parameterAnnotations) {
            if (annotationClassesToUse.contains(paramAnnotation
                    .annotationType().getName())) {
                return (String) AnnotationUtils.getValue(paramAnnotation,
                        "value");
            }
        }
        return null;
    }

    private static final ParameterNameFactory<Constructor<?>> CONSTRUCTOR_METHODPARAM_FACTORY = new ParameterNameFactory<Constructor<?>>() {
        public int getParamCount(Constructor<?> constructor) {
            return constructor.getParameterTypes().length;
        }

        public Annotation[] findAnnotationsAt(Constructor<?> constructor, int index) {
            return constructor.getParameterAnnotations()[index];
        }
    };

    private static final ParameterNameFactory<Method> METHOD_METHODPARAM_FACTORY = new ParameterNameFactory<Method>() {
        public int getParamCount(Method method) {
            return method.getParameterTypes().length;
        }

        public Annotation[] findAnnotationsAt(Method method, int index) {
            return method.getParameterAnnotations()[index];
        }
    };

    /**
     * Strategy interface for looking up the parameter names.
     *
     * @author Rob Winch
     * @since 3.2
     *
     * @param <T> the type to inspect (i.e. {@link Method} or {@link Constructor})
     */
    private interface ParameterNameFactory<T extends AccessibleObject> {
        /**
         * Gets the parameter count
         * @param t
         * @return
         */
        int getParamCount(T t);

        /**
         * Gets the {@link Annotation}s at a specified index
         * @param t
         * @param index
         * @return
         */
        Annotation[] findAnnotationsAt(T t, int index);
    }
}