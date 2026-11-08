package com.buannel.studio.pvt.ltd.zostream.utils;

import android.annotation.SuppressLint;
import android.os.CountDownTimer;
import android.widget.TextView;

import java.util.Locale;

public class CountdownManager {

    public interface Listener {
        void onTick(String formattedTime, long millisRemaining);
        void onFinish();
    }

    private CountDownTimer timer;

    public void start(int totalSeconds, TextView targetView, String prefix, Listener listener) {

        stop(); // cancel existing timer

        timer = new CountDownTimer(totalSeconds * 1000L, 1000) {

            @SuppressLint("SetTextI18n")
            @Override
            public void onTick(long millisUntilFinished) {

                int seconds = (int) (millisUntilFinished / 1000);
                int minutes = seconds / 60;
                int remainingSeconds = seconds % 60;

                String time = String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds);

                if (targetView != null) {
                    targetView.setText(prefix + " " + time);
                }

                if (listener != null) {
                    listener.onTick(time, millisUntilFinished);
                }
            }

            @Override
            public void onFinish() {
                if (listener != null) {
                    listener.onFinish();
                }
            }

        }.start();
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}