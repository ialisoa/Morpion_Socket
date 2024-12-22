import java.io.*;
import java.net.*;

public class MorpionServeur {
    private static final int PORT = 7777;
    private static final int TAILLE = 3;
    private char[][] plateau;
    private char tourActuel;
    private ServerSocket serverSocket;
    private Socket joueur1Socket;
    private Socket joueur2Socket;
    private PrintWriter joueur1Out;
    private PrintWriter joueur2Out;
    private BufferedReader joueur1In;
    private BufferedReader joueur2In;

    public MorpionServeur() {
        plateau = new char[TAILLE][TAILLE];
        initialisationPlateau();
        tourActuel = 'X';
    }

    private void initialisationPlateau() {
        for (int i = 0; i < TAILLE; i++) {
            for (int j = 0; j < TAILLE; j++) {
                plateau[i][j] = ' ';
            }
        }
    }

    public void demarrer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Serveur démarré sur le port " + PORT);
    
            // Connexion joueur 1
            joueur1Socket = serverSocket.accept();
            joueur1Out = new PrintWriter(joueur1Socket.getOutputStream(), true);
            joueur1In = new BufferedReader(new InputStreamReader(joueur1Socket.getInputStream()));
            joueur1Out.println("JOUEUR:X");
            System.out.println("Joueur 1 (X) connecté depuis " + joueur1Socket.getInetAddress());
    
            // Connexion joueur 2
            joueur2Socket = serverSocket.accept();
            joueur2Out = new PrintWriter(joueur2Socket.getOutputStream(), true);
            joueur2In = new BufferedReader(new InputStreamReader(joueur2Socket.getInputStream()));
            joueur2Out.println("JOUEUR:O");
            System.out.println("Joueur 2 (O) connecté depuis " + joueur2Socket.getInetAddress());
    
            // Commencer par le tour du joueur X
            joueur1Out.println("VOTRE_TOUR");
    
            // Lancer les threads pour chaque joueur
            Thread threadJoueur1 = new Thread(new JoueurHandler(joueur1Socket, joueur1In, joueur1Out, 'X'));
            Thread threadJoueur2 = new Thread(new JoueurHandler(joueur2Socket, joueur2In, joueur2Out, 'O'));
    
            threadJoueur1.start();
            threadJoueur2.start();
    
            // Attendre que les deux threads se terminent
            threadJoueur1.join();
            threadJoueur2.join();
    
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private boolean verifierGagnant() {
        // Vérification des lignes
        for (int i = 0; i < TAILLE; i++) {
            if (plateau[i][0] != ' ' && 
                plateau[i][0] == plateau[i][1] && 
                plateau[i][1] == plateau[i][2]) {
                return true;
            }
        }

        // Vérification des colonnes
        for (int j = 0; j < TAILLE; j++) {
            if (plateau[0][j] != ' ' && 
                plateau[0][j] == plateau[1][j] && 
                plateau[1][j] == plateau[2][j]) {
                return true;
            }
        }

        // Vérification des diagonales
        if (plateau[0][0] != ' ' && 
            plateau[0][0] == plateau[1][1] && 
            plateau[1][1] == plateau[2][2]) {
            return true;
        }

        if (plateau[0][2] != ' ' && 
            plateau[0][2] == plateau[1][1] && 
            plateau[1][1] == plateau[2][0]) {
            return true;
        }

        return false;
    }

    private boolean verifierMatchNul() {
        for (int i = 0; i < TAILLE; i++) {
            for (int j = 0; j < TAILLE; j++) {
                if (plateau[i][j] == ' ') {
                    return false;
                }
            }
        }
        return true;
    }

    private void terminerJeu(char gagnant) {
        String resultat = (gagnant == ' ') ? "MATCH_NUL" : "GAGNANT:" + gagnant;
        joueur1Out.println(resultat);
        joueur2Out.println(resultat);
    }

    public static void main(String[] args) {
        new MorpionServeur().demarrer();
    }

    // Classe interne pour gérer un joueur
    private class JoueurHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private char symbole;

        public JoueurHandler(Socket socket, BufferedReader in, PrintWriter out, char symbole) {
            this.socket = socket;
            this.in = in;
            this.out = out;
            this.symbole = symbole;
        }

        @Override
        public void run() {
            try {
                // Gérer la partie pour ce joueur
                while (true) {
                    // Attendre un coup
                    String ligneCoup = in.readLine();
                    if (ligneCoup != null && ligneCoup.matches("\\d,\\d")) {
                        String[] coordonnees = ligneCoup.split(",");
                        int ligne = Integer.parseInt(coordonnees[0]);
                        int colonne = Integer.parseInt(coordonnees[1]);
    
                        // Vérifier si le coup est valide
                        if (plateau[ligne][colonne] == ' ') {
                            // Placer le coup
                            plateau[ligne][colonne] = symbole;
    
                            // Informer tous les joueurs du coup
                            joueur1Out.println(ligneCoup + ":" + symbole);
                            joueur2Out.println(ligneCoup + ":" + symbole);
    
                            // Vérifier si le jeu est terminé
                            if (verifierGagnant()) {
                                terminerJeu(symbole);
                                break;
                            } else if (verifierMatchNul()) {
                                terminerJeu(' ');
                                break;
                            }
    
                            // Passer au joueur suivant
                            tourActuel = (tourActuel == 'X') ? 'O' : 'X';
    
                            // Signaler le tour du prochain joueur
                            if (tourActuel == 'X') {
                                joueur1Out.println("VOTRE_TOUR");
                            } else {
                                joueur2Out.println("VOTRE_TOUR");
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
