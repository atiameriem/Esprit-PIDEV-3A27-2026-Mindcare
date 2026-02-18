package services;

import models.RendezVous;
import models.RendezVousView;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceRendezVous {
 //la connexion JDBC vers ta base (MySQL).
    private final Connection cnx;
 //constructeur qui stocke la connexion dans l’objet
    public ServiceRendezVous(Connection cnx) {
        this.cnx = cnx;
    }

    // ===== READ du psychologue son interface cest le read onlyy)
    //Retourne une liste de RendezVous __ne prendre que les RDV de ce psy sans nom
    public List<RendezVous> findByPsychologist(int idPsychologist) throws SQLException {
        String sql = """
            SELECT id_rv, id_patient, id_psychologist, statutrv, appointment_date, type_rendez_vous, appointment_timerv
            FROM rendez_vous
            WHERE id_psychologist = ?
            ORDER BY appointment_date DESC, appointment_timerv DESC
        """;
        //order by les plus récents en haut.

//On prépare une liste vide.
        List<RendezVous> list = new ArrayList<>();
        //PreparedStatement exécute la requête.
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idPsychologist);
            //executeQuery() car c’est un SELECT.
            //ResultSet contient les lignes.
            //rs.next() avance ligne par ligne.
            //map(rs) transforme une ligne SQL → objet RendezVous.
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }
//Donc elle récupère les RDV du patient.
    public List<RendezVous> findByPatient(int idPatient) throws SQLException {
        String sql = """
            SELECT id_rv, id_patient, id_psychologist, statutrv, appointment_date, type_rendez_vous, appointment_timerv
            FROM rendez_vous
            WHERE id_patient = ?
            ORDER BY appointment_date DESC, appointment_timerv DESC
        """;

        List<RendezVous> list = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idPatient);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    //SELECT 1 = on ne récupère pas les colonnes, juste une seule ligne pour dire “existe ou non”.
    //Je veux une ligne qui a cet id de rendez-vous ET cet id patient
    public boolean existsRendezVousForPatient(int idRv, int idPatient) throws SQLException {
        String sql = "SELECT 1 FROM rendez_vous WHERE id_rv=? AND id_patient=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            //remplace le 1er ? par idRv et 2eme par idpatient
            pst.setInt(1, idRv);
            pst.setInt(2, idPatient);
            try (ResultSet rs = pst.executeQuery()) {
                //rs contient le résultat
                return rs.next();
            }
            //retourne 1 ou 0 lignes
        }
    }


    // ===== VIEWS (avec noms) =====
//Ici tu ne veux pas juste l’entity, tu veux aussi les noms patient/psy.
    public List<RendezVousView> findViewsByPsychologist(int idPsychologist) throws SQLException {
        // afficher  toutes les colonnes du rendez_vous
        //JOINTURE SQL avec users
        String sql = """
            SELECT rv.*,
                   p.nom  AS patient_nom, p.prenom AS patient_prenom,
                   psy.nom AS psy_nom,  psy.prenom AS psy_prenom
            FROM rendez_vous rv
            JOIN users p   ON p.id_users   = rv.id_patient 
            JOIN users psy ON psy.id_users = rv.id_psychologist
            WHERE rv.id_psychologist=?
            ORDER BY rv.appointment_date DESC, rv.appointment_timerv DESC
        """;
        //table principale rendez vous
        //récupère l’utilisateur patient et psy

        List<RendezVousView> out = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idPsychologist);
            try (ResultSet rs = pst.executeQuery()) {
                //mapView crée un RendezVousView
                while (rs.next()) out.add(mapView(rs));
            }
        }
        return out;
    }

    //Donc elle récupère les RDV du patient.
    public List<RendezVousView> findViewsByPatient(int idPatient) throws SQLException {
        String sql = """
            SELECT rv.*,
                   p.nom  AS patient_nom, p.prenom AS patient_prenom,
                   psy.nom AS psy_nom,  psy.prenom AS psy_prenom
            FROM rendez_vous rv
            JOIN users p   ON p.id_users   = rv.id_patient
            JOIN users psy ON psy.id_users = rv.id_psychologist
            WHERE rv.id_patient=?
            ORDER BY rv.appointment_date DESC, rv.appointment_timerv DESC
        """;

        List<RendezVousView> out = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idPatient);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) out.add(mapView(rs));
            }
        }
        return out;
    }
