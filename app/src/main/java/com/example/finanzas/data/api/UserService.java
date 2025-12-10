package com.example.finanzas.data.api;

import android.content.Context;

import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class UserService {

    public interface MeCb { void onOk(int id, String nombre, String email); void onFail(); }
    public interface SimpleCb { void onOk(); void onFail(); }

    public static void getMe(Context ctx, MeCb cb){
        ApiClient.refreshAuthHeader(ctx);
        ApiClient.get(ApiConfig.USER_ME, new JsonHttpResponseHandler() {
            @Override public void onSuccess(int c, Header[] h, JSONObject res) {
                if (res.optBoolean("ok", false)) {
                    JSONObject u = res.optJSONObject("user");
                    if (u != null) cb.onOk(u.optInt("id",0), u.optString("nombre",""), u.optString("email",""));
                    else cb.onFail();
                } else cb.onFail();
            }
            @Override public void onFailure(int c, Header[] h, Throwable t, JSONObject e) { cb.onFail(); }
        });
    }

    public static void changePassword(Context ctx, String oldPass, String newPass, SimpleCb cb){
        ApiClient.refreshAuthHeader(ctx);
        String body = "{\"old_password\":\""+oldPass+"\",\"new_password\":\""+newPass+"\"}";
        ApiClient.postJson(ApiConfig.AUTH_CHANGE_PASS, body, new JsonHttpResponseHandler() {
            @Override public void onSuccess(int c, Header[] h, JSONObject res) {
                if (res.optBoolean("ok", false)) cb.onOk(); else cb.onFail();
            }
            @Override public void onFailure(int c, Header[] h, Throwable t, JSONObject e) { cb.onFail(); }
        });
    }
}
