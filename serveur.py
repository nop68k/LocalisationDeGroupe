# -*- coding: utf-8 -*-
import socket
import threading
import json
import os.path
import logging
import time

################################################################################
# Programme serveur pour l'application "Localisation de groupe" v1.5
#
# Ce script python est fait pour tourner en permanence sur un serveur avec
# lequel il n'y a pas d'interaction. Pour l'arrêter il faut créer un fichier
# (vide éventuellement) dont le nom exact est dans la variable NOM_FICHIER_ARRET
# dans le répertoire courant.
# Le programme maintient un dictionnaire de toutes les positions connues (dans
# la variable globale "positions"). Ces positions sont automatiquement
# enregistrées dans un fichier json (NOM_FICHIER_SAUVEGARDE) à l'arrêt du
# programme et récupérées au démarrage du programme.
# Le programme crée et gère plusieurs fils d'exécution, un par client
# Ces fils récupèrent la position du client et mettent à jour le dictionnaire
# des positions en conséquences et lui diffusent l'ensemble des positions
# Le programme utilise également un fichier journal (NOM_FICHIER_JOURNAL) où
# il consigne les erreurs mais aussi régulièrement les positions qu'il connait
# ainsi que les évènements liés au fonctionnement du programme.
#
################################################################################

PORT = 7777        # Numéro du port utilisé pour la communication
TIMEOUT = 120.0    # Temps d'attente maxi de la carte réseau pr ouvrir socket
TAILLE_BUFFER = 4096
NOM_FICHIER_SAUVEGARDE = "sauvegarde_positions.json"
NOM_FICHIER_ARRET = "arret_programme"
NOM_FICHIER_JOURNAL = 'journal.log'
NOM_ANONYME = "ANONYME"  #  Nom considéré comme anonyme et donc non enregistré
REPONSE_SERVEUR_NOM_ANONYME = "Le nom utilisé est invalide"
LIMITE_NB_MSG_VIDES = 30  # A partir de cette quantité de messages vides reçus,
                          # on ferme le thread

# Caractere par lequel commence l'envoi du client pour savoir le type de
# comportement adopter
CARACTERE_COMMUNICATION_BIDIRECTIONNELLE = '%'
CARACTERE_COMMUNICATION_UNIDIRECTIONNELLE = '>'
CARACTERE_COMMUNICATION_COMMANDE = '#'    # Non encore implémenté

SEPARATEUR_CHAMPS = "||"
SEPARATEUR_LATITUDE_LONGITUDE = ';'
SEPARATEUR_ELEMENTS_REPONSE_SERVEUR = "$*$"
NIVEAU_DE_LOG = logging.ERROR    # Niveau de détail du log
PERIODICITE_AFFICHAGE = 60*60   # Nombre de secondes avant affichage des infos

# Dictionnaire des fils d'exécutions
# Les clés de dictionnaire sont le numéro du thread (commence à 1 et est
# incrémenté par le programme au fur et à mesure). Les valeurs sont des
# dictionnaires du type {'obj': objet thread, 'heure_demarrage': date/heure de
# la création du thread}
threads = {}   # Dictionnaire des threads

# Dictionnaire des positions. Les clés de dictionnaire sont les noms des
# personnes/lieux et les valeurs sont les positions sous la forme d'une chaîne
# contenant les coordonnées GPS suivies du séparateur de champ puis de la
# dernière heure de mise à jour (au format UTC)
positions = {"Maison":"-32.0509067;115.7716450" + SEPARATEUR_CHAMPS + "2025-06-05T09:13:55.866Z"}

# Mise en place du journal de log
logging.basicConfig(filename=NOM_FICHIER_JOURNAL, level=NIVEAU_DE_LOG,
                    format='%(asctime)s %(message)s')
logger = logging.getLogger(__name__)


def premier_nom_msg_client(msg:str) -> str:
    """Renvoie le premier nom dans le message du client fournit en argument"""
    infos = msg.split(SEPARATEUR_CHAMPS)
    if len(infos) > 0:
        return infos[0]
    else:
        return ""

def est_position_valide(position:str) -> bool:
    """Renvoie True si la position GPS a une écriture valide, False sinon"""
    coordonnees = position.split(SEPARATEUR_LATITUDE_LONGITUDE)
    if len(coordonnees) != 2: return False
    for composante in coordonnees:
        try:
            valeur = float(composante)
        except:
            return False
        if valeur < -90 or valeur > 90: return False
        return True

