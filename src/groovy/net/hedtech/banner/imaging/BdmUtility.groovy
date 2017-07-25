/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import grails.util.Holders
import groovy.sql.Sql
import org.apache.commons.lang.StringUtils
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.exceptions.BusinessLogicValidationException
import org.hibernate.SessionFactory

import java.sql.SQLException

class BdmUtility {

    private static final Logger log = Logger.getLogger(BdmUtility.class)

    def static final DEFAULT_MAX_SIZE = 10
    def static final DEFAULT_OFFSET = 0

    static SessionFactory sessionFactory = Holders.servletContext.getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT).sessionFactory

    static String DECRYPT_SQL = "select gskdecr.decrypt_string(:encryptedStr) from dual"

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
        def sessionFactory = Holders.servletContext.
                getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT).sessionFactory
        return sessionFactory.currentSession.connection()
    }




    public static boolean isBDMInstalled(){
        if(!Holders.config.bdm.enabled)
            return false
        def sql
        try {
            sql = new Sql( getConnection())
            def tableSql = """SELECT count(1) from EURVERS where 1 = 2 """
            sql.eachRow(tableSql){ }
            sql?.close()
            return true;
        }
        catch (SQLException ae) {
            return false
        }
        finally {
            sql?.close()
        }
        return false
    }

    /**
     *
     * @return
     */
    public static boolean checkBDMInstallation(){
        if(!Holders.config.bdmInstalled){
            throw new ApplicationException(BdmUtility,
                    new BusinessLogicValidationException("bdm.not.installed", []))
        }
    }


    /**
    *
    * @param str
    * @return
    */
    static String decryptString(String encryptedStr ){
        if (!encryptedStr)
            throw new ApplicationException(BdmUtility, "@@r1:security.@@MissingValue")
        try {
            return sessionFactory.getCurrentSession().createSQLQuery(DECRYPT_SQL)
                    .setString("encryptedStr", encryptedStr)
                    .uniqueResult()
        }catch(SQLException sqle)
        {
            throw ApplicationException(BdmUtility,sqle)
        }
    }


}
