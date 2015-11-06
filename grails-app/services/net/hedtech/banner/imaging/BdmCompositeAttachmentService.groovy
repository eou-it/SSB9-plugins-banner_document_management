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


/**
 * Service class which interacts with bdmAttachmentService
 * to make CRUD operation in AX .
 * */
class BdmCompositeAttachmentService {

    def bdmAttachmentService
    final String SUPPORTED_OPERATOR = "#OR"

    /** Fetch a particular document from Ax server
     *  and wrap the document details(indexes) into
     *  Decorator */
    def get(def encodedDocRef){
        log.debug("BdmCompositeAttachmentService Show :: Encoded document reference ::" + encodedDocRef)

        def docRefMap = getConfigDetailsAndDocId(encodedDocRef)
        def bdmConfigurations = BdmUtility.getBdmServerConfigurations(docRefMap[1] , docRefMap[0])
        def documentDecorator =  getDocumentDecorator(bdmAttachmentService.searchDocument(bdmConfigurations, new String(encodedDocRef.decodeBase64()), null))

        log.debug("BdmCompositeAttachmentService Show :: Response Details ::" + documentDecorator)

        documentDecorator
    }


    private def getConfigDetailsAndDocId(def encodedData){

        byte[] decoded = encodedData.decodeBase64()
        def decodedDocRef= new String(decoded)

        log.info("Decoded document reference is ::" + decodedDocRef )

        def decodedData = decodedDocRef.split("/")
        if(decodedData.length != 3 ){
            log.error("Invalid document reference in the decoded doc ref , Decoded doc ref :: "+new String(decoded))
            throw new ApplicationException("BDM-Documents :Invalid document reference", new BusinessLogicValidationException("", []))
        }

        decodedData
    }


    /** Fetch the documents from Ax server and  wrap the
     * document details(indexes) into Decorator and return
     * the result as PagedResultArrayList  */
    def list(Map params){
        log.debug("Getting list of BDM documents :" + params)


        String vpdiCode = containsInValidVPDICode(params)?null:params?.vpdiCode
        Map bdmServerConfigurations =validateAndGetBdmServerConfigurations(params)
        boolean isPostOperation =  RestfulApiValidationUtility.isQApiRequest(params)
        def criteriaList = []

        if(isPostOperation){
            getListOfCriteria(criteriaList,params)
            log.debug("Post operations criteria  :" + criteriaList)
        }
        def decorators = getBdmAttachementDecorators(bdmAttachmentService.searchDocument(bdmServerConfigurations, criteriaList, vpdiCode))

        log.trace("List of BDM documents details ::" + decorators)
        new PagedResultArrayList(decorators ,decorators.size())
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
     * Returns number of records return by the list method.
     * Called automatically by RestfulServiceAdapter
     * */
    def count(Map params){
        list(params)?.size()
    }

    /**
     * Take the uploaded document from temp path
     * and push the document with indexes to AX
     * */
    def create(Map params) {
        log.debug("Creating BDM documents :" + params)

        def vpdiCode = containsInValidVPDICode(params) ? null : params?.vpdiCode
        def bdmServerConfigurations = validateAndGetBdmServerConfigurations(params)
        def decorators = createDocumentInAX(params, bdmServerConfigurations, vpdiCode)

        log.info("Uploaded file to AX successfully. Response details :" + decorators)
        return decorators
    }


    private def validateAndGetBdmServerConfigurations(def params){
        Map bdmServerConfigurations = BdmUtility.getBdmServerConfigurations(params?.dmType,params?.BdmDataSource)

        if (!bdmServerConfigurations.get("AppName")) {
            throw new ApplicationException("BDM-Documents", new BusinessLogicValidationException("invalid.appName.request", []))
        }
        return bdmServerConfigurations
    }

    /**
     * Take the uploaded document from temp path
     * and push the document with indexes to AX
     * */
    private def createDocumentInAX(params ,bdmServerConfigurations ,vpdiCode){
        def decorators = []
        def dir = ""
        def tempPath = ConfigurationHolder.config.bdmserver.file.location
        params.get('fileRefs')?.each { String fileRefPath -> // If two or more files are pushed to doc then all are uploaded but that is not allowed as if now
            File fileDest = new File(tempPath, fileRefPath)
            if (!fileDest.exists() ) {
                throw new ApplicationException("BDM-Documents", new BusinessLogicValidationException("invalid.fileRef.request", [fileRefPath]))
            }

            bdmAttachmentService.createDocument(bdmServerConfigurations, fileDest.absolutePath, params.indexes, vpdiCode)
            def decorator = getBdmAttachementDecorators(bdmAttachmentService.searchDocument(bdmServerConfigurations, [new JSONObject(params.indexes)], vpdiCode))
            decorators << decorator[0]

            dir = fileRefPath.split("/");

            FileUtils.deleteDirectory(new File(tempPath, dir[0]));

            decorators[0]
        }
    }

    //TODO: Need to update
    def delete(Map params)throws ApplicationException{
        String vpdiCode = params?.vpdiCode
        Map bdmServerConfigurations =BdmUtility.getBdmServerConfigurations()
        if(!(params.id)){
            def criteria =( params.containsKey("indexes"))?addCriteria([:] ,params) :getDocIds(params)

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
        docIds
    }

    /**
    * Update the particular record and
     * return the index details of the record
    * */
    def update(Map params)throws ApplicationException{
        log.debug("Updating BDM document with details ::" + params)

        def docRef = params.docRef;
        docRef = (!docRef.contains('/')) ? new String(docRef.decodeBase64()): docRef

        log.info("decoded docRef= $docRef")

        def vpdiCode = containsInValidVPDICode(params) ? null : params?.vpdiCode
        def bdmServerConfigurations = validateAndGetBdmServerConfigurations(params)
        bdmAttachmentService.updateDocument(bdmServerConfigurations, docRef, params.indexes, vpdiCode)
        def decorator = getBdmAttachementDecorators(bdmAttachmentService.searchDocument(bdmServerConfigurations, [new JSONObject(params.indexes)], vpdiCode))

        log.debug("BDM document after updated :: "+ decorator)
        decorator
    }

    private def containsInValidVPDICode(def params){
        //Some times Vpdi code is received as "null" , so this line of code is added
       return  (params?.vpdiCode == null || params?.vpdiCode == "" || params?.vpdiCode == "null")
    }

}
