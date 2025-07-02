package org.patarasprod.localisationdegroupe;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import org.patarasprod.localisationdegroupe.databinding.FragmentPrincipalBinding;
import org.patarasprod.localisationdegroupe.views.OnPageChangeCallback_ViewPager2;

public class FragmentPrincipal extends NavHostFragment {

    private FragmentPrincipalBinding binding;
    MyAdapter adapter;
    Config cfg;

    TabLayout tabLayout;
    ViewPager2 viewPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cfg = ((MainActivity) requireActivity()).recupere_configuration();
//        cfg.navController = this.getNavController();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentPrincipalBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        cfg = ((MainActivity) requireActivity()).recupere_configuration();

        // Boutons flottants
        cfg.centrerSurMaPosition = binding.fabCentrerSurMaPosition;
        cfg.fabInfo = binding.fabInfo;

        if (Config.DEBUG_LEVEL > 3) Log.v("Fragment principal","CREATION View FRAGMENT PRINCIPAL");

        //setContentView(R.layout.activity_main);
        tabLayout = binding.tabLayout2;    //findViewById(R.id.tabLayout);

        /* POUR DESACTIVER LE SWIPE IL FAUT UTILISER UN ViewPager2 (voir https://developer.android.com/reference/androidx/viewpager2/widget/ViewPager2#summary
          et notamment la méthode setUserInputEnabled pour désactiver le swipe
         */

        viewPager = (ViewPager2) binding.viewPager2; //findViewById(R.id.viewPager2);
        adapter = new MyAdapter(cfg.mainActivity, tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.registerOnPageChangeCallback(new OnPageChangeCallback_ViewPager2(cfg));

        cfg.viewPager = viewPager;
        cfg.adapterViewPager = adapter;

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }
        });

        if (Config.DEBUG_LEVEL > 3) Log.v("Fragment principal","Désactivation du swipe");
        viewPager.setUserInputEnabled(false);

        // Bouton flottant (Floating Action Button) pour centrer sur notre position
        binding.fabCentrerSurMaPosition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Centrage sur ma position", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                // Centrage de la carte sur ma position
                if (cfg != null && cfg.mapController != null)
                    cfg.mapController.setCenter(cfg.maPosition.getGeoPoint());
                // On bascule sur l'onglet 1 (la carte)
                if (cfg != null && cfg.viewPager != null) {
                    cfg.viewPager.setCurrentItem(1);
                    //cfg.viewPager.invalidate();
                }
            }
        });

        // Bouton flottant (Floating Action Button) d'information
        binding.fabInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, getString(R.string.texte_bouton_info), Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });

        if (Config.DEBUG_LEVEL > 3) Log.v("Fragment principal", "---Fin du OnCreateView du fragment principal");

        disableTouchTheft(root);
        return root;
    }

    public static void disableTouchTheft(View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                view.getParent().requestDisallowInterceptTouchEvent(true);
                if ((motionEvent.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}