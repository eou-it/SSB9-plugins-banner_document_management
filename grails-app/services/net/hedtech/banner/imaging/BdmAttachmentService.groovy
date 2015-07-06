/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import net.hedtech.banner.service.ServiceBase
import net.hedtech.bdm.exception.BdmsException
import net.hedtech.bdm.services.BDMManager
import org.json.JSONObject

class BdmAttachmentService extends ServiceBase {

    static transactional = true

    /**
     * This method is used to create Position Description Comment for a posDescId
     * @param positionDescription
     * @param comment
     * @return
     */
    def createDocument(JSONObject bdmParams, String filename, JSONObject docAttributes, String vpdiCode ) throws BdmsException{


        def bdm = new BDMManager();

        bdm.uploadDocument(bdmParams, filename, docAttributes, vpdiCode);


    }



    def viewDocument(JSONObject bdmParams, JSONObject queryCriteria, String vpdiCode ) throws BdmsException{

        def bdm = new BDMManager();

        bdm.getDocuments(bdmParams, queryCriteria, vpdiCode);

    }

    def deleteDocument(JSONObject bdmParams, List docIds, String vpdiCode ) throws BdmsException{

        def bdm = new BDMManager();

        bdm.deleteDocument (bdmParams, docIds, vpdiCode);

    }

    def listDocuments(JSONObject bdmParams, JSONObject queryCriteria, String vpdiCode ) throws BdmsException{

        def bdm = new BDMManager();

        bdm.getDocuments(bdmParams, queryCriteria, vpdiCode);

    }

}
