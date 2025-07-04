package org.patarasprod.localisationdegroupe;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.location.LocationRequest.Builder;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import java.util.Locale;


public final class LocalisationGPS implements LocationListener {

    private final Context mContext;

    Config cfg;                      // Référence vers la config de l'appli

    public boolean GPSDisponible = false;        // flag for GPS status
    boolean NetworkDisponible = false;           // flag for network status

    Location localisation;      // localisation
    double latitude = 1000.0; // latitude
    double longitude = 1000.0; // longitude


    // The minimum distance to change Updates in meters
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 1 mètre mini

    private static final long MIN_TIME_BW_UPDATES = 5000; // 5 secondes mini entre maj position
    private static final long DEFAULT_TIME_BW_UPDATES = 10000; // 10 secondes entre maj position
    private static final String FORMAT_AFFICHAGE_POSITION = "%.7f";
    private static final String FORMAT_AFFICHAGE_ALTITUDE = "%.0f";

    Locale localeEn = new Locale("en", "UK");  // Pour avoir le point en séparateur décimal
//    Locale localeFr = new Locale("fr", "FR");  // Pour avoir la virgule en séparateur décimal

    // Declaring a Location Manager
    public LocationManager locationManager;

    // Location request pour utilisation de FusedLocationProvider
    LocationRequest locationRequest;

    public LocalisationGPS(Context context, Config config) {
        this.mContext = context;
        cfg = config;
        if (Config.DEBUG_LEVEL > 3) Log.v("LocalisationGPS", "Initialisation du système");
        initialisations();



        getLocalisation();
    }

