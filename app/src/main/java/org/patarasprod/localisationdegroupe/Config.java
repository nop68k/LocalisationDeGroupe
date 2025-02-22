package org.patarasprod.localisationdegroupe;


import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import android.app.ActionBar;
import android.app.MediaRouteButton;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.ImageButton;
import android.widget.TextView;


import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.patarasprod.localisationdegroupe.views.RecyclerViewAdapterListeUtilisateurs;

public class Config {

    public static int DEBUG_LEVEL = 4; // Niveau d'expressivité des messages de debug (0 = aucun message)

    public static final String MESSAGE_INFORMATION = "Application de localisation de groupe\n" +
            "Version 1.1\n(Février 2025)";
    public static final String TEXTE_BOUTON_INFO = "Appui court -> centrer la carte sur la personne"+
            "\nAppui long -> appel programme externe";

    protected Context contexte;
    protected AssetManager manager;
    protected ActionBar barreTitre;
    public FragmentManager fragmentManager;
    public NavController navController;

    public ViewPager2 viewPager = null;
    public MyAdapter adapterViewPager;

    // *******************************************
    // *** Préférences (paramètres) sauvegardés
    // *******************************************
    private final String NOM_UTILISATEUR_PAR_DEFAUT = "Anonyme";
    public String nomUtilisateur = NOM_UTILISATEUR_PAR_DEFAUT;
    private final boolean PREF_DIFFUSER_POSITION_PAR_DEFAUT = false;
    public boolean prefDiffuserMaPosition = PREF_DIFFUSER_POSITION_PAR_DEFAUT;
    private final long INTERVALLE_MESURE_SECONDES_PAR_DEFAUT = 5;
    public long intervalleMesureSecondes = INTERVALLE_MESURE_SECONDES_PAR_DEFAUT;
    private final long INTERVALLE_MAJ_SECONDES_PAR_DEFAUT = 10;
    public long intervalleMajSecondes = INTERVALLE_MAJ_SECONDES_PAR_DEFAUT;
    public final String NOM_SERVEUR_PAR_DEFAUT = "Aucun serveur configuré";
    public String adresse_serveur = NOM_SERVEUR_PAR_DEFAUT;
    public final int PORT_SERVEUR_PAR_DEFAUT = 7777;
    public int port_serveur = PORT_SERVEUR_PAR_DEFAUT;


    // ***
    public Position maPosition;    //Position de l'utilisateur principal
    public GestionPositionsUtilisateurs gestionPositionsUtilisateurs;

    public Fragment_1 fragment_1;
    public Fragment_2 fragment_2;
    public Fragment_3 fragment_3;
    public Fragment_parametres fragment_parametres;
    public LocalisationGPS localisation;
    public MainActivity mainActivity = null;   // Référence vers la MainActivity (l'activité du programme)
    public Handler handler;   // Utilisé pour lancer des tâches différées
    public Handler handlerDiffusionPosition;  // Pour diffuser la position via internet

    // Gestion carte
    public MapView map = null;
    public IMapController mapController = null;
    public final double ZOOM_INITIAL_CARTE = 16.0;
    public final float LATITUDE_ORIGINE_PAR_DEFAUT = 0f;
    public final float LONGITUDE_ORIGINE_PAR_DEFAUT = 0f;
    public GeoPoint maPositionSurLaCarte = null;
    public GeoPoint centreCarte = null;
    public double niveauZoomCarte = ZOOM_INITIAL_CARTE;
    public float orientationCarte = 0f;
    public MyLocationNewOverlay maPosition_LocationOverlay;  // Affichage de ma position
    public CompassOverlay mCompassOverlay = null;   // Compas en surimpression sur la carte

    public final String NOM_FICHIERS_MARQUEURS = "marqueurs/map_marker_";
    public final int NB_FICHIERS_MARQUEURS = 50;  // Nombre de fichiers de marqueur différents



    public RecyclerView recyclerViewPositions;  // RecyclerView affichant les différentes positions

    public final String ERREUR_READLINE_SOCKET_DISTANT = "**Pas de réponse du serveur ! **";

    // Nb de caractère minimal pour une réponse complète, c.a.d. contenant toutes les positions
    public final int TAILLE_MINI_REPONSE_COMPLETE = 20;

    public Communication com = null;        // Instance de la classe Communication

