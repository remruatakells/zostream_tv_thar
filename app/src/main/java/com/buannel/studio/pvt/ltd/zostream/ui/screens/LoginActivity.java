package com.buannel.studio.pvt.ltd.zostream.ui.screens;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.buannel.studio.pvt.ltd.zostream.MainActivity;
import com.buannel.studio.pvt.ltd.zostream.R;
import com.buannel.studio.pvt.ltd.zostream.api.Api;
import com.buannel.studio.pvt.ltd.zostream.request.OTPVerifyRequest;
import com.buannel.studio.pvt.ltd.zostream.request.OtpRequest;
import com.buannel.studio.pvt.ltd.zostream.request.QrLoginRequest;
import com.buannel.studio.pvt.ltd.zostream.request.QrPaymentRequest;
import com.buannel.studio.pvt.ltd.zostream.response.ApiResponse;
import com.buannel.studio.pvt.ltd.zostream.response.QrLoginResponse;
import com.buannel.studio.pvt.ltd.zostream.utils.CountdownManager;
import com.buannel.studio.pvt.ltd.zostream.utils.DeviceUtils;
import com.buannel.studio.pvt.ltd.zostream.utils.QRUtils;
import com.buannel.studio.pvt.ltd.zostream.utils.SessionManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends FragmentActivity {

    EditText phoneInput, otpInput;
    LinearLayout loginBtn;
    TextView btnTxt;
    TextView textResend;

    CountdownManager otpCountdown;
    CountdownManager qrCountdown;
    boolean otpRequested = false;
    boolean timerRunning = false;
    private String responseUid;
    TextView qrBtn;
    TextView qrTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        otpCountdown = new CountdownManager();
        qrCountdown = new CountdownManager();

        phoneInput = findViewById(R.id.phoneInput);
        otpInput = findViewById(R.id.otpInput);
        loginBtn = findViewById(R.id.loginBtn);
        btnTxt = findViewById(R.id.btnTxt);
        textResend = findViewById(R.id.txtResend);
        qrBtn = findViewById(R.id.showQR);
        qrTimer = findViewById(R.id.qrCount);

        qrBtn.setOnClickListener(v -> initQR());

        loginBtn.setOnClickListener(v -> {

            if (!otpRequested) {
                initCredential(); // send OTP
            }
            else if (timerRunning) {
                verifyOtp(); // verify OTP
            }
            else {
                resendOtp(); // resend OTP
            }

        });
    }

    @SuppressLint("SetTextI18n")
    private void initQR() {

        qrBtn.setText("Loading QR");

        String deviceName = DeviceUtils.getDeviceName();
        String deviceId = DeviceUtils.getDeviceId(this);

        QrPaymentRequest request = new QrPaymentRequest(
                deviceId,     // deviceId
                null,         // movieId
                deviceName,   // deviceName
                "tv",         // deviceType
                null,         // amount
                null,         // currency
                null,         // planId
                null,         // appPaymentType
                null,         // paymentMethod
                null,         // paymentGateway
                null,         // transactionId
                null,         // note
                "login",      // type
                "",// status
                "",       // userId
                null,      // expiresAt
                null,
                null
        );

        Call<QrLoginResponse> call = Api.getApi().createQr(request);
        call.enqueue(new Callback<QrLoginResponse>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(Call<QrLoginResponse> call, Response<QrLoginResponse> response) {
                if (response.isSuccessful()) {
                    QrLoginResponse qrLoginResponse = response.body();
                    qrBtn.setVisibility(GONE);
                    listenFirebaseQrResponse(Objects.requireNonNull(qrLoginResponse).getToken());
                    startQrCountdown(); // API gives expires_in = 120 seconds
                } else {
                    qrBtn.setVisibility(VISIBLE);
                    qrBtn.setText("Retry");
                }
            }

            @Override
            public void onFailure(Call<QrLoginResponse> call, Throwable throwable) {
                qrBtn.setVisibility(VISIBLE);
                qrBtn.setText("Retry");
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void startQrCountdown() {

        qrCountdown.start(
                120,
                qrTimer,
                "QR expires in",
                new CountdownManager.Listener() {
                    @Override
                    public void onTick(String time, long millisRemaining) {
                        // optional
                    }

                    @Override
                    public void onFinish() {
                        qrTimer.setText("QR expired");
                        qrBtn.setVisibility(VISIBLE);
                        qrBtn.setText("Generate QR");
                    }
                }
        );
    }

    private void listenFirebaseQrResponse(@NotNull String token) {

        // Generate QR on ImageView
        QRUtils.generateQR(findViewById(R.id.imgQR), token);

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("qr_sessions")
                .child(token);

        ref.addValueEventListener(new ValueEventListener() {

            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) return;

                String status = snapshot.child("status").getValue(String.class);

                if ("completed".equals(status)) {

                    DataSnapshot data = snapshot.child("response").child("data");

                    String userId = data.child("uid").getValue(String.class);
                    String deviceId = data.child("device_id").getValue(String.class);
                    String accessToken = data.child("access_token").getValue(String.class);
                    String refreshToken = data.child("refresh_token").getValue(String.class);
                    String deviceName = data.child("device_name").getValue(String.class);
                    boolean isDeviceOwner = Boolean.TRUE.equals(data.child("is_owner_device").getValue(Boolean.class));

                    // ✅ Save user + token (from API)
                    saveUserSession(userId, accessToken, refreshToken, deviceName, deviceId, isDeviceOwner);

                    // Redirect to MainActivity
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);


                } else if ("failed".equals(status)) {

                    String message = snapshot.child("response").child("message").getValue(String.class);

                    Toast.makeText(LoginActivity.this,
                            message != null ? message : "Login failed",
                            Toast.LENGTH_SHORT).show();

                    qrBtn.setVisibility(VISIBLE);
                    qrBtn.setText("Retry");
                } else if ("pending".equals(status)) {
                    qrBtn.setVisibility(VISIBLE);
                    qrBtn.setText("Waiting");
                }
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onCancelled(@NonNull DatabaseError error) {qrBtn.setVisibility(VISIBLE);
                qrBtn.setVisibility(VISIBLE);
                qrBtn.setText("Failed");
            }
        });
    }

    private void initCredential() {

        if (phoneInput.getText().toString().isEmpty()) {
            phoneInput.setError("Phone number is required");
            phoneInput.requestFocus();
        } else {
            requestOTP(phoneInput.getText().toString());
        }
    }

    private void requestOTP(String phone) {
        String uid = DeviceUtils.generateUuid();

        OtpRequest otpRequest = new OtpRequest(uid, phone);

        Call<ApiResponse> call = Api.getApi().requestOtp(otpRequest);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    Toast.makeText(LoginActivity.this, apiResponse.getMessage(), Toast.LENGTH_LONG).show();

                    otpInput.setVisibility(VISIBLE);
                    textResend.setVisibility(VISIBLE);

                    otpRequested = true;
                    timerRunning = true;

                    responseUid = apiResponse.getUserId();

                    btnTxt.setText("Verify OTP");

                    startOtpCountdown();

                } else {
                    Toast.makeText(LoginActivity.this, "Failed to send OTP (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e("API_ERROR", t.getMessage(), t);
                Toast.makeText(LoginActivity.this, "Error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startOtpCountdown() {

        otpCountdown.start(
                300,
                textResend,
                "Resend in",
                new CountdownManager.Listener() {
                    @Override
                    public void onTick(String time, long millisRemaining) {
                        btnTxt.setText("Verify OTP");
                    }

                    @Override
                    public void onFinish() {
                        timerRunning = false;
                        btnTxt.setText("Resend OTP");
                    }
                }
        );
    }

    private void verifyOtp() {

        String otp = otpInput.getText().toString();

        if (otp.isEmpty()) {
            otpInput.setError("Enter OTP");
            otpInput.requestFocus();
            return;
        }

        Toast.makeText(this, "Verify OTP: " + otp, Toast.LENGTH_SHORT).show();

        String deviceName = DeviceUtils.getDeviceName();
        String deviceId = DeviceUtils.getDeviceId(this);
        OTPVerifyRequest otpVerifyRequest = new OTPVerifyRequest(responseUid, otp, deviceName, deviceId, "tv");

        Call<ApiResponse> call = Api.getApi().verifyOtp(otpVerifyRequest);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();

                    Toast.makeText(LoginActivity.this, apiResponse.getMessage(), Toast.LENGTH_LONG).show();

                    if (apiResponse.getStatus().equals("success")) {
                        ApiResponse.Data data = apiResponse.getData();

                        if (data != null) {
                            String newUid = data.getUid();
                            String accessToken = data.getAccessToken();
                            String refreshToken = data.getRefreshToken();
                            String deviceName = data.getDeviceName();
                            String deviceId = data.getDeviceId();
                            boolean isDeviceOwner = data.getIsOwnerDevice();

                            // ✅ Save user + token (from API)
                            saveUserSession(newUid, accessToken, refreshToken, deviceName, deviceId, isDeviceOwner);

                            // Redirect to MainActivity
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            Toast.makeText(LoginActivity.this, "Invalid API response: data missing", Toast.LENGTH_SHORT).show();
                        }
                    }

                } else {
                    Toast.makeText(LoginActivity.this,
                            "Failed to verify OTP (" + response.code() + ")",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e("API_ERROR", t.getMessage(), t);
                Toast.makeText(LoginActivity.this,
                        "Error: " + t.getLocalizedMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resendOtp() {

        requestOTP(phoneInput.getText().toString());

    }

    private void saveUserSession(String uid, String accessToken, String refreshToken, String deviceName, String deviceId, boolean isDeviceOwner) {
        SharedPreferences prefs = getSharedPreferences("ZoStreamPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("uid", uid);
        editor.putString("access_token", accessToken);
        editor.putString("refresh_token", refreshToken);
        editor.putString("device_name", deviceName);
        editor.putString("device_id", deviceId);
        editor.putBoolean("ageRestriction", false);
        editor.putBoolean("isLoggedIn", true);
        editor.putBoolean("isDeviceOwner", isDeviceOwner);
        editor.apply();

        Log.d("SESSION", "Saved → UID=" + uid +
                ", AccessToken=" + accessToken +
                ", RefreshToken=" + refreshToken);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (otpCountdown != null) otpCountdown.stop();
        if (qrCountdown != null) qrCountdown.stop();
    }
}