package com.example.finanzas.data.api;

import android.content.Context;
import android.util.Log;

import com.example.finanzas.util.Prefs;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.message.BasicHeader;
import cz.msebera.android.httpclient.protocol.HTTP;

public class ApiClient {
    private static final AsyncHttpClient client = new AsyncHttpClient();

    static {
        client.setTimeout(10_000);
        client.setConnectTimeout(10_000);
        client.setResponseTimeout(10_000);
        client.addHeader("Accept", "application/json");
    }

    public static void refreshAuthHeader(Context ctx) {

        client.removeAllHeaders();
        client.addHeader("Accept", "application/json");
        String tok = Prefs.getToken(ctx);
        if (tok != null && !tok.isEmpty()) {
            client.addHeader("Authorization", "Bearer " + tok);
        }
    }

    public static void get(String path, AsyncHttpResponseHandler handler) {
        String url = ApiConfig.BASE_URL + path;
        Log.d("ApiClient", "GET " + url);
        client.get(url, handler);
    }

    public static void get(String path, RequestParams params, AsyncHttpResponseHandler handler) {
        String url = ApiConfig.BASE_URL + path;
        Log.d("ApiClient", "GET " + url + (params != null ? ("?" + params.toString()) : ""));
        client.get(url, params, handler);
    }

    public static void postJson(String path, String jsonBody, JsonHttpResponseHandler handler) {
        String url = ApiConfig.BASE_URL + path;
        Log.d("ApiClient", "POST " + url + " body=" + jsonBody);
        try {
            StringEntity entity = new StringEntity(jsonBody, "UTF-8");
            entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            client.post(null, url, entity, "application/json", handler);
        } catch (Exception e) {
            Log.e("ApiClient", "postJson error", e);
        }
    }
}
