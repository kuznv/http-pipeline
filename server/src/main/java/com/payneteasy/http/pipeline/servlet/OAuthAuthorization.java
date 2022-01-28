package com.payneteasy.http.pipeline.servlet;

import java.util.Map;

public class OAuthAuthorization {
    private final Map<String, String> map;

    OAuthAuthorization(Map<String, String> map) {
        super();
        this.map = map;
    }

    public Map<String, String> getMap() {
        return map;
    }

    public String getCallback() {
        return map.get("oauth_callback");
    }

    public String getConsumerKey() {
        return map.get("oauth_consumer_key");
    }

    public String getNonce() {
        return map.get("oauth_nonce");
    }

    public String getSignature() {
        return map.get("oauth_signature");
    }

    public String getSignatureMethod() {
        return map.get("oauth_signature_method");
    }

    public String getTimestamp() {
        return map.get("oauth_timestamp");
    }

    public long getTimestampAsSeconds() {
        return Long.parseLong(getTimestamp());
    }

    public String getToken() {
        return map.get("oauth_token");
    }

    public String getVerifier() {
        return map.get("oauth_verifier");
    }

    public String getVersion() {
        return map.get("oauth_version");
    }

    public String getBodyHash() {
        return map.get("bodyhash");
    }

    //https://developer.mastercard.com/platform/documentation/security-and-authentication/using-oauth-1a-to-access-mastercard-apis/
    public String getBodyHashOauth1a() {
        return map.get("oauth_body_hash");
    }
}
