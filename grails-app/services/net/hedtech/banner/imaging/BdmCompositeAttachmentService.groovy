/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.exceptions.BusinessLogicValidationException
import net.hedtech.banner.restfulapi.RestfulApiValidationUtility
import net.hedtech.restfulapi.PagedResultArrayList
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.json.JSONObject
import org.apache.commons.io.FileUtils;

class BdmCompositeAttachmentService {

    def bdmAttachmentService
    def messageSource
    final String SUPPORTED_OPERATOR = "#OR"

    /** Fetch a particular document from Ax server
     *  and wrap the document details(indexes) into
     *  Decorator */
    def get(def encodedDocRef){
        log.debug("BdmCompositeAttachmentService Show :: Encoded document reference :" + encodedDocRef)

        def docRefMap = getConfigDetailsAndDocId(encodedDocRef)
        log.info("Decoded document reference is :" + docRefMap )

        def bdmConfigurations = BdmUtility.getBdmServerConfigurations(docRefMap.dmType , docRefMap.BdmDataSource)

        return getDocumentDecorator(bdmAttachmentService.searchDocument(bdmConfigurations, new String(encodedDocRef.decodeBase64()), null))
    }


    private def getConfigDetailsAndDocId(def encodedData){
        def configDetails =[:]
        byte[] decoded = encodedData.decodeBase64()
        def decodedData= new String(decoded).split("/")
        if(decodedData.length != 3 ){
            log.error("Invalid document reference in the reference :: "+new String(decoded))
            throw new ApplicationException("BDM-Documents :Invalid document reference", new BusinessLogicValidationException("", []))
        }

        configDetails.put("dmType", decodedData[1])
        configDetails.put("BdmDataSource" ,decodedData[0])

        return configDetails
    }


    /** Fetch the documents from Ax server and  wrap the
     * document details(indexes) into Decorator and return
     * the result as PagedResultArrayList  */
    def list(Map params){
        log.debug("Getting list of BDM documents :" + params)

        String vpdiCode = (params?.vpdiCode == null || params?.vpdiCode == "" || params?.vpdiCode == "null")?null:params?.vpdiCode
        Map bdmServerConfigurations =validateAndGetBdmServerConfigurations(params)

        def criteriaList = []

        boolean isPostOperation =  RestfulApiValidationUtility.isQApiRequest(params)
        if(isPostOperation){
            getListOfCriteria(criteriaList,params)
            log.debug("Post operations criteria  :" + criteriaList)
        }

        def decorators = getBdmAttachementDecorators(bdmAttachmentService.searchDocument(bdmServerConfigurations, criteriaList, vpdiCode))
        return new PagedResultArrayList(decorators ,decorators.size())
    }

    /**
     * Wrap the document details into decorator and
     * return the list of decorators
     * */
    private def getBdmAttachementDecorators(List resourceDetails){
        log.debug("GetBdmAttachementDecorators : " + resourceDetails)
        def decorators =[]
        resourceDetails.each {org.json.JSONObject resourceDetail ->
            decorators << getDocumentDecorator(resourceDetail)
        }
        log.debug("Decorator details :" + decorators)
        decorators
    }

    private def getDocumentDecorator(org.json.JSONObject resourceDetail ){
        def documentDecorator = new BdmAttachmentDecorator()
        documentDecorator.docRef = resourceDetail.opt('docRef')
        documentDecorator.dmType = resourceDetail.opt("dmType")
        documentDecorator.docId = resourceDetail.opt("docId")
        documentDecorator.indexes = toMap(resourceDetail.opt("indexes"))
        return  documentDecorator
    }


    /** Converts JSONObject to map used by view BDM doc method*/
    private def toMap(org.json.JSONObject indexes){
        def indexValuesAsMap = [:]
        def keys = indexes.keys()
        while(keys.hasNext()){
            def key = keys.next()
            indexValuesAsMap.put(key , indexes.opt(key))
        }
        indexValuesAsMap
    }

    /**
     * Used by list method for supporting Logical operator
     * in the INPUT json
     * */
    private  def getListOfCriteria(List criterias ,Map params){
        Map indexes = params.get("indexes")
        if(indexes?.containsKey(SUPPORTED_OPERATOR)){
            org.json.JSONArray  indexValues = indexes.get(SUPPORTED_OPERATOR)
            def indexLength = indexValues.length()
            (0..(indexLength-1)).each { length ->
                criterias <<  new org.json.JSONObject(indexValues.opt(length))
            }
        }else{
            criterias << new org.json.JSONObject(indexes)
        }
        criterias
    }

    /**
     * Used when querying using QAPI . Add query criteria to the document.
     * */
    private  def addCriteria(Map criteria , Map params){
        Map indexes = params.get("indexes")
        if(indexes && indexes instanceof Map){
            indexes.each {key,value ->
                criteria.put(key,value)
            }
        }
        criteria
    }

