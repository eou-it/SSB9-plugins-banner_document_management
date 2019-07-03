/*******************************************************************************
 Copyright 2015-2017 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import grails.util.Holders
import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.exceptions.BusinessLogicValidationException
import net.hedtech.bdm.exception.BdmsException
import java.lang.reflect.UndeclaredThrowableException

class BdmDocumentUploadService {

    def bdmAttachmentService
    def messageSource

    def create(Map params) {
        Map infoMap = [:]
        def file = params.get("file")
        if (file == null) {
            println("Debug file = null")
            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Empty.File.Upload", []))
        }

        try {
            println("messageSource=" + messageSource)
            def map = bdmAttachmentService.createBDMLocation(file)
            infoMap.put("status", messageSource.getMessage("file.upload.success", null, "success", Locale.getDefault()))
            infoMap.put("message", messageSource.getMessage("file.upload.success.message", null, Locale.getDefault()))
            infoMap.put("fileRef", map.get("encryptedFilePath"))
            log.info("Created file successfully. Response details :" + infoMap)
        }
        //CR-000149402 temp folder better error log auditing - DM
        catch (UndeclaredThrowableException ex) {

            File F = new File(Holders.config.bdmserver.file.location)
            if (F.exists() == true) {
                def usableSpace = F.getFreeSpace()
                log.debug("debug here = " + usableSpace)
                if (usableSpace == 0)
                    log.error("Error!! Temporary folder size exceeded", ex)
            } else {
                log.error("Error!! Unable to find temporary folder location", ex)
            }
            throw new ApplicationException(BdmsException, messageSource.getMessage("file.upload.failure.message", "Error!! While placing the file in temporary location, check log file for more details ", Locale.getDefault()), ex)

        }// end of defect CR-000149402

        catch (RuntimeException ex) {
            if (ex.getMessage().equals("File extension")) {
                log.error("File extension is not allowed as per default configuration files extensions", ex)
                String arr = Holders?.config.bdmserver.restrictedFile_ext
                String Extension = arr.replace('[', ' ')
                Extension = Extension.replace(']', ' ')
                throw new BdmsException(messageSource.getMessage("file.upload.failureExtension.message", [] as Object[], Locale.getDefault()), ex)
            } else if (ex.getMessage().equals("File size exceeding")) {
                log.error("File size exceeding from the default value mentioned in configuration files", ex)
                throw new BdmsException(messageSource.getMessage("file.upload.failureFileSize.message", [] as Object[], Locale.getDefault()), ex)
            } else if (ex.getMessage().equals("Upload Size Undefined")) {
                log.error("Please configure Maximum FILE Size for upload", ex)
                throw new BdmsException(messageSource.getMessage("file.upload.UndefinedFileSize.message", [] as Object[], Locale.getDefault()), ex)
            } else
                log.error("Unhandled runtime error occurred please check", ex)
        }
        return infoMap
    }
}