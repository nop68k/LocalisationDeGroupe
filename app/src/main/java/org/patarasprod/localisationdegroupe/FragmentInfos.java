package org.patarasprod.localisationdegroupe;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;

import org.patarasprod.localisationdegroupe.databinding.FragmentInfosBinding;
import org.patarasprod.localisationdegroupe.databinding.DialogCoordonneesBinding;
import org.patarasprod.localisationdegroupe.views.RecyclerViewAdapterListeUtilisateurs;
import org.patarasprod.localisationdegroupe.views.ViewHolderListeUtilisateurs;

import java.util.ArrayList;
import java.util.Objects;

/**
 * A placeholder fragment containing a simple view.
 */
public class FragmentInfos extends Fragment {

    private static final boolean DEBUG_CLASSE = true;  // Drapeau pour autoriser les message de debug dans la classe
    private final long DELAI_MAJ_UI_LISTE = 1*1000;   // Nombre de ms entre maj de l'UI de la liste
    private    FragmentInfosBinding     binding;

    protected  View                     mView ;  // référence sur la View du fragment Infos
    protected  Handler                  mHandler = null ;   // Handler pour programmer la maj de la liste régulièrement
    protected  Runnable                 routineMAJ = null;   // Routine de MAJ régulière de l'UI de la liste
    protected  Config                   cfg;
    protected  ArrayList<Position>      listePositions = null;
    protected  RecyclerViewAdapterListeUtilisateurs adapter = null;
    protected  PasseurDeReference       referenceVH;       // Pour les menus contextuels
    protected  LayoutInflater           inflater;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        cfg = ((MainActivity) requireActivity()).recupere_configuration();
        if (Config.DEBUG_LEVEL > 3) Log.v("infos", "Création du fragment infos");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        this.inflater = inflater;
        cfg = ((MainActivity) requireActivity()).recupere_configuration();
        // On sauvegarde la référence au fragment crée
        cfg.fragment_infos = this;

        binding = FragmentInfosBinding.inflate(inflater, container, false);
        mView = binding.getRoot();

        // Enregistre la référence vers le texte d'infos et fixe sa visibilité en fonction de
        // l'état de l'item de menu "Infos debug"
        cfg.texteInfos = binding.Infos;
        cfg.texteInfos.setVisibility(cfg.itemMenuInfosDebug ? View.VISIBLE : View.GONE);

        // Met en place le passeur de référence pr les menus contextuels
        referenceVH = new PasseurDeReference(null, cfg);
        cfg.recyclerViewPositions = binding.listePositions;
        configureRecyclerView();

        ((MainActivity) requireActivity()).majUI(mView);  // Met à jour la vue

        /* Création du handler pour programmer les maj régulières de la liste
         Lorsque le fragment est affiché, la méthode onResume() est appelée qui va demander l'exécution
         différée (PosteDelayed) de routineMAJ. Cette routine met à jour la liste utilisateur et
         reprogramme une exécution de routineMAJ.
         Cette boucle sera stoppée par la méthode onPause qui est appelée lorsque le fragment n'est plus affiché.
         */
        if (Config.DEBUG_LEVEL > 5) Log.v("infos", "Création du handler pour la mise à jour");
        mHandler = new Handler();

        routineMAJ = new Runnable() {
            public void run() {
                // maj de l'UI si la view est bien visible
                if (mView != null && mView.isShown() && adapter != null) {
                    if (Config.DEBUG_LEVEL > 5) Log.v("infos", "Mise à jour de la View Infos demandée par routineMAJ");
                    adapter.majListeUtilisateurs();
                    ((MainActivity) requireActivity()).majUI(mView);  // Met à jour la vue
                    mHandler.postDelayed(routineMAJ, DELAI_MAJ_UI_LISTE);  // Reprogramme une maj
                }
            }
        };

