package org.patarasprod.localisationdegroupe;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;


import androidx.annotation.NonNull;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;

import org.patarasprod.localisationdegroupe.databinding.FragmentPositionBinding;

import java.util.Objects;

/**
 * A placeholder fragment containing a simple view.
 */
public class FragmentPosition extends NavHostFragment {

    private FragmentPositionBinding binding;
    Config cfg;
    ImageButton positionVersPressePapier;
    ImageButton positionAngleVersPressePapier;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        cfg = ((MainActivity) requireActivity()).recupere_configuration();
        if (Config.DEBUG_LEVEL > 3) Log.v("Fragment Position", "Création du fragment Position");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        cfg = ((MainActivity) requireActivity()).recupere_configuration();
        // On sauvegarde la référence au fragment crée
        cfg.fragment_position = this;

        binding = FragmentPositionBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        cfg.textViewLocalisation = binding.labelMaPosition;
        // Ajoute un listener sur le texte de la position pour que la position soit envoyée au serveur
        // une seule fois après le click sur ce texte
        cfg.textViewLocalisation.setOnClickListener(v -> {
            if (cfg.com != null) {
                Log.v("Fragment Position", "Envoi unique de la position");
                cfg.com.communique1FoisAvecServeur();
            }
        });
        cfg.textViewAltitude = binding.labelAltitude;
        cfg.textViewLatitude = binding.labelLatitude;
        cfg.textViewLongitude = binding.labelLongitude;

        // Mise en place des listener pour les boutons de copie de la position vers le presse-papier
        positionVersPressePapier = binding.posVersPressePapier;
        positionAngleVersPressePapier = binding.posAngleVersPressePapier;
        positionVersPressePapier.setOnClickListener(v -> {
            String pourPressePapier = cfg.textViewLocalisation.getText().toString().replace("\n",",");
            getContext();
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                    Objects.requireNonNull(getContext()).getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Position", pourPressePapier);
            clipboard.setPrimaryClip(clip);
            Snackbar.make(Objects.requireNonNull(cfg.fragment_position.getView()),
                    getString(R.string.msg_position_copiee), Snackbar.LENGTH_SHORT).setAction("Action", null).show();
        });

        positionAngleVersPressePapier.setOnClickListener(v -> {
            String pourPressePapier = cfg.textViewLatitude.getText().toString() + "," +
                                      cfg.textViewLongitude.getText().toString();
            getContext();
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                    Objects.requireNonNull(getContext()).getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Position", pourPressePapier);
            clipboard.setPrimaryClip(clip);
            Snackbar.make(Objects.requireNonNull(cfg.fragment_position.getView()),
                    getString(R.string.msg_position_copiee), Snackbar.LENGTH_SHORT).setAction("Action", null).show();
        });

        if (cfg != null && Config.DEBUG_LEVEL > 3) Log.v("Fragment Position", "Création du fragment Position");
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}