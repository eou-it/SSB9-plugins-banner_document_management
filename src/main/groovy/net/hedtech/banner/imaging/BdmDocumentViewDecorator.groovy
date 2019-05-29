/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

/**
 * Created by rlin on 9/30/2015.
 */
class BdmDocumentViewDecorator implements Serializable {
    String uri
    String status
    String message

    @Override
    public String toString(){
        return """BdmDocumentViewDecorator{
               uri = $uri
               status = $status
               message = $message"""
    }
}