//C’est une fonction de mapping
//Transformer UNE ligne du résultat SQL (ResultSet) en objet Java RendezVousView.
    private RendezVousView mapView(ResultSet rs) throws SQLException {
        //objet vide.
        RendezVousView v = new RendezVousView();

//mapping des id ,id_rv en sql et v.setid en java
        v.setIdRv(rs.getInt("id_rv"));
        v.setIdPatient(rs.getInt("id_patient"));
        v.setIdPsychologist(rs.getInt("id_psychologist"));

        String statut = rs.getString("statutrv");
        if (statut != null && !statut.isEmpty()) v.setStatutRv(RendezVous.StatutRV.valueOf(statut));

        String type = rs.getString("type_rendez_vous");
        if (type != null && !type.isEmpty()) v.setTypeRendezVous(RendezVous.TypeRV.valueOf(type));

        v.setAppointmentDate(rs.getDate("appointment_date"));
        v.setAppointmentTimeRv(rs.getTime("appointment_timerv"));

        //récupère prénom/nom du patient et psy
        String patientFull = safeFullName(rs.getString("patient_prenom"), rs.getString("patient_nom"));
        String psyFull = safeFullName(rs.getString("psy_prenom"), rs.getString("psy_nom"));
        v.setPatientFullName(patientFull);
        v.setPsychologistFullName(psyFull);

        return v;
    }
//safeFullName sert à construire un nom complet sécurisé même si les données sont incomplètes.
    private String safeFullName(String prenom, String nom) {
        //Si prenom == null
        //➜ alors p = "" (chaîne vide)
        //Sinon
        //➜ p = prenom.trim()
        String p = prenom == null ? "" : prenom.trim();
        String n = nom == null ? "" : nom.trim();
        return (p + " " + n).trim();
    }
//vérifie si l’utilisateur existe ET a le rôle psychologue.
//utilisé dans le popup pour empêcher choisir un “psy” invalide.
public boolean isPsychologistUser(int idPsychologist) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE id_users=? AND role='psychologue'";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idPsychologist);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }


    // ===== CRUD (Patient) =====

    public int addAndReturnId(RendezVous rv) throws SQLException {
        String sql = """
            INSERT INTO rendez_vous (id_patient, id_psychologist, statutrv, appointment_date, type_rendez_vous, appointment_timerv)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        //PreparedStatement avec RETURN_GENERATED_KEYS
        // permet de récupérer l’ID auto-incrémenté.
        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            //Remplir paramètres
            pst.setInt(1, rv.getIdPatient());
            pst.setInt(2, rv.getIdPsychologist());
            pst.setString(3, rv.getStatutRv().name());
            pst.setDate(4, rv.getAppointmentDate());
            //.name() convertit l’enum en texte exactement égal au nom enum.
            pst.setString(5, rv.getTypeRendezVous().name());
            pst.setTime(6, rv.getAppointmentTimeRv());
            pst.executeUpdate();
            //Récupérer l’ID généré
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public void updateForPatient(RendezVous rv, int idPatient) throws SQLException {
        //pdate seulement si id_rv correspond ET id_patient correspond au patient connecté
        String sql = """
            UPDATE rendez_vous
            SET id_psychologist=?, statutrv=?, appointment_date=?, type_rendez_vous=?, appointment_timerv=?
            WHERE id_rv=? AND id_patient=?
        """;

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, rv.getIdPsychologist());
            pst.setString(2, rv.getStatutRv().name());
            pst.setDate(3, rv.getAppointmentDate());
            pst.setString(4, rv.getTypeRendezVous().name());
            pst.setTime(5, rv.getAppointmentTimeRv());
            pst.setInt(6, rv.getIdRv());
            pst.setInt(7, idPatient);
            pst.executeUpdate();
        }
    }

    public void deleteForPatient(int idRv, int idPatient) throws SQLException {
    //Pourquoi supprimer CR avant ?
        //Car CR dépend du RDV (clé étrangère)
        String delCR = "DELETE FROM compte_rendu_seance WHERE id_appointment = ?";
        String delRV = "DELETE FROM rendez_vous WHERE id_rv=? AND id_patient=?";

        try {
            cnx.setAutoCommit(false);

            // 1) supprimer le compte-rendu lié au rendez-vous
            try (PreparedStatement pst1 = cnx.prepareStatement(delCR)) {
                pst1.setInt(1, idRv);
                pst1.executeUpdate();
            }

            // 2) supprimer le rendez-vous (sécurisé: seulement si appartient au patient)
            try (PreparedStatement pst2 = cnx.prepareStatement(delRV)) {
                pst2.setInt(1, idRv);
                pst2.setInt(2, idPatient);
                pst2.executeUpdate();
            }
//valide les deux suppressions en même temps.
            cnx.commit();

        } catch (SQLException e) {
            //si une suppression échoue, on annule tout.
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }
























    private RendezVous map(ResultSet rs) throws SQLException {
        return new RendezVous(
                rs.getInt("id_rv"),
                rs.getInt("id_patient"),
                rs.getInt("id_psychologist"),
                RendezVous.StatutRV.valueOf(rs.getString("statutrv")),
                rs.getDate("appointment_date"),
                RendezVous.TypeRV.valueOf(rs.getString("type_rendez_vous")),
                rs.getTime("appointment_timerv")
        );
    }
}