    public String reponse = null ;  //réponse du serveur
    Thread threadCommunication = null;          // Pointeur vers le thread communication
    public boolean communicationEnCours = false; // Le thread d'envoi vers le serveur est dans la boucle d'envoi
    public boolean diffuserMaPosition = true;

    public int nbPositions = 0;    // Nombre de positions suivies

    // Instance de la gestion des positions des utilisateurs
    public GestionPositionsUtilisateurs positions;

    //TextView de l'interface
    public TextView textViewLocalisation = null;  // TextView où écrire la localisation
    public TextView textViewAltitude = null;      // TextView où écrire l'altitude
    public TextView textViewLatitude = null;      // TextView où écrire l'altitude
    public TextView textViewLongitude = null;      // TextView où écrire l'altitude

    public TextView texteInfos;    // TextView d'affichage des infos
    public ImageButton indicateurConnexionServeur ;  // Image indiquant qu'une connexion est active
    public FloatingActionButton centrerSurMaPosition;  // Bouton flottant pour centrer sur ma position
    public FloatingActionButton fabInfo;

    public String fragmentAffiche; // Fragment actuellement affiché
    private SharedPreferences sharedPreferences;   // Stockage persistant pour les paramètres
    private SharedPreferences.Editor editor;

    public Config(MainActivity activite) {
        Position.cfg = this;
        mainActivity = activite;
        contexte = activite.getBaseContext();
        // Mise en place du stockage persistant des paramètres par SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activite.getApplicationContext());
        editor = sharedPreferences.edit();
        chargePreferences();
        gestionPositionsUtilisateurs = new GestionPositionsUtilisateurs(this);
    }

    private void chargePreferences() {
        nomUtilisateur = sharedPreferences.getString("nomUtilisateur", NOM_UTILISATEUR_PAR_DEFAUT);
        prefDiffuserMaPosition = sharedPreferences.getBoolean("diffuserMaPosition", PREF_DIFFUSER_POSITION_PAR_DEFAUT);
        intervalleMesureSecondes = sharedPreferences.getLong("intervalleMesureSecondes", INTERVALLE_MESURE_SECONDES_PAR_DEFAUT);
        intervalleMajSecondes = sharedPreferences.getLong("intervalleMajSecondes", INTERVALLE_MAJ_SECONDES_PAR_DEFAUT);
        adresse_serveur = sharedPreferences.getString("nomServeur", NOM_SERVEUR_PAR_DEFAUT);
        port_serveur = sharedPreferences.getInt("portServeur", PORT_SERVEUR_PAR_DEFAUT);
        centreCarte = new GeoPoint((double) sharedPreferences.getFloat("centreCarte_latitude",
                   maPosition == null ? LATITUDE_ORIGINE_PAR_DEFAUT: (float)maPosition.latitude),
                (double) sharedPreferences.getFloat("centreCarte_longitude",
                   maPosition == null ? LONGITUDE_ORIGINE_PAR_DEFAUT: (float)maPosition.longitude));
        niveauZoomCarte = (double) sharedPreferences.getFloat("niveauZoomCarte", (float)ZOOM_INITIAL_CARTE);
        orientationCarte = sharedPreferences.getFloat("orientationCarte", 0f);
    }

    public void sauvegardePreference(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.commit();
    }
    public void sauvegardePreference(String key, int value) {
        editor.putInt(key, value);
        editor.commit();
    }
    public void sauvegardePreference(String key, long value) {
        editor.putLong(key, value);
        editor.commit();
    }
    public void sauvegardePreference(String key, String value) {
        editor.putString(key, value);
        editor.commit();
    }
    public void sauvegardePreference(String key, float value) {
        editor.putFloat(key, value);
        editor.commit();
    }
    public void sauvegardeToutesLesPreferences() {
        sauvegardePreference("nomUtilisateur", nomUtilisateur);
        sauvegardePreference("diffuserMaPosition", prefDiffuserMaPosition);
        sauvegardePreference("intervalleMesureSecondes", intervalleMesureSecondes);
        sauvegardePreference("intervalleMajSecondes", intervalleMajSecondes);
        sauvegardePreference("nomServeur", adresse_serveur);
        sauvegardePreference("portServeur", (int)port_serveur);
        sauvegardePreference("centreCarte_latitude", (float)centreCarte.getLatitude());
        sauvegardePreference("centreCarte_longitude", (float)centreCarte.getLongitude());
        sauvegardePreference("niveauZoomCarte", (float)niveauZoomCarte);
        sauvegardePreference("orientationCarte", orientationCarte);
    }
}
