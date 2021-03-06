/*******************************************************************************
 Copyright 2015-2017 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import grails.util.Holders
import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.exceptions.BusinessLogicValidationException
import net.hedtech.banner.imaging.BDMFileEncrypt
import net.hedtech.banner.service.ServiceBase
import net.hedtech.bdm.exception.*
import net.hedtech.bdm.services.BDMManager
import org.json.JSONObject

import javax.xml.ws.WebServiceException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/* Import BDM Exception*/


class BdmAttachmentService extends ServiceBase {

    static transactional = true
    def messageSource

    /**
     * Place the uploaded file in a temporary location
     * @param file
     * */

    def Map createBDMLocation(file) {

        def map = [:]
        File fileDest
        String fileName
        def bdmFileEncrypt = new BDMFileEncrypt()

        String tempPath = Holders?.config.bdmserver.file.location
        fileName = file.getOriginalFilename()
        String hashedName = java.util.UUID.randomUUID().toString()
        checkExtension(fileName, file);
        File userDir = new File(tempPath, hashedName)

        boolean b = userDir.mkdir()

        if (!b) {
            throwUploadException("File Temp Location")
        }

        fileDest = new File(userDir, fileName)

        byte[] bytes = file.getBytes();
        Path path = Paths.get(fileDest.toString());
        Files.write(path, bytes);

        log.info("File Transferred Successfully")

        def absoluteFileName = fileDest.getAbsolutePath()
        log.info("absoluteFileName = " + absoluteFileName)
        map.absoluteFileName = absoluteFileName
        map.userDir = userDir
        map.fileName = fileName
        map.hashedName = hashedName

        def encrptedValue = bdmFileEncrypt.encrypt(hashedName + '/' + fileName)
        map.encryptedFilePath = encrptedValue

        log.info("BDM temp file details are :" + map)

        return map
    }
    //This function checks if the file extension and size matches the security rules
    def checkExtension(String fileName, file) {

        String[] arr = Holders?.config.bdmserver.restrictedFile_ext
        def index = fileName.lastIndexOf('.')

        if (index > 0) {
            String extension = fileName.substring(index);
            for (int i = 0; i < arr.length; i++) {
                if (arr[i].equals(extension)) {
                    throwUploadException("File extension")
                }
            }
        }

        def fileSize = ((file.getSize()) / 1024) / 1024
        log.info("File size to upload is =" + fileSize)
        def size = Holders?.config.bdmserver.defaultFileSize

        if (!size) {
            throwUploadException("Upload Size Undefined");
        } else if (fileSize > size) {
            throwUploadException("File size exceeding");
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
            return docRef;
        } catch (Exception e) {
            throwAppropriateException(e)
        }
    }


    def viewDocument(Map params, Map criteria, String vpdiCode) {
        try {
            def bdm = new BDMManager();
            JSONObject bdmParams = new JSONObject(params)
            JSONObject queryCriteria = new JSONObject(criteria)
            bdm.getDocuments(bdmParams, queryCriteria, vpdiCode);
        } catch (Exception e) {
            throw new ApplicationException(BdmAttachmentService, e)
        }
        catch (WebServiceException e) {
            log.error('BdmAttachmentService', e)
            throw new ApplicationException(BdmAttachmentService, e)
        }
    }

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


    def listDocuments(Map params, Map criteria, String vpdiCode) {
        try {
            def bdm = new BDMManager();
            JSONObject bdmParams = new JSONObject(params)
            JSONObject queryCriteria = new JSONObject(criteria)

            bdm.getDocuments(bdmParams, queryCriteria, vpdiCode);
        } catch (Exception e) {
            throw new ApplicationException(BdmAttachmentService, e)
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

    // Exception for Upload Services
    private def throwUploadException(String msg) {
        if (msg.equals("File extension")) {
            log.error("File extension is not allowed as per default configuration files extensions")
            String arr = Holders?.config.bdmserver.restrictedFile_ext
            String extension = arr.replace('[', ' ')
            extension = extension.replace(']', ' ')
            throw new BdmsException(messageSource.getMessage("file.upload.failureExtension.message", [] as Object[], Locale.getDefault()))
        } else if (msg.equals("File size exceeding")) {
            log.error("File size exceeding from the default value mentioned in configuration file")
            throw new BdmsException(messageSource.getMessage("file.upload.failureFileSize.message", [] as Object[], Locale.getDefault()))
        } else if (msg.equals("Upload Size Undefined")) {
            log.error("Please configure Maximum FILE Size for upload")
            throw new BdmsException(messageSource.getMessage("file.upload.UndefinedFileSize.message", [] as Object[], Locale.getDefault()))
        } else if (msg.equals("File Temp Location")) {
            File F = new File(Holders.config.bdmserver.file.location)
            if (F.exists() == true) {
                def usableSpace = F.getFreeSpace()
                log.debug("debug here = " + usableSpace)
                if (usableSpace == 0)
                    log.error("Error!! Temporary folder size exceeded")
            } else {
                log.error("Error!! Unable to find temporary folder location")
            }
            throw new BdmsException(messageSource.getMessage("file.upload.failure.message", [] as Object[], Locale.getDefault()))
        }
    }
}