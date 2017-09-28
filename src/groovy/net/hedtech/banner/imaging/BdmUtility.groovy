/*******************************************************************************
 Copyright 2015-2017 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import grails.util.Holders
import org.apache.commons.lang.StringUtils
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.hibernate.SessionFactory
import org.hibernate.dialect.Dialect
import org.hibernate.engine.SessionFactoryImplementor
import org.hibernate.tool.hbm2ddl.DatabaseMetadata
import org.springframework.web.context.request.RequestContextHolder
import groovy.sql.Sql
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
    public static getLikeFormattedFilter(String filter) {
        def filterText
        if (StringUtils.isBlank(filter)) {
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
    def static getPagingParams(limit, offset) {
        limit = limit ? limit as Integer : DEFAULT_MAX_SIZE
        offset = offset ? offset as Integer : DEFAULT_OFFSET
        return [max: limit, offset: offset * limit]
    }

    /**
     *
     * @return
     */
    public static def getConnection() {
        def sessionFactory = Holders.servletContext.
                getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT).sessionFactory
        return sessionFactory.currentSession.connection()
    }

    /**
     *
     * @return
     */
    public static def getDialect() {
        SessionFactory sessionFactory = Holders.servletContext.
                getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT).sessionFactory
        Dialect dialect = ((SessionFactoryImplementor) sessionFactory).getDialect();
        return dialect
    }


    public static boolean checkIfTableExists(String tableName) {
        try {
            DatabaseMetadata databaseMetadata = new DatabaseMetadata(getConnection(), getDialect());
            if (databaseMetadata.isTable(tableName)) {
                log.info("Table " + tableName + " exists");
                return true;
            }

            log.info("Table " + tableName + " does not exist");
        }
        catch (SQLException sqle) {
            throw sqle
        }
        return false;
    }

    public static boolean isBDMInstalled() {
        def flag = false
        def session = RequestContextHolder?.currentRequestAttributes()?.request?.session
        try {
            flag = checkIfTableExists(BDM_VERSION_TABLE)
        } catch (SQLException sqle) {
            flag = false
        } finally {
            session["BDM_INSTALLED"] = flag
        }
        return flag
    }


    public static def getBdmServerConfigurations(def appName = "", def dataSource = "") {
        def bdmServerConfigurations = [:]

        try {
            Holders.config.bdmserver.each { key, value ->
                bdmServerConfigurations.put(key, value)
            }
            (appName) ? bdmServerConfigurations.put("AppName", appName) : ""
            (dataSource) ? bdmServerConfigurations.put("BdmDataSource", dataSource) : ""
             bdmServerConfigurations = getPassword(bdmServerConfigurations)
        }
        catch (Exception e) {
            log.error("Please check the config file and also refer the error ", e)
               }
        return bdmServerConfigurations

    }

    // to get Decrypted passwords defect no. CR-000149894 - DM
    private static LinkedHashMap getPassword(LinkedHashMap bdmConfig) {
        SessionFactory sessionFactory = Holders.servletContext.getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT).sessionFactory
        def sql = new Sql(sessionFactory.getCurrentSession().connection())
        def keypassword = null
        def username = bdmConfig.get("Username")
        def decryptedPwd = null
        try {
            //to decrypt the password
            sql.call("{ ? = call EOKSECR.f_get_bdmpwd(?)}", [Sql.VARCHAR, username])
                    { result -> decryptedPwd = result }
         
            //decrypt the Keypassword
            sql.call("{? = call EOKSECR.f_get_key()}", [Sql.VARCHAR])
                    { result -> keypassword = result }
            bdmConfig.put("KeyPassword", keypassword)
            bdmConfig.put("Password", decryptedPwd)
        }
        finally {
            sql.close()
        }
        return bdmConfig
    } //end of  CR-000149894
    public static def getGenericErrorMessage(def messageKey, def messageArg, def locale = Locale.getDefault()) {
        def messageSource = Holders.grailsApplication.mainContext.getBean 'messageSource'
        messageSource.getMessage(messageKey, messageArg, "An unknown document exception occurred. Please contact your administrator.", locale)
    }

}
