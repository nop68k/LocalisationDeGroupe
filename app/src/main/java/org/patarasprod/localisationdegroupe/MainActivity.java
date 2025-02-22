package org.patarasprod.localisationdegroupe;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.recyclerview.widget.RecyclerView;

import org.patarasprod.localisationdegroupe.databinding.ActivityMainBinding;
import org.patarasprod.localisationdegroupe.views.RecyclerViewAdapterListeUtilisateurs;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    protected Config cfg;
    org.patarasprod.localisationdegroupe.databinding.ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cfg = new Config(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (Config.DEBUG_LEVEL > 3) Log.v("mainActivity", "SetContentView du Oncreate de l'activité effectué avec succès");
        cfg.fragmentManager = getSupportFragmentManager();

        //setSupportActionBar(binding.toolbar);

/*
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
*/
        initialisations();

        /*
        Navigation.findNavController(this,
                R.id.nav_host_fragment_content_main).popBackStack();
        Navigation.findNavController(this,
                R.id.nav_host_fragment_content_main).navigate(R.id.fragment_principal);

 */

    }
    private void initialisations() {
        cfg.localisation = new LocalisationGPS(this, cfg);
        cfg.com = new Communication(cfg);

        if (cfg.prefDiffuserMaPosition) {
            cfg.diffuserMaPosition = true;
            cfg.com.demarreDiffusionPositionAuServeur();
        }

    }

    /** Fonction de gestion des clics sur les boutons */
    public void onButtonClicked(final View v) {
        if (Config.DEBUG_LEVEL > 2) Log.v("mainActivity","***********Bouton appuyé : " + v.getId());
/*
        switch (v.getId()) {
            case R.id.button_tab1:
                Navigation.findNavController(this, R.id.nav_host_fragment_content_main).popBackStack();
                Navigation.findNavController(this, R.id.nav_host_fragment_content_main).navigate(R.id.fragment_1);
                break;
            case R.id.button_tab2:
                Navigation.findNavController(this, R.id.nav_host_fragment_content_main).popBackStack();
                Navigation.findNavController(this, R.id.nav_host_fragment_content_main).navigate(R.id.fragment_2);
                break;
            case R.id.button_tab3:
                Navigation.findNavController(this, R.id.nav_host_fragment_content_main).popBackStack();
                Navigation.findNavController(this, R.id.nav_host_fragment_content_main).navigate(R.id.fragment_3);
                break;
            default:
                if (Config.DEBUG_LEVEL > 2) Log.v("mainActivity","Bouton inconnu : " + v.getId());
        }

 */
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_menu_actualiser:
                if (cfg.fragment_3 != null) {
                    cfg.fragment_3.majTexteInfos();
                    if (cfg.recyclerViewPositions != null)
                        ( (RecyclerViewAdapterListeUtilisateurs) Objects.requireNonNull(cfg.recyclerViewPositions.getAdapter())).majListeUtilisateurs();
                    Objects.requireNonNull(cfg.fragment_3.getView()).invalidate();   // Pour forcer la mise à jour de l'affichage
                    //cfg.fragment_3.getView().requestLayout();
                    if (cfg.map != null) cfg.map.invalidate();
                    if (cfg.com != null) cfg.com.majIndicateurConnexion();
                }
                if (cfg.com != null) cfg.com.majIndicateurConnexion();
                return true;
            case R.id.item_menu_reinitialiser_positions:
                cfg.gestionPositionsUtilisateurs.reinitialise_positions();
                return true;
            case R.id.item_menu_redemander_autorisations:
                cfg.localisation.demandeAutorisationsLocalisation();
                cfg.com.demandeAutorisationInternet();
                cfg.handler = new Handler();
                cfg.handler.postDelayed(() -> cfg.localisation.getLocalisation(), 3000);
                return true;
            case R.id.item_menu_a_propos:
                AlertDialog.Builder builder = new AlertDialog.Builder(binding.getRoot().getContext());
                builder.setTitle("A propos...");
                builder.setMessage(cfg.MESSAGE_INFORMATION);
                builder.setCancelable(false);
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                builder.show();
                return true;
            case R.id.item_menu_quitter:
                // Annule l'exécution différée s'il y en a une
                if (cfg.handler != null) cfg.handler.removeCallbacksAndMessages(null);
                if (cfg.com != null) cfg.com.stoppeDiffusionPositionAuServeur();
                if (cfg.localisation != null) cfg.localisation.arretMisesAJour();
                if (cfg.gestionPositionsUtilisateurs != null)
                    cfg.gestionPositionsUtilisateurs.sauvegardePositions();
                this.finish();
                System.exit(0);
            default:
                // Rien
        }
        return super.onOptionsItemSelected(item);
    }
    public Config recupere_configuration() {
        return this.cfg;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Config.DEBUG_LEVEL > 2) Log.v("mainActivity","Méthode onResume appellée. cfg = "+ cfg);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Config.DEBUG_LEVEL > 2) Log.v("mainActivity","Méthode onPause appellée. cfg = "+ cfg);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (Config.DEBUG_LEVEL > 4) Log.v("mainActivity","*****************CREATION MENU *****************");
        getMenuInflater().inflate(R.menu.menu_principal, menu);
        return true;
    }
/*
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

 */

    @Override
    public void onDestroy() {
        super.onDestroy();
        cfg.sauvegardeToutesLesPreferences();
        cfg.gestionPositionsUtilisateurs.sauvegardePositions();
    }
}