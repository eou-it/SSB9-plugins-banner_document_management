/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.exceptions.BusinessLogicValidationException
import net.hedtech.banner.exceptions.NotFoundException
import net.hedtech.banner.service.ServiceBase
import net.hedtech.bdm.exception.BdmDocNotFoundException
import net.hedtech.bdm.exception.BdmInvalidAppNameException
import net.hedtech.bdm.exception.BdmInvalidIndexNameException
import net.hedtech.bdm.exception.BdmUniqueKeyViolationException
import net.hedtech.bdm.exception.BdmsException
import net.hedtech.bdm.services.BDMManager
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.json.JSONObject

/**
 * Service class which interacts with the BDM-client jar
 * to make calls to AX services.
 * */
class BdmAttachmentService extends ServiceBase {

    static transactional = true


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
        } catch (BdmInvalidIndexNameException e) {
            log.error("ERROR: Invalid index names in search request", e)

            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.Index.Name.Request", [e.message]))
        }catch(BdmInvalidAppNameException e){
            log.error("ERROR: Invalid App names in search request", e)

            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.AppName.Request", [e.message]))
        }catch(BdmUniqueKeyViolationException e){
            log.error("ERROR: Unique Key Violation", e)

            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.Unique.Constraint", []))
        }catch (Exception e) {
            log.error("ERROR: Error while creating a BDM document", e)
            throw new ApplicationException(BdmAttachmentService,BdmUtility.getGenericErrorMessage("BDM.Unknown.Exception" , null )  ,e)
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

            (queryCriterias instanceof List) ? bdm.searchDocuments(bdmParams, queryCriterias, vpdiCode) : bdm.getDocumentByRef(bdmParams, queryCriterias, vpdiCode)
        }catch (BdmInvalidIndexNameException e) {
            log.error("ERROR: Invalid index names in search request", e)

            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.Index.Name.Request", [e.getMessage()]))
        }catch(BdmInvalidAppNameException e){
            log.error("ERROR: Invalid App names in search request", e)

            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.AppName.Request", [e.message]))
        }catch (BdmDocNotFoundException e) {
            log.error("ERROR: Error while searching  BDM documents", e)

            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Unknown.DocRef.Request",[]))
        }catch (Exception e) {
            log.error("ERROR: Error while creating a BDM document", e)
            throw new ApplicationException(BdmAttachmentService,BdmUtility.getGenericErrorMessage("BDM.Unknown.Exception" , null )  ,e)
        }
    }

    /**
     *Delete a document based on docIds .
     *  @param params
     *  @params docIds
     *  @param vpdiCode
     *
     */
     //TODO: Remove login in last catch block if BDM jar code is corrected
    def deleteDocument(Map params, def docIds, String vpdiCode ) throws BdmsException{

        def bdm = new BDMManager();
        try {
            JSONObject bdmParams = new JSONObject(params)

            bdm.deleteDocument(bdmParams, docIds, vpdiCode);
        } catch (BdmInvalidIndexNameException e) {
            log.error("ERROR: Invalid index names in delet request", e)

            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.Index.Name.Request", [e.getMessage()]))
        }catch (BdmsException bdme) {
            log.error("ERROR: Error while deleting  BDM documents",bdme)

            //TODO: Remove the below if logic
            if(bdme.getCause()?.toString()?.contains("Invalid index value")){
                throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.Index.Name.Request", []))
            }
            throw new ApplicationException(BdmAttachmentService,BdmUtility.getGenericErrorMessage("BDM.Unknown.Exception" , null )  ,bdme)
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
        }catch (BdmInvalidIndexNameException e) {
            log.error("ERROR: Invalid index names in update request", e)

            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.Index.Name.Request", [e.getMessage()]))
        }catch (Exception e) {
            log.error("ERROR: Error while searching  BDM documents", e)

            throw new ApplicationException(BdmAttachmentService,BdmUtility.getGenericErrorMessage("BDM.Unknown.Exception" , null )  ,e)
        }
    }

}