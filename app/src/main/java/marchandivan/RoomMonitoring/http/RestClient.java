package marchandivan.RoomMonitoring.http;

/**
 * Created by imarchand on 6/28/2015.
 */

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

import marchandivan.RoomMonitoring.dialog.SslConfirmDialogBuilder;

public class RestClient {
    private Context mContext;
    private boolean mUseHttps;
    private String mServerHost;
    private Integer mServerPort;
    private String mServerUser;
    private String mServerPassword;
    private String mUrlBase;

    private HashMap<String, String> mUrlParams = new HashMap<>();

    private int mHttpStatusCode = HttpURLConnection.HTTP_NOT_FOUND;
    private List<HttpCookie> mHttpResponseCookies = new LinkedList<>();
    private List<HttpCookie> mHttpRequestCookies;

    private static boolean mShowSslConfirmDialog = true;
    private Handler mUiHandler = new Handler(Looper.getMainLooper());
    private HashMap<String, String> mRequestProperty = new HashMap<>();

    public RestClient(Context context, boolean https, String host, int port) {
        mContext = context;
        mServerHost = host;
        mServerPort = port;
        mUseHttps = https;
        mUrlBase = (mUseHttps ? "https://" : "http://") + mServerHost + ":" + mServerPort.toString();
        mServerUser = null;
        mServerPassword = null;
    }

    public void setUserPassword(final String user, final String password) {
        mServerUser = user;
        mServerPassword = password;
    }

    public void addUrlParam(String key, String value) {
        mUrlParams.put(key, value);
    }

    public int getHttpStatusCode() {
        return mHttpStatusCode;
    }

    public final List<HttpCookie> getHttpResponseCookies() {
        return mHttpResponseCookies;
    }

    public void setHttpRequestCookies(final List<HttpCookie> cookies) {
        mHttpRequestCookies = cookies;
    }

    public void setRequestProperty(String field, String value) {
        mRequestProperty.put(field, value);
    }

    public String get(final String path) {
        return request(path, false, null, null);
    }

    public String post(final String path, final String contentType, final String postData) {
        return request(path, true, contentType, postData);
    }

    private String request(final String path,
                           boolean post,
                           final String contentType,
                           final String postData) {
        String urlString = mUrlBase + path;
        String result = "";
        try {

            // Tell the URLConnection to use a custom SocketFactory from our SSLContext
            // URL params
            String urlParams = new String();
            if (!mUrlParams.isEmpty()) {
                Uri.Builder urlBuilder = new Uri.Builder();
                for (HashMap.Entry<String, String> param: mUrlParams.entrySet()) {
                    urlBuilder.appendQueryParameter(param.getKey(), param.getValue());
                }
                urlParams = urlBuilder.toString();
            }

            URL url = new URL(urlString + urlParams);
            Log.d("RestClient", urlString + urlParams);
            HttpURLConnection urlConnection;
            if (!mUseHttps) {
                urlConnection = (HttpURLConnection) url.openConnection();
            } else {
                HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) url.openConnection();
                httpsUrlConnection.setSSLSocketFactory(SSLTrustManager.instance(mContext, mServerHost, mServerPort.toString()).getSSLSocketFactory());
                urlConnection = httpsUrlConnection;
            }

            // Content type
            if (contentType != null && !contentType.isEmpty()) {
                urlConnection.setRequestProperty("Content-type", contentType);
            }

            // Add Authorization in http header?
            if (mServerUser != null && mServerPassword != null) {
                urlConnection.setRequestProperty("Authorization", "basic " + Base64.encodeToString((mServerUser + ":" + mServerPassword).getBytes(), Base64.NO_WRAP));
            }

            // Additional header param
            for (HashMap.Entry<String, String> requestProperty: mRequestProperty.entrySet()) {
                urlConnection.setRequestProperty(requestProperty.getKey(), requestProperty.getValue());
            }

            // Cookies
            if (mHttpRequestCookies != null && !mHttpRequestCookies.isEmpty()) {
                urlConnection.setRequestProperty("Cookie", TextUtils.join(";",  mHttpRequestCookies));
            }

            // POST?
            if (post) {
                urlConnection.setRequestMethod("POST");
                if (postData != null && !postData.isEmpty()) {
                    DataOutputStream dataOutputStream = new DataOutputStream(urlConnection.getOutputStream());
                    dataOutputStream.write(postData.getBytes());
                }
            }

            // Get response status code
            mHttpStatusCode = urlConnection.getResponseCode();

            // Get cookies
            List<String> cookiesHeader = urlConnection.getHeaderFields().get("Set-Cookie");
            if (cookiesHeader != null) {
                for (String cookieString : cookiesHeader) {
                    List<HttpCookie> cookies = HttpCookie.parse(cookieString);
                    if (!cookies.isEmpty()) {
                        mHttpResponseCookies.add(cookies.get(0));
                    }
                }
            }

            // Decode input stream (body)
            StringBuilder stringBuilder = new StringBuilder();
            if (mHttpStatusCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = urlConnection.getInputStream();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                inputStream.close();
            } else {
                Log.d("RestClient", "Failed to retrieve data, http : x`" + mHttpStatusCode);
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

    public JSONObject getJson(String path) {
        Log.d("RestClient", "GET " + path);
        JSONObject json = new JSONObject();
        if (!mServerHost.isEmpty() && mServerPort != 0) {
            try {
                json = new JSONObject(get(path));
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("RestClient", "Error " + e.getMessage());
            }
        }
        return json;
    }

    public JSONArray getJsonArray(String path) {
        Log.d("RestClient", "GET " + path);
        JSONArray json = new JSONArray();
        if (!mServerHost.isEmpty() && mServerPort != 0) {
            try {
                json = new JSONArray(get(path));
            } catch (Exception e) {
                Log.d("RestClient", "Error " + e.getMessage());
            }
        }
        return json;
    }

    public JSONObject postJson(String path, JSONObject postData) {
        Log.d("RestClient", "POST " + path);
        JSONObject json = new JSONObject();
        if (!mServerHost.isEmpty() && mServerPort != 0) {
            try {
                json = new JSONObject(post(path, "application/json", postData.toString()));
            } catch (Exception e) {
                e.printStackTrace();
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
                        builder.create(mServerHost, mServerPort.toString());
                    }
                } catch (Exception e) {

                }
            }
        });
    }
}
