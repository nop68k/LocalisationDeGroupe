package org.patarasprod.localisationdegroupe;

import static android.content.Context.ACTIVITY_SERVICE;
import static androidx.core.content.ContextCompat.getSystemService;
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
                // This is called when the connection with the service has been
                // established, giving us the service object we can use to
                // interact with the service.  We are communicating with our
                // service through an IDL interface, so get a client-side
                // representation of that from the raw service object.
                mService = new Messenger(service);
                Log.d("AccesService", "Service connecté !");

                // We want to monitor the service for as long as we are
                // connected to it.
                Message msg = Message.obtain(null,
                        LocationUpdateService.MSG_PREMIERE_CONNEXION);
                try {
                    //msg.replyTo = mMessenger;  // Si on veut une réponse
                    mService.send(msg);
                } catch (RemoteException e) {
                    // In this case the service has crashed before we could even
                    // do anything with it; we can count on soon being
                    // disconnected (and then reconnected if it can be restarted)
                    // so there is no need to do anything here.
                }
            }

            public void onServiceDisconnected(ComponentName className) {
                // This is called when the connection with the service has been
                // unexpectedly disconnected -- that is, its process crashed.
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

    /** Envoi un message avec des données dans un bundle
     *
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
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        mContext.bindService(new Intent(mContext,
                LocationUpdateService.class), mConnection, Context.BIND_AUTO_CREATE);
        try {
            sleep(50);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (mService != null) {
            mIsBound = true;
            return true;
        }
        return false;
    }

    void deconnexionService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null,
                            LocationUpdateService.MSG_DECONNEXION);
                    //msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            mContext.unbindService(mConnection);
            mIsBound = false;
        }
    }

    public Bundle creationBundleParametresService(Config config) {
        Bundle donneesServeur = new Bundle();
        donneesServeur.putString("nom", config.nomUtilisateur);
        donneesServeur.putString("adresse", config.adresse_serveur);
        donneesServeur.putInt("port", config.port_serveur);
        donneesServeur.putLong("intervalle_envoi_en_fond", config.intervalleEnvoiEnFond);
        return donneesServeur;
    }

    /**
     * Check if the service is Running
     * @param serviceClass the class of the Service
     *
     * @return true if the service is running otherwise false
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
        //mContext.stopService(new Intent(mContext, LocationUpdateService.class));
    }

    /** Met à jour les paramètres du service à partir de la configuration fournie en argument
     * @param config   Objet configuration contenant les paramètres à jour
     *
     */
    public void majParametresService(Config config) {
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
