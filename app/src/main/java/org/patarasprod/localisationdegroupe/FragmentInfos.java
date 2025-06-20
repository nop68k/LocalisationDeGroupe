package org.patarasprod.localisationdegroupe;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.patarasprod.localisationdegroupe.databinding.FragmentInfosBinding;
import org.patarasprod.localisationdegroupe.views.RecyclerViewAdapterListeUtilisateurs;

import java.util.ArrayList;
import java.util.Objects;

/**
 * A placeholder fragment containing a simple view.
 */
public class FragmentInfos extends Fragment {

    private static final boolean DEBUG_CLASSE = true;  // Drapeau pour autoriser les message de debug dans la classe
    private final long DELAI_MAJ_UI_LISTE = 1*1000;   // Nombre de ms entre maj de l'UI de la liste
    private FragmentInfosBinding binding;

    protected View mView ;  // référence sur la View du fragment Infos
    protected Handler mHandler = null ;   // Handler pour programmer la maj de la liste régulièrement
    protected Runnable routineMAJ = null;   // Routine de MAJ régulière de l'UI de la liste
    Config cfg;
    ArrayList<Position> listePositions = null;
    RecyclerViewAdapterListeUtilisateurs adapter = null;


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

        cfg = ((MainActivity) requireActivity()).recupere_configuration();
        // On sauvegarde la référence au fragment crée
        cfg.fragment_infos = this;

        binding = FragmentInfosBinding.inflate(inflater, container, false);
        mView = binding.getRoot();

        // Enregistre la référence vers le texte d'infos et fixe sa visibilité en fonction de
        // l'état de l'item de menu "Infos debug"
        cfg.texteInfos = binding.Infos;
        cfg.texteInfos.setVisibility(cfg.itemMenuInfosDebug ? View.VISIBLE : View.GONE);

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
        this.adapter = new RecyclerViewAdapterListeUtilisateurs(this.listePositions, this.cfg);
        // On attache l'adapter au recyclerview
        cfg.recyclerViewPositions.setAdapter(this.adapter);
        // 3.4 - Set layout manager to position the items
        cfg.recyclerViewPositions.setLayoutManager(new LinearLayoutManager(getActivity()));
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
            if (cfg.texteInfos != null) {
                cfg.texteInfos.setText(texte);
                cfg.texteInfos.setVisibility(cfg.itemMenuInfosDebug ? View.VISIBLE : View.GONE);
            }
        }
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
}