    /**
     * Parse the urlString and return the docId present in
     * the url
     * */
    private def getDocIdFromUrlString(String urlString){
        return getMapFromUrlString(urlString).get("DocId")?:""
    }

    private Map getMapFromUrlString(String urlString){
        int firstIndex = urlString.indexOf("?");
        urlString = urlString.substring(firstIndex+1);
        String[] splitedData = urlString.split("&");
        def finalMap = [:]
        splitedData.each{token ->
            def tokens = token.split("=")
            finalMap.put(tokens[0],tokens[1])
        }
        return finalMap
    }

    /**
     * Returns number of records return by the list method.
     * Called automatically by RestfulServiceAdapter
     * */
    def count(Map params){
        list(params)?.size()
    }


    def create(Map params) {
        log.debug("Creating BDM documents :" + params)

        String vpdiCode = (params?.vpdiCode == null || params?.vpdiCode == "" || params?.vpdiCode == "null") ? null : params?.vpdiCode

        Map bdmServerConfigurations = validateAndGetBdmServerConfigurations(params)

        def decorators = uploadDocToAX(params, bdmServerConfigurations, vpdiCode)

        log.info("Uploaded file to AX successfully. Response details :" + decorators)
        return decorators
    }



    private def validateAndGetBdmServerConfigurations(def params){
        Map bdmServerConfigurations = BdmUtility.getBdmServerConfigurations()
        bdmServerConfigurations.put("AppName", params?.dmType)

        if (!bdmServerConfigurations.get("AppName")) {
            throw new ApplicationException("BDM-Documents", new BusinessLogicValidationException("invalid.appName.request", []))
        }
        return bdmServerConfigurations
    }





    //TODO : DO JSON validation
    private def uploadDocToAX(params ,bdmServerConfigurations ,vpdiCode){
        def decorators = []
        def dir = ""
        def tempPath = ConfigurationHolder.config.bdmserver.file.location
        params.get('fileRefs')?.each { String fileRefPath ->
            File fileDest = new File(tempPath, fileRefPath)
            if (!fileDest.exists() ) {
                throw new ApplicationException("BDM-Documents", new BusinessLogicValidationException("invalid.fileRef.request", [fileRefPath]))
            }

            bdmAttachmentService.createDocument(bdmServerConfigurations, fileDest.absolutePath, params.indexes, vpdiCode)
            def decorator = getBdmAttachementDecorators(bdmAttachmentService.searchDocument(bdmServerConfigurations, [new JSONObject(params.indexes)], vpdiCode))
            decorators << decorator[0]

            dir = fileRefPath.split("/");
        }

        FileUtils.deleteDirectory(new File(tempPath, dir[0]));

        return decorators[0]
    }

    //TODO: Need to update
    def delete(Map params)throws ApplicationException{
        String vpdiCode = params?.vpdiCode
        Map bdmServerConfigurations =BdmUtility.getBdmServerConfigurations()
        if(!(params.id)){
            def criteria =(isQueryWithAttributes(params))?addCriteria([:] ,params) :getDocIds(params)

            if(!criteria){
                throw new ApplicationException("BDM-Documents" , new BusinessLogicValidationException("invalid.delete.request", [] ))
            }
            bdmAttachmentService.deleteDocument(bdmServerConfigurations,criteria ,vpdiCode)

        }else if(params.id){
            bdmAttachmentService.deleteDocument(bdmServerConfigurations,[params.id] ,vpdiCode)
        }
    }

    private def getDocIds(Map params){
        def docIds = []
        def temp =   (params.containsKey("docIds") && params.get("docIds") instanceof List)?params.get("docIds") :[]
        temp.each{docId-> docIds << docId } //Casting becuase params.get returns org.codehaus.groovy.grails.web.json.JSONArray
        return docIds
    }

    private boolean isQueryWithAttributes(Map params){
        params.containsKey("indexes")
    }

    private  boolean isQApiWithDelete(Map params) {
        boolean qapiReq = false
        if (params && params.action) {
            if (params.action["POST"] == "delete") {
                qapiReq = true
            }
        }
        return qapiReq
    }

    def update(Map params)throws ApplicationException{

        def infoMap = [:]

        def docRef = params.docRef;
        if (!docRef.contains('/'))
            docRef = new String(docRef.decodeBase64());
        log.debug("decoded docRef= $docRef");

        String vpdiCode = (params?.vpdiCode == null || params?.vpdiCode == "" || params?.vpdiCode == "null") ? null : params?.vpdiCode

        Map bdmServerConfigurations = BdmUtility.getBdmServerConfigurations()
        bdmServerConfigurations.put("AppName", params.dmType)

        bdmAttachmentService.updateDocument(bdmServerConfigurations, docRef, params.indexes, vpdiCode)

        infoMap.put("status","Success")
        infoMap.put("message","Document updated successfully!")

        infoMap
    }
}
