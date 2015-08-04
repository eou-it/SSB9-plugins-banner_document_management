/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes

class BdmUtility {

    def static final DEFAULT_MAX_SIZE = 10
    def static final DEFAULT_OFFSET = 0

    /**
     *
     * @param filter
     * @return
     */
    public static getLikeFormattedFilter(String filter){
        def filterText
        if (StringUtils.isBlank( filter )) {
            filterText = "%"
        } else if (!(filter =~ /%/)) {
            filterText = "%" + filter.toUpperCase() + "%"
        } else {
            filterText = filter.toUpperCase()
        }

        return filterText
    }

    /**
     *
     * @param limit
     * @param offset
     * @return
     */
    def static getPagingParams( limit, offset ) {
        limit = limit ? limit as Integer : DEFAULT_MAX_SIZE
        offset = offset ? offset as Integer : DEFAULT_OFFSET
        return [max: limit, offset: offset * limit]
    }

    /**
     *
     * @return
     */
    public static def getConnection(){
        def sessionFactory = ServletContextHolder.servletContext.
                getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT).sessionFactory
        return sessionFactory.currentSession.connection()
    }


}
