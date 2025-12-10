package com.example.finanzas.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.finanzas.R;

/**
 * Helper to trigger an email composer for important processes inside the app.
 */
public final class EmailHelper {

    private EmailHelper() { }

    public static void sendImportantEmail(@NonNull Context context, @NonNull String subject, @NonNull String body) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(Intent.createChooser(intent,
                    context.getString(R.string.email_process_title)));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(context, R.string.email_process_error, Toast.LENGTH_SHORT).show();
        }
    }
}
