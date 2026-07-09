package com.example.smsgateway;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.security.SecureRandom;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String OTP_TEMPLATE = "Your OTP code is %s";
    private static final int OTP_MIN = 100000;
    private static final int OTP_MAX = 900000;
    private static final int DEFAULT_PORT = 8080;

    private final SecureRandom secureRandom = new SecureRandom();

    private EditText phoneInput;
    private EditText messageInput;
    private EditText apiKeyInput;
    private EditText portInput;
    private TextView serverStatusText;
    private TextView endpointText;

    private SmsGatewayServer smsGatewayServer;

    private final ActivityResultLauncher<String> requestSmsPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    sendManualSms();
                } else {
                    Toast.makeText(this, "SMS permission is required to send messages", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        phoneInput = findViewById(R.id.phoneInput);
        messageInput = findViewById(R.id.messageInput);
        apiKeyInput = findViewById(R.id.apiKeyInput);
        portInput = findViewById(R.id.portInput);
        serverStatusText = findViewById(R.id.serverStatusText);
        endpointText = findViewById(R.id.endpointText);

        Button generateOtpButton = findViewById(R.id.generateOtpButton);
        Button sendButton = findViewById(R.id.sendButton);
        Button startServerButton = findViewById(R.id.startServerButton);
        Button stopServerButton = findViewById(R.id.stopServerButton);

        portInput.setText(String.valueOf(DEFAULT_PORT));
        apiKeyInput.setText("change-this-api-key");
        updateServerState(false, DEFAULT_PORT);

        generateOtpButton.setOnClickListener(v -> {
            String otp = createOtpCode();
            messageInput.setText(String.format(OTP_TEMPLATE, otp));
        });

        sendButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    == PackageManager.PERMISSION_GRANTED) {
                sendManualSms();
            } else {
                requestSmsPermission.launch(Manifest.permission.SEND_SMS);
            }
        });

        startServerButton.setOnClickListener(v -> startServer());
        stopServerButton.setOnClickListener(v -> stopServer());
    }

    @Override
    protected void onDestroy() {
        stopServer();
        super.onDestroy();
    }

    private String createOtpCode() {
        int value = OTP_MIN + secureRandom.nextInt(OTP_MAX);
        return Integer.toString(value);
    }

    private void sendManualSms() {
        String phone = phoneInput.getText().toString().trim();
        String message = messageInput.getText().toString().trim();

        if (TextUtils.isEmpty(phone)) {
            phoneInput.setError("Phone number is required");
            return;
        }

        if (TextUtils.isEmpty(message)) {
            messageInput.setError("Message is required");
            return;
        }

        SmsGatewayServer.SendResult result = sendSms(phone, message);
        if (result.success) {
            Toast.makeText(this, "SMS sent", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to send SMS: " + result.message, Toast.LENGTH_LONG).show();
        }
    }

    private SmsGatewayServer.SendResult sendSms(String phone, String message) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            return new SmsGatewayServer.SendResult(false, "SEND_SMS permission not granted");
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(phone, null, parts, null, null);
            return new SmsGatewayServer.SendResult(true, "SMS sent");
        } catch (Exception ex) {
            return new SmsGatewayServer.SendResult(false, ex.getMessage() == null ? "Unknown error" : ex.getMessage());
        }
    }

    private SmsGatewayServer.SendResult handleApiSend(String phone, String message, boolean otpRequested) {
        String finalMessage = message;
        if (otpRequested && TextUtils.isEmpty(finalMessage)) {
            finalMessage = String.format(OTP_TEMPLATE, createOtpCode());
        }

        if (TextUtils.isEmpty(finalMessage)) {
            return new SmsGatewayServer.SendResult(false, "Message is required");
        }

        SmsGatewayServer.SendResult result = sendSms(phone, finalMessage);
        if (result.success) {
            String uiMessage = finalMessage;
            runOnUiThread(() -> {
                phoneInput.setText(phone);
                messageInput.setText(uiMessage);
            });
        }
        return result;
    }

    private void startServer() {
        String apiKey = apiKeyInput.getText().toString().trim();
        if (apiKey.isEmpty()) {
            apiKeyInput.setError("API key is required");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portInput.getText().toString().trim());
            if (port < 1 || port > 65535) {
                portInput.setError("Port must be 1-65535");
                return;
            }
        } catch (Exception ex) {
            portInput.setError("Invalid port");
            return;
        }

        stopServer();

        try {
            smsGatewayServer = new SmsGatewayServer(port, apiKey, this::handleApiSend);
            smsGatewayServer.start();
            updateServerState(true, port);
            Toast.makeText(this, "Server started", Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            smsGatewayServer = null;
            updateServerState(false, port);
            Toast.makeText(this, "Failed to start server: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopServer() {
        if (smsGatewayServer != null) {
            smsGatewayServer.stop();
            smsGatewayServer = null;
            Toast.makeText(this, "Server stopped", Toast.LENGTH_SHORT).show();
        }

        int port = DEFAULT_PORT;
        try {
            port = Integer.parseInt(portInput.getText().toString().trim());
        } catch (Exception ignored) {
        }
        updateServerState(false, port);
    }

    private void updateServerState(boolean running, int port) {
        if (running) {
            serverStatusText.setText("Server: RUNNING");
            endpointText.setText("POST http://<phone-ip>:" + port + "/send");
        } else {
            serverStatusText.setText("Server: STOPPED");
            endpointText.setText("POST http://<phone-ip>:" + port + "/send");
        }
    }
}