def traitement_info_client(msg:str) -> None:
    """Traite le message reçu en provenance du client. La validité du message
    est contrôlée et s'il est valide, la liste des positions (variable globale
    positions) est mise à jour."""
    infos = msg.split(SEPARATEUR_CHAMPS)
    if len(infos) == 0:
        logger.debug("Erreur : message reçu vide")
        return
    elif len(infos) == 1:
        logger.debug(f"Erreur : message ne contenant qu'un élément : {infos[0]}")
        return
    elif len(infos) == 3:
        nom, position, date = infos[0].strip(), infos[1], infos[2]
        position = position.replace('N','')
        position = position.replace('S','')
        position = position.replace('E','')
        position = position.replace('W','')
        position = position.strip()
        if est_position_valide(position):
            positions[nom] = position + SEPARATEUR_CHAMPS + date.strip()
        else:
            logger.warning(f"Position invalide pour {nom} : {position}")
            return
    else:
        logger.debug(f"Nombre d'éléments invalides ({len(infos)} : {infos}")
        pass


def elabore_reponse_serveur() -> str:
    """Renvoi une chaîne formatée contenant les positions de tous les
    utilisateurs dans un format utilisable par l'appli."""
    reponse = ""
    for position in positions.items():
        reponse += position[0]
        reponse += SEPARATEUR_CHAMPS
        reponse += position[1]
        reponse += SEPARATEUR_ELEMENTS_REPONSE_SERVEUR
    return reponse[:-len(SEPARATEUR_ELEMENTS_REPONSE_SERVEUR)] + '\n'

def gestion_1_client(client, positions, numero_thread:int):
    logger.debug(f"Thread n°{numero_thread} démarré avec client = {client}\n et positions =  {positions}")
    nb_msg_vides = 0    # Pour suivre le nombre de messages vides reçus
    fin_thread = False
    while not fin_thread:
        try:
            msgClient = client.recv(TAILLE_BUFFER)  # Récupère le message envoyé par le client
            print(f"{time.time()}-Message brut (longueur {len(msgClient)}) : {msgClient}")
            if len(msgClient) == 0:   # Si le message reçu est vide
                nb_msg_vides += 1
                if nb_msg_vides > LIMITE_NB_MSG_VIDES:   # Si on dépasse la limite
                    fin_tread = True                     # il vaut mieux fermer le thread
                    logger.warning(f"Thread {numero_thread} abandonné suite à " +\
                                   f"{nb_msg_vides} messages vides reçus")
                continue              # on passe directement au suivant
            msgClient = msgClient.decode('utf-8')  # Transforme le flux d'octet en chaîne pour l'afficher
            logger.debug(f"Message reçu du client : '{msgClient}'")
            if msgClient[0] == CARACTERE_COMMUNICATION_BIDIRECTIONNELLE or \
               msgClient[0] == CARACTERE_COMMUNICATION_UNIDIRECTIONNELLE:
                if premier_nom_msg_client(msgClient[1:]) == "" or  \
                   premier_nom_msg_client(msgClient[1:]).upper() == NOM_ANONYME:
                    logger.info(f"Connexion avec nom anonyme '{premier_nom_msg_client(msgClient[1:])}' - Dialogue terminé")
                    client.send(bytes(REPONSE_SERVEUR_NOM_ANONYME, 'utf-8'))
                    continue     # Reprend à l'écoute du client
                traitement_info_client(msgClient[1:])
                if msgClient != "":
                    logger.debug(f"Le thread {numero_thread} a reçu : {msgClient[1:]}")
                    if msgClient[-3:] == "FIN":        # Si on reçoit "FIN" comme derniers caractères,
                        fin_thread = True              # on met fin au thread
                if msgClient[0] == CARACTERE_COMMUNICATION_BIDIRECTIONNELLE:
                    # Répond au client en lui envoyant le dictionnaire des positions
                    reponse_serveur = elabore_reponse_serveur()
                    logger.debug(f"Réponse du serveur (Thread n°{numero_thread}) : \n{reponse_serveur}")
                    client.send(bytes(reponse_serveur, 'utf-8'))
                else:
                    fin_thread = True
            else:
                fin_thread = True
                # En principe c'est une commande
                if msgClient[0] == CARACTERE_COMMUNICATION_COMMANDE:
                    print("Les commandes se sont pas gérées par cette version du serveur !!!")
                else:
                    logger.critical(f"Caractere de début non géré dans le message : {msgClient}")
        except Exception as e:
            logging.error(f"Erreur dans le thread n°{numero_thread} : {e}")
            fin_thread = True
    logger.debug(f"Fermeture du thread n°{numero_thread}")
    client.close()
    del(threads[numero_thread])   # Supprime le thread de la liste des threads
    # La sortie de cette fonction va terminer le thread


