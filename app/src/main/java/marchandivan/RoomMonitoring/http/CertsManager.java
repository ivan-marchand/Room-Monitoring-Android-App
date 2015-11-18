package marchandivan.RoomMonitoring.http;

import java.security.cert.X509Certificate;
import java.util.List;

import android.content.Context;
import android.util.Log;

/**
 * Save the ssl certificates the user has confirmed to trust
 */
public final class CertsManager {
    private static final String DEBUG_TAG = "CertsManager";

    private CertsDBHelper db = null;

    private X509Certificate cachedCert = null;

    private static CertsManager instance;

    CertsManager(Context context) {
        db = CertsDBHelper.getDatabaseHelper(context);
    }

    public static synchronized CertsManager instance(Context context) {
        if (instance == null) {
            instance = new CertsManager(context);
        }

        return instance;
    }

    public void saveCert(final String url, List<X509Certificate> certs, boolean rememberChoice) {
        if (certs == null || certs.size() == 0) {
            return;
        }

        final X509Certificate cert = certs.get(0);
        cachedCert = cert;

        if (rememberChoice) {
            db.saveCertificate(url, cert);
        }

        Log.d(DEBUG_TAG, "saved cert for url " + url);
    }

    public X509Certificate getCertificate(String url) {
        if (cachedCert != null) {
            return cachedCert;
        }

        X509Certificate cert = db.getCertificate(url);
        if (cert != null) {
            cachedCert = cert;
        }

        return cert;
    }
}
