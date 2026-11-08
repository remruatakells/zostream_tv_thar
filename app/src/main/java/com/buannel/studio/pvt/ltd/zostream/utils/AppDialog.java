package com.buannel.studio.pvt.ltd.zostream.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.buannel.studio.pvt.ltd.zostream.R;

import java.lang.ref.WeakReference;

public class AppDialog {

    private static boolean showing = false;

    public static void show(Context context,
                            int icon,
                            String title,
                            String message,
                            boolean showCancel,
                            boolean force,
                            Runnable okAction,
                            Runnable cancelAction) {
        show(
                context,
                icon,
                title,
                message,
                showCancel,
                force,
                "Ok",
                "Cancel",
                okAction,
                cancelAction
        );
    }

    public static void show(Context context,
                            int icon,
                            String title,
                            String message,
                            boolean showCancel,
                            boolean force,
                            String okText,
                            String cancelText,
                            Runnable okAction,
                            Runnable cancelAction) {

        Handler handler = new Handler(Looper.getMainLooper());

        handler.post(() -> {

            if (showing) return;
            if (!(context instanceof Activity activity)) return;

            if (activity.isFinishing() || activity.isDestroyed()) return;

            showing = true;

            Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            View view = LayoutInflater.from(activity).inflate(R.layout.dialog_app, null);
            dialog.setContentView(view);

            Window window = dialog.getWindow();
            if (window != null) {

                window.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CC000000")));

                window.setLayout(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT
                );
            }

            ImageView iconView = view.findViewById(R.id.dialogIcon);
            TextView titleView = view.findViewById(R.id.dialogTitle);
            TextView msgView = view.findViewById(R.id.dialogMessage);

            LinearLayout ok = view.findViewById(R.id.dialogOk);
            LinearLayout cancel = view.findViewById(R.id.dialogCancel);
            TextView okTextView = (TextView) ok.getChildAt(0);
            TextView cancelTextView = (TextView) cancel.getChildAt(0);

            iconView.setImageResource(icon);
            titleView.setText(title);
            msgView.setText(message);
            okTextView.setText(okText);
            cancelTextView.setText(cancelText);

            if (showCancel) {
                cancel.setVisibility(View.VISIBLE);
            } else {
                cancel.setVisibility(View.GONE);
            }

            // Make buttons focusable for TV
            ok.setFocusable(true);
            ok.setFocusableInTouchMode(true);

            cancel.setFocusable(true);
            cancel.setFocusableInTouchMode(true);

            ok.requestFocus(); // initial focus for TV

            WeakReference<Runnable> okRef = new WeakReference<>(okAction);
            WeakReference<Runnable> cancelRef = new WeakReference<>(cancelAction);

            ok.setOnClickListener(v -> {

                dialog.dismiss();
                showing = false;

                Runnable r = okRef.get();
                if (r != null) r.run();

            });

            cancel.setOnClickListener(v -> {

                dialog.dismiss();
                showing = false;

                Runnable r = cancelRef.get();
                if (r != null) r.run();

            });

            dialog.setCancelable(!force);
            dialog.setCanceledOnTouchOutside(!force);

            dialog.setOnDismissListener(d -> showing = false);

            dialog.show();
        });
    }
}
