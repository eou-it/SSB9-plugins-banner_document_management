package net.hedtech.banner.decorators

/**
 * Created by swateekj on 9/15/2015.
 */
class BdmMessageDecorator implements Serializable {

    Map message;

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
