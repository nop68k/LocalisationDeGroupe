package org.patarasprod.localisationdegroupe;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import org.patarasprod.localisationdegroupe.databinding.FragmentParametresBinding;

public class Fragment_parametres extends Fragment {

    // Caractères interdits dans le nom de l'utilisateur
    public static final String CARACTERES_INTERDITS = Communication.CARACTERE_COMMUNICATION_BIDIRECTIONNELLE
            + Communication.CARACTERE_COMMUNICATION_COMMANDE + Communication.CARACTERE_COMMUNICATION_UNIDIRECTIONNELLE
            + GestionPositionsUtilisateurs.SEPARATEUR_ELEMENTS_REPONSE_SERVEUR + Position.SEPARATEUR_CHAMPS
            + Position.SEPARATEUR_LATITUDE_LONGITUDE + "\n\t\\";
    private FragmentParametresBinding binding;
    Config cfg;
    private ColorStateList couleursChamps; // Couleurs utilisées par les champs de saisie (pour pouvoir les restaurer)


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

        // Création du listener pour la validation des champs (par touche entrée ou suivant)
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

        // Remplissage des champs avec le contenu enregistré dans la configuration et création
        // du lien avec les listener (pour la validation et le changement de focus)
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
        couleursChamps = binding.champPortServeur.getTextColors();
        binding.champPortServeur.setOnEditorActionListener(traitementActions);
        binding.champPortServeur.setOnFocusChangeListener(this::onFocusChange);

        // Met le commutateur de diffusion de la position dans le bon état
        binding.switchDiffuserMaPosition.setChecked(cfg.prefDiffuserMaPosition);

        // Mise en place du listener pour réagir au changement d'état du commutateur "Diffuser ma position"
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

        binding.champIntervalleEnvoiService.setText(String.valueOf(cfg.intervalleEnvoiService));
        binding.champIntervalleEnvoiService.setOnEditorActionListener(traitementActions);
        binding.champIntervalleEnvoiService.setOnFocusChangeListener(this::onFocusChange);

        // Mise en place du listener pour réagir au changement d'état du commutateur "Diffuser en tâche de fond"
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

    /**
     * Renvoi le nom d'utilisateur validé en ayant retiré les espaces avant et après ainsi que les
     * caractères non autorisés (dans la chaine constante CARACTERES_INTERDITS) afin d'éviter que
     * cela gène la transmission en se confondant avec les délimiteurs de champ.
     * @param nomAValider String chaîne contenant le nom brut
     * @return String nom validé
     */
    private String validationNom(String nomAValider) {
        nomAValider = nomAValider.trim();   // trim() permet de supprimer les espaces avant et après
        String nomValide = "";
        boolean valide = true;
        for (int i = 0 ; i < nomAValider.length() ; i++) {
            valide = true;
            for (int j = 0 ; j < CARACTERES_INTERDITS.length() ; j++) {
                if (nomAValider.charAt(i) == CARACTERES_INTERDITS.charAt(j)) {
                    valide = false;
                    break;
                }
            }
            if (valide) nomValide += nomAValider.charAt(i);
        }
        return nomValide;
    }

    /**
     * Met à jour la configuration en fonction du contenu des champs de saisie et relance la
     * communication si les paramètres de nom, adresse ou port ont changés
     * provoque également un changement de configuration du service LocationUpdateService s'il y a
     * des changements le concernant.
     */
    public void majParametres() {
        try {
            String ancienNom = cfg.nomUtilisateur;
            cfg.nomUtilisateur = validationNom(String.valueOf(binding.zoneSaisieNom.getText()));
            binding.zoneSaisieNom.setText(cfg.nomUtilisateur);   // Met à jour le champ avec le nom corrigé
            cfg.maPosition.majPosition(cfg.nomUtilisateur); // Met à jour le nom d'utilisateur de la positions
            cfg.intervalleMesureSecondes = Long.parseLong(String.valueOf(binding.champIntervalleMesure.getText()));
            cfg.intervalleMajSecondes = Long.parseLong(String.valueOf(binding.champIntervalleMaj.getText()));

            String ancienAdrServeur = cfg.adresse_serveur;
            int ancienPortServeur = cfg.port_serveur;
            cfg.adresse_serveur = String.valueOf(binding.champAdresseServeur.getText()).trim();
            cfg.port_serveur = Integer.parseInt(String.valueOf(binding.champPortServeur.getText()));
            if (cfg.port_serveur < 0 || cfg.port_serveur > 65535) {
                binding.champPortServeur.setTextColor(Color.RED);   // passe le texte en rouge
                Snackbar.make(this.getView(), getString(R.string.msg_no_port_invalide), Snackbar.LENGTH_LONG).setAction("Action", null).show();
            } else {
                binding.champPortServeur.setTextColor(couleursChamps);  // restaure la couleur d'origine du champ
            }

            long ancienIntervalleEnvoiEnFond = cfg.intervalleEnvoiService;
            cfg.intervalleEnvoiService = Long.parseLong(String.valueOf((binding.champIntervalleEnvoiService.getText())));

            // Si les paramètres du serveur ou le nom ont changé et qu'il y a une communication demandée,
            // on relance la communication avec les nouveaux paramètres
            if ( ( (!ancienAdrServeur.equals(cfg.adresse_serveur)) || (ancienPortServeur != cfg.port_serveur)
                    || (!ancienNom.equals(cfg.nomUtilisateur)))
                    && cfg.prefDiffuserMaPosition && (cfg.com != null && cfg.accesService != null) ) {
                if (Config.DEBUG_LEVEL > 2) Log.v("parametres",
                        "Les paramètres ont changés et nécessitent un redémarrage de la communication");
                cfg.com.stoppeDiffusionPositionAuServeur();
                cfg.com.demarreDiffusionPositionAuServeur();
            }
            if ( ( (!ancienAdrServeur.equals(cfg.adresse_serveur)) || (ancienPortServeur != cfg.port_serveur)
                    || (!ancienNom.equals(cfg.nomUtilisateur))
                    || (ancienIntervalleEnvoiEnFond != cfg.intervalleEnvoiService) )
                    && cfg.prefDiffuserEnFond && (cfg.com != null && cfg.accesService != null) ) {
                Log.v("parametres",
                        "Lancement de la demande de mise à jour des paramètres du service");
                cfg.accesService.majParametresService(cfg);  // Met à jour la configuration du service en arrière plan
            }

            if (Config.DEBUG_LEVEL > 3) {
                String valeurs = "Nom : " + cfg.nomUtilisateur +" \tIntervalle mesure : " +
                        cfg.intervalleMesureSecondes + "s \tIntervalle maj : " + cfg.intervalleMajSecondes
                        + " \tAdr serveur : " + cfg.adresse_serveur + " \tPort : " + cfg.port_serveur;
                Log.v("Fragment parametres", valeurs);
            }

        } catch (Exception e) {
            if (Config.DEBUG_LEVEL > 2) Log.v("parametres",
                    "Problème dans la conversion des entrées : " + e.toString());
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