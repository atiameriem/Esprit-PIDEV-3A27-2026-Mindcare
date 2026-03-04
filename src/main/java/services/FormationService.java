package services;

import models.Formation;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FormationService implements IService<Formation> {

    public FormationService() {
        Connection conn = MyDatabase.getInstance().getConnection();
        if (conn == null) {
            System.err.println("❌ FormationService: Connection is null. Migration skipped.");
            return;
        }
        try {
            DatabaseMetaData dbmd = conn.getMetaData();

            // Check if id_users exists
            boolean idUsersExists = false;
            try (ResultSet rs = dbmd.getColumns(null, null, "formation", "id_users")) {
                if (rs.next())
                    idUsersExists = true;
            }

            if (!idUsersExists) {
                try (Statement st = conn.createStatement()) {
                    st.execute("ALTER TABLE formation ADD COLUMN id_users INT NULL");
                    System.out.println("Column id_users added to formation table.");
                }
            }

            // Check if statut exists to drop it
            boolean statusExists = false;
            try (ResultSet rs = dbmd.getColumns(null, null, "formation", "statut")) {
                if (rs.next())
                    statusExists = true;
            }

            if (statusExists) {
                try (Statement st = conn.createStatement()) {
                    st.execute("ALTER TABLE formation DROP COLUMN statut");
                    System.out.println("Column statut removed from formation table.");
                }
            }

        } catch (SQLException e) {
            System.err.println("Migration error in FormationService: " + e.getMessage());
        }
    }

    private Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    @Override
    public int create(Formation formation) throws SQLException {
        String query = "INSERT INTO formation (titre, description, duree, niveau, categorie, imagePath, id_users) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, formation.getTitre());
            statement.setString(2, formation.getDescription());
            statement.setString(3, formation.getDuree());
            statement.setString(4, formation.getNiveau());
            statement.setString(5, formation.getCategorie());
            statement.setString(6, formation.getImagePath());
            statement.setInt(7, formation.getIdCreateur());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating formation failed, no rows affected.");
            }
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    formation.setId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Creating formation failed, no ID obtained.");
                }
            }
        }
    }

    @Override
    public void update(Formation formation) throws SQLException {
        String query = "UPDATE formation SET titre = ?, description = ?, duree = ?, niveau = ?, categorie = ?, imagePath = ? WHERE id_formation = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, formation.getTitre());
            statement.setString(2, formation.getDescription());
            statement.setString(3, formation.getDuree());
            statement.setString(4, formation.getNiveau());
            statement.setString(5, formation.getCategorie());
            statement.setString(6, formation.getImagePath());
            statement.setInt(7, formation.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM formation WHERE id_formation = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    @Override
    public List<Formation> read() throws SQLException {
        List<Formation> formations = new ArrayList<>();
        String query = "SELECT * FROM formation";
        try (Statement statement = getConnection().createStatement();
                ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                Formation f = mapResultSetToFormation(resultSet);
                formations.add(f);
            }
        }
        return formations;
    }

    public List<Formation> findByOwner(int ownerId) throws SQLException {
        List<Formation> formations = new ArrayList<>();
        String query = "SELECT * FROM formation WHERE id_users = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setInt(1, ownerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    formations.add(mapResultSetToFormation(resultSet));
                }
            }
        } catch (SQLException e) {
            // If column doesn't exist yet, return empty list gracefully
            System.out.println("findByOwner: " + e.getMessage());
        }
        return formations;
    }

    public Formation findById(int id) throws SQLException {
        String query = "SELECT * FROM formation WHERE id_formation = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFormation(rs);
                }
            }
        }
        return null;
    }

    private Formation mapResultSetToFormation(ResultSet rs) throws SQLException {
        Formation f = new Formation();
        f.setId(rs.getInt("id_formation"));
        f.setTitre(rs.getString("titre"));
        f.setDescription(rs.getString("description"));
        f.setDuree(rs.getString("duree"));
        f.setNiveau(rs.getString("niveau"));
        f.setCategorie(rs.getString("categorie"));
        f.setImagePath(rs.getString("imagePath"));
        // id_createur may not yet exist in older DBs (column added on next restart)
        try {
            f.setIdCreateur(rs.getInt("id_users"));
        } catch (SQLException e) {
            f.setIdCreateur(0); // default — safe fallback
        }

        // Calculer la moyenne des notes
        f.setAverageRating(getAverageRating(f.getId()));

        return f;
    }

    private double getAverageRating(int formationId) {
        String query = "SELECT AVG(rating) as avg_r FROM participation WHERE id_formation = ? AND rating > 0";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setInt(1, formationId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("avg_r");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error calculating average rating: " + e.getMessage());
        }
        return 0.0;
    }
}
