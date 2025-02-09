package org.patarasprod.localisationdegroupe;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.patarasprod.localisationdegroupe.databinding.FragmentParametresBinding;

/**
 * A placeholder fragment containing a simple view.
 */
public class Fragment_parametres extends Fragment {

    private FragmentParametresBinding binding;
    Config cfg;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        cfg = ((MainActivity) requireActivity()).recupere_configuration();
        if (cfg != null && Config.DEBUG_LEVEL > 3) Log.v("Fragment 3", "Création du fragment parametres");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentParametresBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        cfg = ((MainActivity) requireActivity()).recupere_configuration();
        binding.zoneSaisieNom.setText(cfg.nomUtilisateur);
        binding.zoneSaisieNom.setOnFocusChangeListener(this::onFocusChange);
        binding.champIntervalleMesure.setText(String.valueOf(cfg.intervalleMesureSecondes));
        binding.champIntervalleMesure.setOnFocusChangeListener(this::onFocusChange);
        binding.champIntervalleMaj.setText(String.valueOf(cfg.intervalleMajSecondes));
        binding.champIntervalleMaj.setOnFocusChangeListener(this::onFocusChange);
        binding.champAdresseServeur.setText(String.valueOf(cfg.adresse_serveur));
        binding.champAdresseServeur.setOnFocusChangeListener(this::onFocusChange);
        binding.champPortServeur.setText(String.valueOf(cfg.port_serveur));
        binding.champPortServeur.setOnFocusChangeListener(this::onFocusChange);

        // Met le commutateur de diffusion de la position dans le bon état
        binding.switchDiffuserMaPosition.setChecked(cfg.prefDiffuserMaPosition);

        binding.switchDiffuserMaPosition.setOnCheckedChangeListener((v, isChecked) -> {
            cfg.prefDiffuserMaPosition = isChecked;
            if (isChecked) {
                cfg.com.demarreDiffusionPositionAuServeur();
            } else {
                cfg.com.stoppeDiffusionPositionAuServeur();
            }
            cfg.sauvegardePreference("diffuserMaPosition", cfg.prefDiffuserMaPosition);
        });

        // Indicateur de connexion
        cfg.indicateurConnexionServeur = binding.indicateurConnexionServeur ;
        if (cfg.com != null) cfg.com.majIndicateurConnexion();

        return root;
    }


    public void onFocusChange(View v, boolean hasFocus) {
        if (Config.DEBUG_LEVEL > 3) Log.v("Parametres", "******************perte de focus de " + v.getId());
        try {
            // trim() permet de supprimer les espaces avant et après
            cfg.nomUtilisateur = String.valueOf(binding.zoneSaisieNom.getText()).trim();
            cfg.intervalleMesureSecondes = Long.parseLong(String.valueOf(binding.champIntervalleMesure.getText()));
            cfg.intervalleMajSecondes = Long.parseLong(String.valueOf(binding.champIntervalleMaj.getText()));
            cfg.adresse_serveur = String.valueOf(binding.champAdresseServeur.getText()).trim();
            cfg.port_serveur = Integer.parseInt(String.valueOf(binding.champPortServeur.getText()));
            if (Config.DEBUG_LEVEL > 3) {
                String valeurs = "Nom : " + cfg.nomUtilisateur +"\nIntervalle mesure : " +
                        cfg.intervalleMesureSecondes + "s\nIntervalle maj : " + cfg.intervalleMajSecondes;
                Log.v("Fragment parametres", valeurs);
        }

        } catch (Exception e) {

        }
        if (Config.DEBUG_LEVEL > 2) Log.v("parametres", "Sauvegarde des paramètres");
        cfg.sauvegardeToutesLesPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        cfg = ((MainActivity) requireActivity()).recupere_configuration();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}