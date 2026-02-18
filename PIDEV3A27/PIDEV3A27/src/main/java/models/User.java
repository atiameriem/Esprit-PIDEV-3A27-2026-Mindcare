package models;

import java.time.LocalDate;

public class User {

    private int id;
    private int age;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private LocalDate dateInscription;
    private String motDePasse; // ✅ correction ici
    private Role role;




    public enum Role {
        Admin,
        RespensableC,
        Patient,
        Psychologue
    }

    public User() {
    }

    public User(int id, int age, String nom, String prenom,
                String email, String telephone,
                LocalDate dateInscription,
                String motDePasse, Role role) {

        this.id = id;
        this.age = age;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
        this.dateInscription = dateInscription;
        this.motDePasse = motDePasse; // ✅ correction
        this.role = role;
    }

    public User(int age, String nom, String prenom,
                String email, String telephone,
                LocalDate dateInscription,
                String motDePasse, Role role) {

        this.age = age;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
        this.dateInscription = dateInscription;
        this.motDePasse = motDePasse; // ✅ correction
        this.role = role;
    }

    // ===== GETTERS & SETTERS =====

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
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

    public String getMotDePasse() { // ✅ getter cohérent
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) { // ✅ setter cohérent
        this.motDePasse = motDePasse;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
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
