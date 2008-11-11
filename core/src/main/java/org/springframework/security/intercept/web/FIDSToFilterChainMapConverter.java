package org.springframework.security.intercept.web;

import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.security.ConfigAttribute;
import org.springframework.security.util.FilterChainProxy;
import org.springframework.security.util.UrlMatcher;

import javax.servlet.Filter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Used internally to provide backward compatibility for configuration of FilterChainProxy using a
 * FilterInvocationDefinitionSource. This is deprecated in favour of namespace-based configuration.
 *
 * This class will convert a FilterInvocationDefinitionSource into a suitable Map, provided it is one of the
 * recognised implementations (ant path or regular expression). The order of the mappings will be
 * preserved in the Map.
 *
 * @author Luke Taylor
 * @version $Id$
 */
public class FIDSToFilterChainMapConverter {

    private LinkedHashMap<String, List<Filter>> filterChainMap = new LinkedHashMap<String, List<Filter>>();
    private UrlMatcher matcher;

    public FIDSToFilterChainMapConverter(DefaultFilterInvocationDefinitionSource fids, ApplicationContext appContext) {
        // TODO: Check if this is necessary. Retained from refactoring of FilterChainProxy
        Assert.notNull(fids.getAllConfigAttributes(), "FilterChainProxy requires the " +
                "FilterInvocationDefinitionSource to return a non-null response to getAllConfigAttributes()");
        matcher = fids.getUrlMatcher();
        Map<Object, List<ConfigAttribute>> requestMap = fids.getRequestMap();

        for(Object entry : requestMap.keySet()) {
            String path = entry instanceof Pattern ? ((Pattern)entry).pattern() : (String)entry;
            List<ConfigAttribute> configAttributeDefinition = requestMap.get(entry);
            List<Filter> filters = new ArrayList<Filter>();

            for(ConfigAttribute attr : configAttributeDefinition) {
                String filterName = attr.getAttribute();

                Assert.notNull(filterName, "Configuration attribute: '" + attr + "' returned null to the getAttribute() " +
                        "method, which is invalid when used with FilterChainProxy");

                if (!filterName.equals(FilterChainProxy.TOKEN_NONE)) {
                    filters.add((Filter) appContext.getBean(filterName, Filter.class));
                }
            }

            filterChainMap.put(path, filters);
        }
    }

    public Map<String, List<Filter>> getFilterChainMap() {
        return filterChainMap;
    }

    public UrlMatcher getMatcher() {
        return matcher;
    }
}
