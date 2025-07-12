package org.patarasprod.localisationdegroupe;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;

/**
 * Classe gérant les communications avec le serveur (sauf celle en tâche de fond gérée directement
 * par le service).
 */
public class Communication {
    private static final boolean DEBUG_CLASSE = false;  // Drapeau pour autoriser les message de debug dans la classe
    // Constantes pour la communication
    public static final String CARACTERE_COMMUNICATION_BIDIRECTIONNELLE = "%";
    public static final String CARACTERE_COMMUNICATION_UNIDIRECTIONNELLE = ">";
    public static final String CARACTERE_COMMUNICATION_COMMANDE = "^";

    public static final String COMMANDE_SUPPRIMER = "SUPPR";  // Pour supprimer une position sur le serveur
    public static final String COMMANDE_SYNCHRONISER = "SYNC";  // Pour synchroniser l'heure avec le serveur

    Config cfg;


    public Communication(Config config) {
        cfg = config;
        demandeAutorisationInternet();
        synchronisationHeureServeur();
    }

    /**
     * Crée un socket client et met à jour l'indicateur de connexion au serveur si besoin
     *
     * @param adresse String : adresse du serveur
     * @param port    Int : port où joindre le serveur
     * @return null ou objet Socket si le socket a pu être crée
     */
    public Socket creationSocketClient(String adresse, int port) {
        if (Config.DEBUG_LEVEL > 4) Log.v("Communication", "Création du socket client");
        try {
            Socket socketClient = new Socket(adresse, port);
            if (Config.DEBUG_LEVEL > 4) {
                Log.v("Communication", "Socket client crée");
                Log.v("Communication", "connectionAuServeurOK : " + cfg.connectionAuServeurOK);
            }

            if (cfg.connectionAuServeurOK) return socketClient;
            // La connection n'était pas établie, donc on l'indique
            cfg.connectionAuServeurOK = true;
            majIndicateurConnexion();     // Et on met à jour le voyant
            return socketClient;
        } catch (Exception e) { //(IOException e) {
            if (Config.DEBUG_LEVEL > 1 && DEBUG_CLASSE) Log.v("Communication",
                    "Problème lors de la création du socket client :");
            if (Config.DEBUG_LEVEL > 1 && DEBUG_CLASSE) Log.v("Communication", e.toString());
            Log.v("Communication", "connectionAuServeurOK : " + cfg.connectionAuServeurOK);
            if (!cfg.connectionAuServeurOK) return null;
            // La connection était établie, donc on indique qu'elle est tombée
            cfg.connectionAuServeurOK = false;
            majIndicateurConnexion();     // Et on met à jour le voyant
            return null;
        }
    }

