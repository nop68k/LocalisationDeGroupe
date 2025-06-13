package org.patarasprod.localisationdegroupe;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.patarasprod.localisationdegroupe.databinding.FragmentCarteBinding;

/**
 * A placeholder fragment containing a simple view.
 */
public class FragmentCarte extends Fragment {

    private static final boolean DEBUG_CLASSE = false;  // Drapeau pour autoriser les message de debug dans la classe
    private FragmentCarteBinding binding;
    Config cfg;
    RotationGestureOverlay mRotationGestureOverlay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        cfg = ((MainActivity) requireActivity()).recupere_configuration();
        if (Config.DEBUG_LEVEL > 3) Log.v("Fragment Carte", "Création du fragment Carte");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        cfg = ((MainActivity) requireActivity()).recupere_configuration();
        // On sauvegarde la référence au fragment crée
        cfg.fragment_carte = this;

        binding = FragmentCarteBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Mise en place de la carte
        if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE) Log.v("Fragment Carte","onCreateView FragmentCarte cfg = " + cfg);
        cfg.map = binding.map;

        // Nécessaire pour que la mapView ne soit pas détachée de la fenêtre dès qu'on quitte l'onglet
        binding.map.setDestroyMode(false);

        cfg.map.setTileSource(TileSourceFactory.MAPNIK);
        //Configuration du user-agent pour indiquer à Mapnik qui utilise leurs services
        Configuration.getInstance().setUserAgentValue("Localisation-de-groupe");

        //Configuration de la carte
        cfg.map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        cfg.map.setMultiTouchControls(true);   // Possibilité de zoomer
        mRotationGestureOverlay = new RotationGestureOverlay(cfg.map);
        mRotationGestureOverlay.setEnabled(true);
        cfg.map.setMultiTouchControls(true);
        cfg.map.getOverlays().add(this.mRotationGestureOverlay);

        // Centrage et réglage du zoom
        cfg.mapController = cfg.map.getController();
        cfg.mapController.setZoom(cfg.niveauZoomCarte);
        if (cfg.localisation != null) {
            Location maLocalisation = cfg.localisation.getLocalisation();
            if (maLocalisation != null) cfg.maPositionSurLaCarte =
                    new GeoPoint(maLocalisation.getLatitude(), maLocalisation.getLongitude());
        } else {
            cfg.maPositionSurLaCarte = new GeoPoint(cfg.centreCarte.getLatitude(),
                    cfg.centreCarte.getLongitude());
        }
        cfg.mapController.setCenter(cfg.centreCarte);

        //Compas en surimpression
        cfg.mCompassOverlay = new CompassOverlay((Context)requireActivity(),
                new InternalCompassOrientationProvider((Context)requireActivity()), cfg.map);
        cfg.mCompassOverlay.enableCompass();
        cfg.map.getOverlays().add(cfg.mCompassOverlay);

        // Mylocation : Surimpression de ma position
        cfg.maPosition_LocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider((Context)requireActivity()), cfg.map);
        cfg.maPosition_LocationOverlay.enableMyLocation();
        cfg.map.getOverlays().add(cfg.maPosition_LocationOverlay);

        // Scalemap : échelle en surimpression
        final DisplayMetrics dm = requireActivity().getResources().getDisplayMetrics();
        ScaleBarOverlay mScaleBarOverlay = new ScaleBarOverlay(cfg.map);
        mScaleBarOverlay.setCentred(true);
        //play around with these values to get the location on screen in the right place for your application
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);
        cfg.map.getOverlays().add(mScaleBarOverlay);

        cfg.gestionPositionsUtilisateurs.majPositionsSurLaCarte();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE) Log.v("Fragment Carte","onResume FragmentCarte cfg = " + cfg);
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        if (cfg.map != null) {
            if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE) Log.v("Fragment Carte","cfg.map = " + cfg.map + "  - Appel de map.onResume()");
            cfg.map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE) Log.v("Fragment Carte","onPause FragmentCarte cfg = " + cfg);
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        if (cfg.map != null) {
            if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE) Log.v("Fragment Carte","cfg.map = " + cfg.map + "  - Appel de map.onPause()");
            cfg.centreCarte = (GeoPoint) cfg.map.getMapCenter();
            cfg.niveauZoomCarte = cfg.map.getZoomLevelDouble();
            cfg.orientationCarte = cfg.map.getMapOrientation();
            cfg.map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
        }
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Restaure les caractéristiques de visualisation de la carte (centre, zoom, orientation
        cfg.map.post(
                new Runnable() {
                    @Override
                    public void run() {
                        cfg.mapController.setCenter(cfg.centreCarte);
                        cfg.mapController.setZoom(cfg.niveauZoomCarte);
                        cfg.map.setMapOrientation(cfg.orientationCarte);
                    }
                }
        );
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}