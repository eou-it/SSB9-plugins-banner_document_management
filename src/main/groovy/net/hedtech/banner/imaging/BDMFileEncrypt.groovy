/*******************************************************************************
 Copyright 2015-2017 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.imaging

import org.apache.log4j.Logger
import sun.misc.BASE64Decoder

import java.security.Key;
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec
import javax.crypto.spec.SecretKeySpec;
import com.sun.org.apache.xml.internal.security.utils.Base64;

class BDMFileEncrypt {

    static transactional = true
    def messageSource
    static final String UNICODE_FORMAT = "UTF8";
    private static final def Logger log = Logger.getLogger(this.getClass())
    static final String secretKey ='#BD@#=@M!@9#Y@M$'  // Should be 16 characters long
    static final byte[] raw = secretKey.getBytes();

    def encrypt(String unencryptedString) {
        def encryptedString
        String result ;
        try {
            def cipher = Cipher.getInstance('AES')
            Key key = new SecretKeySpec(raw, 'AES');
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] utf8 = unencryptedString.getBytes("UTF8");
            byte[] encryptedData = cipher.doFinal(utf8);
            result = Base64.encode(encryptedData)
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("Encrypted value is " + result)
        return result.toString();
    }


    def decrypt(String encryptedString) {
        def decryptedText
        String result ;
        try {
            def cipher = Cipher.getInstance('AES')
            Key key = new SecretKeySpec(raw, 'AES');
            cipher.init(Cipher.DECRYPT_MODE, key)
            byte[] decodedData = Base64.decode(encryptedString)
            byte[] utf8 = cipher.doFinal(decodedData);
            result = new String(utf8, "UTF8")
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("Decrypted value is " + result)
        return result.toString();
    }


}