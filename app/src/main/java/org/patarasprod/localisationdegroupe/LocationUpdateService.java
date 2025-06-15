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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/** Classe du service d'envoi en tâche de fond de la localisation
 *
 */
public class LocationUpdateService extends Service {
    private static final boolean DEBUG_CLASSE = true;  // Drapeau pour autoriser les message de debug dans la classe
    public final long INTERVALLE_MAJ_MINI = 3000;   // Temps mini entre 2 m.a.j. (en ms)
    public final long INTERVALLE_MAJ_MAX = 10*1000;   // Temps maxi entre 2 m.a.j. (en ms)

    public static final int MSG_PREMIERE_CONNEXION = 0;
    public static final int MSG_DECONNEXION = 1;
    public static final int MSG_ARRET_SERVICE = 2;   // Demande l'arrêt du service
    public static final int MSG_MAJ_CONFIG = 3;   // Demande la mise à jour des paramètres de config

    // Références vers les objets utilisées par le service
    private NotificationManager mNotifManager;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Context mContext;

    // Configurations pour le fonctionnement du service
    protected Config cfg;
    protected String nomUtilisateur;
    protected String adresseServeur;
    protected int portServeur;
    protected long intervalleEnvoiEnFond;

    // Variables utilisées par le service pour son fonctionnement
    private Location localisation; // Contient la localisation récupérée par le service
    public Position maPosition = null;  // Position de l'utilisateur



    public LocationUpdateService() {
        // Constructeur vide pour éviter les erreurs dans le manifest
    }

    public LocationUpdateService(Config cfg) {
        // Constructeur vide pour éviter les erreurs dans le manifest
        this.cfg = cfg;
    }

    /**
     * Handler of incoming messages from clients.
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
                case 3:
                    int mValue = msg.arg1;
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());


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
                ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 1);
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

    private void prepareForegroundNotification() {
        // Création de la notification pour le service en premier plan
        NotificationChannel channel = new NotificationChannel("location_channel", "Location Service", NotificationManager.IMPORTANCE_DEFAULT);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        Notification notification = new NotificationCompat.Builder(this, "location_channel")
                .setContentTitle("Suivi GPS en arrière-plan")
                .setContentText("Localisation de groupe en arrière plan")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        if (Config.DEBUG_LEVEL > 1 && DEBUG_CLASSE) Log.d("LocationUpdateService", "Notification = " + notification);
        /*
        mNotifManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mNotifManager.notify(R.string.service_demarre, notification);
        */
        startForeground(1, notification);
    }

    /**
     * Show a notification while this service is running.
     */
    /*
    private void showNotification() {
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_MUTABLE);

        // Set the info for the views that show in the notification panel.
        NotificationChannel channel = new NotificationChannel("location_channel2", "Location Service2", NotificationManager.IMPORTANCE_DEFAULT);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        Notification notification = new NotificationCompat.Builder(this, "location_channel2")
                .setContentTitle("Suivi GPS en arrière-plan2")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        Log.d("LocationUpdateService", "Notification = " + notification);
        mNotifManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mNotifManager.notify(R.string.service_demarre, notification);
    }
*/

    /** Démarre la demande de mise à jour périodique de la localisation
     *
     */
    private void startLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        LocationRequest.Builder request = new LocationRequest.Builder(Long.MAX_VALUE);
        //request.setMinUpdateDistanceMeters(1.0f); // Distance mini de m.a.j.
        request.setMinUpdateIntervalMillis(INTERVALLE_MAJ_MINI); // 3 secondes
        request.setMaxUpdateDelayMillis(INTERVALLE_MAJ_MAX);
        request.setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location == null) {
                    if (Config.DEBUG_LEVEL > 0 && DEBUG_CLASSE) Log.d("LocationUpdateService", "location == null à l'appel de 'onLocationResult'");
                    return;
                }
                if (maPosition != null) {
                    maPosition.majPosition(location.getLatitude(), location.getLongitude());
                } else {
                    maPosition = new Position(nomUtilisateur,location.getLatitude(), location.getLongitude());
                }

                Log.d("LocationUpdateService", "Lat: " + location.getLatitude() + " Lng: " + location.getLongitude()
                + " \tcfg=" + cfg + " \tmaPosition:" + maPosition.toString());
                envoiLocalisation(maPosition);
            }
            public void envoiLocalisation(Position maPosition) {
                // La création de socket doit se faire dans un autre Thread obligatoirement
                Thread threadCommunication = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Socket socketClient = null;
                        PrintWriter out = null;
                        try {
                            //Création d'un socket
                            if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE) Log.d("LocationUpdateService", "Création du socket sur " + adresseServeur + ":" + portServeur);
                            socketClient = new Socket(adresseServeur, portServeur);
                            if (socketClient != null) {
                                if (Config.DEBUG_LEVEL > 5 && DEBUG_CLASSE) Log.d("LocationUpdateService", "Socket = " + socketClient);
                                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socketClient.getOutputStream())), true);
                                if (out != null) {
                                    String msg = Communication.CARACTERE_COMMUNICATION_UNIDIRECTIONNELLE + maPosition.toString();
                                    if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE) Log.d("LocationUpdateService", "Envoi de " + msg);
                                    out.println(msg);
                                }
                            }
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
                                return;
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

    /**
     * @param intent The Intent that was used to bind to this service,
     *               as given to {@link android.content.Context#bindService
     *               Context.bindService}.  Note that any extras that were included with
     *               the Intent at that point will <em>not</em> be seen here.
     * @return
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    public void test(String msg) {
        Log.d("LocationUpdateService", "Message : " + msg);
        Log.d("LocationUpdateService", "cfg : " + cfg);
    }

    public void setCfg(Config cfg) {
        this.cfg = cfg;
    }

    public void stopLocationUpdateService() {
        stopForeground(true);
        stopSelf();
    }
}

