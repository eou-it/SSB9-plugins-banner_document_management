/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging


class BdmAttachmentDecorator implements Serializable{

    String type
    String docId
    String ref
    Map indexes

    @Override
    public String toString(){
     return """BdmAttachmentDecorator{
               type=$type
               docId =$docId
               ref = $ref
               indexed = $indexes"""
   }
}
