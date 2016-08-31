package marchandivan.RoomMonitoring.http;

/**
 * Created by imarchand on 6/28/2015.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

import marchandivan.RoomMonitoring.dialog.SslConfirmDialogBuilder;

public class RestClient {
    private String mServerHost;
    private String mServerPort;
    private String mServerUser;
    private String mServerPassword;
    private boolean mUseHttps;
    private Context mContext;
    private String mUrlBase;
    private static boolean mShowSslConfirmDialog = true;
    private Handler mUiHandler = new Handler(Looper.getMainLooper());

    public RestClient(Context context) {
        mContext = context;
    }

    public void configure(SharedPreferences sharedPreferences) {
        mServerHost = sharedPreferences.getString("server_host", "");
        mServerPort = sharedPreferences.getString("server_port", "");
        mServerUser = sharedPreferences.getString("server_user", "");
        mServerPassword = sharedPreferences.getString("server_password", "");
        mUseHttps = sharedPreferences.getBoolean("use_https", false);
        mUrlBase = (mUseHttps ? "https://" : "http://") + mServerHost + ":" + mServerPort;
    }

    public String readJSONFeed(String urlString) {
        String result = "";
        try {

            StringBuilder stringBuilder = new StringBuilder();

            // Tell the URLConnection to use a custom SocketFactory from our SSLContext
            URL url = new URL(urlString);
            Log.d("RestClient", urlString);
            HttpURLConnection urlConnection;
            if (!mUseHttps) {
                urlConnection = (HttpURLConnection) url.openConnection();
            } else {
                HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) url.openConnection();
                httpsUrlConnection.setSSLSocketFactory(SSLTrustManager.instance(mContext, mServerHost, mServerPort).getSSLSocketFactory());
                urlConnection = httpsUrlConnection;
            }
            urlConnection.setRequestProperty("Authorization", "basic " + Base64.encodeToString((mServerUser + ":" + mServerPassword).getBytes(), Base64.NO_WRAP));

            // Get response status code
            int statusCode = urlConnection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = urlConnection.getInputStream();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                inputStream.close();
            } else {
                Log.d("RestClient", "Failed to retrieve data");
            }
            urlConnection.disconnect();

            // Get the result
            result = stringBuilder.toString();
            Log.d("RestClient", "JsonDoc: " + result);
        } catch (SSLHandshakeException e) {
            Log.d("RestClient", "Error " + e.toString());
            if (mShowSslConfirmDialog) {
                mShowSslConfirmDialog = false;
                showSslConfirmDialog();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("RestClient", "Error " + e.toString());
        }
        return result;
    }

    public JSONObject get(String path) {
        Log.d("RestClient", "GET " + path);
        JSONObject json = new JSONObject();
        if (!mServerHost.isEmpty() && !mServerPort.isEmpty()) {
            try {
                String url = mUrlBase + path;
                json = new JSONObject(readJSONFeed(url));
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("RestClient", "Error " + e.getMessage());
            }
        }
        return json;
    }

    public JSONArray getArray(String path) {
        Log.d("RestClient", "GET " + path);
        JSONArray json = new JSONArray();
        if (!mServerHost.isEmpty() && !mServerPort.isEmpty()) {
            try {
                String url = mUrlBase + path;
                json = new JSONArray(readJSONFeed(url));
            } catch (Exception e) {
                Log.d("RestClient", "Error " + e.getMessage());
            }
        }
        return json;
    }

    private void showSslConfirmDialog() {
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    SslConfirmDialogBuilder builder = new SslConfirmDialogBuilder(mContext);
                    if (builder != null) {
                        builder.create(mServerHost, mServerPort);
                    }
                } catch (Exception e) {

                }
            }
        });
    }
}
