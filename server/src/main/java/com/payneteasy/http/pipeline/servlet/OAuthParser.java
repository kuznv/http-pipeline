package com.payneteasy.http.pipeline.servlet;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OAuthParser {
    public static final String HMAC_SHA1 = "HMAC-SHA1";
    public static final String RSA_SHA1 = "RSA-SHA1";
    public static final String RSA_SHA256 = "RSA-SHA256";

    public static OAuthAuthorization decodeAuthorization(String authorization) {
        Map<String, String> into = new HashMap<>();
        if (authorization != null) {
            Matcher m = AUTHORIZATION.matcher(authorization);
            if (m.matches()) {
                if (AUTH_SCHEME.equalsIgnoreCase(m.group(1))) {
                    for (String nvp : m.group(2).split("\\s*,\\s*")) {
                        m = NVP.matcher(nvp);
                        if (m.matches()) {
                            String name = decodePercent(m.group(1));
                            String value = decodePercent(m.group(2));
                            into.put(name, value);
                        }
                    }
                }
            }
        }
        return new OAuthAuthorization(into);
    }

    private static String decodePercent(String s) {
        try {
            return URLDecoder.decode(s, ENCODING);
            // This implements http://oauth.pbwiki.com/FlexibleDecoding
        } catch (java.io.UnsupportedEncodingException wow) {
            throw new RuntimeException(wow.getMessage(), wow);
        }
    }
    
    /** The encoding used to represent characters as bytes. */
    private static final String ENCODING = "UTF-8";
    private static final String AUTH_SCHEME = "OAuth";
    private static final Pattern AUTHORIZATION = Pattern.compile("\\s*(\\w*)\\s+(.*)");
    private static final Pattern NVP = Pattern.compile("(\\S*)\\s*\\=\\s*\"([^\"]*)\"");


}
