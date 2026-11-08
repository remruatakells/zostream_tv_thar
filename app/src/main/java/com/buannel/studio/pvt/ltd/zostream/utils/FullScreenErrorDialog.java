package com.buannel.studio.pvt.ltd.zostream.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FullScreenErrorDialog {

    public static void show(
            Context context,
            String title,
            String message,
            boolean cancelable,
            Runnable onRetry
    ) {

        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setCancelable(cancelable);

        // Root layout
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(40, 40, 40, 40);

        // Background gradient
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        Color.parseColor("#0F172A"), // dark top
                        Color.parseColor("#020617")  // darker bottom
                }
        );
        root.setBackground(bg);

        // Title
        TextView tvTitle = new TextView(context);
        tvTitle.setText(title != null ? title : "Error");
        tvTitle.setTextSize(24);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(30, 0, 30, 20);

        // Message
        TextView tvMessage = new TextView(context);
        tvMessage.setText(message != null ? message : "Something went wrong");
        tvMessage.setTextSize(16);
        tvMessage.setTextColor(Color.LTGRAY);
        tvMessage.setGravity(Gravity.CENTER);
        tvMessage.setPadding(30, 0, 30, 40);

        // Close Button
        Button btnClose = new Button(context);
        btnClose.setText("Close");
        btnClose.setTextColor(Color.WHITE);

        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setColor(Color.parseColor("#374151"));
        closeBg.setCornerRadius(20);
        btnClose.setBackground(closeBg);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Layout params
        LinearLayout.LayoutParams btnParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        btnParams.setMargins(0, 10, 0, 10);

        root.addView(tvTitle);
        root.addView(tvMessage);
        root.addView(btnClose, btnParams);

        dialog.setContentView(root);
        dialog.show();
    }
}