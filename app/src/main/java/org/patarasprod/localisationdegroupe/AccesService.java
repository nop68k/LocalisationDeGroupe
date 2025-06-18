package org.patarasprod.localisationdegroupe;

import static java.lang.Thread.sleep;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.List;

/**
 * Classe permettant d'interagir avec le service en arrière plan (classe LocationUpdateService)
 * Cette classe permet de démarrer et d'arrêter le service, mais aussi de mettre à jour sa configuration
 * ou sa notification
 */
public class AccesService {

    private final Context mContext;
    ServiceConnection mConnection;
    protected Config cfg;  // Référence locale à la configuration du programme
    Messenger mService = null;  // Objet Messenger pour communiquer avec le service
    boolean mIsBound;

    public AccesService(Config cfg, Context context) {
        this.cfg = cfg;
        this.mContext = context;

        mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                // Fonction appelée lorsque l'on s'est connecté au service en arrière plan
                mService = new Messenger(service);
                Log.d("AccesService", "Service connecté !");
                mIsBound = true;   // Indique que la connexion au service est bien faite

                // On envoi un message au service pour lui indiquer qu'on est connecté
                Message msg = Message.obtain(null,
                        LocationUpdateService.MSG_PREMIERE_CONNEXION);
                try {
                    //msg.replyTo = mMessenger;  // Si on veut une réponse
                    mService.send(msg);
                } catch (RemoteException e) {
                    // Si le service s'est crashé le message échouera
                }
            }

            public void onServiceDisconnected(ComponentName className) {
                // Appelée si le service a été déconnecté (crash du service par exemple)
                mService = null;
                Log.d("AccesService", "Service déconnecté !");
            }
        };
    }

    /** Envoi un message au service LocationUpdateService en utilisant le code message
     *  fournit en argument
     * @param codeMsg  Code (int) de la commande à exécuter pour le service
     */
    public void envoiMsgService(int codeMsg) {
        Message msg = Message.obtain(null, codeMsg);
        try {
            //msg.replyTo = mMessenger;  // Si on veut une réponse
            mService.send(msg);
        } catch (RemoteException e) {
            Log.d("AccesService", "Erreur dans l'envoi du message " + codeMsg);
        }
    }

    /**
     * Envoi un message avec des données dans un bundle
     * @param codeMsg  Code de la commande
     * @param bundle   Données à transmettre avec le message
     */
    public void envoiMsgService(int codeMsg, Bundle bundle) {
        Message msg = Message.obtain(null, codeMsg);
        msg.setData(bundle);  // inclu les données fournies au message
        try {
            //msg.replyTo = mMessenger;  // Si on veut une réponse
            mService.send(msg);
        } catch (RemoteException e) {
            Log.d("AccesService", "Erreur dans l'envoi du message " + codeMsg);
        }
    }

    public boolean connexionService() {
        mContext.bindService(new Intent(mContext,
                LocationUpdateService.class), mConnection, Context.BIND_AUTO_CREATE);
        try {
            sleep(50);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (mService != null) {
            mIsBound = true;    // Drapeau indiquant que la liaison est effective
            return true;
        }
        return false;
    }

    void deconnexionService() {
        if (mIsBound) {                // Si le service est effectivement connecté
            if (mService != null) {    // Et qu'on a bien une référence sur le service
                try {                  // On va lui envoyer le message de déconnexion
                    Message msg = Message.obtain(null,
                            LocationUpdateService.MSG_DECONNEXION);
                    //msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // Si le service a déjà crashé, il n'y a rien à faire
                }
            }
            // On détache la connexion existante
            mContext.unbindService(mConnection);
            mIsBound = false;
        }
    }

    /**
     * Création du bundle avec les paramètres nécessaires au fonctionnement du service en arrière plan
     * @param config  Un objet Config contenant les informations
     * @return un objet Bundle contenant uniquement les informations utiles
     */
    public Bundle creationBundleParametresService(Config config) {
        Bundle donneesServeur = new Bundle();
        donneesServeur.putString("nom", config.nomUtilisateur);
        donneesServeur.putString("adresse", config.adresse_serveur);
        donneesServeur.putInt("port", config.port_serveur);
        donneesServeur.putLong("intervalle_envoi_en_fond", config.intervalleEnvoiService);
        return donneesServeur;
    }

    /**
     * Vérifie si le service dont la classe est donnée en argument est en cours d'exécution (en
     * le cherchant dans la liste des services actuellement en exécution).
     * @param serviceClass Classe du service cherché
     * @return true si le service est actuellement en fonctionnement
     */
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = manager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo service : services) {
            if (service.service.getClassName().equals(serviceClass.getName())) {
                return true;
            }
        }
        return false;
    }

    public void demarrageService() {
        if (isMyServiceRunning(LocationUpdateService.class)) {  // On teste si le service ne fonctionne pas déjà
            Log.d("AccesService", "*** Le service est déjà en cours d'exécution !");
            connexionService();  // Dans ce cas on se contente de se reconnecter au service
        } else { // Sinon on le démarre
            Log.d("AccesService", "Démarrage du service ...");
            // D'abord on crée un intent
            Intent serviceIntent = new Intent(mContext, LocationUpdateService.class);
            // puis on prépare un bundle avec les données pour le serveur
            serviceIntent.putExtra("data", creationBundleParametresService(cfg));
            ContextCompat.startForegroundService(mContext, serviceIntent);
            connexionService();
        }
    }

    public void arreteService() {
        Log.d("AccesService", "Arrêt du service ...");
        envoiMsgService(LocationUpdateService.MSG_ARRET_SERVICE);
        deconnexionService();
    }

    /**
     * Met à jour les paramètres du service à partir de la configuration fournie en argument
     * @param config   Objet configuration contenant les paramètres à jour
     */
    public void majParametresService(Config config) {
        Log.d("AccesService", "demande de maj avec mIsbound = " + mIsBound);
        if (!mIsBound) return;   // Si pas de connexion au service, on ne fait rien
        if (config == null) config = cfg;
        Bundle bundle = null;
        if (config != null) {
            bundle = creationBundleParametresService(config);
        } else {
            // Si pas de config, on ne met pas à jour
            return;
        }
        envoiMsgService(LocationUpdateService.MSG_MAJ_CONFIG, bundle);
    }
}
