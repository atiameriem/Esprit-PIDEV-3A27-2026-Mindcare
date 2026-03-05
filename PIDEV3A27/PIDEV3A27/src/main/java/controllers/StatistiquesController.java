package controllers;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import models.User;
import utils.MyDatabase;
import utils.UserSession;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class StatistiquesController {

    @FXML
    private Label psychoNameLabel;
    @FXML
    private Label totalRdvLabel;
    @FXML
    private Label rdvMoisLabel;
    @FXML
    private Label totalCrLabel;
    @FXML
    private Label patientsLabel;
    @FXML
    private VBox rdvListContainer;

    @FXML
    public void initialize() {
        try {
            User psychologue = UserSession.getInstance().getUser();
            if (psychologue == null) {
                psychoNameLabel.setText("Session expirée");
                return;
            }
            psychoNameLabel.setText("Dr. " + psychologue.getPrenom() + " " + psychologue.getNom());
            loadStatistiques(psychologue.getId());
        } catch (Exception e) {
            System.err.println("❌ StatistiquesController.initialize() : " + e.getMessage());
            e.printStackTrace();
            showEmptyState("Erreur d'initialisation : " + e.getMessage());
        }
    }

    private void loadStatistiques(int psychoId) {
        Connection conn = MyDatabase.getInstance().getConnection();
        if (conn == null) {
            showEmptyState("Connexion à la base de données impossible.");
            return;
        }

        // ---- Essayer de lire les KPIs ----
        int totalRdv = queryCount(conn,
                "SELECT COUNT(*) FROM rendezvous WHERE id_psychologue = ?", psychoId);
        int rdvMois = queryCountMonth(conn, psychoId);
        int totalCr = queryCount(conn,
                "SELECT COUNT(*) FROM compterendu WHERE id_psychologue = ?", psychoId);
        int patients = queryCount(conn,
                "SELECT COUNT(DISTINCT id_patient) FROM rendezvous WHERE id_psychologue = ?", psychoId);

        totalRdvLabel.setText(String.valueOf(totalRdv));
        rdvMoisLabel.setText(String.valueOf(rdvMois));
        totalCrLabel.setText(String.valueOf(totalCr));
        patientsLabel.setText(String.valueOf(patients));

        // ---- Construire la liste des RDV ----
        buildRdvList(conn, psychoId);
    }

    private void buildRdvList(Connection conn, int psychoId) {
        rdvListContainer.getChildren().clear();

        // En-tête du tableau
        HBox header = buildRow("📅 Date", "👤 Patient", "📝 Motif", "🔵 Statut", true);
        rdvListContainer.getChildren().add(header);

        boolean hasRows = false;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT r.date_rdv, u.nom, u.prenom, r.motif, r.statut " +
                        "FROM rendezvous r " +
                        "JOIN users u ON r.id_patient = u.id_users " +
                        "WHERE r.id_psychologue = ? " +
                        "ORDER BY r.date_rdv DESC LIMIT 50")) {
            ps.setInt(1, psychoId);
            ResultSet rs = ps.executeQuery();
            int i = 0;
            while (rs.next()) {
                hasRows = true;
                Date d = rs.getDate("date_rdv");
                String date = d != null ? fmt.format(d.toLocalDate()) : "—";
                String patient = rs.getString("prenom") + " " + rs.getString("nom");
                String motif = safe(rs.getString("motif"));
                String statut = safe(rs.getString("statut"));
                HBox row = buildRow(date, patient, motif, statut, false);
                // Alternating row color
                if (i % 2 == 0)
                    row.setStyle(row.getStyle() + "-fx-background-color: #fafafa;");
                rdvListContainer.getChildren().add(row);
                i++;
            }
        } catch (SQLException e) {
            // Table rendezvous n'existe pas encore
            System.err.println("ℹ️ Table rendezvous introuvable : " + e.getMessage());
        }

        if (!hasRows)
            showEmptyState("Aucun rendez-vous enregistré pour le moment.");
    }

    private HBox buildRow(String col1, String col2, String col3, String col4, boolean isHeader) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        String baseStyle = isHeader
                ? "-fx-background-color: #f1f3f4; -fx-padding: 12 20; -fx-border-color: transparent transparent #e0e0e0 transparent;"
                : "-fx-padding: 12 20; -fx-border-color: transparent transparent #f1f3f4 transparent;";
        row.setStyle(baseStyle);

        String labelStyle = isHeader
                ? "-fx-font-weight: bold; -fx-text-fill: #5f6368; -fx-font-size: 13px;"
                : "-fx-text-fill: #202124; -fx-font-size: 13px;";

        Label l1 = mkLabel(col1, labelStyle, 140);
        Label l2 = mkLabel(col2, labelStyle, 190);
        Label l3 = mkLabel(col3, labelStyle, -1); // hgrow
        Label l4 = mkLabel(col4, getStatutStyle(col4, isHeader), 120);

        HBox.setHgrow(l3, Priority.ALWAYS);
        row.getChildren().addAll(l1, l2, l3, l4);
        return row;
    }

    private Label mkLabel(String text, String style, double minW) {
        Label l = new Label(text);
        l.setStyle(style);
        l.setWrapText(false);
        l.setEllipsisString("…");
        if (minW > 0) {
            l.setMinWidth(minW);
            l.setMaxWidth(minW);
        }
        return l;
    }

    private String getStatutStyle(String statut, boolean isHeader) {
        if (isHeader)
            return "-fx-font-weight: bold; -fx-text-fill: #5f6368; -fx-font-size: 13px;";
        return switch (statut.toLowerCase()) {
            case "confirmé", "confirme" ->
                "-fx-text-fill: #34a853; -fx-font-weight: bold; -fx-font-size: 13px;";
            case "annulé", "annule" ->
                "-fx-text-fill: #ea4335; -fx-font-weight: bold; -fx-font-size: 13px;";
            case "en attente" ->
                "-fx-text-fill: #f29900; -fx-font-weight: bold; -fx-font-size: 13px;";
            default -> "-fx-text-fill: #5f6368; -fx-font-size: 13px;";
        };
    }

    private void showEmptyState(String message) {
        rdvListContainer.getChildren().clear();
        Label lbl = new Label("📭  " + message);
        lbl.setStyle("-fx-text-fill: #9aa0a6; -fx-font-size: 14px; -fx-padding: 30 20;");
        rdvListContainer.getChildren().add(lbl);
    }

    private int queryCount(Connection conn, String sql, int id) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("ℹ️ queryCount [" + sql + "] : " + e.getMessage());
        }
        return 0;
    }

    private int queryCountMonth(Connection conn, int psychoId) {
        LocalDate now = LocalDate.now();
        String sql = "SELECT COUNT(*) FROM rendezvous WHERE id_psychologue = ? " +
                "AND MONTH(date_rdv) = ? AND YEAR(date_rdv) = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, psychoId);
            ps.setInt(2, now.getMonthValue());
            ps.setInt(3, now.getYear());
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("ℹ️ queryCountMonth : " + e.getMessage());
        }
        return 0;
    }

    private String safe(String s) {
        return s != null && !s.isBlank() ? s : "—";
    }

    @FXML
    private void handleRefresh() {
        try {
            User psychologue = UserSession.getInstance().getUser();
            if (psychologue != null)
                loadStatistiques(psychologue.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
