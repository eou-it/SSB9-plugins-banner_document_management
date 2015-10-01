/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import jline.internal.Log
import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.exceptions.BusinessLogicValidationException
import net.hedtech.bdm.exception.BdmsException
import net.hedtech.bdm.services.BDMManager
import org.apache.log4j.Logger
import org.json.JSONObject

import javax.xml.ws.WebServiceException

class BdmDocumentViewService {

    private static final Logger log = Logger.getLogger(getClass())

    def create(Map params) {
        BdmDocumentViewDecorator info = new BdmDocumentViewDecorator()

        String vpdiCode = params?.vpdiCode
        String docRef = params?.docRef;

        try {

            info.uri = createDocumentViewUri(docRef, vpdiCode);
            info.status = "OK"
        } catch (Exception ex) {
            info.status = "ERROR"
            info.message = ex.getMessage();
        }

        return info
    }

    def createDocumentViewUri(String docRef, String vpdiCode ) throws BdmsException {

        Map bdmServerConfig = BdmUtility.getBdmServerConfigurations()

        try {
            def bdm = new BDMManager();

            JSONObject bdmParams = new JSONObject(bdmServerConfig)
            String uri = bdm.createViewDocumentUrl(bdmParams, docRef, vpdiCode);

        } catch(Exception ex){
            Log.debug ex.stackTrace
            throw ex;
        }
    }
}
