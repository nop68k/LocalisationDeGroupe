package org.patarasprod.localisationdegroupe;

import static android.content.Context.CLIPBOARD_SERVICE;
import static androidx.core.content.ContextCompat.getSystemService;

import android.content.ClipboardManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;

import org.patarasprod.localisationdegroupe.databinding.Fragment1Binding;

/**
 * A placeholder fragment containing a simple view.
 */
public class Fragment_1 extends NavHostFragment {

    private Fragment1Binding binding;
    Config cfg;
    ImageButton positionVersPressePapier;
    ImageButton positionAngleVersPressePapier;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {


        binding = Fragment1Binding.inflate(inflater, container, false);
        View root = binding.getRoot();
        cfg = ((MainActivity) requireActivity()).recupere_configuration();
        cfg.textViewLocalisation = binding.labelMaPosition;
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


        if (cfg != null && Config.DEBUG_LEVEL > 3) Log.v("Fragment 1", "Création du fragment 1");
        cfg.localisation.getLocalisation();

        return root;
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}