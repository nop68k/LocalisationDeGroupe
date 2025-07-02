package org.patarasprod.localisationdegroupe.views;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import org.patarasprod.localisationdegroupe.Config;
import org.patarasprod.localisationdegroupe.FragmentInfos;
import org.patarasprod.localisationdegroupe.Position;
import org.patarasprod.localisationdegroupe.R;

import java.util.List;

public class RecyclerViewAdapterListeUtilisateurs extends RecyclerView.Adapter<ViewHolderListeUtilisateurs> {
        private static final boolean DEBUG_CLASSE = false;  // Drapeau pour autoriser les message de debug dans la classe
        private       List<Position>    listeUtilisateurs;
        protected     MenuInflater      menuInflater;
        protected     Config            cfg;
        protected     FragmentInfos.PasseurDeReference referenceVH;       // Pour les menus contextuels

        public RecyclerViewAdapterListeUtilisateurs(List<Position> listeUtilisateurs, Config cfg, MenuInflater m, FragmentInfos.PasseurDeReference referenceVH) {
            this.listeUtilisateurs = listeUtilisateurs;
            this.cfg = cfg;
            this.menuInflater = m;
            this.referenceVH = referenceVH;
        }

        @Override
        public ViewHolderListeUtilisateurs onCreateViewHolder(ViewGroup parent, int viewType) {
            // CREATE VIEW HOLDER AND INFLATING ITS XML LAYOUT
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.item_liste_utilisateurs, parent, false);
            return new ViewHolderListeUtilisateurs(view, cfg, menuInflater, referenceVH);
        }

        @Override
        public void onBindViewHolder(ViewHolderListeUtilisateurs viewHolder, int positionDsListe) {
            viewHolder.remplisUtilisateur(this.listeUtilisateurs.get(positionDsListe), positionDsListe);
        }

        public void majListeUtilisateurs() {
            listeUtilisateurs.clear();
            if (DEBUG_CLASSE && Config.DEBUG_LEVEL > 3) Log.v("RecyclerViewAdapter", "Liste vidée : " + listeUtilisateurs);
            listeUtilisateurs = cfg.gestionPositionsUtilisateurs.getListePositions();
            if (DEBUG_CLASSE && Config.DEBUG_LEVEL > 3) Log.v("RecyclerViewAdapter", "Liste maj : " + listeUtilisateurs);
            notifyDataSetChanged();
        }

        // Renvoie le nombre d'items dans la liste
        @Override
        public int getItemCount() {
            return this.listeUtilisateurs.size();
        }
    }


