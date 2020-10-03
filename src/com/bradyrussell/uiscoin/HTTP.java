package com.bradyrussell.uiscoin;

import com.bradyrussell.uiscoin.block.Block;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Logger;

public class HTTP {
    private static final Logger Log = Logger.getLogger(HTTP.class.getName());
    public static boolean LogRequests = true;
    public static boolean LogRequestTimes = true;

    public static @Nullable
    String Request(String URL, String Method, Map<String, String> Properties, String Content) {
        long StartMs = System.currentTimeMillis();

        if (LogRequests)
            Log.info("[LogRequests] " + Method + " " + URL + " " + (Content != null ? Content : ""));

        try {

            java.net.URL url = new URL(URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod(Method);

            conn.setRequestProperty("Accept", "application/json");

            if (Properties != null) Properties.forEach(conn::setRequestProperty);

            if (Content != null && !Content.isEmpty() && !Content.isBlank()) {
                byte[] postDataBytes = Content.getBytes(StandardCharsets.UTF_8);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                conn.setDoOutput(true);
                conn.getOutputStream().write(postDataBytes);
            }

            if (conn.getResponseCode() < 200 || conn.getResponseCode() > 299) { // expect a 200 response
                throw new RuntimeException("Request Failed: "
                        + conn.getResponseCode() + ": " + conn.getResponseMessage());
            }
            InputStreamReader in = new InputStreamReader(conn.getInputStream());
            BufferedReader br = new BufferedReader(in);

            //StringBuilder content = new StringBuilder();
            String output = br.readLine(); // responses should only be one line

            //while ((output = br.readLine()) != null) {
            //    content.append(output).append("\r\n");
            //}

            in.close();
            br.close();
            conn.disconnect();

            if (LogRequests)
                Log.info("[LogRequests] " + conn.getResponseCode() + " " + conn.getResponseMessage());
            if (LogRequestTimes)
                Log.info("[LogRequestTime] Request completed in " + (System.currentTimeMillis() - StartMs) + " ms.");
            return output;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
