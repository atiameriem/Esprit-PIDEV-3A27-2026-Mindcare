package models;

import java.time.LocalDate;

public class AppUser {
    private int idUsers;
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private String role;
    private LocalDate dateInscription;

    public AppUser() {}

    public AppUser(int idUsers, String nom, String prenom, String email, String motDePasse, String role, LocalDate dateInscription) {
        this.idUsers = idUsers;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = role;
        this.dateInscription = dateInscription;
    }

    public int getIdUsers() { return idUsers; }
    public void setIdUsers(int idUsers) { this.idUsers = idUsers; }

    // Compat
    public int getId() { return idUsers; }
    public void setId(int id) { this.idUsers = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDate getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDate dateInscription) { this.dateInscription = dateInscription; }

    public String getFullName() {
        String n = nom == null ? "" : nom;
        String p = prenom == null ? "" : prenom;
        return (n + " " + p).trim();
    }
}
