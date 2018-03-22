/*******************************************************************************
 Copyright 2015-2017 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging


import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.exceptions.BusinessLogicValidationException
import net.hedtech.banner.service.ServiceBase
import net.hedtech.bdm.exception.BdmDocNotFoundException
import net.hedtech.bdm.exception.BdmInvalidAppNameException
import net.hedtech.bdm.exception.BdmInvalidDataSourceNameException
import net.hedtech.bdm.exception.BdmInvalidIndexNameException
import net.hedtech.bdm.exception.BdmInvalidIndexValueException
import net.hedtech.bdm.exception.BdmInvalidMepDataSourceNameException
import net.hedtech.bdm.exception.BdmInvalidQueryValueException
import net.hedtech.bdm.exception.BdmMalformedDocRefException
import net.hedtech.bdm.exception.BdmMismatchDataSourceNameException
import net.hedtech.bdm.exception.BdmUniqueKeyViolationException
import net.hedtech.bdm.exception.BdmsException
import net.hedtech.bdm.services.BDMManager
import grails.util.Holders
import org.json.JSONObject


/**
 * Service class which interacts with the BDM-client jar
 * to make calls to AX services.
 * */
class BdmAttachmentService extends ServiceBase {

    static transactional = true
    def messageSource

    /**
     * Place the uploaded file in a temporary location
     * @param file
     * */
    def Map createBDMLocation(file) {
        def map = [:]
        def exception = java.io.FileNotFoundException
        File fileDest
        String fileName
        String tempPath = Holders.config.bdmserver.file.location
        fileName = file.getOriginalFilename()
        String hashedName = java.util.UUID.randomUUID().toString()
        checkExtension(fileName, file);
        File userDir = new File(tempPath, hashedName)
        boolean b = userDir.mkdir()
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
    //This function checks if the file extension and size matches the security rules
    // mentioned in Config file - DM
    def checkExtension(String fileName, file) {

        String[] arr = Holders.config.bdmserver.defaultfile_ext

        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(fileName.substring(fileName.length() - 4))) {
                throw new RuntimeException("File extension");
            }
        }

        def fileSize = ((file.getSize()) / 1024) / 1024
        log.error("File size trying to upload is =" + fileSize)
        def size = Holders.config.bdmserver.defaultFileSize
        if (fileSize > size) {
            throw new RuntimeException("File size exceeding");
        }

    }//end of checkExtension
    /**
     * This method is used to create Position Description Comment for a posDescId
     * @param positionDescription
     * @param comment
     * @return
     */
    def createDocument(Map params, String filename, Map attribs, String vpdiCode) throws BdmsException {

        try {

            def bdm = new BDMManager();
            JSONObject docAttributes = new JSONObject(attribs)
            JSONObject bdmParams = new JSONObject(params)
            String docRef = bdm.uploadDocument(bdmParams, filename, docAttributes, vpdiCode);
            println("docref=" + docRef);
            return docRef;
        } catch (Exception e) {
            throwAppropriateException(e)
        }

    }

    /**
     * Search documents/document based on query criteria .
     * @param queryCriterias can be a list or string
     * @param params
     * @param vpdiCode
     * */
    def searchDocument(Map params, def queryCriterias, String vpdiCode) throws BdmsException {

        try {
            def bdm = new BDMManager();
            JSONObject bdmParams = new JSONObject(params)

            (queryCriterias instanceof List) ? bdm.searchDocuments(bdmParams, queryCriterias, vpdiCode) : bdm.getDocumentByRef(bdmParams, queryCriterias, vpdiCode)

        } catch (Exception e) {
            throwAppropriateException(e)
        }
    }

    /**
     * Delete documents based on docIds .
     * @param params
     * @params docIds
     * @param vpdiCode
     * @return
     * @throws BdmsException
     *
     */

    //  BDM 9.1.1 changes -
    def deleteDocument(Map params, ArrayList docIds, String vpdiCode) throws BdmsException {

        def bdm = new BDMManager();
        try {
            JSONObject bdmParams = new JSONObject(params)

            bdm.deleteDocument(bdmParams, docIds, vpdiCode);
        } catch (Exception e) {
            throwAppropriateException(e)
        }
    }

    /**
     * Delete documents based on doc index values
     * @param params
     * @param attribs
     * @param vpdiCode
     * @return
     * @throws BdmsException
     */
    def deleteDocument(Map params, Map attribs, String vpdiCode) throws BdmsException {
        def bdm = new BDMManager();
        try {
            JSONObject bdmParams = new JSONObject(params)
            JSONObject docIndexes = new JSONObject(attribs)
            bdm.deleteDocument(bdmParams, docIndexes, vpdiCode);
        } catch (Exception e) {
            throwAppropriateException(e)
        }
    }

    def deleteDocumentByDocRef(Map params, String docRef, String vpdiCode) throws BdmsException {
        def bdm = new BDMManager();
        try {
            JSONObject bdmParams = new JSONObject(params)
            bdm.deleteDocumentByDocRef(bdmParams, docRef, vpdiCode);

        } catch (Exception e) {
            throwAppropriateException(e)
        }
    }

    /**
     * Update a document indexes based on document reference(docRef) passed.
     * @param params
     * @param docRef
     * @params attribs
     * @param vpdiCode
     */
    def updateDocument(Map params, String docRef, Map attribs, String vpdiCode) throws BdmsException {

        def bdm = new BDMManager();
        JSONObject updtIndexes = new JSONObject(attribs)
        JSONObject bdmParams = new JSONObject(params)

        try {
            bdm.updateDocument(bdmParams, docRef, updtIndexes, vpdiCode)
        } catch (Exception e) {
            throwAppropriateException(e)
        }
    }


    private def throwAppropriateException(Exception e) {
        if (e instanceof BdmInvalidIndexNameException) {
            log.error("ERROR: Invalid index names in search request", e)
            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.File.Name.Request", [e.message]))
        } else if (e instanceof BdmInvalidIndexValueException) {
            log.error("ERROR: Invalid index value in update request", e)
            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.Index.Value.Request", [e.message]))
        } else if (e instanceof BdmUniqueKeyViolationException) {
            log.error("ERROR: Unique Key Violation", e)
            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.Unique.Constraint", []))
        } else if (e instanceof BdmInvalidAppNameException) {
            log.error("ERROR: Invalid App names in search request", e)
            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.AppName.Request", [e.message]))
        } else if ((e instanceof BdmInvalidDataSourceNameException) || ((e instanceof BdmMismatchDataSourceNameException) || (e instanceof BdmInvalidMepDataSourceNameException))) {
            log.error("ERROR: Invalid query value in update request", e)
            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.DataSourceName", [e.message]))
        } else if (e instanceof BdmDocNotFoundException) {
            log.error("ERROR: Error while searching  BDM documents", e)
            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Unknown.DocRef.Request", []))
        } else if (e instanceof BdmMalformedDocRefException) {
            log.error("ERROR: Error while searching  BDM documents", e)
            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Invalid.DocRef.Data", []))
        } else if (e instanceof Exception) {
            throw new ApplicationException(BdmAttachmentService, BdmUtility.getGenericErrorMessage("BDM.Unknown.Exception", null), e)
        }

    }


}
