package models;

import java.time.LocalDate;
import java.time.Period;

public class User {

    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private LocalDate dateInscription;
    private String motDePasse;
    private Role role;
    private String badgeImage; // badge_image in DB
    private LocalDate dateNaissance;

    public enum Role {
        Admin,
        ResponsableC,
        Patient,
        Psychologue
    }

    public User() {
    }

    public User(int id, String nom, String prenom,
            String email, String telephone,
            LocalDate dateInscription,
            String motDePasse, Role role, String badgeImage, LocalDate dateNaissance) {

        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
        this.dateInscription = dateInscription;
        this.motDePasse = motDePasse;
        this.role = role;
        this.badgeImage = badgeImage;
        this.dateNaissance = dateNaissance;
    }

    // ================= GETTERS & SETTERS =================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAge() {
        if (dateNaissance != null) {
            return Period.between(dateNaissance, LocalDate.now()).getYears();
        }
        return 0;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public LocalDate getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(LocalDate dateInscription) {
        this.dateInscription = dateInscription;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getBadge() {
        return badgeImage;
    }

    public void setBadge(String badgeImage) {
        this.badgeImage = badgeImage;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    /**
     * Méthode de compatibilité : retourne "Prénom Nom"
     */
    public String getFullname() {
        String p = prenom == null ? "" : prenom.trim();
        String n = nom == null ? "" : nom.trim();
        return (p + " " + n).trim();
    }

    /**
     * Méthode de compatibilité : alias de getId() pour le module Forum.
     */
    public int getIdUsers() {
        return id;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", dateInscription=" + dateInscription +
                '}';
    }
}