def met_a_jour_liste_threads() -> None:
    """Met à jour la liste des threads en supprimant les threads terminés"""
    for no_thread in threads.keys():
        try:
            if not threads[no_thread]['obj'].is_alive():
                logger.debug(f"Suppression du thread terminé - thread n°" + \
                             f"{no_thread} démarré le {threads[no_thread]['heure_demarrage']}")
                del(threads[no_thread])
        except Exception as e:
            logger.warning(f"Problème à l'examen du thread {no_thread} ({threads[no_thread]}) : {e}")


def lecture_positions_sauvegardees() -> None:
    """Fonction qui lit le fichier json des positions sauvegardées et met à
       jour la variable globale positions s'il y a des données exploitables."""
    # Si le fichier de sauvegarde des positions existe, on le charge
    if os.path.isfile(NOM_FICHIER_SAUVEGARDE):
        logger.info("Lecture des positions sauvegardées")
        try:
            with open(NOM_FICHIER_SAUVEGARDE,"rt") as fichier:
                positions = json.load(fichier)
        except Exception as e:
            logger.error(f"Erreur lors de l'ouverture du fichier des positions sauvegardées : {e}")

def sauvegarde_positions() -> None:
    """Procédure pour sauvegarder les positions connues dans le fichier json
    de sauvegarde des positions."""
    positions_a_ecrire = positions.copy()
    with open(NOM_FICHIER_SAUVEGARDE,"wt") as fichier:
        json.dump(positions_a_ecrire, fichier, indent=2)


lecture_positions_sauvegardees()
socket.setdefaulttimeout(TIMEOUT)  # Fixe un temp maximum pour obtenir une réponse de la carte réseau
socketServeur = socket.socket(socket.AF_INET, socket.SOCK_STREAM)   # Création du socket
socketServeur.bind(('', PORT))    # Fixe le port d'écoute du socket serveur
logger.setLevel(logging.INFO)
logger.info(f"Le serveur démarre et écoute sur le port {PORT}")
logger.setLevel(NIVEAU_DE_LOG)
print(time.strftime("%Y-%m-%d %H:%M:%S") + f" Le serveur démarre et écoute sur le port {PORT}")

finProgramme = False
no_thread = 1
heure_dernier_pointage = time.time()
while not finProgramme:
    try:
        e = socketServeur.listen()    # Met en pause le programme jusqu'à recevoir une demande de connexion
        client, adresse = socketServeur.accept()  # Accepte la connexion et récupère les informations sur le client
        logger.info("{} connecté".format( adresse ))
        thread = threading.Thread(target=gestion_1_client,args=[client, positions, no_thread])
        threads[no_thread] = {'obj':thread, 'heure_demarrage':time.strftime("%Y-%m-%d %H:%M:%S")}
        thread.start()
        no_thread += 1
    except Exception as e:
        logger.debug(f"Exception levée : {e}")
    logger.debug("Sauvegarde des positions")
    try:
        positions_a_ecrire = positions.copy()
        with open(NOM_FICHIER_SAUVEGARDE,"wt") as fichier:
            json.dump(positions_a_ecrire, fichier, indent=2)
    except Exception as e:
        logger.critical(f"Exception au moment d'écrire les positions dans le fichier de sauvegarde : {e}")
        finProgramme = True

    met_a_jour_liste_threads()

    # On teste la présence du fichier NOM_FICHIER_ARRET dans le répertoire
    # courant. S'il est présent le programme s'arrête

    if (time.time() - heure_dernier_pointage) >= PERIODICITE_AFFICHAGE:
        logger.setLevel(logging.INFO)
        if len(threads) > 5:
            logger.info(f"{len(threads)} threads actifs - numéro du dernier thread : {no_thread}")
        else:
            logger.info(f"{len(threads)} threads actifs : {[t for t in threads.keys()]}")
        logger.info(f"{len(positions)} positions suivies : {[n for n in positions.keys()]}")
        heure_dernier_pointage = time.time()
        logger.setLevel(NIVEAU_DE_LOG)

    if os.path.isfile(NOM_FICHIER_ARRET):
        logger.setLevel(logging.INFO)
        logger.info("Détection du fichier provoquant la fermeture du programme")
        print(time.strftime("%Y-%m-%d %H:%M:%S") + " Détection du fichier provoquant la fermeture du programme")
        finProgramme = True

logger.setLevel(logging.INFO)
logger.info("Fermeture du serveur")
socketServeur.close()
logger.info("----------FIN DU PROGRAMME----------")
print(time.strftime("%Y-%m-%d %H:%M:%S") + " ----------FIN DU PROGRAMME----------")