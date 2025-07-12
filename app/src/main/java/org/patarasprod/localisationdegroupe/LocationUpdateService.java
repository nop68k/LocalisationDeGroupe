package org.patarasprod.localisationdegroupe;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Classe du service d'envoi en tâche de fond de la localisation
 *
 */
public class LocationUpdateService extends Service {
    private static final boolean DEBUG_CLASSE = true;  // Drapeau pour autoriser les message de debug dans la classe

    public final long INTERVALLE_MAJ_MAX = 12*60*60*1000;   // Temps maxi entre 2 m.a.j. (en ms)

    private final String ID_CHANNEL_NOTIFICATION = "location_channel";  // Id du channel de notification
    private static final int ID_NOTIFICATION = 42;
    public static final int MSG_PREMIERE_CONNEXION = 0;
    public static final int MSG_DECONNEXION = 1;
    public static final int MSG_ARRET_SERVICE = 2;   // Demande l'arrêt du service
    public static final int MSG_MAJ_CONFIG = 3;   // Demande la mise à jour des paramètres de config
    public static final int MSG_MAJ_NOTIFICATION = 4;
    public static final int MSG_MAJ_TEXTE_NOTIFICATION = 5;

    // Références vers les objets utilisées par le service
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Context mContext;
    protected PendingIntent contentIntent = null;   // Pour lancer l'appli si on appui sur sa notification

    // Configurations pour le fonctionnement du service
    protected Config cfg;
    protected String nomUtilisateur;
    protected String adresseServeur;
    protected int portServeur;
    protected long intervalleEnvoiEnFond;

    // Variables utilisées par le service pour son fonctionnement
    private Location localisation; // Contient la localisation récupérée par le service
    public Position maPosition = null;  // Position de l'utilisateur
    public String date_dernier_envoi = "jamais";   // Date du dernier envoi réussi vers le serveur



    public LocationUpdateService() {
        // Constructeur vide pour éviter les erreurs dans le manifest
    }

    /**
     * Handler pour dialoguer (récupérer les messages) avec l'activité principale
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PREMIERE_CONNEXION:
                    if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE) Log.d("LocationUpdateService", "Connexion au service");
                    break;
                case MSG_DECONNEXION:
                    if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE) Log.d("LocationUpdateService", "Déconnexion du service");
                    break;
                case MSG_ARRET_SERVICE:
                    if (Config.DEBUG_LEVEL > 2 && DEBUG_CLASSE) Log.d("LocationUpdateService", "Arrêt du service demandé");
                    stopLocationUpdateService();
                    break;
                case MSG_MAJ_CONFIG:
                    if (Config.DEBUG_LEVEL > 4 && DEBUG_CLASSE) Log.d("LocationUpdateService", "Mise à jour des paramètres du service");
                    //
                    Bundle b = msg.getData();
                    Log.d("LocationUpdateService", "NomUtilisateur = " + b.getString("nom") +
                            " \tadresseServeur = " + b.getString("adresse") +
                            " \tportServeur = " + b.getInt("port") +
                            " \tintervalleEnvoiEnFond = " + b.getLong("intervalle_envoi_en_fond"));
                    // Teste si l'intervalle entre 2 envoi a changé
                    if (intervalleEnvoiEnFond != msg.getData().getLong("intervalle_envoi_en_fond")) {
                        // Si oui, il faut arrêter les requête de maj de position, les reparamètrer
                        // et relancer les requêtes
                        fusedLocationClient.removeLocationUpdates(locationCallback);
                        majConfigurationService(msg.getData());
                        startLocationUpdates();
                    } else {
                        majConfigurationService(msg.getData());
                    }
                    break;
                case MSG_MAJ_NOTIFICATION:
                    majNotificationService();
                    break;
                case MSG_MAJ_TEXTE_NOTIFICATION:
                    // Met à jour la notification avec le texte stockée dans la clé 'texte_notification'
                    // des données jointes
                    // TODO
                    break;
                case 999:
                    //utilisation d'arguments avec :  int mValue = msg.arg1;
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    // Instanciation de l'handler pour la communication
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }


    public void majConfigurationService(Bundle bundle) {
        this.nomUtilisateur = bundle.getString("nom");
        this.adresseServeur = bundle.getString("adresse");
        this.portServeur = bundle.getInt("port");
        this.intervalleEnvoiEnFond = bundle.getLong("intervalle_envoi_en_fond");
    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Config.DEBUG_LEVEL > 2 && DEBUG_CLASSE) Log.d("LocationUpdateService", "Démarrage du service");

        this.mContext = this.getBaseContext();
        // On commence par récupérer les données de configuration stockées dans l'intent s'il est présent
        if (intent != null) {
            Bundle donneesServeur = intent.getBundleExtra("data");
            if (donneesServeur != null) {
                majConfigurationService(donneesServeur);
                if (Config.DEBUG_LEVEL > 1 && DEBUG_CLASSE) Log.d("LocationUpdateService", "Données récupérées : \tNom : " + nomUtilisateur +
                        " \tServeur : " + adresseServeur + ":" + portServeur);
            } else Log.d("LocationUpdateService",
                    "ERREUR : Impossible de récupérer les données de configuration");
        }

        if (demandeAutorisationsLocalisation()) {
            LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                localisation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                localisation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            } else {
                // Aucune méthode de localisation dispo (en principe on arrive pas là car test déjà fait)
                stopLocationUpdateService();
            }
            if (localisation != null) {
                maPosition = new Position(nomUtilisateur, localisation.getLatitude(), localisation.getLongitude());
            }
        }
        prepareForegroundNotification();
        //showNotification();
        startLocationUpdates();
        return START_STICKY;
    }

    private boolean demandeAutorisationsLocalisation() {
        /* Fonction qui vérifie qu'on a les autorisations de localisation et de localisation en
         * tâche de fond et les demande le cas échant. Renvoie true si on a toutes les autorisations
         * et false sinon.
         */
        if (ActivityCompat.checkSelfPermission(this.mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this.mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        if (ActivityCompat.checkSelfPermission(this.mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this.mContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // On a eu une autorisation de localisation, maintenant on va demander à l'avoir en tâche de fond
            if (ActivityCompat.checkSelfPermission(this.mContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 1);
                }
            }
            if (ActivityCompat.checkSelfPermission(this.mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        } else {
            return false;
        }
        Log.d("LocationUpdateService", "Les autorisations sont toutes accordées");
        return true;
    }

    protected Notification creationNotificationService() {
        Notification notification;
        notification = new NotificationCompat.Builder(this, ID_CHANNEL_NOTIFICATION)
                .setContentTitle(getString(R.string.titre_notif_service))
                .setContentText(getString(R.string.texte_notif_date_dernier_envoi) + " "+ date_dernier_envoi)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(contentIntent)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)  // Affichage immédiat de la notification
                .setOngoing(true)   // Notification non-dissipable
                .build();
        return notification;
    }

    /**
     * Met à jour la notification du service en arrire plan avec la date de dernier envoi réussi
     */
    protected void majNotificationService() {
        // Met à jour la notification avec la date de dernier envoi des données
        Notification notification;
        notification = new NotificationCompat.Builder(this, ID_CHANNEL_NOTIFICATION)
                .setContentTitle(getString(R.string.titre_notif_service))
                .setContentText(getString(R.string.texte_notif_date_dernier_envoi) + " "+ date_dernier_envoi)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(contentIntent)
                .setSilent(true)
                .build();
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(ID_NOTIFICATION, notification);
    }

    private void prepareForegroundNotification() {
        // Création de la notification pour le service en premier plan
        // Le PendingIntent sert à lancer notre application si l'utilisateur touche la notification
        contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_MUTABLE);
        NotificationChannel channel = new NotificationChannel(ID_CHANNEL_NOTIFICATION, "Location Service", NotificationManager.IMPORTANCE_DEFAULT);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        Notification notification = creationNotificationService();
        if (Config.DEBUG_LEVEL > 1 && DEBUG_CLASSE) Log.d("LocationUpdateService", "Notification = " + notification);
        startForeground(ID_NOTIFICATION, notification);
    }

    /**
     *  Démarre la demande de mise à jour périodique de la localisation
     */
    private void startLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        LocationRequest.Builder request = new LocationRequest.Builder(Long.MAX_VALUE);
        //request.setMinUpdateDistanceMeters(1.0f); // Distance mini de m.a.j.
        request.setMinUpdateIntervalMillis(intervalleEnvoiEnFond*1000); // secondes converties en ms
        request.setMaxUpdateDelayMillis(INTERVALLE_MAJ_MAX);
        request.setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location == null) {
                    if (Config.DEBUG_LEVEL > 0 && DEBUG_CLASSE) Log.d("LocationUpdateService", "location == null à l'appel de 'onLocationResult'");
                    return;
                }
                maPosition = new Position(nomUtilisateur,location.getLatitude(), location.getLongitude());

