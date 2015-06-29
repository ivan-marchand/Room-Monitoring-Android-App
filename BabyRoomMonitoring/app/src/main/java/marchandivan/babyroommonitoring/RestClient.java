package marchandivan.babyroommonitoring;

/**
 * Created by imarchand on 6/28/2015.
 */

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RestClient {

    public String readJSONFeed(String urlString) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection ();
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
            Log.d("RestClient", "Error " + e.getMessage());
        }
        return stringBuilder.toString();
    }

    protected JSONObject get(String url) {
        JSONObject json = new JSONObject();
        try {
            json = new JSONObject(readJSONFeed(url));
        } catch (Exception e) {
            Log.d("RestClient", "Error " + e.getMessage());
        }
        return json;
    }
}
