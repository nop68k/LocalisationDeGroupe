package org.patarasprod.localisationdegroupe.views;

import android.util.Log;
import android.view.View;

import androidx.viewpager2.widget.ViewPager2;

import org.patarasprod.localisationdegroupe.Config;
import org.patarasprod.localisationdegroupe.MainActivity;
import org.patarasprod.localisationdegroupe.MyAdapter;

public class OnPageChangeCallback_ViewPager2 extends ViewPager2.OnPageChangeCallback {

    // Tableau donnant la visibilité du bouton permettant de centrer sur ma position en fonction du numéro de l'onglet
    private final int[] visibiliteFabCentrer= {View.VISIBLE, View.VISIBLE, View.GONE, View.GONE};
    private final int[] visibiliteFabInfo= {View.GONE, View.GONE, View.VISIBLE, View.GONE};

    Config cfg;

    public OnPageChangeCallback_ViewPager2(Config config) {
        cfg = config;
    }

    public void onPageSelected(int position) {
        if (Config.DEBUG_LEVEL > 3) Log.v("OnPageChangeCallback", "*****-*** Onglet n°" + position + " sélectionné !");
        // Affiche ou non le floating action button pour centrer la carte sur MaPosition
        cfg.centrerSurMaPosition.setVisibility(visibiliteFabCentrer[position]);
        // Affiche ou non le floating action button d'information
        cfg.fabInfo.setVisibility(visibiliteFabInfo[position]);
        cfg.viewPager.setCurrentItem(position);
    }
}
