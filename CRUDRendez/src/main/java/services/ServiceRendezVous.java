// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
package services;

import entities.RendezVous;
import entities.RendezVous.StatutRV;
import entities.RendezVous.TypeRV;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import utils.MyDatabase;

public class ServiceRendezVous implements IService<RendezVous> {
    private final Connection cnx = MyDatabase.getInstance().getCnx();

    public ServiceRendezVous() {
    }

    public void add(RendezVous rv) throws SQLException {
        String sql = "INSERT INTO rendez_vous (id_patient, id_psychologist, statutrv, appointment_date, type_rendez_vous, appointment_timerv) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement pst = this.cnx.prepareStatement(sql);
        pst.setInt(1, rv.getIdPatient());
        pst.setInt(2, rv.getIdPsychologist());
        pst.setString(3, rv.getStatutRv().name());
        pst.setDate(4, rv.getAppointmentDate());
        pst.setString(5, rv.getTypeRendezVous().name());
        pst.setTime(6, rv.getAppointmentTimeRv());
        pst.executeUpdate();
        System.out.println("✅ Rendez-vous ajouté");
    }

    public void update(RendezVous rv) throws SQLException {
        String sql = "UPDATE rendez_vous SET id_patient=?, id_psychologist=?, statutrv=?, appointment_date=?, type_rendez_vous=?, appointment_timerv=? WHERE id_rv=?";
        PreparedStatement pst = this.cnx.prepareStatement(sql);
        pst.setInt(1, rv.getIdPatient());
        pst.setInt(2, rv.getIdPsychologist());
        pst.setString(3, rv.getStatutRv().name());
        pst.setDate(4, rv.getAppointmentDate());
        pst.setString(5, rv.getTypeRendezVous().name());
        pst.setTime(6, rv.getAppointmentTimeRv());
        pst.setInt(7, rv.getIdRv());
        int rows = pst.executeUpdate();
        if (rows > 0) {
            System.out.println("✅ Rendez-vous modifié");
        } else {
            System.out.println("⚠️ Aucun rendez-vous modifié (id_rv introuvable)");
        }

    }

    public void delete(RendezVous rv) throws SQLException {
        String sql = "DELETE FROM rendez_vous WHERE id_rv=?";
        PreparedStatement pst = this.cnx.prepareStatement(sql);
        pst.setInt(1, rv.getIdRv());
        int rows = pst.executeUpdate();
        if (rows > 0) {
            System.out.println("✅ Rendez-vous supprimé (id_rv=" + rv.getIdRv() + ")");
        } else {
            System.out.println("⚠️ Aucun rendez-vous supprimé (id_rv introuvable : " + rv.getIdRv() + ")");
        }

    }

    public List<RendezVous> getAll() throws SQLException {
        List<RendezVous> list = new ArrayList();
        String sql = "SELECT * FROM rendez_vous";
        Statement st = this.cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while(rs.next()) {
            RendezVous rv = new RendezVous(rs.getInt("id_rv"), rs.getInt("id_patient"), rs.getInt("id_psychologist"), StatutRV.valueOf(rs.getString("statutrv")), rs.getDate("appointment_date"), TypeRV.valueOf(rs.getString("type_rendez_vous")), rs.getTime("appointment_timerv"));
            list.add(rv);
        }

        return list;
    }

    public void updateFields(int idRv, RendezVous.StatutRV statut, RendezVous.TypeRV type, Date date, Time time) throws SQLException {
        String sql = "UPDATE rendez_vous SET statutrv=?, type_rendez_vous=?, appointment_date=?, appointment_timerv=? WHERE id_rv=?";
        PreparedStatement pst = this.cnx.prepareStatement(sql);
        pst.setString(1, statut.name());
        pst.setString(2, type.name());
        pst.setDate(3, date);
        pst.setTime(4, time);
        pst.setInt(5, idRv);
        int rows = pst.executeUpdate();
        if (rows > 0) {
            System.out.println("✅ Rendez-vous modifié");
        } else {
            System.out.println("⚠️ Rendez-vous introuvable");
        }

    }

    public boolean userExiste(int idUser, String role) throws SQLException {
        String sql = "SELECT id_users FROM users WHERE id_users = ? AND role = ?";
        PreparedStatement pst = this.cnx.prepareStatement(sql);
        pst.setInt(1, idUser);
        pst.setString(2, role);
        ResultSet rs = pst.executeQuery();
        return rs.next();
    }

    public boolean rendezVousExiste(int idRv) throws SQLException {
        String sql = "SELECT id_rv FROM rendez_vous WHERE id_rv=?";
        PreparedStatement pst = this.cnx.prepareStatement(sql);
        pst.setInt(1, idRv);
        ResultSet rs = pst.executeQuery();
        return rs.next();
    }
}
