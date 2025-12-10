package com.example.finanzas.data.api;

import android.content.Context;

import com.example.finanzas.data.local.LocalRepository;

public class AuthService {

    public interface Callback {
        void onSuccess(String token, int userId, String nombre, String email);
        void onError();
    }

    public static void login(Context ctx, String email, String pass, Callback cb) {
        LocalRepository.UserHolder holder = new LocalRepository.UserHolder();
        boolean ok = LocalRepository.getInstance(ctx).login(email, pass, holder);
        if (ok) {
            cb.onSuccess("local-token", holder.id, holder.nombre, holder.email);
        } else {
            cb.onError();
        }
    }

    public static void register(Context ctx, String nombre, String email, String pass, Callback cb) {
        int[] outId = new int[1];
        boolean ok = LocalRepository.getInstance(ctx).registerUser(nombre, email, pass, outId);
        if (ok) {
            cb.onSuccess("local-token", outId[0], nombre, email);
        } else {
            cb.onError();
        }
    }
}
