package com.demo.demogalleries;

import android.content.Context;
import android.net.Uri;

import com.demo.demogalleries.utils.HttpClientHelper;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import java.util.Observer;

/**
 * Created by caominhvu on 2/26/16.
 */
public class HttpClientReq extends HttpClientHelper implements IHttpClientReq {
    public HttpClientReq(Context context, Observer observer) {
        super(context, observer);
    }

    @Override
    public void getPhotosOfCategory(String category) {
        category = category.replace(" ", "%20");
        final String URI = SERVER_URL + "/photos?only=" + category + "&consumer_key=" + KEY + "&sort=created_at";
        HttpRequest httpRequest = null;
        HttpHost httpHost = null;
        HttpContext httpContext = null;

        Uri uri = Uri.parse(URI);
        int port = uri.getPort();
        if(port == -1) {
            port = uri.getScheme().contains(HttpClientHelper.SCHEME_SECURE) ? 443 : 80;
        }

        httpHost = new HttpHost(uri.getHost(), port, uri.getScheme());
        httpRequest = new BasicHttpEntityEnclosingRequest(HttpGet.METHOD_NAME, URI, HttpVersion.HTTP_1_1);

        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 10*1000);
        HttpConnectionParams.setSoTimeout(params, 10*1000);
        httpRequest.setParams(params);

        execute(httpHost, httpRequest, httpContext);
    }

    @Override
    public void getPhotoDetail(String id) {
        final String URI = SERVER_URL + "/photos/" + id + "?image_size=3" + "&consumer_key=" + KEY;
        HttpRequest httpRequest = null;
        HttpHost httpHost = null;
        HttpContext httpContext = null;

        Uri uri = Uri.parse(URI);
        int port = uri.getPort();
        if(port == -1) {
            port = uri.getScheme().contains(HttpClientHelper.SCHEME_SECURE) ? 443 : 80;
        }

        httpHost = new HttpHost(uri.getHost(), port, uri.getScheme());
        httpRequest = new BasicHttpEntityEnclosingRequest(HttpGet.METHOD_NAME, URI, HttpVersion.HTTP_1_1);

        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 10*1000);
        HttpConnectionParams.setSoTimeout(params, 10*1000);
        httpRequest.setParams(params);

        execute(httpHost, httpRequest, httpContext);
    }
}
