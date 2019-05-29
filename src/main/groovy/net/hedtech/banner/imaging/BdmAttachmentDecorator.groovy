/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging


class BdmAttachmentDecorator implements Serializable{

    String dmType
    String docId
    String docRef
    Map indexes

    @Override
    public String toString(){
     return """BdmAttachmentDecorator{
               type=$dmType
               docId =$docId
               docRef = $docRef
               indexed = $indexes"""
   }
}
