/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import org.apache.commons.lang.StringUtils
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.hibernate.SessionFactory
import org.hibernate.dialect.Dialect
import org.hibernate.engine.SessionFactoryImplementor
import org.hibernate.tool.hbm2ddl.DatabaseMetadata
import org.springframework.web.context.request.RequestContextHolder

import java.sql.SQLException

class BdmUtility {

    private static final Logger log = Logger.getLogger(BdmUtility.class)

    static final String BDM_VERSION_TABLE = "EURVERS"

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


    /**
     *
     * @return
     */
    public static def getDialect(){
        SessionFactory sessionFactory = ServletContextHolder.servletContext.
                getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT).sessionFactory
        Dialect dialect = ((SessionFactoryImplementor) sessionFactory).getDialect();
        return dialect
    }


    public static boolean checkIfTableExists(String tableName){
        try {
            DatabaseMetadata databaseMetadata=new DatabaseMetadata(getConnection(),getDialect());
            if (databaseMetadata.isTable(tableName)) {
                log.info("Table " + tableName + " exists");
                return true;
            }

            log.info("Table " + tableName + " does not exist");
        }
        catch (  SQLException sqle) {
              return false
        }
        return false;
    }

    public static boolean isBDMInstalled()
    {
        def session = RequestContextHolder?.currentRequestAttributes()?.request?.session
        boolean isBdmInstalled = false

        if (session?.getAttribute("BDM_INSTALLED") != null) {
            return session?.getAttribute("BDM_INSTALLED")
        }else{
            isBdmInstalled = checkIfTableExists(BDM_VERSION_TABLE)
            session?.setAttribute("BDM_INSTALLED",isBdmInstalled)
        }

        return isBdmInstalled
    }

}
