package com.example.finanzas.data.api;

import android.content.Context;

import com.example.finanzas.data.local.LocalRepository;
import com.example.finanzas.data.model.User;

public class UserService {

    public interface MeCb { void onOk(int id, String nombre, String email); void onFail(); }
    public interface SimpleCb { void onOk(); void onFail(); }

    public static void getMe(Context ctx, String email, MeCb cb){
        if (ctx == null || cb == null || email == null || email.trim().isEmpty()) { cb.onFail(); return; }
        User user = LocalRepository.getInstance(ctx).getUserByEmailBlocking(email);
        if (user == null) cb.onFail(); else cb.onOk((int) user.id, user.nombre, user.email);
    }

    public static void changePassword(Context ctx, String email, String oldPass, String newPass, SimpleCb cb){
        if (ctx == null || cb == null || email == null) { if (cb!=null) cb.onFail(); return; }
        boolean ok = LocalRepository.getInstance(ctx).changePasswordBlocking(email, oldPass, newPass);
        if (ok) cb.onOk(); else cb.onFail();
    }
}
