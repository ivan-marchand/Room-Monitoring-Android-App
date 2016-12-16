package marchandivan.RoomMonitoring.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;

import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.http.CertificateInfo;
import marchandivan.RoomMonitoring.http.SSLTrustManager;

/**
 * Created by ivan on 11/2/15.
 */
public class SslConfirmDialogBuilder {
    private Context mContext;
    private SSLTrustManager mSSLTrustManager;
    private AlertDialog.Builder mBuilder;

    public SslConfirmDialogBuilder(Context context, SSLTrustManager sslTrustManager) {
        mContext = context;
        mSSLTrustManager = sslTrustManager;
        mBuilder = new AlertDialog.Builder(context);
    }

    public AlertDialog create(String host, String port) {
        // Inflate layout
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View dialogView = inflater.inflate(R.layout.ssl_confirm_dialog, null, false);
        mBuilder.setView(dialogView);

        TextView messageText = (TextView) dialogView.findViewById(R.id.message);
        TextView commonNameText = (TextView) dialogView.findViewById(R.id.common_name);
        TextView sha1Text = (TextView) dialogView.findViewById(R.id.sha1);
        TextView md5Text = (TextView) dialogView.findViewById(R.id.md5);
        TextView serialNumberText = (TextView) dialogView.findViewById(R.id.serial_number);
        TextView notBeforeText = (TextView) dialogView.findViewById(R.id.not_before);
        TextView notAfterText = (TextView) dialogView.findViewById(R.id.not_after);
        final CheckBox rememberChoice = (CheckBox) dialogView.findViewById(R.id.remember_choice);

        // Get certificate info
        X509Certificate cert = null;
        try {
            cert = SSLTrustManager.instance(mContext, host, port).getCertificateInfo();
       }
        catch (CertificateParsingException e) {
            // Ignore
        }

        // Title
        mBuilder.setTitle(R.string.ssl_confirm_title);

        // Message
        String msg = "";
        SSLTrustManager.SslFailureReason reason = SSLTrustManager.instance(mContext, host, port).getFailureReason();
        if (reason == SSLTrustManager.SslFailureReason.CERT_NOT_TRUSTED) {
            msg = mContext.getString(R.string.ssl_confirm, host);
        } else {
            msg = mContext.getString(R.string.ssl_confirm_cert_changed, host);
        }
        messageText.setText(msg);

        // Certificate information
        if (cert != null) {
            CertificateInfo certInfo = new CertificateInfo(cert);
            commonNameText.setText(certInfo.getSubjectName());
            sha1Text.setText(mContext.getString(R.string.sha1, certInfo.getSignature("SHA-1")));
            md5Text.setText(mContext.getString(R.string.md5, certInfo.getSignature("MD5")));
            serialNumberText.setText(mContext.getString(R.string.serial_number, certInfo.getSerialNumber()));
            notBeforeText.setText(mContext.getString(R.string.not_before, certInfo.getNotBefore().toLocaleString()));
            notAfterText.setText(mContext.getString(R.string.not_after, certInfo.getNotAfter().toLocaleString()));
        } else {
            // No certificate found
            return null;
        }
        // Accept, reject buttons
        mBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mSSLTrustManager.saveCertificate(rememberChoice.isChecked());
            }
        });
        mBuilder.setNegativeButton("No", null);

        return mBuilder.create();
    }


}
