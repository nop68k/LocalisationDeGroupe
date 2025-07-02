package org.patarasprod.localisationdegroupe;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import org.patarasprod.localisationdegroupe.databinding.ActivityMainBinding;
import org.patarasprod.localisationdegroupe.views.RecyclerViewAdapterListeUtilisateurs;

import java.util.Objects;

public class MainActivity extends AppCompatActivity implements Handler.Callback {
    private static final boolean DEBUG_CLASSE = false;  // Drapeau pour autoriser les message de debug dans la classe
    public static final int MSG_MAJ_VIEW = 1;   // Message pour mettre à jour la vue

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

        initialisations();
    }
    private void initialisations() {
        // Instanciation de l'handler pour la communication
        cfg.handlerMainThread = new Handler(this);

        cfg.localisation = new LocalisationGPS(this, cfg);
        cfg.com = new Communication(cfg);

        if (cfg.prefDiffuserMaPosition) {
            cfg.diffuserMaPosition = true;
            cfg.com.demarreDiffusionPositionAuServeur();
        }

        // On instancie la classe de communication avec le service en arrière plan
        cfg.accesService = new AccesService(cfg, getApplicationContext());
        // Et si c'est dans les préférences, on lance le service de localisation en continu
        if (cfg.prefDiffuserEnFond) cfg.accesService.demarrageService();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_menu_actualiser:
                if (cfg.fragment_infos != null) {
                    cfg.fragment_infos.majTexteInfos();
                    if (cfg.recyclerViewPositions != null)
                        ( (RecyclerViewAdapterListeUtilisateurs) Objects.requireNonNull(cfg.recyclerViewPositions.getAdapter())).majListeUtilisateurs();
                    Objects.requireNonNull(cfg.fragment_infos.getView()).invalidate();   // Pour forcer la mise à jour de l'affichage
                    if (cfg.map != null) cfg.map.invalidate();
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
            case R.id.item_menu_infos_debug:
                if (cfg != null) {
                    item.setChecked(!item.isChecked());   // inverse l'état de l'item
                    cfg.itemMenuInfosDebug = item.isChecked();
                    cfg.sauvegardePreference("itemMenuInfosDebug", cfg.itemMenuInfosDebug);
                    if (cfg.texteInfos != null) cfg.texteInfos.setVisibility(
                            cfg.itemMenuInfosDebug ? View.VISIBLE : View.GONE);
                }
                return true;
            case R.id.item_menu_a_propos:
                AlertDialog.Builder builder = new AlertDialog.Builder(binding.getRoot().getContext());
                builder.setTitle(getString(R.string.titre_dialogue_a_propos));
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
                // Stoppe le service de mise à jour de la position en arrière plan
                if (cfg.accesService != null) cfg.accesService.arreteService();
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

    /**
     * Handler pour dialoguer (récupérer les messages) avec les autres threads de l'application
     *
     * @return true si le traitement est terminé
     */
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_MAJ_VIEW:
                if (Config.DEBUG_LEVEL > 6 && DEBUG_CLASSE) Log.d("mainActivity", "Mise à jour de la vue demandée");
                View mView = (View)msg.obj;   // La vue doit être spécifiée dans l'objet
                if (mView == null) {
                    if (Config.DEBUG_LEVEL > 0 && DEBUG_CLASSE) Log.d("mainActivity",
                            "Mise à jour vue demandée, mais la vue n'est pas spécifiée !");
                    break;
                }
                if (mView.getVisibility() == View.VISIBLE) {  // Si la vue est visible
                    mView.invalidate();
                } else {
                    if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE) Log.d("mainActivity",
                            "Mise à jour de la vue demandée ... mais la vue est non visible");
                }
                break;

            default:
                if (Config.DEBUG_LEVEL > 0 && DEBUG_CLASSE) Log.d("mainActivity",
                        "Code message inconnu ! : " + msg.what);
                return false;
        }
        return true;
    }

    /**
     * Fonction pour demander la mise à jour par le thread UI d'une view pasée en argument
     * @param viewAMettreAJour  View à mettre à jour
     */
    public void majUI(View viewAMettreAJour) {
        if (cfg.handlerMainThread != null) {   // Si le handler n'est pas null
            // On crée un message pour demander au thread principal de mettre à jour la vue
            // parametres qui contient l'indicateur de connexion
            Message msg = Message.obtain(cfg.handlerMainThread, MainActivity.MSG_MAJ_VIEW);
            msg.obj = viewAMettreAJour;
            cfg.handlerMainThread.sendMessage(msg);
        }
    }

    @Override
    public void onResume() {
        if (Config.DEBUG_LEVEL > 2) Log.v("mainActivity","Méthode onResume appellée. cfg = "+ cfg);
        super.onResume();
    }

    @Override
    public void onPause() {
        if (Config.DEBUG_LEVEL > 2) Log.v("mainActivity","Méthode onPause appellée. cfg = "+ cfg);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (Config.DEBUG_LEVEL > 4) Log.v("mainActivity","*****************CREATION MENU *****************");
        getMenuInflater().inflate(R.menu.menu_principal, menu);
        super.onCreateOptionsMenu(menu);
        // Met la checkbox de l'item de menu "Infos debug" dans le bon état
        menu.findItem(R.id.item_menu_infos_debug).setChecked(cfg.itemMenuInfosDebug);
        return true;
    }

    @Override
    public void onDestroy() {
        if (Config.DEBUG_LEVEL > 2) Log.v("mainActivity","Méthode onDetroy appellée. cfg = "+ cfg);
        cfg.sauvegardeToutesLesPreferences();
        cfg.gestionPositionsUtilisateurs.sauvegardePositions();
        super.onDestroy();
    }
}