/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.exceptions.BusinessLogicValidationException
import net.hedtech.banner.service.ServiceBase
import net.hedtech.bdm.exception.BdmsException
import net.hedtech.bdm.services.BDMManager
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.json.JSONObject

import javax.xml.ws.WebServiceException

class BdmAttachmentService extends ServiceBase {

    static transactional = true

    def Map createBDMLocation(file)
    {
        def map = [:]
        File fileDest
        String fileName

        String tempPath =  ConfigurationHolder.config.bdm.file.location

        fileName = file.getOriginalFilename()
        String hashedName = java.util.UUID.randomUUID().toString()

        File userDir = new File(tempPath, hashedName)
        userDir.mkdir()

        fileDest = new File(userDir, fileName)
        file.transferTo(fileDest)

        def absoluteFileName = fileDest.getAbsolutePath()
        map.absoluteFileName = absoluteFileName
        map.userDir = userDir
        map.fileName = fileName

        return map
    }

    /**
     * This method is used to create Position Description Comment for a posDescId
     * @param positionDescription
     * @param comment
     * @return
     */
    def createDocument(Map bdmParams, String filename, Map attribs , String vpdiCode ) throws BdmsException{

        try {
            def bdm = new BDMManager();

            JSONObject docAttributes = new JSONObject(attribs)

            bdm.uploadDocument(bdmParams, filename, docAttributes, vpdiCode);
        }    catch (BdmsException bdme) {
            if(bdme?.cause?.toString()?.contains("Invalid index value"))
            {
                throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("default.invalid.type.exception", []))
            }
            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("default.BdmAttachmentService", []))
        }

    }



    def viewDocument(Map params, Map criteria, String vpdiCode ) throws BdmsException{
        try {
            def bdm = new BDMManager();
            JSONObject bdmParams = new JSONObject(params)
            JSONObject queryCriteria = new JSONObject(criteria)
            bdm.getDocuments(bdmParams, queryCriteria, vpdiCode);
        } catch (BdmsException bdme) {
                if(bdme.getCause()?.toString()?.contains("Invalid index value"))
                {
                    throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("default.invalid.type.exception", []))
                }
                throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("default.BdmAttachmentService", []))
            }
        catch(WebServiceException e)
        {
            log.error('BdmAttachmentService',e)
            throw new BdmsException( 'BdmAttachmentService', e )
        }
    }

    def deleteDocument(Map params, List docIds, String vpdiCode ) throws BdmsException{

        def bdm = new BDMManager();
        try {
            JSONObject bdmParams = new JSONObject(params)

            bdm.deleteDocument(bdmParams, docIds, vpdiCode);
        } catch (BdmsException bdme) {
            if(bdme.getCause()?.toString()?.contains("Invalid index value"))
            {
                throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("default.invalid.type.exception", []))
            }
            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("default.BdmAttachmentService", []))
        }
    }

    def listDocuments(Map params, Map criteria, String vpdiCode ) throws BdmsException{

        def bdm = new BDMManager();
        JSONObject bdmParams = new JSONObject(params)
        JSONObject queryCriteria = new JSONObject(criteria)

        bdm.getDocuments(bdmParams, queryCriteria, vpdiCode);
    }

}
