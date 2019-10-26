import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.lang.invoke.MethodHandles;
import java.security.KeyStore;

/**
 * Example: uses a java KeyStore on the local file system
 * containing the required CA certificates, then loads the keystore into a
 * TrustManager and sets up the SSLContext to use the more secure TLSv1.2 protocol.
 * <p>
 * It can (and should) also be configured using a common name host verifier that checks the certificate originates
 * from the correct MarkLogic host server, i.e certificate commonName == host name.
 * To do this, you would change the SSLHostnameVerifier to SSLHostnameVerifier.COMMON
 * <p>
 * See: https://docs.oracle.com/cd/E19509-01/820-3503/ggfen/index.html
 * <p>
 * To create the keystore:
 * keytool -keystore clientkeystore -genkey -alias client
 * <p>
 * To create the cert:
 * - Follow ML steps
 * - Export the .crt file in MarkLogic
 * <p>
 * Then import into your clientkeystore:
 * keytool -import -keystore clientkeystore -file certificate.crt -alias theCARoot
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
        for (TrustManager trustMgr : trustManagerFactory.getTrustManagers()) {
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
                                //.withSSLHostnameVerifier(DatabaseClientFactory.SSLHostnameVerifier.COMMON)
                                // IMPORTANT: Note that we're using a generic (ANY) SSL Hostname verifier here, so a connection to "localhost" will work
                                .withSSLHostnameVerifier(DatabaseClientFactory.SSLHostnameVerifier.ANY)
                );

        // Simple test to show the server is able to evaluate
        LOG.info("Test Connection (eval 1+1): " + client.newServerEval().xquery("1+1").evalAs(String.class));

        client.release();

    }

}

