package models;

public class Users {

    private int    idUsers;
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private String role;      // "PATIENT" | "PSYCHOLOGUE"

    // ── Constructeurs ─────────────────────────────────────────────
    public Users() {}

    public Users(int idUsers, String nom, String prenom, String email, String role) {
        this.idUsers  = idUsers;
        this.nom      = nom;
        this.prenom   = prenom;
        this.email    = email;
        this.role     = role;
    }

    // ── Getters / Setters ─────────────────────────────────────────
    public int    getIdUsers()              { return idUsers;    }
    public void   setIdUsers(int idUsers)   { this.idUsers = idUsers; }

    public String getNom()                  { return nom;        }
    public void   setNom(String nom)        { this.nom = nom;    }

    public String getPrenom()               { return prenom;     }
    public void   setPrenom(String prenom)  { this.prenom = prenom; }

    public String getEmail()                { return email;      }
    public void   setEmail(String email)    { this.email = email; }

    public String getMotDePasse()                      { return motDePasse;  }
    public void   setMotDePasse(String motDePasse)     { this.motDePasse = motDePasse; }

    public String getRole()                 { return role;       }
    public void   setRole(String role)      { this.role = role;  }

    @Override
    public String toString() {
        return prenom + " " + nom + " [" + role + "]";
    }
}