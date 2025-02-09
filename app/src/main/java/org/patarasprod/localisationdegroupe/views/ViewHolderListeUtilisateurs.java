package org.patarasprod.localisationdegroupe.views;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.patarasprod.localisationdegroupe.Config;
import org.patarasprod.localisationdegroupe.Position;
import org.patarasprod.localisationdegroupe.R;


public class ViewHolderListeUtilisateurs extends RecyclerView.ViewHolder {

    //@BindView(R.id.item_liste_utilisateurs)  TextView textView;
    TextView textViewNom;
    TextView textViewPosition;
    TextView textViewAnciennete;
    Position position;

    public ViewHolderListeUtilisateurs(View itemView, Config cfg) {
        super(itemView);
        this.textViewNom = itemView.findViewById(R.id.nom_utilisateur);
        this.textViewPosition = itemView.findViewById(R.id.position_utilisateur);
        this.textViewAnciennete = itemView.findViewById(R.id.anciennete_utilisateur);
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cfg.mapController.setCenter(position.getGeoPoint()); // Centre sur cette position
                cfg.viewPager.setCurrentItem(1);   // Ramène à l'onglet 1 (carte)
            }
        });
        itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                // Build the intent.
                Uri location = Uri.parse("geo:" + position.latitude + "," +
                        position.longitude);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, location);
                // Try to invoke the intent.
                try {
                    if (Config.DEBUG_LEVEL > 2) Log.v("ViewHolderUtilisateurs",
                      "Appel d'une appli externe sur l'uri : "+"geo:"+
                           String.valueOf(position.latitude).replace(',','.') + "," +
                           String.valueOf(position.longitude).replace(',','.'));
                    cfg.mainActivity.startActivity(mapIntent);
                } catch (ActivityNotFoundException e) {
                    Log.v("ViewHolderUtilisateurs", "Activité introuvable pour voir une uri de type geo");
                    // Define what your app should do if no activity can handle the intent.
                    return false;  // click non traité
                }
                return true;  // click traité
            }
        });
    }

    public void remplitUtilisateur(Position position, int numero){
        this.position = position;
        this.textViewNom.setText(position.nom);
        this.textViewPosition.setText(position.latitude + "N, " + position.longitude + "E");
        this.textViewAnciennete.setText(position.getAnciennete());
        this.textViewAnciennete.setTextColor(position.couleurAnciennete());
        //if (numero % 2 == 1) this.itemView.setBackgroundColor(Color.DKGRAY);
    }

    public void majUtilisateur(int numero) {
        this.textViewNom.setText(this.position.nom);
        this.textViewPosition.setText(this.position.latitude + "N, " + this.position.longitude + "E");
        this.textViewAnciennete.setText(this.position.dateMesure.toString());
    }
}