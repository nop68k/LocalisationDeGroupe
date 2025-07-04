package org.patarasprod.localisationdegroupe.views;

import android.content.Context;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.patarasprod.localisationdegroupe.Config;
import org.patarasprod.localisationdegroupe.FragmentInfos;
import org.patarasprod.localisationdegroupe.Position;
import org.patarasprod.localisationdegroupe.R;

/**
 * Classe gérant les items de la liste des noms dans la vue Infos (contenus dans le recyclerview)
 * Cette classe étend ViewHolder et implémente View.OnCreateContextMenuListener pour pouvoir
 * afficher un menu contextuel sur l'appui long d'un item.
 */
public class ViewHolderListeUtilisateurs extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {

    //@BindView(R.id.item_liste_utilisateurs)  TextView textView;
    private final TextView        textViewNom;
    private final TextView        textViewPosition;
    private final TextView        textViewAnciennete;
    private     Position        position;
    private final MenuInflater    menuInflater;
    protected   FragmentInfos.PasseurDeReference referenceVH;       // Pour les menus contextuels

    public ViewHolderListeUtilisateurs(View itemView, Config cfg, MenuInflater m, FragmentInfos.PasseurDeReference referenceVH) {
        super(itemView);
        this.textViewNom = itemView.findViewById(R.id.nom_utilisateur);
        this.textViewPosition = itemView.findViewById(R.id.position_utilisateur);
        this.textViewAnciennete = itemView.findViewById(R.id.anciennete_utilisateur);
        this.menuInflater = m;
        this.referenceVH = referenceVH;

        // L'appui long ouvre un menu contextuel
        //itemView.setLongClickable(true);
        itemView.setOnCreateContextMenuListener(this);

        // Gestion de l'appui court => Centre la carte sur cette personne et bascule sur l'onglet carte
        itemView.setOnClickListener(view -> {
            cfg.mapController.setCenter(position.getGeoPoint()); // Centre sur cette position
            cfg.viewPager.setCurrentItem(1);   // Ramène à l'onglet 1 (carte)
        });
    }

    /**
     * Remplis les champs du viewHolder avec les informatios données dans l'argument position
     * et met à jour ses attributs.
     * @param position  objet Position contenant les données à jour
     * @param ignoredNumero    position (ordre) du viewHolder dans le recyclerView (la liste)
     */
    public void remplisUtilisateur(Position position, int ignoredNumero){
        this.position = position;
        this.textViewNom.setText(position.nom);
        this.textViewPosition.setText(this.textViewNom.getContext().getString(
                R.string.chaine_affichage_coordonnees_GPS,position.latitude, position.longitude));
        this.textViewAnciennete.setText(position.getAnciennete());
        this.textViewAnciennete.setTextColor(position.couleurAnciennete());
        //if (numero % 2 == 1) this.itemView.setBackgroundColor(Color.DKGRAY);
    }

    public Position getPos() {
        return this.position;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        menuInflater.inflate(R.menu.menu_contextuel_noms, menu);
        // On fixe la référence à notre ViewHolder pour que la méthode onContextItemSelected du
        // fragment Infos puisse savoir quel élément a été cliqué
        referenceVH.setReference(this);
    }

}