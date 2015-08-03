package marchandivan.babyroommonitoring;

/**
 * Created by imarchand on 6/28/2015.
 */

import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyStore;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

public class RestClient {
    private String mServerHost;
    private Integer mServerPort;

    public void configure(SharedPreferences sharedPreferences) {
        mServerHost = sharedPreferences.getString("server_host", "");
        mServerPort = Integer.parseInt(sharedPreferences.getString("server_port", ""));
    }

    public String readJSONFeed(String urlString) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            // Get KeyStore for server
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(getCertificates()[0].getPublicKey().toString());

            // Build SSL Context
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
            kmf.init(keyStore, "".toCharArray());
            KeyManager[] keyManagers = kmf.getKeyManagers();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, null, null);

            // Open connection
            URL url = new URL(urlString);
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection ();
            urlConnection.setSSLSocketFactory(sslContext.getSocketFactory());
            int statusCode = urlConnection.getResponseCode();
            if (statusCode == HttpsURLConnection.HTTP_OK) {
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
            Log.d("RestClient", "Error " + e.getMessage());
        }
        return stringBuilder.toString();
    }

    protected JSONObject get() {
        JSONObject json = new JSONObject();
        if (!mServerHost.isEmpty() && mServerPort != 0) {
            try {
                String url = "https://" + mServerHost + ":" + mServerPort + "/api/v1/get/temperature/salon";
                json = new JSONObject(readJSONFeed(url));
            } catch (Exception e) {
                Log.d("RestClient", "Error " + e.getMessage());
            }
        }
        return json;
    }
}