    @SuppressLint("MissingPermission")
    private void initialisations() {
        try {
            // Vérification des autorisations
            if (demandeAutorisationsLocalisation()){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    locationRequest = new Builder(DEFAULT_TIME_BW_UPDATES)
                            .setMinUpdateIntervalMillis(MIN_TIME_BW_UPDATES)
                            .build();
                }

                locationManager = (LocationManager) mContext
                        .getSystemService(Context.LOCATION_SERVICE);

                // Est-ce que la localisation par le réseau (Network) est dispo ?
                NetworkDisponible = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                if (Config.DEBUG_LEVEL > 1) Log.v("LocalisationGPS", "Network disponible :" + NetworkDisponible);

                // Le GPS est-il disponible ?
                GPSDisponible = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                if (Config.DEBUG_LEVEL > 1) Log.v("LocalisationGPS", "GPS disponible :" + GPSDisponible);

                metEnPlaceActualisationPosition();
            }
        } catch (Exception e) {
            if (Config.DEBUG_LEVEL > 1) Log.d("LocalisationGPS", "Problème lors de l'initialisation : " + e);
        }
    }

    /**
     * Fonction qui vérifie qu'on a les autorisations de localisation et de localisation en
     * tâche de fond et les demande le cas échant.
     * @return Renvoie true si on a toutes les autorisations et false sinon.
     */
    public boolean demandeAutorisationsLocalisation() {
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
        return true;
    }

    /**
     * Récupère la localisation actuelle
     * @return localisation actuelle (objet Location) ou null si échec
     */
    @SuppressLint("MissingPermission")
    public Location getLocalisation() {
        if (demandeAutorisationsLocalisation()) {
            locationManager = (LocationManager) mContext
                    .getSystemService(Context.LOCATION_SERVICE);
            NetworkDisponible = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            GPSDisponible = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (!GPSDisponible && !NetworkDisponible) {
                // no network provider is enabled
                if (Config.DEBUG_LEVEL > 0) Log.v("LocalisationGPS", "Aucun accès à une méthode de localisation");
                return null;
            } else if (GPSDisponible) {
                localisation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } else {
                localisation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (localisation != null) {
                latitude = localisation.getLatitude();
                longitude = localisation.getLongitude();
            }
            if (cfg.maPosition != null) {
                cfg.maPosition.majPosition(latitude, longitude);
            } else cfg.maPosition = new Position(cfg.nomUtilisateur, latitude, longitude);
            actualise_position();
            return localisation;
        }
        return null;
    }

    /**
     * Fonction pour mettre en place l'actualisation régulière de la position
     * Basé sur le LocationManager et la méthode requestLocationUpdates
     */
    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    public void metEnPlaceActualisationPosition() {
        if (demandeAutorisationsLocalisation()) {
            locationManager = (LocationManager) mContext
                    .getSystemService(Context.LOCATION_SERVICE);
            NetworkDisponible = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            GPSDisponible = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (!GPSDisponible && !NetworkDisponible) {
                // no network provider is enabled
                if (Config.DEBUG_LEVEL > 0) Log.v("LocalisationGPS", "Aucun accès à une méthode de localisation");
                return ;
            } else if (GPSDisponible) {
                localisation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } else {
                localisation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (localisation != null) {
                latitude = localisation.getLatitude();
                longitude = localisation.getLongitude();
                if (cfg.maPosition != null) {
                    cfg.maPosition.majPosition(latitude, longitude);
                } else cfg.maPosition = new Position(cfg.nomUtilisateur, latitude, longitude);
                actualise_position();
            }
            // Demande l'actualisation si disponible
            if (NetworkDisponible) locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    cfg.intervalleMesureSecondes * 1000,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
            if (GPSDisponible) locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    cfg.intervalleMesureSecondes * 1000,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
        }
    }

    /**
     * Actualise la position affichée dans l'onglet "Position"
     */
    @SuppressLint("DefaultLocale")
    public void actualise_position() {
        if (cfg.textViewLocalisation != null) {
            cfg.textViewLocalisation.setText(coordsSurDeuxLignes());
        }
        if (cfg.textViewAltitude != null) {
            String texte;
            if (localisation != null && localisation.hasAltitude()) {
                texte = String.format(FORMAT_AFFICHAGE_ALTITUDE, localisation.getAltitude()) + " m";
            } else {
                texte = "N/A";
            }
            cfg.textViewAltitude.setText(texte);
        }
        if (cfg.textViewLatitude != null) {
            cfg.textViewLatitude.setText(conversionEnDegresMinutesSecondes(latitude));
        }
        if (cfg.textViewLongitude != null) {
            cfg.textViewLongitude.setText(conversionEnDegresMinutesSecondes(longitude));
        }
        if (cfg.handlerMainThread != null && cfg.fragment_position != null) { // Si possible
            // On crée un message pour demander au thread principal de mettre à jour le fragment position
            Message msg = Message.obtain(cfg.handlerMainThread, MainActivity.MSG_MAJ_VIEW);
            msg.obj = cfg.fragment_position.getView();
            cfg.handlerMainThread.sendMessage(msg);
        }
    }

    /**
     * Arrêt des mises à jour de la position (quand on ferme l'application)
     */
    public void arretMisesAJour() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }


    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (Config.DEBUG_LEVEL > 4) Log.v("LocalisationGPS", "Position modifiée");
        getLocalisation();
        actualise_position();
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        // TODO
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        // TODO
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (Config.DEBUG_LEVEL > 3) Log.v("LocalisationGPS","Status modifié");
        // TODO
    }

    public String coordsSurDeuxLignes() {
        return String.format(localeEn, FORMAT_AFFICHAGE_POSITION,latitude) + "N\n"
               + String.format(localeEn, FORMAT_AFFICHAGE_POSITION, longitude) + "E";
    }
    public String conversionEnDegresMinutesSecondes(double angle) {
        String signe = "";
        if (angle < 0) {
            signe = "-";
            angle = - angle;
        }
        int degres = (int) angle;
        int minutes = (int) ((angle - ((double)degres) ) * 60);
        double secondes = (angle - ((double)degres) - ((double)minutes/60)) * 3600;
        return signe + degres + "°" + minutes + "'" + String.format(localeEn, "%.3f", secondes) + '"';
    }
}