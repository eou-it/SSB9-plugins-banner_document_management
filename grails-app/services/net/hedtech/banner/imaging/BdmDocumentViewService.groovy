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


/**
 * The Service helps create a file in temp path .
 * Mainly used by Restful plugin to support create operation
 * */
class BdmDocumentViewService {

    /**
     * Upload a file to temp location in BDM serice.
     * @params params
     * */
    def create(Map params) {
        log.debug("requestParams: ${params}")

        def returnedInfo = new BdmDocumentViewDecorator()
        def vpdiCode = params?.vpdiCode
        def docRef = params?.docRef;

        try {
            returnedInfo.uri = createDocumentViewUri(docRef, vpdiCode);
            returnedInfo.status = "OK"
        } catch (Exception ex) {
            returnedInfo.status = "ERROR"
            returnedInfo.message = ex.getMessage();
            log.error(ex.getMessage());
        }

        log.debug("reponse: ${returnedInfo}")
        returnedInfo
    }

    private def createDocumentViewUri(String docRef, String vpdiCode ) throws BdmsException {
        log.debug("createDocumentViewUri: docRef=${docRef} vpdiCode= ${vpdiCode}")

        def Map bdmServerConfig = BdmUtility.getBdmServerConfigurations()

        try {
            def bdm = new BDMManager();
            def decodedDocRef = (!docRef.contains('/')) ?  new String(docRef.decodeBase64()):docRef
            log.debug("decodedDocRef= $decodedDocRef");

            JSONObject bdmParams = new JSONObject(bdmServerConfig)
            bdm.createViewDocumentUrl(bdmParams, decodedDocRef, vpdiCode);
        } catch(BdmsException ex){
            log.error (ex.getMessage())
            throw new ApplicationException(BdmDocumentViewService, new BusinessLogicValidationException("failure: ${ex}", ["${ex}"]))
        } catch(Exception ex) {
           log.error (ex.getMessage())
            throw new BdmsException( 'BdmAttachmentService', ex )
        }
    }
}
