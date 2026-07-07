package fr.dossierfacile.logging;

import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * TrustAllStatusListener est un listener d'état Logback personnalisé.
 * 
 * Son rôle principal est d'enregistrer dynamiquement un fournisseur de sécurité
 * Java ("TrustAllProvider") au tout début de l'initialisation de Logback, permettant
 * de contourner la validation SSL (trust-all) lors des connexions à Logstash avec
 * des certificats auto-générés.
 * 
 * POURQUOI CETTE CLASSE ET CE MÉCANISME ?
 * --------------------------------------
 * 1. Cycle de démarrage : Spring Boot initialise le système de logging (Logback)
 *    TRÈS TÔT, bien avant de charger la classe principale ou les beans Spring.
 *    Un bloc statique classique dans l'application démarrerait trop tard.
 * 2. Chargement précoce : Logback instancie et appelle le constructeur de chaque
 *    <statusListener> déclaré dès le début de l'analyse du fichier XML. C'est le point
 *    d'entrée idéal pour enregistrer des configurations JVM système.
 * 
 * COMMENT ÇA FONCTIONNE ?
 * -----------------------
 * 1. Le constructeur enregistre "TrustAllProvider" dans la liste des fournisseurs de sécurité
 *    globale de la JVM (java.security.Security).
 * 2. Ce fournisseur (TrustAllProvider) expose un algorithme de TrustManagerFactory nommé "TrustAll".
 * 3. Logback utilise ensuite cet algorithme dans sa configuration SSL pour obtenir un TrustManager
 *    dont les méthodes de validation de certificat (checkServerTrusted) sont vides (permissives).
 */
@Slf4j
public class TrustAllStatusListener implements StatusListener {
    public TrustAllStatusListener() {
        try {
            if (Security.getProvider("TrustAllProvider") == null) {
                Security.addProvider(new TrustAllProvider());
            }
        } catch (Exception e) {
            log.error("Failed to register TrustAllProvider in StatusListener: ", e);
        }
    }

    @Override
    public void addStatusEvent(Status status) {
        // Cette méthode est laissée vide intentionnellement.
        // Ce StatusListener sert uniquement de déclencheur (via son constructeur) pour
        // enregistrer le Security Provider "TrustAllProvider" au tout début de l'analyse
        // de la configuration Logback. Nous n'avons pas besoin de réagir ou de traiter
        // les événements d'état de Logback eux-mêmes.
    }

    public static class TrustAllProvider extends Provider {
        public TrustAllProvider() {
            super("TrustAllProvider", "1.0", "Trust All Provider");
            put("TrustManagerFactory.TrustAll", TrustAllManagerFactory.class.getName());
        }
    }

    public static class TrustAllManagerFactory extends TrustManagerFactorySpi {
        @Override
        protected void engineInit(KeyStore keyStore) {
            // Pas d'initialisation nécessaire depuis un KeyStore
            // car notre TrustManager accepte inconditionnellement tous les certificats.
        }

        @Override
        protected void engineInit(ManagerFactoryParameters managerFactoryParameters) {
            // Pas d'initialisation nécessaire depuis des paramètres
            // car notre TrustManager n'a besoin d'aucune règle de validation.
        }

        @Override
        protected TrustManager[] engineGetTrustManagers() {
            return new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        // Méthode vide : aucun contrôle n'est effectué sur le certificat client
                        // pour autoriser toutes les connexions entrantes.
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        // Méthode vide : aucun contrôle n'est effectué sur le certificat serveur,
                        // acceptant ainsi les certificats autosignés ou expirés.
                    }
                }
            };
        }
    }
}
