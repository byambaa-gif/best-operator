package com.example.smsgateway;

import org.json.JSONObject;

import fi.iki.elonen.NanoHTTPD;

public class SmsGatewayServer extends NanoHTTPD {

    public interface SmsRequestHandler {
        SendResult onSendRequest(String phone, String message, boolean otpRequested);
    }

    public static class SendResult {
        public final boolean success;
        public final String message;

        public SendResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private final String apiKey;
    private final SmsRequestHandler handler;

    public SmsGatewayServer(int port, String apiKey, SmsRequestHandler handler) {
        super(port);
        this.apiKey = apiKey;
        this.handler = handler;
    }

    @Override
    public Response serve(IHTTPSession session) {
        if (!Method.POST.equals(session.getMethod()) || !"/send".equals(session.getUri())) {
            return json(404, false, "Not found. Use POST /send");
        }

        String providedApiKey = session.getHeaders().get("x-api-key");
        if (apiKey == null || apiKey.isBlank() || !apiKey.equals(providedApiKey)) {
            return json(401, false, "Unauthorized");
        }

        try {
            java.util.Map<String, String> files = new java.util.HashMap<>();
            session.parseBody(files);
            String rawBody = files.get("postData");
            if (rawBody == null || rawBody.isBlank()) {
                return json(400, false, "Request body is required");
            }

            JSONObject body = new JSONObject(rawBody);
            String phone = body.optString("phone", "").trim();
            String message = body.optString("message", "").trim();
            boolean otpRequested = body.optBoolean("otp", false);

            if (phone.isEmpty()) {
                return json(400, false, "phone is required");
            }

            if (!otpRequested && message.isEmpty()) {
                return json(400, false, "message is required when otp=false");
            }

            SendResult result = handler.onSendRequest(phone, message, otpRequested);
            if (!result.success) {
                return json(400, false, result.message);
            }

            return json(200, true, result.message);
        } catch (Exception ex) {
            return json(500, false, "Server error: " + ex.getMessage());
        }
    }

    private Response json(int statusCode, boolean success, String message) {
        JSONObject responseBody = new JSONObject();
        responseBody.put("ok", success);
        responseBody.put("message", message);

        Response.Status status;
        switch (statusCode) {
            case 200:
                status = Response.Status.OK;
                break;
            case 400:
                status = Response.Status.BAD_REQUEST;
                break;
            case 401:
                status = Response.Status.UNAUTHORIZED;
                break;
            case 404:
                status = Response.Status.NOT_FOUND;
                break;
            default:
                status = Response.Status.INTERNAL_ERROR;
                break;
        }

        Response response = newFixedLengthResponse(status, "application/json", responseBody.toString());
        response.addHeader("Access-Control-Allow-Origin", "*");
        return response;
    }
}
