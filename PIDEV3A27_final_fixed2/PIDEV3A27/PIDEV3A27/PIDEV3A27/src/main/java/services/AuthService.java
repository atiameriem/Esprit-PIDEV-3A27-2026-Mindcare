package services;

import utils.MyDatabase;
import utils.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Auth minimal pour la session (2 users ou plus) : email + mot_de_passe.
 */
//Vérifier si l’email et le mot de passe existent en base
//Créer la session utilisateur
public class AuthService {

    //cnx = connexion à la base de données.
    //final = elle ne changera jamais après initialisation.
    private final Connection cnx;
    //MyDatabase.getInstance()
// → Singleton (une seule connexion pour toute l’application)
    //retourne la connexion MySQL et on la stock dans cnx
    public AuthService() {
        this.cnx = MyDatabase.getInstance().getConnection();
    }

    public boolean login(String email, String password) throws SQLException {
        //Vérifier si l’utilisateur existe
        //On cherche un utilisateur qui a :
        //cet email et ce mot de passe
        String sql = "SELECT id_users, nom, prenom, role FROM users WHERE email = ? AND mot_de_passe = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            //Remplir les paramètres
            pst.setString(1, email);
            pst.setString(2, password);
//Exécuter la requête et Vérifier si utilisateur existe
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return false;

                //: Récupérer les infos utilisateur
                int id = rs.getInt("id_users");
                String fullName = rs.getString("prenom") + " " + rs.getString("nom");
                String roleDb = rs.getString("role"); // patient | psychologue
                //Est-ce que roleDb est égal à "psychologue" ?
                Session.Role role = "psychologue".equalsIgnoreCase(roleDb)
                        ? Session.Role.PSYCHOLOGUE
                        : Session.Role.PATIENT;

                //Enregistrer l’utilisateur comme connecté.
                Session.login(id, role, fullName);
                return true;
            }
        }
    }
}
