package com.ylw.shareurl;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = getIntent();
        if (intent.getExtras() != null) {
            TextView subjectView = findViewById(R.id.subject);
            TextView textView = findViewById(R.id.text);
            CharSequence subject = intent.getExtras().getString(Intent.EXTRA_SUBJECT);
            CharSequence text = intent.getExtras().getString(Intent.EXTRA_TEXT);

            subjectView.setText(subject);
            textView.setText(text);

            try {
                JSONObject file = new JSONObject();
                file.put("time", System.currentTimeMillis());
                file.put("url", text);
                JSONObject jsonObject = new JSONObject("{\n" +
                        "            \"access_token\": \"" + BuildConfig.ACCESS_TOKEN + "\",\n" +
                        "            \"files\": { \"ShareUrl\": { \"content\": \"" + file.toString().replaceAll("\"", "\\\\\"") + "\" } }, \"description\": \"ShareUrl\"\n" +
                        "        }");

                new Thread(() -> {
                    HttpURLConnection conn = null;
                    OutputStream os = null;
                    InputStream is = null;
                    try {
                        URL url = new URL("https://gitee.com/api/v5/gists/" + BuildConfig.GIST_ID);
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("PATCH");
                        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                        conn.connect();
                        os = new DataOutputStream(conn.getOutputStream());
                        os.write(jsonObject.toString().getBytes());

                        int responseCode = conn.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            is = conn.getInputStream();
                            ByteArrayOutputStream bos;
                            bos = new ByteArrayOutputStream();
                            byte[] b = new byte[1024];
                            int len;
                            while ((len = is.read(b)) != -1) {  //先读到内存
                                bos.write(b, 0, len);
                            }
                            Log.i(TAG, "onCreate: " + new String(bos.toByteArray()));
                            showToast("SUCCESS");
                            new Handler(Looper.getMainLooper()).post(this::finish);
                        } else {
                            showToast("responseCode:" + responseCode);
                        }
                    } catch (Exception e) {
                        showToast(e.getMessage());
                        e.printStackTrace();
                    } finally {
                        close(is);
                        close(os);
                        if (conn != null) {
                            conn.disconnect();
                        }
                    }
                }).start();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            finish();
        }
    }

    void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showToast(String msg) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
    }
}
