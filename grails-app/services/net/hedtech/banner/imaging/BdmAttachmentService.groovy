/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.exceptions.BusinessLogicValidationException
import net.hedtech.banner.service.ServiceBase
import net.hedtech.bdm.exception.BdmsException
import net.hedtech.bdm.services.BDMManager
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.json.JSONObject

import javax.xml.ws.WebServiceException

/**
 * Service class which interacts with the BDM-client jar
 * to make calls to AX services.
 * */
class BdmAttachmentService extends ServiceBase {

    static transactional = true
    private static final Logger log = Logger.getLogger(getClass())

    /**
     * Place the uploaded file in a temporary location
     * @param file
     * */
    def Map createBDMLocation(file)
    {
        def map = [:]
        File fileDest
        String fileName

        String tempPath =  ConfigurationHolder.config.bdmserver.file.location
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
        map.hashedName = hashedName

        log.debug("BDM temp file details are :" + map)

        return map
    }

    /**
     * This method is used to create Position Description Comment for a posDescId
     * @param positionDescription
     * @param comment
     * @return
     */
    def createDocument(Map params, String filename, Map attribs , String vpdiCode ) throws BdmsException{

        try {
            def bdm = new BDMManager();
            JSONObject docAttributes = new JSONObject(attribs)
            JSONObject bdmParams = new JSONObject(params)

            bdm.uploadDocument(bdmParams, filename, docAttributes, vpdiCode);
        }catch (BdmsException bdme) {
            log.error("ERROR: Error while creating a BDM document",bdme)

            def message = "unknown.exception"
            def messageArray =[]

            if(bdme?.cause?.toString()?.contains("Invalid index")){
                message = "invalid.index.value"
                messageArray = [bdme?.cause?.message]
            }else if(bdme?.cause?.toString()?.contains("A unique key violation has occured")){
                message = "Invalid.Unique.Constraint"
            }
            throw new ApplicationException(BdmAttachmentService,
                    new BusinessLogicValidationException(message, messageArray))
        }

    }

    /**
     * Search documents/document based on query criteria .
     * @param queryCriterias  can be a list or string
     * @param params
     * @param vpdiCode
     * */
    def searchDocument(Map params, def queryCriterias, String vpdiCode ) throws BdmsException{
        try {
            def bdm = new BDMManager();
            JSONObject bdmParams = new JSONObject(params)

            (queryCriterias instanceof List) ? bdm.searchDocuments(bdmParams, queryCriterias, vpdiCode) : bdm.getDocumentByRef(bdmParams,queryCriterias,vpdiCode)
        } catch (BdmsException bdme) {
            log.error("ERROR: Error while searching  BDM documents",bdme)

            def message  =(bdme.getCause()?.toString()?.contains("Invalid index value")) ?"default.invalid.type.exception":"default.BdmAttachmentService"
            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException(message, []))
        }
        catch(WebServiceException e){
            log.error('BdmAttachmentService',e)
            throw new BdmsException( 'BdmAttachmentService', e )
        }
    }

    /**
     *Delete a document based on docIds .
     *  @param params
     *  @params docIds
     *  @param vpdiCode
     * */
    def deleteDocument(Map params, List docIds, String vpdiCode ) throws BdmsException{

        def bdm = new BDMManager();
        try {
            JSONObject bdmParams = new JSONObject(params)

            bdm.deleteDocument(bdmParams, docIds, vpdiCode);
        } catch (BdmsException bdme) {
            log.error("ERROR: Error while deleting  BDM documents",bdme)

            if(bdme.getCause()?.toString()?.contains("Invalid index value")){
                throw new ApplicationException(BdmAttachmentService,
                        new BusinessLogicValidationException("default.invalid.type.exception", []))
            }
            throw new ApplicationException(BdmAttachmentService,
                    new BusinessLogicValidationException("default.BdmAttachmentService", []))
        }
    }


    /**
     * Update a document indexes based on document reference(docRef) passed.
     * @param params
     * @param docRef
     * @params attribs
     * @param vpdiCode
     */
    def updateDocument(Map params, String docRef, Map attribs , String vpdiCode ) throws BdmsException{

        def bdm = new BDMManager();
        JSONObject updtIndexes = new JSONObject(attribs)
        JSONObject bdmParams = new JSONObject(params)

        try{
            bdm.updateDocument(bdmParams, docRef, updtIndexes, vpdiCode)
         }catch (Exception e){
            log.error("Failed to update document indexes (${docRef})", e);
            throw new ApplicationException(BdmAttachmentService,
                    new BusinessLogicValidationException("Updating document failed", []))
         }
    }

}