package org.patarasprod.localisationdegroupe;

import static androidx.core.content.ContextCompat.startActivity;
import static java.lang.Thread.sleep;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Objects;

/**
 * Classe permettant d'interagir avec le service en arrière plan (classe LocationUpdateService)
 * Cette classe permet de démarrer et d'arrêter le service, mais aussi de mettre à jour sa configuration
 * ou sa notification
 */
public class AccesService {
    private static final boolean DEBUG_CLASSE = true;  // Drapeau pour autoriser les message de debug dans la classe
    private final Context mContext;
    ServiceConnection mConnection;
    protected Config cfg;  // Référence locale à la configuration du programme
    Messenger mService = null;  // Objet Messenger pour communiquer avec le service
    boolean mIsBound;

    public AccesService(Config cfg, Context context) {
        this.cfg = cfg;
        this.mContext = context;
        demandeAutorisationNotification(false);


        mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                // Fonction appelée lorsque l'on s'est connecté au service en arrière plan
                mService = new Messenger(service);
                if (DEBUG_CLASSE && Config.DEBUG_LEVEL > 4) Log.d("AccesService", "Service connecté !");
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
                if (DEBUG_CLASSE && Config.DEBUG_LEVEL > 4) Log.d("AccesService", "Service déconnecté !");
            }
        };
    }

    /**
     * Vérifie si l'autorisation d'afficher des autorisations (POST_NOTIFICATIONS) a bien été
     * accordée à l'application et si ce n'est pas le cas la demande en affichant éventuellement
     * un message pour expliquer pourquoi elle est utile au programme
     *
     * @param force_demande true si on demande l'autorisation sans se demander si on affiche le
     *                      message d'explication
     */
    public void demandeAutorisationNotification(boolean force_demande) {
        if (DEBUG_CLASSE && Config.DEBUG_LEVEL > 3) Log.v("AccesService",
                "Demande autorisation Notification avec force_demande = " + force_demande );
        if (ActivityCompat.checkSelfPermission(this.mContext, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED || (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)) {
            // Si on a l'autorisation ou que cette autorisationn'existe pas ds cette version d'android
            if (DEBUG_CLASSE && Config.DEBUG_LEVEL > 3) Log.v("AccesService",
                    "Autorisation Notification déja accordée !");
            return;  // il n'y a rien à faire
        }
        // Préparation de la demande d'autorisation (si l'application n'a pas déjà démarré)
        if (!force_demande) {
            ActivityResultLauncher<String> requestPermissionLauncher = cfg.mainActivity.registerForActivityResult(new ActivityResultContracts.RequestPermission(), autorise -> {
                if (!autorise) {
                    // L'utilisateur n'a pas donné l'autorisation
                    Snackbar.make(Objects.requireNonNull(cfg.mainActivity.binding.getRoot()),
                            mContext.getString(R.string.msg_autorisation_notification_non_accordee),
                            Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                }
            });
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    cfg.mainActivity, Manifest.permission.POST_NOTIFICATIONS)) {
                // Si on a pas l'autorisation de notification, on affiche un message d'information
                // avec un callback pour le bouton Ok qui lance la demande d'autorisation
                AlertDialog.Builder builder = new AlertDialog.Builder(cfg.mainActivity.binding.getRoot().getContext());
                builder.setTitle(mContext.getString(R.string.titre_requete_autorisation));
                builder.setMessage(mContext.getString(R.string.msg_autorisation_notification_neccessaire));
                builder.setCancelable(false);
                builder.setPositiveButton("Ok", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                });
                builder.show();
            } else { // Sinon on demande directement l'autorisation
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            if (DEBUG_CLASSE && Config.DEBUG_LEVEL > 3) Log.v("AccesService",
                    "Requête d'autorisation Notification avec le code 25 effectuée");
            ActivityCompat.requestPermissions(cfg.mainActivity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, MainActivity.CODE_REDEMANDE_PERMISSIONS_VIA_MENU);
        }
    }


    /** Envoi un message au service LocationUpdateService en utilisant le code message
     *  fournit en argument
     * @param codeMsg  Code (int) de la commande à exécuter pour le service
     */
    public void envoiMsgService(int codeMsg) {
        Message msg = Message.obtain(null, codeMsg);
        try {
            //msg.replyTo = mMessenger;  // Si on veut une réponse
            if (mService != null)  mService.send(msg);
        } catch (RemoteException e) {
            if (DEBUG_CLASSE && Config.DEBUG_LEVEL > 0) Log.d("AccesService",
                    "Erreur dans l'envoi du message " + codeMsg);
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
            if (DEBUG_CLASSE && Config.DEBUG_LEVEL > 0) Log.d("AccesService",
                    "Erreur dans l'envoi du message " + codeMsg);
        }
    }

    public void connexionService() {
        mContext.bindService(new Intent(mContext,
                LocationUpdateService.class), mConnection, Context.BIND_AUTO_CREATE);
        try {
            sleep(50);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (mService != null) {
            mIsBound = true;    // Drapeau indiquant que la liaison est effective
        }
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
            if (DEBUG_CLASSE && Config.DEBUG_LEVEL > 1) Log.d("AccesService", "*** Le service est déjà en cours d'exécution !");
            connexionService();  // Dans ce cas on se contente de se reconnecter au service
        } else { // Sinon on le démarre
            if (DEBUG_CLASSE && Config.DEBUG_LEVEL > 3) Log.v("AccesService", "Démarrage du service ...");
            // D'abord on crée un intent
            Intent serviceIntent = new Intent(mContext, LocationUpdateService.class);
            // puis on prépare un bundle avec les données pour le serveur
            serviceIntent.putExtra("data", creationBundleParametresService(cfg));
            mContext.startService(serviceIntent);

            connexionService();
        }
    }

    public void arreteService() {
        if (DEBUG_CLASSE && Config.DEBUG_LEVEL > 3) Log.v("AccesService", "Arrêt du service ...");
        envoiMsgService(LocationUpdateService.MSG_ARRET_SERVICE);
        deconnexionService();
    }

    /**
     * Met à jour les paramètres du service à partir de la configuration fournie en argument
     * @param config   Objet configuration contenant les paramètres à jour
     */
    public void majParametresService(Config config) {
        if (DEBUG_CLASSE && Config.DEBUG_LEVEL > 3) Log.d("AccesService", "demande de maj avec mIsbound = " + mIsBound);
        if (!mIsBound) return;   // Si pas de connexion au service, on ne fait rien
        if (config == null) config = cfg;
        Bundle bundle;
        if (config != null) {
            bundle = creationBundleParametresService(config);
        } else {
            // Si pas de config, on ne met pas à jour
            return;
        }
        envoiMsgService(LocationUpdateService.MSG_MAJ_CONFIG, bundle);
    }
}
