/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.extractor

import net.hedtech.restfulapi.extractors.RequestExtractor
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartRequest

import javax.servlet.http.HttpServletRequest

class BdmDocumentExtractor implements RequestExtractor{

    @Override
    Map extract(HttpServletRequest request) {

        Map responseMap = [:]

        if(request instanceof  MultipartRequest){
            MultipartFile file = request.getFile("file".toString())
            if(file.empty || file == null) {
                responseMap.put("error","Cannot accept an empty file.")
            } else {
                responseMap.put("file", file)
            }

        }else {
            responseMap = request.JSON
        }

        return responseMap

    }
}
