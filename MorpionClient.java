import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class MorpionClient extends JFrame {
    private static final int TAILLE = 3;
    private static final Color COULEUR_FOND = new Color(255, 228, 225); // Couleur fond rose clair
    private static final Color COULEUR_TEXTE = Color.BLACK;
    private static final Font POLICE_JEU = new Font("Comic Sans MS", Font.BOLD, 60); // Police plus stylée
    private static final Font POLICE_CHAT = new Font("Segoe UI", Font.PLAIN, 14);
    
    private JButton[][] boutons;
    private char monSymbole;
    private boolean monTour = false;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private JTextArea zoneChat;
    private JTextField champSaisieChat;
    private JLabel statutJeu;

    public MorpionClient() {
        setTitle("Morpion en Réseau");
        setSize(600, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(COULEUR_FOND);
        setLayout(new BorderLayout(20, 20));

        // Panel principal avec padding
        JPanel panelPrincipal = new JPanel(new BorderLayout(20, 20));
        panelPrincipal.setBackground(COULEUR_FOND);
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Statut du jeu
        statutJeu = new JLabel("En attente de connexion...", SwingConstants.CENTER);
        statutJeu.setFont(new Font("Segoe UI", Font.BOLD, 18));
        statutJeu.setForeground(COULEUR_TEXTE);
        panelPrincipal.add(statutJeu, BorderLayout.NORTH);

        // Création du plateau personnalisé
        JPanel panneauPlateau = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int width = getWidth();
                int height = getHeight();
                
                // Dessiner les lignes du plateau
                g2d.setColor(new Color(255, 105, 180)); // Rose vif pour les lignes
                g2d.setStroke(new BasicStroke(3));
                
                // Lignes verticales
                g2d.drawLine(width/3, 20, width/3, height-20);
                g2d.drawLine(2*width/3, 20, 2*width/3, height-20);
                
                // Lignes horizontales
                g2d.drawLine(20, height/3, width-20, height/3);
                g2d.drawLine(20, 2*height/3, width-20, 2*height/3);
            }
        };
        panneauPlateau.setBackground(COULEUR_FOND);
        panneauPlateau.setLayout(new GridLayout(TAILLE, TAILLE));
        
        boutons = new JButton[TAILLE][TAILLE];
        for (int i = 0; i < TAILLE; i++) {
            for (int j = 0; j < TAILLE; j++) {
                final int ligne = i;
                final int colonne = j;
                
                boutons[i][j] = new JButton("") {
                    @Override
                    public void paintComponent(Graphics g) {
                        Graphics2D g2d = (Graphics2D) g;
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        
                        // Rendre le bouton transparent
                        setContentAreaFilled(false);
                        setBorderPainted(false);
                        
                        // Dessiner le symbole (X ou O)
                        if (!getText().isEmpty()) {
                            g2d.setFont(POLICE_JEU);
                            g2d.setColor(COULEUR_TEXTE);
                            FontMetrics fm = g2d.getFontMetrics();
                            int x = (getWidth() - fm.stringWidth(getText())) / 2;
                            int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                            g2d.drawString(getText(), x, y);
                        }
                    }
                };
                
                boutons[i][j].addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent evt) {
                        if (monTour && boutons[ligne][colonne].getText().isEmpty()) {
                            setCursor(new Cursor(Cursor.HAND_CURSOR));
                        }
                    }
                    public void mouseExited(MouseEvent evt) {
                        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    }
                });
                
                boutons[i][j].addActionListener(e -> {
                    if (monTour && boutons[ligne][colonne].getText().isEmpty()) {
                        out.println(ligne + "," + colonne);
                        boutons[ligne][colonne].setText(String.valueOf(monSymbole));
                        boutons[ligne][colonne].setEnabled(false);
                        monTour = false;
                        desactiverTousBoutons();
                        statutJeu.setText("Tour de l'adversaire");
                    }
                });
                
                boutons[i][j].setEnabled(false);
                panneauPlateau.add(boutons[i][j]);
            }
        }

        // Ajout d'une taille minimale pour le plateau
        panneauPlateau.setPreferredSize(new Dimension(400, 400));

        // Zone de chat stylisée
        zoneChat = new JTextArea();
        zoneChat.setFont(POLICE_CHAT);
        zoneChat.setEditable(false);
        zoneChat.setLineWrap(true);
        zoneChat.setWrapStyleWord(true);
        zoneChat.setBackground(new Color(255, 240, 245)); // Fond de chat rose clair
        zoneChat.setForeground(COULEUR_TEXTE);
        
        JScrollPane defilementChat = new JScrollPane(zoneChat);
        defilementChat.setPreferredSize(new Dimension(500, 150));
        defilementChat.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLACK),
            "Chat",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14),
            COULEUR_TEXTE
        ));
        
        champSaisieChat = new JTextField();
        champSaisieChat.setFont(POLICE_CHAT);
        champSaisieChat.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 105, 180)), // Bordure rose
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        champSaisieChat.addActionListener(e -> {
            String message = champSaisieChat.getText().trim();
            if (!message.isEmpty()) {
                out.println("CHAT:" + message);
                zoneChat.append("Vous : " + message + "\n");
                champSaisieChat.setText("");
            }
        });

        JPanel panneauChat = new JPanel(new BorderLayout(0, 5));
        panneauChat.setBackground(COULEUR_FOND);
        panneauChat.add(defilementChat, BorderLayout.CENTER);
        panneauChat.add(champSaisieChat, BorderLayout.SOUTH);

        panelPrincipal.add(panneauPlateau, BorderLayout.CENTER);
        panelPrincipal.add(panneauChat, BorderLayout.SOUTH);
        
        add(panelPrincipal);
        
        setLocationRelativeTo(null);
        setVisible(true);

        connecterAuServeur();
    }

    // Méthode pour connecter le client au serveur
    private void connecterAuServeur() {
        try {
            String ip ="localhost";
            socket = new Socket(ip, 7777);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Thread pour lire les messages du serveur sans bloquer l'interface utilisateur
            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        traiterMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            zoneChat.append("Erreur de connexion : " + e.getMessage() + "\n");
            statutJeu.setText("Erreur de connexion");
            statutJeu.setForeground(Color.RED);
        }
    }

    private void traiterMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith("JOUEUR:")) {
                monSymbole = message.charAt(7);
                zoneChat.append("Vous jouez avec : " + monSymbole + "\n");
                statutJeu.setText("Vous jouez avec : " + monSymbole);
            } 
            else if (message.equals("VOTRE_TOUR")) {
                monTour = true;
                activerBoutonsLibres();
                statutJeu.setText("C'est votre tour");
                zoneChat.append("C'est votre tour de jouer.\n");
            } 
            else if (message.matches("\\d,\\d:\\w")) {
                String[] parts = message.split(":");
                String[] coords = parts[0].split(",");
                int ligne = Integer.parseInt(coords[0]);
                int colonne = Integer.parseInt(coords[1]);
                char symboleCoup = parts[1].charAt(0);
                
                if (boutons[ligne][colonne].getText().isEmpty()) {
                    boutons[ligne][colonne].setText(String.valueOf(symboleCoup));
                    boutons[ligne][colonne].setEnabled(false);
                }
            } 
            else if (message.startsWith("GAGNANT:")) {
                char gagnant = message.charAt(8);
                String resultat = gagnant == monSymbole ? "Vous avez gagné!" : "Vous avez perdu!";
                statutJeu.setText(resultat);
                statutJeu.setForeground(gagnant == monSymbole ? new Color(0, 128, 0) : Color.RED);
                zoneChat.append("Résultat : " + resultat + "\n");
                desactiverTousBoutons();
            } 
            else if (message.equals("MATCH_NUL")) {
                statutJeu.setText("Match nul!");
                statutJeu.setForeground(COULEUR_TEXTE);
                zoneChat.append("Match nul!\n");
                desactiverTousBoutons();
            }
            else if (message.startsWith("CHAT:")) {
                zoneChat.append(message.substring(5) + "\n");
            }
        });
    }

    private void activerBoutonsLibres() {
        for (int i = 0; i < TAILLE; i++) {
            for (int j = 0; j < TAILLE; j++) {
                boutons[i][j].setEnabled(boutons[i][j].getText().isEmpty());
            }
        }
    }

    private void desactiverTousBoutons() {
        for (int i = 0; i < TAILLE; i++) {
            for (int j = 0; j < TAILLE; j++) {
                boutons[i][j].setEnabled(false);
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new MorpionClient());
    }
}
