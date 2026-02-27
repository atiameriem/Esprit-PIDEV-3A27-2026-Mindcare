package models;

import java.sql.Timestamp;

public class User {

    private int id_users;
    private String nom;
    private String prenom;
    private String email;
    private String mot_de_passe;
    private String role;
    private Timestamp date_inscription;

    public User() {
    }

    public User(int id_users, String nom, String prenom, String email, String mot_de_passe, String role,
            Timestamp date_inscription) {
        this.id_users = id_users;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.mot_de_passe = mot_de_passe;
        this.role = role;
        this.date_inscription = date_inscription;
    }

    public User(String nom, String prenom, String email, String mot_de_passe, String role) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.mot_de_passe = mot_de_passe;
        this.role = role;
    }

    public int getId_users() {
        return id_users;
    }

    public int getId() {
        return id_users;
    }

    public void setId_users(int id_users) {
        this.id_users = id_users;
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

    public String getMot_de_passe() {
        return mot_de_passe;
    }

    public void setMot_de_passe(String mot_de_passe) {
        this.mot_de_passe = mot_de_passe;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Timestamp getDate_inscription() {
        return date_inscription;
    }

    public void setDate_inscription(Timestamp date_inscription) {
        this.date_inscription = date_inscription;
    }

    @Override
    public String toString() {
        return "User{" +
                "id_users=" + id_users +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
