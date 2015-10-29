package marchandivan.RoomMonitoring.http;

/**
 * Created by imarchand on 6/28/2015.
 */

import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class RestClient {
    private String mServerHost;
    private String mServerPort;
    private String mServerUser;
    private String mServerPassword;
    private AssetManager mAssetManager;

    public RestClient(AssetManager assetManager) {
        mAssetManager = assetManager;
    }

    public void configure(SharedPreferences sharedPreferences) {
        mServerHost = sharedPreferences.getString("server_host", "");
        mServerPort = sharedPreferences.getString("server_port", "");
        mServerUser = sharedPreferences.getString("server_user", "");
        mServerPassword = sharedPreferences.getString("server_password", "");
    }

    public String readJSONFeed(String urlString) {
        StringBuilder stringBuilder = new StringBuilder();
        try {

            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            InputStream caInput = mAssetManager.open("public.crt");
            Certificate ca = cf.generateCertificate(caInput);
            System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());

            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);

            // Tell the URLConnection to use a SocketFactory from our SSLContext
            URL url = new URL(urlString);
            HttpsURLConnection urlConnection = (HttpsURLConnection)url.openConnection();
            urlConnection.setSSLSocketFactory(context.getSocketFactory());
            urlConnection.setRequestProperty("Authorization", "basic " + Base64.encodeToString((mServerUser + ":" + mServerPassword).getBytes(), Base64.NO_WRAP));

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
                Log.d("RestClient", "Failed to download file");
            }
            urlConnection.disconnect();
        } catch (Exception e) {
            Log.d("RestClient", "Error " + e.toString());
        }
        String result = stringBuilder.toString();
        Log.d("RestClient", "JsonDoc: " + result);
        return result;
    }

    public JSONObject get(String path) {
        Log.d("RestClient", "GET " + path);
        JSONObject json = new JSONObject();
        if (!mServerHost.isEmpty() && !mServerPort.isEmpty()) {
            try {
                String url = "https://" + mServerHost + ":" + mServerPort + path;
                json = new JSONObject(readJSONFeed(url));
            } catch (Exception e) {
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
                String url = "https://" + mServerHost + ":" + mServerPort + path;
                json = new JSONArray(readJSONFeed(url));
            } catch (Exception e) {
                Log.d("RestClient", "Error " + e.getMessage());
            }
        }
        return json;
    }

}
