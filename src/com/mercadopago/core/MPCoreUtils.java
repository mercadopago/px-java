package com.mercadopago.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mercadopago.exceptions.MPException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import java.io.InputStream;

/**
 * Mercado Pago SDK
 * Core utils class
 *
 * Created by Eduardo Paoletta on 11/17/16.
 */
public class MPCoreUtils {

    /**
     * Static method that transform an Input Stream to a String object, returns an empty string if InputStream is null.
     *
     * @param is                    Input Stream to process
     * @return                      a String with the stream content
     * @throws MPException
     */
    public static String inputStreamToString(InputStream is) throws MPException {
        String value = "";
        if (is != null) {
            try {
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) != -1)
                    result.write(buffer, 0, length);
                value = result.toString("UTF-8");
            } catch (Exception ex) {
                throw new MPException(ex);
            }
        }
        return value;

    }

}