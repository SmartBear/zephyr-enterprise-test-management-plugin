package com.thed.service.impl;

import com.thed.service.HttpClientService;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by prashant on 18/6/19.
 */
public class HttpClientServiceImpl implements HttpClientService {

    private BasicCookieStore cookieStore; // This stores cookies for created by client or set by server side.
    private List<Header> headers;
    private CloseableHttpClient httpClient;

    public HttpClientServiceImpl() {
        cookieStore = new BasicCookieStore();
        headers = new ArrayList<>();
        if(httpClient == null) {
            initHttpClient();
        }
    }

    private void initHttpClient() {
        HttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultCookieStore(cookieStore)
                .build();
    }

    @Override
    public List<Header> getHeaders() {
        return headers;
    }

    @Override
    public void addHeader(Header header) {
        this.headers.add(header);
    }

    @Override
    public BasicCookieStore getCookieStore() {
        return cookieStore;
    }

    @Override
    public void setCookieStore(BasicCookieStore cookieStore) {
        this.cookieStore = cookieStore;
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

    @Override
    public String getRequest(String url) throws IOException {
        if(StringUtils.isEmpty(url)) {
            return null;
        }

        String result = "";
        CloseableHttpClient httpClient = getHttpClient();
        HttpGet httpGet = new HttpGet(url);
        for (Header header:headers){
            httpGet.addHeader(header);
        }

        CloseableHttpResponse response = httpClient.execute(httpGet);
        try {
            if(response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() <= 299) {
                result = convertInputStreamToString(response.getEntity().getContent());
            } else {
                throw new HttpResponseException(response.getStatusLine().getStatusCode(), "GET: " + url + "\n" + "Response:" + convertInputStreamToString(response.getEntity().getContent()));
            }
        } finally {
            response.close();
            httpGet.releaseConnection();
        }

        return result;
    }

    @Override
    public String postRequest(String url, String content) throws IOException {
        StringEntity stringEntity = null;
        if(!StringUtils.isEmpty(content)) {
            stringEntity = new StringEntity(content, ContentType.APPLICATION_JSON);
        }
        return postRequest(url, stringEntity);
    }

    @Override
    public String postRequest(String url, HttpEntity httpEntity) throws IOException {
        if(StringUtils.isEmpty(url)) {
            return null;
        }

        String result = "";
        CloseableHttpClient httpClient = getHttpClient();
        HttpPost httpPost = new HttpPost(url);

        for (Header header:headers){
            httpPost.addHeader(header);
        }

        if(httpEntity != null) {
            httpPost.setEntity(httpEntity);
        }

        CloseableHttpResponse response = httpClient.execute(httpPost);
        try {
            if(response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() <= 299) {
                result = convertInputStreamToString(response.getEntity().getContent());
            } else {
                throw new HttpResponseException(response.getStatusLine().getStatusCode(), "POST: " + url
                        + "\n" + "Payload: " + (httpEntity != null ? convertInputStreamToString(httpEntity.getContent()) : "")
                        + "\nResponse: " + convertInputStreamToString(response.getEntity().getContent()));
            }
        } finally {
            response.close();
            httpPost.releaseConnection();
        }
        return result;
    }

    @Override
    public String putRequest(String url, String content) throws IOException {
        if(StringUtils.isEmpty(url)) {
            return null;
        }

        String result = "";
        CloseableHttpClient httpClient = getHttpClient();
        HttpPut httpPut = new HttpPut(url);

        for (Header header:headers){
            httpPut.addHeader(header);
        }

        if(!StringUtils.isEmpty(content)) {
            StringEntity stringEntity = new StringEntity(content, ContentType.APPLICATION_JSON);
            httpPut.setEntity(stringEntity);
        }

        CloseableHttpResponse response = httpClient.execute(httpPut);
        try {
            if(response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() <= 299) {
                result =  convertInputStreamToString(response.getEntity().getContent());
            } else {
                throw new HttpResponseException(response.getStatusLine().getStatusCode(), "PUT: " + url
                        + "\n" + "Payload: " + content
                        + "\nResponse: " + convertInputStreamToString(response.getEntity().getContent()));
            }
        } finally {
            response.close();
            httpPut.releaseConnection();
        }
        return result;
    }

    @Override
    public void clear() {
        cookieStore.clear();
        headers.clear();
    }

    private CloseableHttpClient getHttpClient() {
        if(httpClient == null) {
            initHttpClient();
        }
        return httpClient;
    }

    public void closeHttpClient() throws IOException {
        httpClient.close();
        httpClient = null;
    }
}
