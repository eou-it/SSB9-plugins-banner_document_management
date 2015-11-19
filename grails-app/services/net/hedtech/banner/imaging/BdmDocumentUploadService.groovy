/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
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
            throw new ApplicationException(BdmAttachmentService,new BusinessLogicValidationException("Empty.File.Upload", []))}

        try{
            def map = bdmAttachmentService.createBDMLocation(file)
            infoMap.put("status", messageSource.getMessage("file.upload.success",null,"success",Locale.getDefault()))
            infoMap.put("message",messageSource.getMessage("file.upload.success.message",null,Locale.getDefault()))
            infoMap.put("fileRef", map.get("hashedName") + '/' + map.get('fileName'))
        }catch(ex){
            throw new ApplicationException(BdmsException ,messageSource.getMessage("file.upload.failure.message","Error!! While placing the file in temporary location",Locale.getDefault()),ex)
        }

        log.info("Created file successfully. Response details :" + infoMap)
        return infoMap
    }
}
