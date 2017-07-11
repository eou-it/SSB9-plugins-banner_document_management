/*******************************************************************************
 Copyright 2015-2017 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import grails.util.Holders
import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.exceptions.BusinessLogicValidationException
import net.hedtech.bdm.exception.BdmsException
import org.apache.commons.io.FileUtils

import javax.lang.model.element.ExecutableElement
import javax.xml.bind.DatatypeConverter
import java.nio.file.FileStore
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.text.NumberFormat

class BdmDocumentUploadService {

    def bdmAttachmentService
    def messageSource

    def create(Map params) {
        Map infoMap = [:]
        def file = params.get("file")
        println("Debug file = " + file)
        if (file == null) {
            println("Debug file = null")
            throw new ApplicationException(BdmAttachmentService, new BusinessLogicValidationException("Empty.File.Upload", []))
        }

        try {
          def map = bdmAttachmentService.createBDMLocation(file)
            infoMap.put("status", messageSource.getMessage("file.upload.success", null, "success", Locale.getDefault()))
            infoMap.put("message", messageSource.getMessage("file.upload.success.message", null, Locale.getDefault()))
            infoMap.put("fileRef", map.get("hashedName")+'/'+map.get('fileName'))

          }
        //CR-000149402 temp folder better error log auditing - DM
        catch (ex) {
                File F = new File (Holders.config.bdmserver.file.location)
            println("debug ="+ F)
                if (F.exists()==true){
                   def usableSpace = F.getFreeSpace()
                    println("debug here = " +usableSpace)
                    if (usableSpace == 0)
                    log.error("Error!! Temporary folder size exceeded", ex)
                   }
                else
                    log.error("Error!! Unable to find temporary folder location", ex)

                    throw new ApplicationException(BdmsException, messageSource.getMessage("file.upload.failure.message", "Error!! While placing the file in temporary location, check log file for more details ", Locale.getDefault()), ex)

        }// end of defect CR-000149402

            log.info("Created file successfully. Response details :" + infoMap)
            return infoMap
        }
    }
