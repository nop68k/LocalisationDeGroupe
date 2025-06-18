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

        // Gestion de l'appui court => Centre la carte sur cette personne et bascule sur l'onglet carte
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cfg.mapController.setCenter(position.getGeoPoint()); // Centre sur cette position
                cfg.viewPager.setCurrentItem(1);   // Ramène à l'onglet 1 (carte)
            }
        });

        // Festion de l'appui long : ouvre la position dans une appli externe de cartographie
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

    /**
     * Remplis les champs du viewHolder avec les informatios données dans l'argument position
     * et met à jour ses attributs.
     * @param position  objet Position contenant les données à jour
     * @param numero    position (ordre) du viewHolder dans le recyclerView (la liste)
     */
    public void remplisUtilisateur(Position position, int numero){
        this.position = position;
        this.textViewNom.setText(position.nom);
        this.textViewPosition.setText(position.latitude + "N, " + position.longitude + "E");
        this.textViewAnciennete.setText(position.getAnciennete());
        this.textViewAnciennete.setTextColor(position.couleurAnciennete());
        //if (numero % 2 == 1) this.itemView.setBackgroundColor(Color.DKGRAY);
    }

    /**
     * Met à jour le ViewHolder avec les données stockées dans ses attributs
     * @param numero   position (ordre) du viewHolder dans le recyclerView (la liste)
     */
    public void majUtilisateur(int numero) {
        this.textViewNom.setText(this.position.nom);
        this.textViewPosition.setText(this.position.latitude + "N, " + this.position.longitude + "E");
        this.textViewAnciennete.setText(this.position.getAnciennete());
    }


}