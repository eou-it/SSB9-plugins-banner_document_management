/*******************************************************************************
 Copyright 2017 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

package net.hedtech.banner.imaging

import grails.util.Holders
import groovy.sql.Sql
import net.hedtech.bdm.ax.WxCrypto
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.hibernate.SessionFactory

import java.sql.SQLException

/**
 * Created by deeptim on 9/15/2017.
 */
class BdmForRDSService {
//fetching decrypted values by passing encrypted value using a function
    def create(Map params) {
        SessionFactory sessionFactory = Holders.servletContext.getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT).sessionFactory
        def sql = new Sql(sessionFactory.getCurrentSession().connection())

        WxCrypto wxc = new WxCrypto();
        String s = (String) params.get("function");
        String result;
        def parameter1
        def parameter2
        def parameter3
        try {

            //Passing decrypted value to CMHash function
            if (s.equals("f_cmhash")) {
                parameter1 = (String) params.get("parameter")
                sql.call("{ ? = call eoksecr.f_gspcrpu_decrypt(?)}", [Sql.VARCHAR, parameter1])
                        { result1 -> parameter1 = result1 }
                result = wxc.hash(parameter1)
            }
            //Passing decrypted value to generateCredential function
            else if (s.equals("f_generate_credential")) {
                parameter1 = (String) params.get("username");
                sql.call("{ ? = call eoksecr.f_gspcrpu_decrypt(?)}", [Sql.VARCHAR, parameter1])
                        { result1 -> parameter1 = result1 }
                parameter2 = (String) params.get("password");
                sql.call("{ ? = call eoksecr.f_gspcrpu_decrypt(?)}", [Sql.VARCHAR, parameter2])
                        { result1 -> parameter2 = result1 }
                parameter3 = (String) params.get("key");
                sql.call("{ ? = call eoksecr.f_gspcrpu_decrypt(?)}", [Sql.VARCHAR, parameter3])
                        { result1 -> parameter3 = result1 }
                result = wxc.generateCredentials(parameter1, parameter2, parameter3)
            }
            //Passing decrypted value to generateParameter function
            else if (s.equals("f_paras")) {
                parameter1 = (String) params.get("username");
                sql.call("{ ? = call eoksecr.f_gspcrpu_decrypt(?)}", [Sql.VARCHAR, parameter1])
                        { result1 -> parameter1 = result1 }
                parameter2 = (String) params.get("password");
                sql.call("{ ? = call eoksecr.f_gspcrpu_decrypt(?)}", [Sql.VARCHAR, parameter2])
                        { result1 -> parameter2 = result1 }
                parameter3 = (String) params.get("key");
                sql.call("{ ? = call eoksecr.f_gspcrpu_decrypt(?)}", [Sql.VARCHAR, parameter3])
                        { result1 -> parameter3 = result1 }
                result = wxc.generateParameters(parameter1, parameter2, parameter3)
            }


        }
        catch (SQLException e) {
            result = e.message
            log.error("error occured during fetching decrypting the values: " + e)
        }
        catch (Exception ex) {
            result = ex.message
            log.error("error occured " + ex)
        }
        finally {
            sql.close();
        }
        return [new BdmForRdsDecorator(result)]
    }

}
