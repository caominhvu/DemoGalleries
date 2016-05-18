package com.demo.demogalleries.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Observable;
import java.util.Observer;

import javax.net.ssl.TrustManagerFactory;

public abstract class HttpClientHelper extends Observable {
    private static final String TAG = "HttpClientHelper";

    final public static String SCHEME_UNSECURE = "http";
    final public static String SCHEME_SECURE = "https";

    protected Context mContext;
    private String mCertificate;

    public HttpClientHelper() {
    }

    public HttpClientHelper(Context context, Observer observer, String certificate) {
        mCertificate = certificate;
        mContext = context;
        if (observer != null) {
            addObserver(observer);
        }
    }

    public HttpClientHelper(Context context, Observer observer) {
        this(context, observer, null);
    }

    protected void execute(HttpHost httpHost, HttpRequest httpRequest,
                           HttpContext httpContext) {
        execute(httpHost, httpRequest, httpContext, null);
    }

    protected void execute(HttpHost httpHost, HttpRequest httpRequest,
                           HttpContext httpContext, IDataResponseParser parser) {
        if (mContext != null) {
            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni == null || !ni.isConnected()) {
                Object data = null;
                try {
                    data = new JSONObject(new JSONStringer().object().key("error_code").value(Error.NETWORK).endObject().toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                setChanged();
                notifyObservers(data.toString());
                deleteObservers();
            } else {
                new HttpTask().execute(httpHost, httpRequest, httpContext, parser);
            }
        } else {
            setChanged();
            notifyObservers(null);
            deleteObservers();
        }
    }


    private class HttpTask extends AsyncTask<Object, Void, Object> {
        private static final int HTTP_HOST = 0;
        private static final int HTTP_REQUEST = 1;
        private static final int HTTP_CONTEXT = 2;
        private static final int DATA_PARSER = 3;
        private static final int TOTAL_PARAMS = 4;

        boolean isStatusCodeErr(int statusCode) {
            return (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_CREATED && statusCode != HttpStatus.SC_ACCEPTED && statusCode != HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION && statusCode != HttpStatus.SC_NO_CONTENT);
        }

        private DefaultHttpClient getHttpClient(String schema,
                                                int port) {

            final int DEFAULT_SECURE_PORT = 443;
            final int DEFAULT_UNSECURE_PORT = 80;

            DefaultHttpClient defaultHttpClient = null;
            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
            SchemeRegistry scheme = new SchemeRegistry();
            if (schema.equals(SCHEME_UNSECURE)) {
                scheme.register(new Scheme(SCHEME_UNSECURE, PlainSocketFactory
                        .getSocketFactory(), port != -1 ? port : DEFAULT_UNSECURE_PORT));
            } else if (schema.equals(SCHEME_SECURE)) {
                if(mCertificate != null) {
                    try {
                        Certificate ca = CertificateFactory.getInstance("X.509").generateCertificate(IOUtils.toInputStream(mCertificate));

                        String keyStoreType = KeyStore.getDefaultType();
                        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                        keyStore.load(null, null);
                        keyStore.setCertificateEntry("ca", ca);

                        // Create a TrustManager that trusts the CAs in our KeyStore
                        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                        tmf.init(keyStore);

                        // Create an SSLContext that uses our TrustManager
//                        SSLContext sslContext = SSLContext.getInstance("TLS");
//                        sslContext.init(null, tmf.getTrustManagers(), null);
                        scheme.register(new Scheme(SCHEME_SECURE, new SSLSocketFactory(keyStore), port != -1 ? port : DEFAULT_SECURE_PORT));
                    } catch (Exception e) {
                        e.printStackTrace();
                        scheme.register(new Scheme(SCHEME_SECURE, SSLSocketFactory
                                .getSocketFactory(), port != -1 ? port : DEFAULT_SECURE_PORT));
                    }
                } else {
                    scheme.register(new Scheme(SCHEME_SECURE, SSLSocketFactory
                            .getSocketFactory(), port != -1 ? port : DEFAULT_SECURE_PORT));
                }
            }
            ClientConnectionManager clntConnMgr = new ThreadSafeClientConnManager(
                    params, scheme);
            return new DefaultHttpClient(clntConnMgr, params);
        }

        @Override
        protected Object doInBackground(Object... params) {
            Object result = null;
            String errMsg = null;
            Error errCode = Error.NONE;

            IDataResponseParser parser = null;
            if (params.length >= TOTAL_PARAMS) {
                parser = (IDataResponseParser) params[DATA_PARSER];
            }
            HttpHost httpHost = (HttpHost) params[HTTP_HOST];
            HttpRequest httpRequest = (HttpRequest) params[HTTP_REQUEST];
            HttpContext httpContext = (HttpContext) params[HTTP_CONTEXT];

            DefaultHttpClient httpClient = getHttpClient(httpHost.getSchemeName(), httpHost.getPort());

            InputStream inputStream = null;
            try {
                HttpResponse response = httpClient.execute(httpHost, httpRequest, httpContext);

                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();

                if (!isStatusCodeErr(statusCode) && entity != null) {
                    inputStream = entity.getContent();
                } else {
                    errCode = Error.SERVER_COMM;
                }
            } catch (Exception e) {
                e.printStackTrace();
                errCode = Error.NETWORK;
                errMsg = e.getMessage();
            }
            try {
                if (parser != null) {
                    result = parser.parseData(inputStream, errCode, errMsg);
                } else if (inputStream != null) {
                    result = IOUtils.toString(inputStream);
                }
            } catch (IOException e) {
                e.printStackTrace();
                result = null;
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(Object result) {
            super.onPostExecute(result);
            setChanged();
            notifyObservers(result);
            deleteObservers();
        }
    }
}
