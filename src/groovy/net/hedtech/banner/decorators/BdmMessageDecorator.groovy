/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.decorators

class BdmMessageDecorator implements Serializable {

    private Map message;

    Map getMessage() {
        return message
    }

    void setMessage(Map message) {
        this.message = message
    }

    @Override
    public String toString() {
        return "BdmMessageDecorator{" +
                " Message =" + message +
                '}';
    }
}
