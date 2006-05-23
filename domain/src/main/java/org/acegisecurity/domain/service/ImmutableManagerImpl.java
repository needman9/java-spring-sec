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

package org.acegisecurity.domain.service;

import org.acegisecurity.domain.PersistableEntity;
import org.acegisecurity.domain.dao.Dao;
import org.acegisecurity.domain.dao.PaginatedList;
import org.acegisecurity.domain.util.GenericsUtils;

import org.springframework.context.support.ApplicationObjectSupport;

import org.springframework.util.Assert;

import java.io.Serializable;

import java.util.Collection;
import java.util.List;


/**
 * Base {@link ImmutableManager} implementation.
 *
 * @author Ben Alex
 * @version $Id$
 *
 * @param <E> DOCUMENT ME!
 */
public class ImmutableManagerImpl<E extends PersistableEntity> extends ApplicationObjectSupport
    implements ImmutableManager<E> {
    //~ Instance fields ================================================================================================

    /** The class that this instance provides services for */
    private Class supportsClass;
    protected Dao<E> dao;

    //~ Constructors ===================================================================================================

    public ImmutableManagerImpl(Dao<E> dao) {
        // work out what domain object we support
        this.supportsClass = GenericsUtils.getGeneric(getClass());
        Assert.notNull(this.supportsClass, "Could not determine the generics type");
        Assert.isTrue(PersistableEntity.class.isAssignableFrom(supportsClass),
            "supportClass is not an implementation of PersistableEntity");

        // store the DAO and check it also supports our domain object type
        Assert.notNull(dao,
            "Non-null DAO (that supports the same domain object class as this services layer) is required as a constructor argument");
        Assert.isTrue(dao.supports(supportsClass), "Dao '" + dao + "' does not support '" + supportsClass + "'");
        this.dao = dao;
    }

    //~ Methods ========================================================================================================

    public List<E> findAll() {
        return dao.findAll();
    }

    public List<E> findId(Collection<Serializable> ids) {
        Assert.notNull(ids, "Collection of IDs cannot be null");
        Assert.notEmpty(ids, "There must be some values in the Collection list");

        return dao.findId(ids);
    }

    /**
     * 
    DOCUMENT ME!
     *
     * @return the sort order column to be used by default by the scroll methods
     */
    protected String getDefaultSortOrder() {
        return "id";
    }

    public E readId(Serializable id) {
        Assert.notNull(id);

        return dao.readId(id);
    }

    public PaginatedList<E> scroll(E value, int firstElement, int maxElements) {
        Assert.notNull(value);
        Assert.isInstanceOf(this.supportsClass, value, "Can only scroll with values this manager supports");

        return dao.scroll(value, firstElement, maxElements, getDefaultSortOrder());
    }

    public PaginatedList<E> scrollWithSubclasses(E value, int firstElement, int maxElements) {
        Assert.notNull(value);
        Assert.isInstanceOf(this.supportsClass, value, "Can only scroll with values this manager supports");

        return dao.scrollWithSubclasses(value, firstElement, maxElements, getDefaultSortOrder());
    }

    public boolean supports(Class clazz) {
        Assert.notNull(clazz);

        return this.supportsClass.equals(clazz);
    }
}
