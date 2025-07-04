package org.patarasprod.localisationdegroupe;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;


public class MyAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {

    int totalTabs;

    public MyAdapter(FragmentActivity fa, int totalTabs) {
        super(fa);
        this.totalTabs = totalTabs;
    }
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (Config.DEBUG_LEVEL > 3) Log.v("MyAdapter","Méthode Fragment appellée avec position = " + position);
        switch (position) {
            case 0:
                return new FragmentPosition();
            case 1:
                return new FragmentCarte();
            case 2:
                return new FragmentInfos();
            case 3:
                return new Fragment_parametres();
            default:
                throw new RuntimeException("@@@@ ERREUR : Méthode Fragment appellée avec position = " + position);
        }
    }

    @Override
    public int getItemCount() {
        return totalTabs;
    }
}