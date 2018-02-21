package net.hedtech.banner.imaging

/**
 * Created by deeptim on 2/15/2018.
 */
class BdmDocumentDeleteDecorator implements Serializable{
    def docId
    def status

//    BdmDocumentDeleteDecorator(String status,String docid) {
//        println("in constructor="+status +" docid="+docid)
//       this.status=status
//        this.docId =docid
//    }
    public String toString(){
        return """BdmDocumentDeleteDecorator{
               docId =$docId
               status=$status"""
    }
}
