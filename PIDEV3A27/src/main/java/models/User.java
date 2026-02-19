package models;

import java.time.LocalDate;

/**
 * Modèle User aligné avec la table `users` de la base `projet_psychologie`.
 *
 * Table:
 *  id_users (INT, PK, AI)
 *  nom, prenom, email, image, mot_de_passe, role, date_inscription
 */
public class User {

    private int idUsers;
    private String nom;
    private String prenom;
    private String email;
    private String image;
    private String motDePasse;
    /** patient | psychologue | admin | responsable_centre */
    private String role;
    private LocalDate dateInscription;

    public User() {}

    public User(int idUsers, String nom, String prenom, String email, String image,
                String motDePasse, String role, LocalDate dateInscription) {
        this.idUsers = idUsers;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.image = image;
        this.motDePasse = motDePasse;
        this.role = role;
        this.dateInscription = dateInscription;
    }

    /** Constructor utile pour insertion (id auto) */
    public User(String nom, String prenom, String email, String motDePasse, String role) {
        this(0, nom, prenom, email, "default.png", motDePasse, role, LocalDate.now());
    }

    public int getIdUsers() { return idUsers; }
    public void setIdUsers(int idUsers) { this.idUsers = idUsers; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDate getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDate dateInscription) { this.dateInscription = dateInscription; }

    public String getFullname() {
        String p = prenom == null ? "" : prenom.trim();
        String n = nom == null ? "" : nom.trim();
        return (p + " " + n).trim();
    }

    @Override
    public String toString() {
        return "User{" +
                "idUsers=" + idUsers +
                ", fullname='" + getFullname() + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
