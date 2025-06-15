package org.patarasprod.localisationdegroupe;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class GestionPositionsUtilisateurs {

    private static final boolean DEBUG_CLASSE = false;  // Drapeau pour autoriser les message de debug dans la classe
    public Map positions = new HashMap();   // Positions des utilisateurs

    final String SEPARATEUR_ELEMENTS_REPONSE_SERVEUR = "$*$";
    final int TAILLE_LABEL_MARQUEUR = 45;  // Taille des label sous les marqueurs
    final static long DUREE_TRES_ANCIEN = 3600 * 24;  // Durée depuis laquelle on a pas vu la position
    final static int COULEUR_TRES_ANCIEN = Color.RED;
    final static long DUREE_ANCIEN = 60 * 10;
    final static int COULEUR_ANCIEN = Color.GRAY;
    final static int COULEUR_RECENT = Color.BLACK;

    private final String NOM_FICHIER_SAUVEGARDE_POSITIONS = "positions.json";

    Config cfg;

    public GestionPositionsUtilisateurs(Config config) {
        this.cfg = config;
        restaurePositionsAPartirDeLaSauvegarde();

        /* TEST
        String test = "Maison||48.56388;2.606280||2023-07-05T09:13:55.866Z$*$Florent||48.5638947;2.6062540||2023-07-17T09:32:06.918Z$*$Célian ||48.5639130;2.6064107||2023-07-15T18:43:49.511Z";
        majPositions(test);
        sauvegardePositions();

*/
    }

    /** Ajoute ou insère une position dans la hashmap 'positions' à partir de la chaîne
     *  de caractère donnée en argument.
     * Si la position est invalide, logge une erreur avec la chaine d'origine.
     */
    private void ajouteOuMetAJourPosition(String chainePosition) {
        Position position = new Position(chainePosition);
        if (position.nom.equals(Position.NOM_INVALIDE)) {
            if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE) Log.v("Cartographie", "Position invalide détectée dans la chaîne " + chainePosition);
        }
        else {
            if (positions.containsKey(position.nom)) {
                ((Position) Objects.requireNonNull(this.positions.get(position.nom))).majPosition(position);
            } else {
                positions.put(position.nom, position);
            }
        }
    }

    /** Met à jour la hashmap des positions à partir de la réponse du serveur
     * Celle-ci est sous la forme d'un dictionnaire python (entre accolades) avec des clés
     * représentant les noms des utilisateurs
     */
    public void majPositions(String reponseServeur) {
        if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE) Log.v("Cartographie", "appel de la mise à jour position");
        int separateur = reponseServeur.indexOf(SEPARATEUR_ELEMENTS_REPONSE_SERVEUR);
        while (separateur >= 0) {
            // On mémorise la ligne actuelle
            ajouteOuMetAJourPosition(reponseServeur.substring(0, separateur));
            // Puis on l'enlève de la réponse du serveur
            reponseServeur = reponseServeur.substring(separateur +
                                                     SEPARATEUR_ELEMENTS_REPONSE_SERVEUR.length());
            separateur = reponseServeur.indexOf(SEPARATEUR_ELEMENTS_REPONSE_SERVEUR);
        }
        // Ajoute la dernière position si elle n'est pas invalide
        ajouteOuMetAJourPosition(reponseServeur);
        // Toutes les positions ont été mises à jour
        cfg.nbPositions = positions.size();
        if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE) Log.v("Cartographie", "Parsing de la réponse effectué, " +
                cfg.nbPositions + " positions trouvées");
        if (cfg.map != null) {   // Si la carte a été crée, on la met à jour
            if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE) Log.v("Cartographie", "MàJ des positions sur la carte");
            majPositionsSurLaCarte();
        }
    }

    /** Procédure qui met à jour les marqueurs de position sur la carte à partir de la liste
     * d'objets Position se trouvant dans la hashmap "positions"
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    public void majPositionsSurLaCarte() {
        if (cfg.map == null) {
            Log.v("Cartographie", "appel de la mise à jour sur carte null");
            return ;
        }
        // On commence par retirer tous les marqueurs
        if (Config.DEBUG_LEVEL > 4 && DEBUG_CLASSE)  Log.v("GestionPositionUtilisateurs",
                "### Mise à jour marqueurs avec map qui vaut :" + cfg.map);
        Iterator<Overlay> iter = cfg.map.getOverlays().iterator();
        Overlay item;
        while (iter.hasNext()) {
            item = iter.next();
            if (item.getClass().equals(MarkerWithLabel.class)) cfg.map.getOverlays().remove(item);
        }
        if (Config.DEBUG_LEVEL > 4 && DEBUG_CLASSE)  Log.v("GestionPositionUtilisateurs",
                "#### Fin suppression anciens marqueurs avec map qui vaut :" + cfg.map);

        // Puis on ajoute des noyveaux marqueurs correspondant aux positions :
        Iterator<String> iter2 = positions.keySet().iterator();
        String nomUtilisateur;
        MarkerWithLabel marqueur;
        Position position;
        while (iter2.hasNext()) {
            if (Config.DEBUG_LEVEL > 4 && DEBUG_CLASSE)  Log.v("GestionPositionUtilisateurs",
                    "#### Mise en place nième marqueur avec map qui vaut :" + cfg.map);
            nomUtilisateur = iter2.next();
            position = (Position) this.positions.get(nomUtilisateur);
            if (estPositionValide(position) && !nomUtilisateur.equals(cfg.nomUtilisateur)) {
                marqueur = new MarkerWithLabel(cfg.map, nomUtilisateur);
                if (Config.DEBUG_LEVEL > 4 && DEBUG_CLASSE)  Log.v("GestionPositionUtilisateurs",
                        "#### Juste APRES instruction qui fait planter avec map qui vaut :" + cfg.map);
                marqueur.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marqueur.setIcon(ResourcesCompat.getDrawable(
                        cfg.mainActivity.getBaseContext().getResources(),
                        idImageMarqueur(position),
                        cfg.mainActivity.getBaseContext().getTheme()));
                marqueur.setTitle(nomUtilisateur);
                marqueur.setLabelTextColor(couleurEnFonctionDeLAnciennete(position));
                marqueur.setLabelFontSize(TAILLE_LABEL_MARQUEUR);
                marqueur.setDraggable(false);
                marqueur.setPosition(new GeoPoint(position.latitude, position.longitude));
                cfg.map.getOverlays().add(marqueur);
            }
        }
        cfg.map.invalidate();  // Pour forcer à redessiner
    }

    /** Renvoie true si la position donnée est valide (vérifie la présence des champs et
     *  leurs valeurs (dans les bornes acceptables)
     */
    private boolean estPositionValide(Position position) {
        if (position == null) return false;
        return position.estPositionValide();
    }

    public int couleurEnFonctionDeLAnciennete(Position position) {
        if (position.dateMesure.isBefore(Instant.now().minusSeconds(DUREE_TRES_ANCIEN))) {
            return COULEUR_TRES_ANCIEN;
        } else if (position.dateMesure.isBefore(Instant.now().minusSeconds(DUREE_ANCIEN))) {
            return COULEUR_ANCIEN;
        } else {
            return COULEUR_RECENT;
        }
    }
    public String[] listePositions() {
        int i = 0;
        String nomUtilisateur;
        String[] liste = new String[this.positions.size()];
        Iterator<String> iter = positions.keySet().iterator();
        while (iter.hasNext()) {
            nomUtilisateur = iter.next();
            liste[i] = Objects.requireNonNull(this.positions.get(nomUtilisateur)).toString();
            if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE)
                Log.v("GestionPositionUtilisateurs","Elément n°" + i + " (" + nomUtilisateur + ") : " + liste[i]);
            i++;
        }
        return liste;
    }

    public void sauvegardePositions() {
        JSONArray positionsASauvegarder = new JSONArray();
        Iterator<String> iter = positions.keySet().iterator();
        String nomUtilisateur;
        Position position;
        while (iter.hasNext()) {
            nomUtilisateur = iter.next();
            position = (Position) this.positions.get(nomUtilisateur);
            JSONObject objetPositionASauvegarder = position.toJSONObject();
            positionsASauvegarder.put(objetPositionASauvegarder);
        }

        File fichier = new File(cfg.mainActivity.getBaseContext().getFilesDir(), NOM_FICHIER_SAUVEGARDE_POSITIONS);
        if (!fichier.exists()) {
            try {
                fichier.createNewFile();
            } catch (IOException e) {
                Log.v("GestionPositions", "ERREUR à la création du fichier de sauvegarde - " + e);
            }
        }
        try {
            FileOutputStream out = cfg.mainActivity.getBaseContext().openFileOutput(fichier.getName(), Context.MODE_PRIVATE);
            out.write(positionsASauvegarder.toString().getBytes());
            out.flush();
            out.close();
            if (cfg != null && Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE)
                Log.v("GestionPositions", "Sauvegarde des positions dans le fichier effectuée");
        } catch (FileNotFoundException e) {
            Log.v("GestionPositions", "ERREUR : le fichier de sauvegarde est introuvable");
        } catch (IOException e) {
            Log.v("GestionPositions", "ERREUR pendant l'écriture du fichier de sauvegarde"+
                    " - " + e);
        }
    }

    public void restaurePositionsAPartirDeLaSauvegarde() {
        Map positionsLues = new HashMap();
        File fichier = new File(cfg.mainActivity.getBaseContext().getFilesDir(), NOM_FICHIER_SAUVEGARDE_POSITIONS);
        if (!fichier.exists()) {
            Log.v("GestionPositions", "Chargement des positions impossible car le fichier de sauvegarde n'existe pas");
        } else {
            try {
                BufferedReader lecteur = new BufferedReader(new InputStreamReader(cfg.mainActivity.getBaseContext().openFileInput(fichier.getName())));
                StringBuilder contenuFichier = new StringBuilder();
                String ligne;
                while ((ligne = lecteur.readLine()) != null) {
                    contenuFichier.append(ligne);
                }
                lecteur.close();
                if (contenuFichier.length() > 10) {  // Si des choses ont étées lues
                    JSONArray contenuJSON = new JSONArray(contenuFichier.toString());
                    fusionnePositions(contenuJSON);
                }
                if (cfg != null && Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE)
                    Log.v("GestionPositions", "Sauvegarde des positions dans le fichier effectuée");
            } catch (FileNotFoundException e) {
                Log.v("GestionPositions", "ERREUR : le fichier de sauvegarde est introuvable");
            } catch (IOException e) {
                Log.v("GestionPositions", "ERREUR pendant l'écriture du fichier de sauvegarde" +
                        " - " + e);

            } catch (JSONException e) {
                Log.v("GestionPositions", "Erreur lors de la conversion JSON du fichier " +
                        "de sauvegarde des positions : " + e);
            }
        }
    }

    /** Fonction qui fusionne le tableau de positions dans le JSONArray fournit en argument
     * avec les positions actuellement stockées : ajoute les positions manquante et met à jour
     * les positions existantes si elles sont plus récentes.
     */
    private void fusionnePositions(JSONArray contenuJSON) {
        Position positionJSON;
        for (int i = 0 ; i < contenuJSON.length() ; i++) {
            try {
                positionJSON = new Position(contenuJSON.getJSONObject(i));
            } catch (JSONException e) {
                Log.v("GestionPositions", "Erreur lors de la conversion JSON de la " +
                        "position " + i + " : " + e);
                return;
            }
            if (!positions.containsKey(positionJSON.nom) || positionJSON.dateMesure.isAfter( ((Position)positions.get(positionJSON.nom)).dateMesure ) ) {
                // Si la position n'est pas dans la liste courante ou plus récente dans la sauvegarde
                positions.put(positionJSON.nom, positionJSON);  // elle remplace la position actuelle
            }
        }
    }

            public int idImageMarqueur(Position position) {
        int[] tabIdMarqueurs = {R.drawable.map_marker_1, R.drawable.map_marker_2,
 R.drawable.map_marker_3, R.drawable.map_marker_4, R.drawable.map_marker_5, R.drawable.map_marker_6,
 R.drawable.map_marker_7, R.drawable.map_marker_8, R.drawable.map_marker_9, R.drawable.map_marker_10,
 R.drawable.map_marker_11, R.drawable.map_marker_12, R.drawable.map_marker_13, R.drawable.map_marker_14,
 R.drawable.map_marker_15, R.drawable.map_marker_16}; /*, R.drawable.map_marker_17, R.drawable.map_marker_18,
 R.drawable.map_marker_19, R.drawable.map_marker_20, R.drawable.map_marker_21, R.drawable.map_marker_22,
 R.drawable.map_marker_23, R.drawable.map_marker_24, R.drawable.map_marker_25, R.drawable.map_marker_26,
 R.drawable.map_marker_27, R.drawable.map_marker_28, R.drawable.map_marker_29, R.drawable.map_marker_30,
 R.drawable.map_marker_31, R.drawable.map_marker_32, R.drawable.map_marker_33, R.drawable.map_marker_34,
 R.drawable.map_marker_35, R.drawable.map_marker_36, R.drawable.map_marker_37, R.drawable.map_marker_38,
 R.drawable.map_marker_39, R.drawable.map_marker_40, R.drawable.map_marker_41, R.drawable.map_marker_42,
 R.drawable.map_marker_43, R.drawable.map_marker_44, R.drawable.map_marker_45, R.drawable.map_marker_46,
 R.drawable.map_marker_47, R.drawable.map_marker_48, R.drawable.map_marker_49, R.drawable.map_marker_50};
*/
        if (position.nom.length() == 0) return R.drawable.map_marker_0;
        int hash = position.nom.charAt(0);
        if (position.nom.length() > 1) hash += position.nom.charAt(1);
        return tabIdMarqueurs[hash % (tabIdMarqueurs.length)];
    }

    /** Fonction qui renvoie un ArrayList des positions gérées **/
    public ArrayList<Position> getListePositions() {
        ArrayList<Position> liste = new ArrayList<Position>();
        Iterator<String> iter = positions.keySet().iterator();
        while (iter.hasNext()) {
            liste.add((Position) positions.get(iter.next()));
        }
        return liste;
    }


    @NonNull
    @Override
    public String toString() {
        String[] lignes = this.listePositions();
        StringBuilder chaine = new StringBuilder();
        for (String ligne : lignes) {
            chaine.append(ligne).append("\n");
        }
        return chaine.toString();
    }

    /** Réinitialise les positions gérées en vidant la liste **/
    public void reinitialise_positions() {
        this.positions.clear();
    }
}

