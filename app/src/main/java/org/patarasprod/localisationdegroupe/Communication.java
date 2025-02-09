package org.patarasprod.localisationdegroupe;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Communication {

    Config cfg;
    Socket socketClient;
    PrintWriter out;
    BufferedReader in;

    public Communication(Config config) {
        cfg = config;
        demandeAutorisationInternet();
    }

    public void demarreCommunicationAvecServeur() {
        // La création de scoket doit se faire dans un autre Thread obligatoirement
        cfg.threadCommunication = new Thread(new Runnable() {
            @Override
            public void run() {
                cfg.communicationEnCours = false;
                // Si on ne connaît pas notre position, on ne démarre pas la communication et on sort tout de suite
                if (cfg.maPosition == null) {
                    if (Config.DEBUG_LEVEL > 1) Log.v("Communication","Communication non initiée car notre position est inconnue");
                    return;
                }
                try {
                    //Création d'un socket
                    //if (Config.DEBUG_LEVEL > 4) Log.v("Communication","Création du socket client");
                    socketClient = new Socket(cfg.adresse_serveur, cfg.port_serveur);
                    //if (Config.DEBUG_LEVEL > 4) Log.v("Communication", "Socket client crée");
                    out = null;
                    in = null;
                    out = new PrintWriter(socketClient.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
                    //if (Config.DEBUG_LEVEL > 4) Log.v("Communication", "Création réussie des 'in' et 'out' du socket");
                } catch (Exception e) { //(IOException e) {
                    if (Config.DEBUG_LEVEL > 1) Log.v("Communication","Problème lors de la création du socket client :");
                    if (Config.DEBUG_LEVEL > 1) Log.v("Communication",e.toString());
                    fermeture();
                    return;
                }
                if (Config.DEBUG_LEVEL > 3) Log.v("Communication","-------Démarrage de la communication bi-directionelle");
                cfg.reponse = "";
                boolean erreurCommunication = false;
                while (cfg.reponse != null && cfg.diffuserMaPosition && !erreurCommunication &&
                        !cfg.reponse.equals("FIN")) {
                    cfg.communicationEnCours = true;
                    if (Config.DEBUG_LEVEL > 3) Log.v("Communication","Envoi du message : " + cfg.maPosition.toString());
                    // Envoi de la position au serveur
                    out.println(cfg.maPosition.toString());
                    // Récupération de la réponse
                    if (Config.DEBUG_LEVEL > 3) Log.v("Communication","Lecture de la réponse du serveur sur " + in.toString());
                    try {
                        cfg.reponse = in.readLine();
                        if (Config.DEBUG_LEVEL > 3) Log.v("Communication","Réponse du serveur : " + cfg.reponse);
                        // Attente avant de recommencer le dialogue
                        if (Config.DEBUG_LEVEL > 3) Log.v("Communication","Attente pendant " +
                                cfg.intervalleMajSecondes + " s");
                        traitementReponse(cfg.reponse);
                        if (cfg.fragment_3 != null) cfg.fragment_3.majTexteInfos();
                        android.os.SystemClock.sleep(cfg.intervalleMajSecondes*1000);
                    } catch (Exception e) {
                        cfg.reponse = cfg.ERREUR_READLINE_SOCKET_DISTANT;
                        erreurCommunication = true;
                    }
                }
                if (Config.DEBUG_LEVEL > 2) Log.v("Communication","-------Fermeture de la communication bi-directionelle");
                if (Config.DEBUG_LEVEL > 3) Log.v("Communication","-------Dernière réponse du serveur : "+cfg.reponse);
                cfg.communicationEnCours = false;
                fermeture();
            }

            public void traitementReponse(String reponse) {
                if (cfg.reponse != null) {
                    if (cfg.reponse.length() > cfg.TAILLE_MINI_REPONSE_COMPLETE) cfg.gestionPositionsUtilisateurs.majPositions(reponse);
                    if (cfg.fragment_3 != null) {
                        cfg.fragment_3.majTexteInfos();
                    }
                }
            }

            public void fermeture() {
                if (out != null) out.close();
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (socketClient != null) {
                    try {
                        socketClient.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            public void stop() {
                fermeture();
            }
        });
        cfg.threadCommunication.start();
    }

    public boolean demandeAutorisationInternet() {
        Context contexte = cfg.mainActivity.getBaseContext();
        if (ActivityCompat.checkSelfPermission(contexte, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(contexte, Manifest.permission.ACCESS_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity)contexte,
                    new String[]{Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.INTERNET}, 2);
            if (ActivityCompat.checkSelfPermission(contexte, Manifest.permission.INTERNET)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(contexte, Manifest.permission.ACCESS_NETWORK_STATE)
                            != PackageManager.PERMISSION_GRANTED) {
                if (Config.DEBUG_LEVEL > 1) Log.v("Communication","Autorisation internet NON accordée");
                return false;
            }
        }
        if (Config.DEBUG_LEVEL > 3) Log.v("Communication","Autorisation internet accordée");
        return true;
    }

    public void stopThreadCommunication() {
        if (cfg.threadCommunication != null) {  // Thread communication lancé
            cfg.diffuserMaPosition = false;     // Pour signifier au thread qu'il doit s'arrêter
            try {
                cfg.threadCommunication.join(100L);  // Attend 100 ms que le thread se termine
                if (cfg.communicationEnCours) { // Si le thread est encore actif
                    cfg.threadCommunication.interrupt();   // On le force à l'arrêt
                    cfg.threadCommunication = null;
                    cfg.communicationEnCours = false;
                }
            } catch (InterruptedException e) {
                Log.v("Communication","EXCEPTION lors de l'attente de la fin du thread communication");
            }
        }
    }


    public void demarreDiffusionPositionAuServeur() {
        // On commence par vérifier qu'il n'y a pas de communication en cours et la stopper si nécessaire
        stopThreadCommunication();
        if (cfg.handlerDiffusionPosition == null) cfg.handlerDiffusionPosition = new Handler();
        cfg.handlerDiffusionPosition.postDelayed(() -> cfg.com.diffusionPositionAuServeur(),
                cfg.intervalleMajSecondes * 1000);
        cfg.diffuserMaPosition = true;
        demarreCommunicationAvecServeur();
    }

    public void stoppeDiffusionPositionAuServeur() {
        if (cfg.handlerDiffusionPosition != null)
            cfg.handlerDiffusionPosition.removeCallbacksAndMessages(null);
        stopThreadCommunication();
        cfg.diffuserMaPosition = false;  // Ceci arrêtera le thread de communication avec le serveur
    }

    /** Fonction qui s'assure périodiquement que la communication avec le serveur est bien
     *  active
     **/
    public void diffusionPositionAuServeur() {
        if (cfg.diffuserMaPosition) {
            if (Config.DEBUG_LEVEL > 2) Log.v("Communication","Diffusion de la position via internet");
            if (cfg.fragment_3 != null) cfg.fragment_3.majTexteInfos();
            if (cfg.com != null) {
                // Teste si la communication n'est pas déjà en cours
                if (cfg.threadCommunication == null) {  // Si le thread n'est pas crée
                    cfg.com.demarreCommunicationAvecServeur();  // On le crée et le démarre
                } else if (!cfg.communicationEnCours) {   // Sinon s'il n'est pas actif
                    try {
                        cfg.threadCommunication.join(30L);    // on attend sa fin
                        cfg.com.demarreCommunicationAvecServeur();  // puis on le relance
                    } catch (InterruptedException e) {
                        Log.v("Communication", "ERREUR : le thread communication est " +
                                "bloqué alors que communicationEnCours est à false");
                        cfg.threadCommunication.interrupt();        // Force l'arrêt
                        cfg.com.demarreCommunicationAvecServeur();  // puis on le relance
                    }
                } else {
                    // Sinon c'est que la communication est déjà active et il n'y a rien à faire
                }
            } else {
                Log.v("Communication","*********ERREUR : le module communication n'est pas" +
                        " actif et il y a demande de diffusion de la position !");
            }
            // Plannifie la prochaine diffusion
            cfg.handlerDiffusionPosition.postDelayed(() -> this.diffusionPositionAuServeur(),
                    cfg.intervalleMajSecondes * 1000);
        } else {
            if (Config.DEBUG_LEVEL > 0) Log.v("Communication",
           "Appel de 'diffusionPositionAuServeur' alors que 'diffuserMaPosition' est à false");
        }

    }

    /** Met à jour le voyant indicateur de connexion au serveur (présent sur la page
     * paramètres) en fonction de l'état de la variable cfg.communicationEnCours
     */
    public void majIndicateurConnexion() {
        if (cfg.indicateurConnexionServeur != null) {
            if (cfg.communicationEnCours) {
                cfg.indicateurConnexionServeur.setImageDrawable(ResourcesCompat.getDrawable(
                        cfg.mainActivity.getBaseContext().getResources(),
                        R.drawable.serveur_actif,
                        cfg.mainActivity.getBaseContext().getTheme()));
            } else {
                cfg.indicateurConnexionServeur.setImageDrawable(ResourcesCompat.getDrawable(
                        cfg.mainActivity.getBaseContext().getResources(),
                        R.drawable.serveur_injoignable,
                        cfg.mainActivity.getBaseContext().getTheme()));
            }
            if (cfg.fragment_3 != null && cfg.fragment_3.getView() != null) {
                cfg.fragment_3.getView().invalidate();
                cfg.fragment_3.getView().requestLayout();
            }
        }
    }
}
