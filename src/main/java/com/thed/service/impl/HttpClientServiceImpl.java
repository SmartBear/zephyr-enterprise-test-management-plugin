package com.thed.service.impl;

import com.thed.service.HttpClientService;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by prashant on 18/6/19.
 */
public class HttpClientServiceImpl implements HttpClientService {

    private BasicCookieStore cookieStore; // This stores cookies for created by client or set by server side.

    public HttpClientServiceImpl() {
        cookieStore = new BasicCookieStore();
    }

    private String convertInputStreamToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        byte[] byteArray = buffer.toByteArray();

        return new String(byteArray, StandardCharsets.UTF_8);
    }

    public String authenticationGetRequest(String url, String username, String password) {
        if(StringUtils.isEmpty(url)) {
            return null;
        }

        try{
            UsernamePasswordCredentials usernamePasswordCredentials = new UsernamePasswordCredentials(username, password);

            HttpClient httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore)
                    .build();
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader(new BasicScheme().authenticate(usernamePasswordCredentials, httpGet, null));

            HttpResponse response = httpClient.execute(httpGet);

            if(response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() <= 299) {
                return convertInputStreamToString(response.getEntity().getContent());
            }

        }catch(Exception e) {

        }

        return null;

    }

    @Override
    public String getRequest(String url) {
        if(StringUtils.isEmpty(url)) {
            return null;
        }

        try{
            HttpClient httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();
            HttpGet httpGet = new HttpGet(url);

            HttpResponse response = httpClient.execute(httpGet);
            if(response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() <= 299) {
                return convertInputStreamToString(response.getEntity().getContent());
            }

        }catch(Exception e) {

        }

        return null;

    }

    @Override
    public void postRequest() {
        System.out.println("post request in impl");
    }

    @Override
    public void putRequest() {
        System.out.println("put request in impl");
    }

    @Override
    public void clear() {
        cookieStore.clear();
    }
}
