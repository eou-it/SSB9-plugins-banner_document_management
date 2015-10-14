package net.hedtech.banner.imaging

import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.exceptions.BusinessLogicValidationException
import net.hedtech.bdm.exception.BdmsException

class BdmDocumentUploadService {

    def bdmAttachmentService
    def messageSource

    def create(Map params) {
        Map infoMap = [:]
        def file = params.get("file")

        if(file==null){
            throw new ApplicationException(BdmAttachmentService,new BusinessLogicValidationException("invalid.file.empty", []))
        }
        def map = bdmAttachmentService.createBDMLocation(file)
        infoMap.put("status", messageSource.getMessage("file.upload.success",null,"success",Locale.getDefault()))
        infoMap.put("message",messageSource.getMessage("file.upload.success.message",null,Locale.getDefault()))
        infoMap.put("fileRef", map.get("hashedName") + '/' + map.get('fileName'))

        log.info("Created file successfully. Response details :" + infoMap)
        return infoMap
    }
}