    public void demarreCommunicationAvecServeur() {
        // La création de socket doit se faire dans un autre Thread obligatoirement
        cfg.threadCommunication = new Thread(new Runnable() {
            // Variables du thread
            Socket socketClient;    // Socket de connexion
            PrintWriter out;        // flux de sortie (pour envoyer au serveur)
            BufferedReader in;      // flux d'entrée (pour recevoir la réponse du serveur)
            @Override
            public void run() {
                cfg.communicationEnCours = false;
                // Si on ne connaît pas notre position, on ne démarre pas la communication et on sort tout de suite
                if (cfg.maPosition == null) {
                    if (Config.DEBUG_LEVEL > 1 && DEBUG_CLASSE)
                        Log.v("Communication", "Communication non initiée car notre position est inconnue");
                    return;
                }
                try {
                    socketClient = creationSocketClient(cfg.adresse_serveur, cfg.port_serveur);
                    if (socketClient == null) throw new IOException("Socket client null");
                    out = null;
                    in = null;
                    out = new PrintWriter(socketClient.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
                } catch (Exception e) { //(IOException e) {
                    if (Config.DEBUG_LEVEL > 1 && DEBUG_CLASSE) Log.v("Communication",
                            "Problème lors de la création du socket client :");
                    if (Config.DEBUG_LEVEL > 1 && DEBUG_CLASSE)
                        Log.v("Communication", e.toString());
                    fermeture();
                    return;
                }
                if (out == null) {
                    if (Config.DEBUG_LEVEL > 1 && DEBUG_CLASSE) Log.d("Communication",
                            "Problème à la création du flux sortant du socket");
                    fermeture();
                    return;
                }
                if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE)
                    Log.v("Communication", "-------Démarrage de la communication bi-directionelle");
                cfg.reponse = "";
                boolean erreurCommunication = false;
                cfg.communicationEnCours = true;
                while (cfg.reponse != null && cfg.diffuserMaPosition && !erreurCommunication &&
                        !cfg.reponse.equals("FIN")) {
                    if (Config.DEBUG_LEVEL > 4 && DEBUG_CLASSE)
                        Log.v("Communication", "Envoi du message : " +
                                CARACTERE_COMMUNICATION_BIDIRECTIONNELLE + cfg.maPosition.toString(false));
                    // Envoi de la position au serveur
                    out.println(CARACTERE_COMMUNICATION_BIDIRECTIONNELLE + cfg.maPosition.toString(false));
                    out.flush();
                    // Récupération de la réponse
                    if (Config.DEBUG_LEVEL > 4 && DEBUG_CLASSE)
                        Log.v("Communication", "Lecture de la réponse du serveur sur " + in.toString());
                    try {
                        cfg.reponse = in.readLine();
                        if (Config.DEBUG_LEVEL > 4 && DEBUG_CLASSE)
                            Log.v("Communication", "Réponse du serveur : " + cfg.reponse);
                        // Attente avant de recommencer le dialogue
                        if (Config.DEBUG_LEVEL > 4 && DEBUG_CLASSE)
                            Log.v("Communication", "Attente pendant " + cfg.intervalleMajSecondes + " s");
                        traitementReponse(cfg.reponse);
                        if (cfg.fragment_infos != null) cfg.fragment_infos.majTexteInfos();
                        android.os.SystemClock.sleep(cfg.intervalleMajSecondes * 1000);
                    } catch (Exception e) {
                        cfg.reponse = cfg.ERREUR_READLINE_SOCKET_DISTANT;
                        erreurCommunication = true;
                    }
                }
                if (Config.DEBUG_LEVEL > 2 && DEBUG_CLASSE)
                    Log.v("Communication", "-------Fermeture de la communication bi-directionelle");
                if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE)
                    Log.v("Communication", "-------Dernière réponse du serveur : " + cfg.reponse);
                cfg.communicationEnCours = false;
                fermeture();
            }

            public void traitementReponse(String reponse) {
                if (cfg.reponse != null) {
                    if (cfg.reponse.length() > cfg.TAILLE_MINI_REPONSE_COMPLETE)
                        cfg.gestionPositionsUtilisateurs.majPositions(reponse);
                    if (cfg.fragment_infos != null) {
                        cfg.fragment_infos.majTexteInfos();
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
        });
        cfg.threadCommunication.start();
    }

    /**
     * Fonction pour envoyer sa position , récupérer la réponse, mettre à jour en conséquence
     * et fermer la connexion (1 seul couple envoi/réception)
     */
    public void communique1FoisAvecServeur() {
        // La création de socket doit se faire dans un autre Thread obligatoirement
        cfg.threadCommunication1Fois = new Thread(new Runnable() {
            // Variables du thread
            Socket socketClient;    // Socket de connexion
            PrintWriter out;        // flux de sortie (pour envoyer au serveur)
            BufferedReader in;      // flux d'entrée (pour recevoir la réponse du serveur)
            @Override
            public void run() {
                // Si on ne connaît pas notre position, on ne démarre pas la communication et on sort tout de suite
                if (cfg.maPosition == null) {
                    if (Config.DEBUG_LEVEL > 1 && DEBUG_CLASSE)
                        Log.v("Communication", "Com1fois : Communication non initiée car notre position est inconnue");
                    return;
                }
                try {
                    socketClient = creationSocketClient(cfg.adresse_serveur, cfg.port_serveur);
                    if (socketClient == null) throw new IOException("Socket client null");
                    out = null;
                    in = null;
                    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socketClient.getOutputStream())), true);
                    in = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
                    if (Config.DEBUG_LEVEL > 4)
                        Log.v("Communication", "Com1fois : Création réussie des 'in' et 'out' du socket");
                } catch (Exception e) { //(IOException e) {
                    if (Config.DEBUG_LEVEL > 1 && DEBUG_CLASSE) Log.v("Communication",
                            "Com1fois : Problème lors de la création du socket client :");
                    if (Config.DEBUG_LEVEL > 1 && DEBUG_CLASSE)
                        Log.v("Communication", e.toString());
                    fermeture();
                    return;
                }
                if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE)
                    Log.v("Communication", "Com1fois : envoi du message avec la position " +
                            CARACTERE_COMMUNICATION_BIDIRECTIONNELLE + cfg.maPosition.toString(false));
                // Envoi de la position au serveur
                try {
                    out.println(CARACTERE_COMMUNICATION_BIDIRECTIONNELLE + cfg.maPosition.toString(false));
                    out.flush();
                    out.close();    // Ferme le canal d'envoi
                    out=null;
                    // Récupération de la réponse
                    if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE)
                        Log.v("Communication", "Com1fois : Lecture de la réponse du serveur sur " + in.toString());
                    cfg.reponse = "";
                    cfg.reponse = in.readLine();
                    if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE)
                        Log.v("Communication", "Com1fois : Réponse du serveur : " + cfg.reponse);
                    traitementReponse(cfg.reponse);
                    if (cfg.fragment_infos != null) cfg.fragment_infos.majTexteInfos();
                } catch (Exception e) {
                    cfg.reponse = cfg.ERREUR_READLINE_SOCKET_DISTANT;
                }
                if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE)
                    Log.v("Communication", "Com1fois : -------Fermeture de la communication bi-directionelle");
                fermeture();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) { }
            }

            public void traitementReponse(String reponse) {
                if (cfg.reponse != null) {
                    if (cfg.reponse.length() > cfg.TAILLE_MINI_REPONSE_COMPLETE)
                        cfg.gestionPositionsUtilisateurs.majPositions(reponse);
                    if (cfg.fragment_infos != null) {
                        cfg.fragment_infos.majTexteInfos();
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
        });
        cfg.threadCommunication1Fois.start();
    }

    public void demandeAutorisationInternet() {
        Context contexte = cfg.mainActivity.getBaseContext();
        if (ActivityCompat.checkSelfPermission(contexte, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(contexte, Manifest.permission.ACCESS_NETWORK_STATE)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) contexte,
                    new String[]{Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.INTERNET}, 2);
            if (ActivityCompat.checkSelfPermission(contexte, Manifest.permission.INTERNET)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(contexte, Manifest.permission.ACCESS_NETWORK_STATE)
                            != PackageManager.PERMISSION_GRANTED) {
                if (Config.DEBUG_LEVEL > 1 && DEBUG_CLASSE)
                    Log.v("Communication", "Autorisation internet NON accordée");
                return;
            }
        }
        if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE)
            Log.v("Communication", "Autorisation internet accordée");
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
                    cfg.connectionAuServeurOK = false;
                    majIndicateurConnexion();
                }
            } catch (InterruptedException e) {
                Log.v("Communication", "EXCEPTION lors de l'attente de la fin du thread communication");
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

    /**
     * Fonction qui s'assure périodiquement que la communication avec le serveur est bien
     * active
     **/
    public void diffusionPositionAuServeur() {
        if (cfg.diffuserMaPosition) {
            if (Config.DEBUG_LEVEL > 2 && DEBUG_CLASSE)
                Log.v("Communication", "Diffusion de la position via internet");
            if (cfg.fragment_infos != null) cfg.fragment_infos.majTexteInfos();
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
                } // Sinon c'est que la communication est déjà active et il n'y a rien à faire
            } else {
                Log.v("Communication", "*********ERREUR : le module communication n'est pas" +
                        " actif et il y a demande de diffusion de la position !");
            }
            // Plannifie la prochaine diffusion
            cfg.handlerDiffusionPosition.postDelayed(this::diffusionPositionAuServeur,
                    cfg.intervalleMajSecondes * 1000);
        } else {
            if (Config.DEBUG_LEVEL > 0 && DEBUG_CLASSE) Log.v("Communication",
                    "Appel de 'diffusionPositionAuServeur' alors que 'diffuserMaPosition' est à false");
        }

    }

    /**
     * Met à jour le voyant indicateur de connexion au serveur (présent sur la page
     * paramètres) en fonction de l'état de la variable cfg.communicationEnCours
     */
    public void majIndicateurConnexion() {
        if (cfg.indicateurConnexionServeur != null) {
            if (cfg.connectionAuServeurOK) {
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
            if (cfg.handlerMainThread != null) {   // Si le handler n'est pas null
                // On crée un message pour demander au thread principal de mettre à jour la vue
                // parametres qui contient l'indicateur de connexion
                Message msg = Message.obtain(cfg.handlerMainThread, MainActivity.MSG_MAJ_VIEW);
                msg.obj = cfg.fragment_parametres.getView();
                cfg.handlerMainThread.sendMessage(msg);
            }
        }
    }

    /**
     * Fonction qui envoie une commande au serveur
     * @param commande String contenant le code commande
     * @param argument String argument inséré juste après le code commande
     */
    public String envoiCommande(String commande, String argument, boolean attendReponse) {
        String requete = CARACTERE_COMMUNICATION_COMMANDE + commande + Position.SEPARATEUR_CHAMPS
                + argument;
        Thread threadCommande = new Thread(() -> {
            BufferedReader in = null;
            try (Socket socketClient = creationSocketClient(cfg.adresse_serveur, cfg.port_serveur); PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socketClient.getOutputStream())), true)) {
                if (attendReponse)
                    in = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
                out.println(requete);
                out.flush();
                if (attendReponse) cfg.reponseServeur = in.readLine();
            } catch (Exception e) { //(IOException e) {
                // Rien à faire
            }
        });
        threadCommande.start();
        try { Thread.sleep(Config.TEMPS_ATTENTE_REPONSE_SERVEUR_MS);
        } catch (Exception ignored) {  }
        return cfg.reponseServeur;
    }

    /**
     * Envoi une commande SYNC pour demander au serveur l'écart entre son heure et l'heure locale
     * Si l'écart est plus important que ECART_TEMPS_MAXIMAL_AVEC_SERVEUR_MS, un dialog est affiché
     * indiquant que le programme ne fonctionnera pas correctement.
     * Si l'écart est plus important que ECART_TEMPS_AVERTISSEMENT_AVEC_SERVEUR_MS, un message
     * furtif d'avertissement est affiché mais comme dans le cas où l'écart est plus grand, celui-ci
     * est stocké dans cfg.ecartTempsServeur pour servir de tolérance lors de la validation des
     * positions diffusées par le serveur.
     */
    @SuppressLint("WrongConstant")
    public void synchronisationHeureServeur() {
        String delta = envoiCommande(Communication.COMMANDE_SYNCHRONISER, Instant.now().toString(), true);
        if (delta != null) {
            float fDelta = Float.parseFloat(delta) * 1000;
            long lDelta = Float.valueOf(fDelta).longValue();
            if (lDelta > Config.ECART_TEMPS_MAXIMAL_AVEC_SERVEUR_MS ||
                    lDelta < - Config.ECART_TEMPS_MAXIMAL_AVEC_SERVEUR_MS) {
                // Si l'écart est inacceptable, affiche un message indiquant le problème
                cfg.mainActivity.dialogSimple(cfg.mainActivity.getString(R.string.titre_ecart_temps_trop_grand),
                        cfg.mainActivity.getString(R.string.msg_ecart_temps_trop_grand,lDelta/1000));
                lDelta = 0;   // Remet l'écart à zéro
            } else if (lDelta > Config.ECART_TEMPS_AVERTISSEMENT_AVEC_SERVEUR_MS ||
                    lDelta < - Config.ECART_TEMPS_AVERTISSEMENT_AVEC_SERVEUR_MS) {
                // Si l'écart est important, on le signale
                Snackbar.make(cfg.mainActivity.findViewById(android.R.id.content), cfg.mainActivity.getString(R.string.msg_ecart_temps_important),
                        Config.DUREE_AFFICHAGE_MSG_ECART_TEMPS_IMPORTANT_MS).setAction("Action", null).show();
            }
            cfg.ecartTempsServeur = Duration.ofMillis(lDelta);
        }
        if (Config.DEBUG_LEVEL > 3 && DEBUG_CLASSE) Log.v("mainActivity",
                "Différence de temps avec le serveur : " + delta + " s");
    }

}
//package:org.patarasprod.localisationdegroupe
// Communication | parametres