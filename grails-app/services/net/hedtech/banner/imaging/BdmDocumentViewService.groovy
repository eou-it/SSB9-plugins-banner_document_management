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
        log.debug("requestParams: ${params}")

        def BdmDocumentViewDecorator returnedInfo = new BdmDocumentViewDecorator()
        def String vpdiCode = params?.vpdiCode
        def String docRef = params?.docRef;

        try {
            returnedInfo.uri = createDocumentViewUri(docRef, vpdiCode);
            returnedInfo.status = "OK"
        } catch (Exception ex) {
            returnedInfo.status = "ERROR"
            returnedInfo.message = ex.getMessage();
            log.error(ex.getMessage());
        }

        log.debug("reponse: ${returnedInfo}")
        return returnedInfo
    }

    def createDocumentViewUri(String docRef, String vpdiCode ) throws BdmsException {
        log.debug("createDocumentViewUri: docRef=${docRef} vpdiCode= ${vpdiCode}")

        def Map bdmServerConfig = BdmUtility.getBdmServerConfigurations()

        try {
            def bdm = new BDMManager();
            def decodedDocRef = docRef;
            if (!docRef.contains('/'))
                decodedDocRef = new String(docRef.decodeBase64());
            log.debug("decodedDocRef= $decodedDocRef");

            JSONObject bdmParams = new JSONObject(bdmServerConfig)
            String uri = bdm.createViewDocumentUrl(bdmParams, decodedDocRef, vpdiCode);
            return uri;
        } catch(BdmsException ex){
            Log.error ex.stackTrace
            throw new ApplicationException(BdmDocumentViewService,
                                            new BusinessLogicValidationException("failure: ${ex}", ["${ex}"]))
        } catch(Exception ex) {
            Log.error ex.stackTrace
        }
    }
}
