package org.patarasprod.localisationdegroupe;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
    private FragmentInfosBinding binding;
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
        View root = binding.getRoot();

        cfg.texteInfos = binding.Infos;
        cfg.recyclerViewPositions = binding.listePositions;
        configureRecyclerView();
        return root;
    }

    // 3 - Configure RecyclerView, Adapter, LayoutManager & glue it together
    private void configureRecyclerView(){
        // 3.1 - Reset list
        this.listePositions = cfg.gestionPositionsUtilisateurs.getListePositions();
        // 3.2 - Create adapter passing the list of users
        this.adapter = new RecyclerViewAdapterListeUtilisateurs(this.listePositions, this.cfg);
        // 3.3 - Attach the adapter to the recyclerview to populate items
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
            if (cfg.texteInfos != null) cfg.texteInfos.setText(texte);
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
        super.onDestroyView();
        binding = null;
    }
}