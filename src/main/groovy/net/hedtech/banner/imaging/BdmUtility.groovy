/*******************************************************************************
 Copyright 2015-2017 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import grails.util.Holders
import groovy.sql.Sql
import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.exceptions.BusinessLogicValidationException
import org.apache.commons.lang.StringUtils
import org.grails.web.util.GrailsApplicationAttributes
import org.hibernate.SessionFactory

import java.sql.SQLException

class BdmUtility {

    def static final DEFAULT_MAX_SIZE = 10
    def static final DEFAULT_OFFSET = 0


    //static SessionFactory sessionFactory = Holders.servletContext.getAttribute( GrailsApplicationAttributes.APPLICATION_CONTEXT ).sessionFactory
    static SessionFactory sessionFactory = Holders.getGrailsApplication().getMainContext().sessionFactory
    static String DECRYPT_SQL = "select gskdsec.decrypt_string(:encryptedStr) from dual"
    static String FETCH_CRYPTO_KEY_SQL = "select eoksecr.f_get_key() from dual"

    /**
     *
     * @param filter
     * @return
     */
    public static getLikeFormattedFilter( String filter ) {
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
    public static def getConnection() {
        def sessionFactory = Holders.getGrailsApplication().getMainContext().sessionFactory
        return sessionFactory.currentSession.connection()
    }


    public static boolean isBDMInstalled() {
        if (!Holders.config.bdm.enabled)
            return false
        def sql
        try {
            sql = new Sql( getConnection() )
            def tableSql = """SELECT count(1) from EURVERS where 1 = 2 """
            sql.eachRow( tableSql ) {}
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
    public static boolean checkBDMInstallation() {
        if (!Holders.config.bdmInstalled) {
            throw new ApplicationException( BdmUtility,
                                            new BusinessLogicValidationException( "bdm.not.installed", [] ) )
        }
    }

    /**
     * Decrypts encrypted String
     * @param str, The encrypted String
     * @return
     */
    static String decryptString( String encryptedStr ) {
        if (!encryptedStr) {
            throw new ApplicationException( BdmUtility, "@@r1:security.@@MissingValue" )
        }
        try {
            sessionFactory.getCurrentSession().createSQLQuery( DECRYPT_SQL )
                    .setString( "encryptedStr", encryptedStr )
                    .uniqueResult()
        } catch (SQLException sqle) {
            throw ApplicationException( BdmUtility, sqle )
        }
    }

    /**
     * Fetches BDM Crypto Key
     * @return
     */
    static String fetchBdmCryptoKey() {
        try {
            sessionFactory.getCurrentSession().createSQLQuery( FETCH_CRYPTO_KEY_SQL )
                    .uniqueResult()
        } catch (SQLException sqle) {
            throw ApplicationException( BdmUtility, sqle )
        }
    }

}