                Log.d("LocationUpdateService", "Lat: " + location.getLatitude() + " Lng: " + location.getLongitude()
                + " \tcfg=" + cfg + " \tmaPosition:" + maPosition.toString());
                envoiLocalisation(maPosition);
            }
            public void envoiLocalisation(Position maPosition) {
                // La création de socket doit se faire dans un autre Thread obligatoirement
                Thread threadCommunication = new Thread(new Runnable() {
                    @SuppressLint("SimpleDateFormat")
                    @Override
                    public void run() {
                        Socket socketClient = null;
                        PrintWriter out = null;
                        try {
                            //Création d'un socket
                            if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE) Log.d("LocationUpdateService",
                                    "Création du socket sur " + adresseServeur + ":" + portServeur);
                            socketClient = new Socket(adresseServeur, portServeur);
                            if (Config.DEBUG_LEVEL > 5 && DEBUG_CLASSE)
                                Log.d("LocationUpdateService",
                                    "Socket = " + socketClient);
                            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socketClient.getOutputStream())), true);
                            String msg = Communication.CARACTERE_COMMUNICATION_UNIDIRECTIONNELLE + maPosition.toString(false);
                            if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE) Log.d("LocationUpdateService", "Envoi de " + msg);
                            out.println(msg);
                            date_dernier_envoi = new SimpleDateFormat("EEE d MMM HH'h'mm").format(new Date());
                            majNotificationService();  // Met à jour la notification avec la date du dernier envoi
                        } catch (Exception e) { //(IOException e)
                            Log.d("LocationUpdateService", "EXCEPTION : " + e);
                        } finally {
                            if (out != null) {
                                out.flush();
                                out.close();
                            }
                            if (socketClient != null) {
                                try {
                                    socketClient.close();
                                } catch (IOException ex) {
                                    if (Config.DEBUG_LEVEL > 1 && DEBUG_CLASSE) Log.d("LocationUpdateService", "ERREUR de fermeture du socket");
                                }
                            }
                        }
                    }
                });
                threadCommunication.start();
            }
        };

        if (ActivityCompat.checkSelfPermission(this.mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            demandeAutorisationsLocalisation();
            return;
        }
        fusedLocationClient.requestLocationUpdates(request.build(), locationCallback, Looper.getMainLooper());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    public void setCfg(Config cfg) {
        this.cfg = cfg;
    }

    public void stopLocationUpdateService() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        stopForeground(true);
        stopSelf();
    }
}

