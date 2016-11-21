package com.mercadopago.net;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mercadopago.core.MPBaseResponse;
import com.mercadopago.core.RestAnnotations.PayloadType;
import com.mercadopago.exceptions.MPRestException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.entity.EntitySerializer;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.io.InputStream;
import java.util.*;

/**
 * Mercado Pago SDK
 * Simple Rest Client
 *
 * Created by Eduardo Paoletta on 11/11/16.
 */
public class MPRestClient {

    private static String proxyHostName = null;
    private static int proxyPort = -1;


    public MPRestClient() {
        new MPRestClient(null, -1);
    }

    public MPRestClient(String proxyHostName, int proxyPort) {
        this.proxyHostName = proxyHostName;
        this.proxyPort = proxyPort;
    }

    /**
     * Executes a http request and returns a response
     *
     * @param httpMethod                a String with the http method to execute
     * @param uri                       a String with the uri
     * @param payloadType               PayloadType NONE, JSON, FORM_DATA, X_WWW_FORM_URLENCODED
     * @param payload                   JosnObject with the payload
     * @param colHeaders                custom headers to add in the request
     * @return                          MPBaseResponse with parsed info of the http response
     * @throws MPRestException
     */
    public MPBaseResponse executeRequest(String httpMethod, String uri, PayloadType payloadType, JsonObject payload, Collection<Header> colHeaders)
            throws MPRestException {
        HttpClient httpClient = null;
        try {
            httpClient = getClient();
            if (colHeaders == null)
                colHeaders = new Vector<Header>();
            HttpEntity entity = normalizePayload(payloadType, payload, colHeaders);
            HttpRequestBase request = getRequestMethod(httpMethod, uri, entity);
            for (Header header : colHeaders)
                request.addHeader(header);
            HttpResponse response = httpClient.execute(request);

            return new MPBaseResponse(response);

        } catch (MPRestException restEx) {
            throw restEx;
        } catch (Exception ex) {
            throw new MPRestException(ex);
        } finally {
            try {
                if (httpClient != null)
                    httpClient.getConnectionManager().shutdown();
            } catch (Exception ex) {
                //Do Nothing
            }
        }
    }

    /**
     * Returns a DefaultHttpClient instance.
     * If proxy information exists, its setted on the client.
     *
     * @return                          a DefaultHttpClient
     */
    private HttpClient getClient() {
        HttpClient httpClient = new DefaultHttpClient();

        //Proxy
        if (StringUtils.isNotEmpty(proxyHostName)) {
            HttpHost proxy = new HttpHost(proxyHostName, proxyPort);
            httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }
        return httpClient;
    }

    /**
     * Prepares the payload to be sended in the request.
     *
     * @param payloadType               PayloadType NONE, JSON, FORM_DATA, X_WWW_FORM_URLENCODED
     * @param payload                   JosnObject with the payload
     * @param colHeaders                Collection of headers. Content type header will be added by the method
     * @return
     * @throws MPRestException          HttpEntity with the normalized payload
     */
    private HttpEntity normalizePayload(PayloadType payloadType, JsonObject payload, Collection<Header> colHeaders) throws MPRestException {
        BasicHeader header = null;
        HttpEntity entity = null;
        if (payload != null) {
            if (payloadType == PayloadType.JSON) {
                header = new BasicHeader(HTTP.CONTENT_TYPE, "application/json");
                StringEntity stringEntity = null;
                try {
                    stringEntity = new StringEntity(payload.toString());
                } catch(Exception ex) {
                    throw new MPRestException(ex);
                }
                stringEntity.setContentType(header);
                entity = stringEntity;

            } else {
                Map<String, Object> map = new Gson().fromJson(payload.toString(), new TypeToken<Map<String, Object>>(){}.getType());
                List<NameValuePair> params = new ArrayList<NameValuePair>(2);
                for (Map.Entry<String, Object> entry : map.entrySet())
                    params.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
                UrlEncodedFormEntity urlEncodedFormEntity = null;
                try {
                    urlEncodedFormEntity = new UrlEncodedFormEntity(params, "UTF-8");
                } catch(Exception ex) {
                    throw new MPRestException(ex);
                }

                //if (payloadType == PayloadType.FORM_DATA)
                //    header = new BasicHeader(HTTP.CONTENT_TYPE, "multipart/form-data");
                //else if (payloadType == PayloadType.X_WWW_FORM_URLENCODED)
                    header = new BasicHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencoded");
                urlEncodedFormEntity.setContentType(header);
                entity = urlEncodedFormEntity;
            }
        }
        colHeaders.add(header);

        return entity;
    }

    /**
     * Returns the HttpRequestBase to be used by the HttpClient.
     *
     * @param httpMethod                a String with the http method to execute
     * @param uri                       a String with the uri
     * @param entity                    HttpEntity with the normalized payload
     * @return                          HttpRequestBase object
     * @throws MPRestException
     */
    private HttpRequestBase getRequestMethod(String httpMethod, String uri, HttpEntity entity) throws MPRestException {
        HttpRequestBase request = null;
        if (httpMethod.equals("GET")) {
            if (entity != null)
                throw new MPRestException("Not supported for this method.");
            request = new HttpGet(uri);
        } else if (httpMethod.equals("POST")) {
            if (entity == null)
                throw new MPRestException("Not supported for this method.");
            HttpPost post = new HttpPost(uri);
            post.setEntity(entity);
            request = post;
        } else if (httpMethod.equals("PUT")) {
            if (entity == null)
                throw new MPRestException("Not supported for this method.");
            HttpPut put = new HttpPut(uri);
            put.setEntity(entity);
            request = put;
        } else if (httpMethod.equals("DELETE")) {
            if (entity != null)
                throw new MPRestException("Not supported for this method.");
            request = new HttpDelete(uri);
        }
        return request;
    }

}