/*******************************************************************************
 Copyright 2015-2017 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging


import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.exceptions.BusinessLogicValidationException

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
        // Moved to BdmAttachmentService to support FSS (Plugin level) and APP level
        catch (RuntimeException ex) {
            log.error("Unhandled runtime error occurred please check", ex)
        }
        return infoMap
    }
}