/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.exceptions.BusinessLogicValidationException
import net.hedtech.banner.restfulapi.RestfulApiRequestParams
import net.hedtech.bdm.exception.BdmInvalidAppNameException
import net.hedtech.bdm.exception.BdmInvalidDataSourceNameException
import net.hedtech.bdm.exception.BdmInvalidMepDataSourceNameException
import net.hedtech.bdm.exception.BdmsException
import net.hedtech.bdm.services.BDMManager
import org.json.JSONObject
import org.springframework.web.context.request.RequestContextHolder

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
        def docRef = params?.docRef;

        returnedInfo.uri = createDocumentViewUri(docRef, getVpdiCode());
        returnedInfo.status = "OK"

        log.debug("reponse: ${returnedInfo}")
        returnedInfo
    }

    private def createDocumentViewUri(String docRef, String vpdiCode) throws BdmsException {
        log.debug("createDocumentViewUri: docRef=${docRef} vpdiCode= ${vpdiCode}")

        def Map bdmServerConfig = BdmUtility.getBdmServerConfigurations()
        try {
            def bdm = new BDMManager();
            def decodedDocRef = (!docRef.contains('/')) ?  new String(docRef.decodeBase64()):docRef

            log.debug("decodedDocRef= $decodedDocRef");

            JSONObject bdmParams = new JSONObject(bdmServerConfig)
            bdm.createViewDocumentUrl(bdmParams, decodedDocRef, vpdiCode);

        } catch(BdmInvalidAppNameException e){
            log.error("ERROR: Invalid App names in request", e)
            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.AppName.Request", [e.message]))
        } catch(BdmInvalidDataSourceNameException e){
            log.error("ERROR: Invalid MEP data source name in request", e)
            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.DataSourceName", [e.message]))
        } catch(BdmInvalidMepDataSourceNameException e){
            log.error("ERROR: Invalid MEP data source name in request", e)
            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.Mep.DataSourceName", [e.message]))
        } catch(Exception ex) {
            log.error (ex ,ex)

            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Bdm.Service.Exception", [ex.message ]))
        }
    }

    private def getVpdiCode() {
        def params = RestfulApiRequestParams.get();
        def mepCode = params?.get("mepCode")?.toUpperCase()
        return mepCode;
    }
}
