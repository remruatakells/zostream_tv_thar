package com.buannel.studio.pvt.ltd.zostream.ui.screens;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;

import com.buannel.studio.pvt.ltd.zostream.MainActivity;
import com.buannel.studio.pvt.ltd.zostream.api.Api;
import com.buannel.studio.pvt.ltd.zostream.api.ApiInterface;
import com.buannel.studio.pvt.ltd.zostream.request.OTPVerifyRequest;
import com.buannel.studio.pvt.ltd.zostream.request.OtpRequest;
import com.buannel.studio.pvt.ltd.zostream.request.QrPaymentRequest;
import com.buannel.studio.pvt.ltd.zostream.response.ApiResponse;
import com.buannel.studio.pvt.ltd.zostream.response.QrLoginResponse;
import com.buannel.studio.pvt.ltd.zostream.utils.CountdownManager;
import com.buannel.studio.pvt.ltd.zostream.utils.DeviceUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends ComponentActivity {

    private final LoginUiState uiState = new LoginUiState();
    private final List<LoginCountryOption> countryOptions = new ArrayList<>();
    private CountdownManager otpCountdown;
    private CountdownManager qrCountdown;
    private boolean otpRequested = false;
    private boolean timerRunning = false;
    private String responseUid;
    private String selectedCountryCode = "91";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Api.init(this);

        otpCountdown = new CountdownManager();
        qrCountdown = new CountdownManager();

        loadCountries();
        updateCountryPicker();

        String loginNotice = getIntent().getStringExtra("login_notice");
        if (loginNotice != null && !loginNotice.trim().isEmpty()) {
            Toast.makeText(this, loginNotice.trim(), Toast.LENGTH_LONG).show();
        }

        LoginComposeHost.install(this, uiState, countryOptions, new LoginCallbacks() {
            @Override
            public void onShowQr() {
                initQR();
            }

            @Override
            public void onSubmitLogin(@NotNull String phone, @NotNull String otp) {
                if (!otpRequested) {
                    initCredential(phone);
                } else if (timerRunning) {
                    verifyOtp(otp);
                } else {
                    resendOtp(phone);
                }
            }

            @Override
            public void onCountrySelected(@NotNull String countryCode) {
                selectedCountryCode = countryCode;
                updateCountryPicker();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void initQR() {
        uiState.setQrButtonText("Loading QR");
        uiState.setQrButtonVisible(true);

        String deviceName = DeviceUtils.getDeviceName();
        String deviceId = DeviceUtils.getDeviceId(this);

        QrPaymentRequest request = new QrPaymentRequest(
                deviceId,
                null,
                deviceName,
                "tv",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "login",
                "",
                "",
                null,
                null,
                null
        );

        ApiInterface api = getApiOrShowError();
        if (api == null) return;

        Call<QrLoginResponse> call = api.createQr(request);
        call.enqueue(new Callback<QrLoginResponse>() {
            @Override
            public void onResponse(Call<QrLoginResponse> call, Response<QrLoginResponse> response) {
                if (response.isSuccessful()) {
                    QrLoginResponse qrLoginResponse = response.body();
                    String token = Objects.requireNonNull(qrLoginResponse).getToken();
                    uiState.setQrButtonVisible(false);
                    uiState.setQrToken(token);
                    listenFirebaseQrResponse(token);
                    startQrCountdown();
                } else {
                    uiState.setQrButtonVisible(true);
                    uiState.setQrButtonText("Retry");
                }
            }

            @Override
            public void onFailure(Call<QrLoginResponse> call, Throwable throwable) {
                uiState.setQrButtonVisible(true);
                uiState.setQrButtonText("Retry");
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void startQrCountdown() {
        qrCountdown.start(
                120,
                null,
                "QR expires in",
                new CountdownManager.Listener() {
                    @Override
                    public void onTick(String time, long millisRemaining) {
                        uiState.setQrTimerText("QR expires in " + time);
                    }

                    @Override
                    public void onFinish() {
                        uiState.setQrTimerText("QR expired");
                        uiState.setQrButtonVisible(true);
                        uiState.setQrButtonText("Generate QR");
                    }
                }
        );
    }

    private void listenFirebaseQrResponse(@NotNull String token) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("qr_sessions")
                .child(token);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String status = firebaseString(snapshot.child("status"));

                if ("completed".equals(status)) {
                    DataSnapshot data = snapshot.child("response").child("data");

                    String userId = firebaseString(data.child("uid"));
                    String deviceId = firebaseString(data.child("device_id"));
                    String accessToken = firebaseString(data.child("access_token"));
                    String refreshToken = firebaseString(data.child("refresh_token"));
                    String deviceName = firebaseString(data.child("device_name"));
                    boolean isDeviceOwner = Boolean.TRUE.equals(data.child("is_owner_device").getValue(Boolean.class));

                    saveUserSession(userId, accessToken, refreshToken, deviceName, deviceId, isDeviceOwner);

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else if ("failed".equals(status)) {
                    String message = firebaseString(snapshot.child("response").child("message"));

                    Toast.makeText(LoginActivity.this,
                            message != null ? message : "Login failed",
                            Toast.LENGTH_SHORT).show();

                    uiState.setQrButtonVisible(true);
                    uiState.setQrButtonText("Retry");
                } else if ("pending".equals(status)) {
                    uiState.setQrButtonVisible(true);
                    uiState.setQrButtonText("Waiting");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                uiState.setQrButtonVisible(true);
                uiState.setQrButtonText("Failed");
            }
        });
    }

    private void loadCountries() {
        countryOptions.clear();

        try (InputStream inputStream = getAssets().open("country.json")) {
            byte[] buffer = new byte[inputStream.available()];
            int ignored = inputStream.read(buffer);
            JSONArray countries = new JSONArray(new String(buffer, StandardCharsets.UTF_8));

            for (int i = 0; i < countries.length(); i++) {
                JSONObject item = countries.getJSONObject(i);
                String name = item.optString("name");
                String dialCode = item.optString("dial_code");
                String emoji = item.optString("emoji");
                String code = normalizeCountryCode(dialCode);

                if (!code.isEmpty()) {
                    LoginCountryOption option = new LoginCountryOption(name, emoji, code);
                    countryOptions.add(option);
                    if ("91".equals(code)) {
                        selectedCountryCode = code;
                    }
                }
            }
        } catch (IOException | JSONException e) {
            Log.e("COUNTRY_JSON", "Unable to load country.json", e);
            countryOptions.add(new LoginCountryOption("India", "🇮🇳", "91"));
            selectedCountryCode = "91";
        }
    }

    private void updateCountryPicker() {
        uiState.setSelectedCountryCode(selectedCountryCode);
    }

    private String normalizeCountryCode(String dialCode) {
        if (dialCode == null) {
            return "";
        }
        return dialCode.replace("+", "").replaceAll("\\D", "");
    }

    private void initCredential(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            uiState.setPhoneError("Phone number is required");
        } else {
            uiState.setPhoneError(null);
            requestOTP(phone.trim());
        }
    }

    private void requestOTP(String phone) {
        String uid = DeviceUtils.generateUuid();

        OtpRequest otpRequest = new OtpRequest(uid, phone, selectedCountryCode);

        ApiInterface api = getApiOrShowError();
        if (api == null) return;

        Call<ApiResponse> call = api.requestOtp(otpRequest);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    Toast.makeText(LoginActivity.this, apiResponse.getMessage(), Toast.LENGTH_LONG).show();

                    uiState.setOtpVisible(true);
                    uiState.setResendVisible(true);

                    otpRequested = true;
                    timerRunning = true;

                    responseUid = apiResponse.getUserId();

                    uiState.setLoginButtonText("Verify OTP");

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
                null,
                "Resend in",
                new CountdownManager.Listener() {
                    @Override
                    public void onTick(String time, long millisRemaining) {
                        uiState.setLoginButtonText("Verify OTP");
                        uiState.setResendText("Resend in " + time);
                    }

                    @Override
                    public void onFinish() {
                        timerRunning = false;
                        uiState.setLoginButtonText("Resend OTP");
                    }
                }
        );
    }

    private void verifyOtp(String otp) {
        if (otp == null || otp.trim().isEmpty()) {
            uiState.setOtpError("Enter OTP");
            return;
        }

        uiState.setOtpError(null);
        Toast.makeText(this, "Verify OTP: " + otp, Toast.LENGTH_SHORT).show();

        String deviceName = DeviceUtils.getDeviceName();
        String deviceId = DeviceUtils.getDeviceId(this);
        OTPVerifyRequest otpVerifyRequest = new OTPVerifyRequest(responseUid, otp.trim(), deviceName, deviceId, "tv");

        ApiInterface api = getApiOrShowError();
        if (api == null) return;

        Call<ApiResponse> call = api.verifyOtp(otpVerifyRequest);
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

                            saveUserSession(newUid, accessToken, refreshToken, deviceName, deviceId, isDeviceOwner);

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

    private void resendOtp(String phone) {
        requestOTP(phone);
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

    private ApiInterface getApiOrShowError() {
        try {
            return Api.getApi();
        } catch (IllegalStateException e) {
            Toast.makeText(
                    this,
                    "App verification is not ready. Please close and reopen the app.",
                    Toast.LENGTH_LONG
            ).show();
            uiState.setQrButtonVisible(true);
            uiState.setQrButtonText("Retry");
            return null;
        }
    }

    private static String firebaseString(DataSnapshot snapshot) {
        Object value = snapshot.getValue();
        if (value instanceof String) return (String) value;
        if (value instanceof Number || value instanceof Boolean) return String.valueOf(value);
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (otpCountdown != null) otpCountdown.stop();
        if (qrCountdown != null) qrCountdown.stop();
    }
}