        return mView;
    }


    @Override
    public void onPause() {
        if (Config.DEBUG_LEVEL > 5) Log.v("infos", "Methode onPause appelée");
        if (mHandler != null) mHandler.removeCallbacksAndMessages(null);  // Stoppe les MAJ
        super.onPause();
    }

    @Override
    public void onResume() {
        if (Config.DEBUG_LEVEL > 5) Log.v("infos", "Methode onResume appelée");
        if (adapter != null) adapter.majListeUtilisateurs();
        if(mHandler != null && routineMAJ != null) mHandler.postDelayed(routineMAJ, DELAI_MAJ_UI_LISTE);
        super.onResume();
    }


    /**
     * Crée et configure le RecycleViewAdapter qui gère la liste des utilisateurs affichée par le
     * fragment.
     */
    private void configureRecyclerView(){
        // On récupère une référence sur la liste des posistions
        this.listePositions = cfg.gestionPositionsUtilisateurs.getListePositions();
        // On instancie le RecyclerViewAdapter en lui fournissant la liste des utilisateurs
        MenuInflater menuInflater=requireActivity().getMenuInflater();
        this.adapter = new RecyclerViewAdapterListeUtilisateurs(this.listePositions, this.cfg, menuInflater, referenceVH);
        // On attache l'adapter au recyclerview
        cfg.recyclerViewPositions.setAdapter(this.adapter);
        // 3.4 - Set layout manager to position the items
        cfg.recyclerViewPositions.setLayoutManager(new LinearLayoutManager(getActivity()));
        registerForContextMenu(cfg.recyclerViewPositions);   // Rend la liste sensible au menu contextuel
    }



    public void majTexteInfos() {
        String texte;
        if (cfg == null) {
            texte = "ERREUR : cfg est à null !!!!!";
            binding.Infos.setText(texte);
        } else {
            texte = "nomUtilisateur : " + cfg.nomUtilisateur + "\n";
            texte += "prefDiffuserMaPosition : " + cfg.prefDiffuserMaPosition + "\n";
            texte += "Intervalle mesure : " + cfg.intervalleMesureSecondes + " s" + "\n";
            texte += "Intervalle maj position : " + cfg.intervalleMajSecondes + " s\n" + "\n";
            texte += "com : " + cfg.com + "\n";
            texte += "gestionPositionsUtilisateurs : " + cfg.gestionPositionsUtilisateurs + "\n";
            texte += "localisation : " + cfg.localisation + "\n" + "\n";
            texte += "diffuserMaPosition : " + cfg.diffuserMaPosition + "\n";
            texte += "communicationEnCours : " + cfg.communicationEnCours + "\n";
            texte += "Nombre de positions suivies : " + cfg.nbPositions + "\n";
            texte += "threadCommunication : " + cfg.threadCommunication + "\n";
            texte += "Adresse serveur : " + cfg.adresse_serveur + "\n";
            texte += "port_serveur : " + cfg.port_serveur + "\n";
            texte += "handlerDiffuserPosition :" + cfg.handlerDiffusionPosition + "\n" + "\n";
            texte += "reponse : " + cfg.reponse + "\n";
            texte += "\n\n\n";
            if (cfg.texteInfos != null) {
                cfg.texteInfos.setText(texte);
                cfg.texteInfos.setVisibility(cfg.itemMenuInfosDebug ? View.VISIBLE : View.GONE);
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (Config.DEBUG_LEVEL > 5) Log.v("infos", "onContextItemSelected appelé");
        ViewHolderListeUtilisateurs vh = referenceVH.getReference();  // On récupère la ref sur l'élément cliqué
        if (vh == null) {
            Log.d ("infos", "La référence de l'objet cliqué est null !");
            return false;
        }
        switch (item.getItemId()) {
            case R.id.menuitem_centrer_sur:
                cfg.mapController.setCenter(vh.getPos().getGeoPoint()); // Centre sur cette position
                cfg.viewPager.setCurrentItem(1);   // Ramène à l'onglet 1 (carte)
                return true;  // click traité
            case R.id.menuitem_ouvrir_ds_app_externe:
                ouvrirDansAppExterne(vh);
                return true;  // click traité
            case R.id.menuitem_rentrer_coordonnees:
                rentrerLesCoordonnees(vh);
                return true;  // click traité
            case R.id.menuitem_supprimer_nom:
                supprimerNom(vh);
                return true;  // click traité
        }
        return super.onContextItemSelected(item);
    }

    /**
     * Ouvre une appli externe en lui fournissant les coordonnées GPS de la position cliquée.
     * @param vh Référence sur le viewHolder concerné (celui sur lequel porte le menu contextuel)
     */
    protected void ouvrirDansAppExterne(ViewHolderListeUtilisateurs vh) {
        // Création de l'intent.
        Uri location = Uri.parse("geo:" + vh.getPos().latitude + "," +
                vh.getPos().longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, location);
        // On essaie d'invoquer l'intent.
        try {
            if (Config.DEBUG_LEVEL > 2) Log.v("infos",
                    "Appel d'une appli externe sur l'uri : "+"geo:"+
                            String.valueOf(vh.getPos().latitude).replace(',','.') + "," +
                            String.valueOf(vh.getPos().longitude).replace(',','.'));
            cfg.mainActivity.startActivity(mapIntent);
        } catch (ActivityNotFoundException e) {
            Log.v("ViewHolderUtilisateurs", "Activité introuvable pour voir une uri de type geo");
        }
    }


    /**
     * Procédure qui gère l'affichage de la boîte de dialogue pour changer la position d'un des
     * noms de la liste et effectue le changement si les coordonnées fournies sont valides.
     * @param vh Référence sur le viewHolder concerné (celui sur lequel porte le menu contextuel)
     */
    protected void rentrerLesCoordonnees(ViewHolderListeUtilisateurs vh) {
        if (Config.DEBUG_LEVEL > 5) Log.v("infos", "Rentrer les coordonnées pour " + vh.getPos().nom);

        // Création de la boîte de dialogue
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Comme c'est une fenêtre personnalisées, il faut gonfler son layout en lui
        //passant null pour la view parent car on va l'attacher au dialogue
        DialogCoordonneesBinding binding_dialog = DialogCoordonneesBinding.inflate(inflater);
        builder.setView(binding_dialog.getRoot());
        // Maintenant on rajoute les autres éléments
        builder.setTitle(getString(R.string.titre_saisie_coordonnees_GPS, vh.getPos().nom))
                .setCancelable(true);  // On peut cliquer en dehors pour annuler

        builder.setPositiveButton(R.string.dialog_OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (Config.DEBUG_LEVEL > 5) Log.v("infos","J'ai reçu " + String.valueOf(binding_dialog.saisieCoordonneesGPS.getText()).trim());
                try {
                    double[] coordonnees = Position.convertiChaineCoord(String.valueOf(binding_dialog.saisieCoordonneesGPS.getText()).trim());
                    Log.v("infos","Latitude : " + coordonnees[0] + " , Longitude : " + coordonnees[1]);
                    if (cfg != null && cfg.gestionPositionsUtilisateurs != null)
                        cfg.gestionPositionsUtilisateurs.majPositions((new Position(vh.getPos().nom,
                                coordonnees[0], coordonnees[1])).toString());
                } catch (Exception e) {
                    // Si ça s'est mal passé, on affiche un message furtif pour indiquer le problème
                    Snackbar.make(Objects.requireNonNull(getView()),
                            getString(R.string.msg_coordonnes_rentrees_invalides),
                            Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    Log.d("infos",e.toString());
                }
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton(getString(R.string.dialog_annuler), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog dialog = builder.create();  // Construit la boîte de dialogue
        dialog.show();          // Et l'affiche
    }

    /**
     * Procédure qui affiche une fenêtre de confirmation et supprime le nom de la liste si on clique
     * sur OK. Une case à cocher "Supprimer aussi sur le serveur" est ajoutée si on est en mode debug
     * @param vh ViewHolder pour lequel le menu contextuel a été ouvert
     */
    protected void supprimerNom(ViewHolderListeUtilisateurs vh) {
        if (Config.DEBUG_LEVEL > 5) Log.v("infos", "Demande de suppression pour " + vh.getPos().nom);
        // Création de la boîte de dialogue
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // On ajoute les éléments
        builder.setTitle("Supprimer " + vh.getPos().nom + " ?");
        builder.setCancelable(true);  // On peut cliquer en dehors pour annuler

        boolean[] case_a_cocher = {false};
        // Si on est en mode debug, on affiche la case à cocher pour proposer de supprimer sur le serveur
        Log.v("infos", "cfg = " + cfg + " \titemMenuInfosDebug = " + cfg.itemMenuInfosDebug);
        if (cfg != null && cfg.itemMenuInfosDebug) {
            Log.v("infos", "Mise en place de la case à cocher");
            builder.setMultiChoiceItems(R.array.case_a_cocher_suppression_serveur, case_a_cocher,
                    (dialog, which, isChecked) -> {
                        //
                    });
        }
        // Definition des boutons
        builder.setPositiveButton(R.string.dialog_OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // On tente la suppression et si elle réussit on affiche un message éphémère
                if (cfg != null && cfg.gestionPositionsUtilisateurs != null &&
                        cfg.gestionPositionsUtilisateurs.supprimePosition(vh.getPos().nom))
                    Snackbar.make(Objects.requireNonNull(getView()),
                            getString(R.string.msg_suppression_position_nom, vh.getPos().nom),
                            Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                // Si on a demandé la suppression sur le serveur, on la demande avec une commande
                if (case_a_cocher[0]) {   // Si la case est cochée
                    if (cfg != null) {                 // On demande au serveur de supprimer le nom
                        cfg.com.envoiCommande(Communication.SUPPRIMER, vh.getPos().nom);
                    }
                }
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton(getString(R.string.dialog_annuler), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog dialog = builder.create();  // Construit la boîte de dialogue
        dialog.show();          // Et l'affiche
    }

    public void majListeUtilisateurs() {
        RecyclerViewAdapterListeUtilisateurs adapter = (RecyclerViewAdapterListeUtilisateurs) cfg.recyclerViewPositions.getAdapter();
        for (int i = 0; i < Objects.requireNonNull(adapter).getItemCount() ; i++) {
            long noView = adapter.getItemId(i);
            View view = cfg.mainActivity.findViewById((int) adapter.getItemId(i));
        }
    }

    @Override
    public void onDestroyView() {
        if (Config.DEBUG_LEVEL > 3)
            Log.v("infos", "Méthode onDestroy appelée");
        if (mHandler != null) mHandler.removeCallbacksAndMessages(null);  // Stoppe les MAJ
        super.onDestroyView();
        binding = null;
    }

    /**
     * Classe support pour stocker la référence vers le ViewHolder qui a déclenché l'apparition du menu contextuel.
     */
    public class PasseurDeReference {
        private ViewHolderListeUtilisateurs reference;
        private Config cfg;         // Référence locale sur la configuration

        public PasseurDeReference(ViewHolderListeUtilisateurs reference, Config cfg) {
            this.reference = reference;
            this.cfg = cfg;
        }

        public void setReference(ViewHolderListeUtilisateurs reference) {
            this.reference = reference;
        }

        public Config getConfig() {
            return this.cfg;
        }

        public ViewHolderListeUtilisateurs getReference() {
            ViewHolderListeUtilisateurs ref = this.reference;  // On sauvegarde la référence
            // Met à null la référence pour qu'il n'y ait plus de référence vers le ViewHolder
            // dont on va fournir la référence et qu'il puisse être recyclé par le GC si besoin
            // (On évite les fuites de mémoire)
            this.setReference(null);
            return ref;
        }
    }
}