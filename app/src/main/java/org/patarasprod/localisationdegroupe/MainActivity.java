package org.patarasprod.localisationdegroupe;

import static androidx.core.content.ContextCompat.startActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.patarasprod.localisationdegroupe.databinding.ActivityMainBinding;
import org.patarasprod.localisationdegroupe.views.RecyclerViewAdapterListeUtilisateurs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements Handler.Callback {
    private static final boolean DEBUG_CLASSE = true;  // Drapeau pour autoriser les message de debug dans la classe
    public static final int MSG_MAJ_VIEW = 1;   // Message pour mettre à jour la vue
    public static final int MSG_MAJ_ANCIENNETES = 2;  // Pour mise à jour des anciennetés
    public static final int CODE_REDEMANDE_PERMISSIONS_VIA_MENU = 25;  // Code de demande

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

    /**
     * Crée et affiche un dialogue simple avec un seul bouton OK
     * @param titre   String Titre de la boîte de dialogue
     * @param contenu String Contenu de la boîte de dialogue
     */
    public void dialogSimple(String titre, String contenu) {
        AlertDialog.Builder builder = new AlertDialog.Builder(binding.getRoot().getContext());
        builder.setTitle(titre);
        builder.setMessage(contenu);
        builder.setCancelable(false);
        builder.setPositiveButton("Ok", (dialogInterface, i) -> dialogInterface.dismiss());
        builder.show();
    }


    @SuppressLint("NonConstantResourceId")
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
                // On crée une boîte de dialogue de confirmation
                AlertDialog.Builder builderC = new AlertDialog.Builder(this);
                builderC.setMessage(getString(R.string.confirmation_effacer_positions))
                        .setCancelable(true)
                        .setPositiveButton(getString(R.string.dialog_OK), (dialog, id) -> {
                            cfg.gestionPositionsUtilisateurs.reinitialise_positions();
                            dialog.dismiss();
                        })
                        .setNegativeButton(getString(R.string.dialog_annuler), (dialog, id) -> dialog.dismiss());
                AlertDialog alert = builderC.create();
                alert.show();
                return true;
            case R.id.item_menu_redemander_autorisations:
                cfg.localisation.demandeAutorisationsLocalisation();
                cfg.com.demandeAutorisationInternet();
                cfg.accesService.demandeAutorisationNotification(true);
                cfg.handler = new Handler();
                cfg.handler.postDelayed(() -> cfg.localisation.getLocalisation(), 3000);
                return true;
            case R.id.item_menu_synchroniser_heure:
                if (cfg != null && cfg.com != null) {
                    cfg.com.synchronisationHeureServeur();
                }
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
                dialogSimple(getString(R.string.titre_dialogue_a_propos), Config.MESSAGE_INFORMATION);
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
            case MSG_MAJ_ANCIENNETES:
                if (Config.DEBUG_LEVEL > 0 && DEBUG_CLASSE) Log.d("mainActivity",
                        "Mise à jour des anciennetés demandé");
                ((RecyclerViewAdapterListeUtilisateurs) Objects.requireNonNull(cfg.recyclerViewPositions.getAdapter())).majTempsEcoule();
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
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE) Log.d("mainActivity",
                "Résultat des demandes d'autorisation pour la requête " + requestCode + "\n" +
                        Arrays.toString(permissions) + "\n" + Arrays.toString(grantResults));
        if (requestCode == CODE_REDEMANDE_PERMISSIONS_VIA_MENU) {
            List<String> deniedPermissions = new ArrayList<>();
            // Vérifie et ajoute les permissions non accordées à la liste
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i]);
                }
            }
            if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE) Log.d("mainActivity",
                    "Autorisations refusées : " + deniedPermissions);
            if (deniedPermissions.isEmpty()) {
                // Affiche un message pr dire que les autorisations sont toutes accordées
                Toast.makeText(this, getString(R.string.toutes_autorisations_accordees), Toast.LENGTH_SHORT).show();
            } else {
                // On affiche les permission refusées dans un message éphémère
                Toast.makeText(this, getString(R.string.liste_permissions_manquantes, deniedPermissions), Toast.LENGTH_LONG).show();

                // Ouvre les paramètres pour que l'utilisateur accorde les autorisations manquantes
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", getPackageName(), null));
                startActivity(intent);
            }
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