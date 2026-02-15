// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
package services;

import entities.CompteRenduSeance;
import entities.CompteRenduSeance.ProgresCR;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import utils.MyDatabase;

public class ServiceCompteRenduSeance implements IService<CompteRenduSeance> {
    private final Connection cnx = MyDatabase.getInstance().getCnx();

    public ServiceCompteRenduSeance() {
    }

    public void add(CompteRenduSeance cr) throws SQLException {
        String sql = "INSERT INTO compte_rendu_seance (id_appointment, date_creationcr, progrescr, resumeseancecr, prochainesActioncr) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement pst = this.cnx.prepareStatement(sql);
        pst.setInt(1, cr.getIdAppointment());
        pst.setTimestamp(2, cr.getDateCreationCr());
        pst.setString(3, cr.getProgresCr().name());
        pst.setString(4, cr.getResumeSeanceCr());
        pst.setString(5, cr.getProchainesActionCr());
        pst.executeUpdate();
        System.out.println("✅ Compte rendu ajouté");
    }

    public void update(CompteRenduSeance cr) throws SQLException {
        String sql = "UPDATE compte_rendu_seance SET id_appointment=?, date_creationcr=?, progrescr=?, resumeseancecr=?, prochainesActioncr=? WHERE id_compterendu=?";
        PreparedStatement pst = this.cnx.prepareStatement(sql);
        pst.setInt(1, cr.getIdAppointment());
        pst.setTimestamp(2, cr.getDateCreationCr());
        pst.setString(3, cr.getProgresCr().name());
        pst.setString(4, cr.getResumeSeanceCr());
        pst.setString(5, cr.getProchainesActionCr());
        pst.setInt(6, cr.getIdCompteRendu());
        pst.executeUpdate();
        System.out.println("✅ Compte rendu modifié");
    }

    public void delete(CompteRenduSeance cr) throws SQLException {
        String sql = "DELETE FROM compte_rendu_seance WHERE id_compterendu=?";
        PreparedStatement pst = this.cnx.prepareStatement(sql);
        pst.setInt(1, cr.getIdCompteRendu());
        pst.executeUpdate();
        System.out.println("✅ Compte rendu supprimé");
    }

    public List<CompteRenduSeance> getAll() throws SQLException {
        List<CompteRenduSeance> list = new ArrayList();
        String sql = "SELECT * FROM compte_rendu_seance";
        Statement st = this.cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while(rs.next()) {
            CompteRenduSeance cr = new CompteRenduSeance(rs.getInt("id_compterendu"), rs.getInt("id_appointment"), rs.getTimestamp("date_creationcr"), ProgresCR.valueOf(rs.getString("progrescr")), rs.getString("resumeseancecr"), rs.getString("prochainesActioncr"));
            list.add(cr);
        }

        return list;
    }

    public void updateFields(int idCr, CompteRenduSeance.ProgresCR progres, String resume, String actions) throws SQLException {
        String sql = "UPDATE compte_rendu_seance SET progrescr=?, resumeseancecr=?, prochainesActioncr=? WHERE id_compterendu=?";
        PreparedStatement pst = this.cnx.prepareStatement(sql);
        pst.setString(1, progres.name());
        pst.setString(2, resume);
        pst.setString(3, actions);
        pst.setInt(4, idCr);
        int rows = pst.executeUpdate();
        if (rows > 0) {
            System.out.println("✅ Compte rendu modifié");
        } else {
            System.out.println("⚠️ Compte rendu introuvable");
        }

    }

    public boolean compteRenduExiste(int idCr) throws SQLException {
        String sql = "SELECT id_compterendu FROM compte_rendu_seance WHERE id_compterendu=?";
        PreparedStatement pst = this.cnx.prepareStatement(sql);
        pst.setInt(1, idCr);
        return pst.executeQuery().next();
    }
}
