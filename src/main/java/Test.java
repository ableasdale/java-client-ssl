import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;
import java.lang.invoke.MethodHandles;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

/**
 *  a java KeyStore on the local file system
 * containing the required CA certificates, then loads the keystore into a
 * TrustManager and sets up the SSLContext to use the more secure TLSv1.2 protocol.
 * It also uses a common name host verifier that checks the certificate originates
 * from the correct ML host server, i.e certificate commonName == host name.
 *
 * See: https://docs.oracle.com/cd/E19509-01/820-3503/ggfen/index.html
 *
 * To create the keystore:
 * keytool -keystore clientkeystore -genkey -alias client
 *
 * To create the cert:
 * - ML steps
 * - Export
 *
 * Then:
 * keytool -import -keystore clientkeystore -file certificate.crt -alias theCARoot
 *
 */
public class Test {

    private static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void main(String[] args) throws Exception {

        FileInputStream mlca = new FileInputStream("src/main/resources/clientkeystore");
        KeyStore ks = KeyStore.getInstance("JKS");
        // Note that the password below is the password used when the keystore file was created
        ks.load(mlca, "test123".toCharArray());
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance("PKIX");
        trustManagerFactory.init(ks);
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

        X509TrustManager x509trustMgr = null;
        for (TrustManager trustMgr: trustManagerFactory.getTrustManagers()) {
            if (trustMgr instanceof X509TrustManager) {
                x509trustMgr = (X509TrustManager) trustMgr;
                break;
            }
        }

        DatabaseClient client =
                DatabaseClientFactory.newClient(
                        "localhost", 8000,
                        new DatabaseClientFactory.DigestAuthContext("admin", "admin")
                                .withSSLContext(sslContext, x509trustMgr)
                                // Note that we're using a generic (ANY) SSL Hostname verifier here, so a connection to "localhost" will work
                                .withSSLHostnameVerifier(DatabaseClientFactory.SSLHostnameVerifier.ANY)
                );

        // Simple test to show the server is able to evaluate
        LOG.info("Test Connection (eval 1+1): "+client.newServerEval().xquery("1+1").evalAs(String.class));

        client.release();

    }

}

