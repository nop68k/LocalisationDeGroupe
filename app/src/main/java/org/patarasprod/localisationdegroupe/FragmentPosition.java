package org.patarasprod.localisationdegroupe;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;


import androidx.annotation.NonNull;
import androidx.navigation.fragment.NavHostFragment;

import org.patarasprod.localisationdegroupe.databinding.FragmentPositionBinding;

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
        cfg.textViewLocalisation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cfg.com != null) {
                    Log.v("Fragment Position", "Envoi unique de la position");
                    cfg.com.communique1FoisAvecServeur();
                }
            }
        });
        cfg.textViewAltitude = binding.labelAltitude;
        cfg.textViewLatitude = binding.labelLatitude;
        cfg.textViewLongitude = binding.labelLongitude;

        // Mise en place des listener pour les boutons de copie de la position vers le presse-papier
        positionVersPressePapier = binding.posVersPressePapier;
        positionAngleVersPressePapier = binding.posAngleVersPressePapier;
        positionVersPressePapier.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pourPressePapier = cfg.textViewLocalisation.getText().toString();
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(getContext().CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", pourPressePapier);
                clipboard.setPrimaryClip(clip);
            }
        });

        positionAngleVersPressePapier.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pourPressePapier = cfg.textViewLatitude.getText().toString() + " ; " +
                                          cfg.textViewLongitude.getText().toString();
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(getContext().CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", pourPressePapier);
                clipboard.setPrimaryClip(clip);
            }
        });


        if (cfg != null && Config.DEBUG_LEVEL > 3) Log.v("Fragment Position", "Création du fragment Position");
        //cfg.localisation.getLocalisation();

        return root;
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}