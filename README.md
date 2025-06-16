# LocalisationDeGroupe
Application android pour se localiser à l'intérieur d'un groupe en gardant la maitrise de ses données.

L'application nécessite un serveur dont on donne l'adresse et le port dans l'onglet "paramètres".
Un exemple de serveur est donné en python (fichier serveur.py). Il peut fonctionner par exemple sur un nas synology.

Chaque utilisateur du groupe doit avoir installé l'application et l'avoir paramétré correctement
(notamment la partie serveur qui doit être la même pour tout le monde).

Lorsqu'elle tourne en premier plan et que le commutateur "Diffuser ma position" est activé,
l'application envoi régulièrement au serveur la position mesurée par le GPS ou le réseau et
récupère du serveur la position des autres utilisateurs. Celles-ci s'affichent dans l'onglet "Info"
et dans l'onglet "Carte" (cartes fournies par OpenStreetMap et nécessitant un accès à internet pour
être téléchargées la première fois).

Le commutateur "Diffuser en tâche de fond" permet d'envoyer régulièrement la position au serveur
mais sans récupérer les positions des autres utilisateurs. La périodicité est différente du cas précédent
(réglée sur 5 minutes (300 secondes) par défaut) et est destinée juste à maintenir l'information
pour le serveur lorsque l'application n'est pas utilisée.
