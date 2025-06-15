package org.patarasprod.localisationdegroupe;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

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
        if (Config.DEBUG_LEVEL > 3) Log.v("parametres", "Création du fragment parametres");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        cfg = ((MainActivity) requireActivity()).recupere_configuration();
        // On sauvegarde la référence au fragment crée
        cfg.fragment_parametres = this;

        binding = FragmentParametresBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        TextView.OnEditorActionListener traitementActions = new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE ||
                        event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    majParametres();
                    return true;
                }
                return false;
            }
        };

        cfg = ((MainActivity) requireActivity()).recupere_configuration();
        binding.zoneSaisieNom.setText(cfg.nomUtilisateur);
        binding.zoneSaisieNom.setOnFocusChangeListener(this::onFocusChange);
        binding.zoneSaisieNom.setOnEditorActionListener(traitementActions);
        binding.champIntervalleMesure.setText(String.valueOf(cfg.intervalleMesureSecondes));
        binding.champIntervalleMesure.setOnFocusChangeListener(this::onFocusChange);
        binding.champIntervalleMesure.setOnEditorActionListener(traitementActions);
        binding.champIntervalleMaj.setText(String.valueOf(cfg.intervalleMajSecondes));
        binding.champIntervalleMaj.setOnFocusChangeListener(this::onFocusChange);
        binding.champIntervalleMaj.setOnEditorActionListener(traitementActions);
        binding.champAdresseServeur.setText(String.valueOf(cfg.adresse_serveur));
        binding.champAdresseServeur.setOnEditorActionListener(traitementActions);
        binding.champAdresseServeur.setOnFocusChangeListener(this::onFocusChange);
        binding.champPortServeur.setText(String.valueOf(cfg.port_serveur));
        binding.champPortServeur.setOnEditorActionListener(traitementActions);
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

        binding.champIntervalleMajService.setText(String.valueOf(cfg.intervalleEnvoiEnFond));
        binding.champIntervalleMajService.setOnEditorActionListener(traitementActions);
        binding.champIntervalleMajService.setOnFocusChangeListener(this::onFocusChange);

        // Met le commutateur de diffusion en tâche de fond dans le bon état
        binding.switchDiffuserEnTacheDeFond.setChecked(cfg.prefDiffuserEnFond);

        binding.switchDiffuserEnTacheDeFond.setOnCheckedChangeListener((v, isChecked) -> {
            if (cfg.accesService == null) {
                if (Config.DEBUG_LEVEL > 0) Log.v("parametres",
                        "ERREUR : La classe AccesService n'est pas instanciée alors qu'on veut modifier l'état du service !");
                return;
            }
            cfg.prefDiffuserEnFond = isChecked;
            if (isChecked) {
                cfg.accesService.demarrageService();
            } else {
                cfg.accesService.arreteService();
            }
            cfg.sauvegardePreference("diffuserEnFond", cfg.prefDiffuserEnFond);
        });


        return root;
    }

    /** Met à jour la configuration en fonction du contenu des champs de saisie et relance la
     * communication si les paramètres de nom, adresse ou port ont changés
     * provoque également un changement de configuration du service LocationUpdateService s'il y a
     * des changements le concernant.
     */
    public void majParametres() {
        try {
            String ancienNom = cfg.nomUtilisateur;
            // trim() permet de supprimer les espaces avant et après
            cfg.nomUtilisateur = String.valueOf(binding.zoneSaisieNom.getText()).trim();
            cfg.maPosition.majPosition(cfg.nomUtilisateur); // Met à jour le nom d'utilisateur de la positions
            cfg.intervalleMesureSecondes = Long.parseLong(String.valueOf(binding.champIntervalleMesure.getText()));
            cfg.intervalleMajSecondes = Long.parseLong(String.valueOf(binding.champIntervalleMaj.getText()));

            String ancienAdrServeur = cfg.adresse_serveur;
            int ancienPortServeur = cfg.port_serveur;
            cfg.adresse_serveur = String.valueOf(binding.champAdresseServeur.getText()).trim();
            cfg.port_serveur = Integer.parseInt(String.valueOf(binding.champPortServeur.getText()));

            long ancienIntervalleEnvoiEnFond = cfg.intervalleEnvoiEnFond;
            cfg.intervalleEnvoiEnFond = Long.parseLong(String.valueOf((binding.champIntervalleMajService)));

            // Si les paramètres du serveur ou le nom ont changé, on relance la communication avec les nouveau paramètres
            if ((!ancienAdrServeur.equals(cfg.adresse_serveur) || ancienPortServeur != cfg.port_serveur
                    || !ancienNom.equals(cfg.nomUtilisateur)) && cfg.com != null) {
                if (Config.DEBUG_LEVEL > 2) Log.v("parametres",
                        "Les paramètres ont changés et nécessitent un redémarrage de la communication");
                cfg.com.stoppeDiffusionPositionAuServeur();
                cfg.com.demarreDiffusionPositionAuServeur();
            }
            if ((!ancienAdrServeur.equals(cfg.adresse_serveur) || ancienPortServeur != cfg.port_serveur
                    || !ancienNom.equals(cfg.nomUtilisateur))
                    ||ancienIntervalleEnvoiEnFond != cfg.intervalleEnvoiEnFond
                    && cfg.com != null && cfg.accesService != null) {
                cfg.accesService.majParametresService(cfg);  // Met à jour la configuration du service en arrière plan
            }

            if (Config.DEBUG_LEVEL > 3) {
                String valeurs = "Nom : " + cfg.nomUtilisateur +" \tIntervalle mesure : " +
                        cfg.intervalleMesureSecondes + "s \tIntervalle maj : " + cfg.intervalleMajSecondes
                        + " \tAdr serveur : " + cfg.adresse_serveur + " \tPort : " + cfg.port_serveur;
                Log.v("Fragment parametres", valeurs);
            }

        } catch (Exception e) {

        }
        if (Config.DEBUG_LEVEL > 2) Log.v("parametres", "Sauvegarde des paramètres");
        cfg.sauvegardeToutesLesPreferences();
    }

    public boolean onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            return false; // Si c'est un gain de focus, cela ne valide rien
        } else {
            if (Config.DEBUG_LEVEL > 3)
                Log.v("Parametres", "******************perte de focus de " + v.getId());
            majParametres();
            return true;
        }
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