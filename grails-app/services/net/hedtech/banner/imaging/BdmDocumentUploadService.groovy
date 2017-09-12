/*******************************************************************************
 Copyright 2015-2017 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.ibm.icu.util.ICUUncheckedIOException
import grails.util.Holders
import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.exceptions.BusinessLogicValidationException
import net.hedtech.bdm.exception.BdmsException
import org.apache.commons.io.FileUtils
import org.omg.CORBA.portable.ApplicationException

import javax.lang.model.element.ExecutableElement
import javax.xml.bind.DatatypeConverter
import java.lang.reflect.UndeclaredThrowableException
import java.nio.file.FileStore
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.text.NumberFormat

class BdmDocumentUploadService{

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


        catch (UndeclaredThrowableException ex) {
                      println("in first catch ex="+ex)
                File F = new File (Holders.config.bdmserver.file.location)
            println("debug ="+ F)
                if (F.exists()==true){
                   def usableSpace = F.getFreeSpace()
                    println("debug here = " +usableSpace)
                    if (usableSpace == 0)
                    log.error("Error!! Temporary folder size exceeded", ex)
                   }
                else {
                    log.error("Error!! Unable to find temporary folder location", ex)
                }
                    throw new ApplicationException(BdmsException, messageSource.getMessage("file.upload.failure.message", "Error!! While placing the file in temporary location, check log file for more details ", Locale.getDefault()), ex)

        }// end of defect CR-000149402

        catch (RuntimeException ex){
            println("in second catch ex="+ex.getMessage())
            println("in ex="+ex)
            //   ex.printStackTrace()
            if(ex.getMessage().equals("File extension")) {
                log.error("File extension is not allowed as per  default configuration files", ex)
                throw new ApplicationException(BdmsException, messageSource.getMessage("file.upload.failureExtension.message", "Error!! you can not upload a file with this extension", Locale.getDefault()), ex)
            }else if (ex.getMessage().equals("File size exceeding")){
                log.error("File size exceeding from the default value mentioned in configuration files", ex)
                throw new ApplicationException(BdmsException, messageSource.getMessage("file.upload.failureFileSize.message", "Error!! you can not upload a file whose file size is exceeding default size value ", Locale.getDefault()), ex)
            }
            else
                log.error("Unhandled runtime error ocurred please check",ex)
        }
            log.info("Created file successfully. Response details :" + infoMap)
            return infoMap
        }
    }
