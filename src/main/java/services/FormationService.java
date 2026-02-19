package services;

import models.Formation;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FormationService implements IService<Formation> {

    private final Connection connection;

    public FormationService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public int create(Formation formation) throws SQLException {
        String query = "INSERT INTO formation (titre, description, duree, niveau, imagePath) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, formation.getTitre());
            statement.setString(2, formation.getDescription());
            statement.setString(3, formation.getDuree());
            statement.setString(4, formation.getNiveau());
            statement.setString(5, formation.getImagePath());

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
        String query = "UPDATE formation SET titre = ?, description = ?, duree = ?, niveau = ?, imagePath = ? WHERE id_formation = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, formation.getTitre());
            statement.setString(2, formation.getDescription());
            statement.setString(3, formation.getDuree());
            statement.setString(4, formation.getNiveau());
            statement.setString(5, formation.getImagePath());
            statement.setInt(6, formation.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM formation WHERE id_formation = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    @Override
    public List<Formation> read() throws SQLException {
        List<Formation> formations = new ArrayList<>();
        String query = "SELECT * FROM formation";
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                Formation f = new Formation();
                f.setId(resultSet.getInt("id_formation"));
                f.setTitre(resultSet.getString("titre"));
                f.setDescription(resultSet.getString("description"));
                f.setDuree(resultSet.getString("duree"));
                f.setNiveau(resultSet.getString("niveau"));
                f.setImagePath(resultSet.getString("imagePath"));
                formations.add(f);
            }
        }
        return formations;
    }

    public Formation findById(int id) throws SQLException {
        String query = "SELECT * FROM formation WHERE id_formation = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    Formation f = new Formation();
                    f.setId(rs.getInt("id_formation"));
                    f.setTitre(rs.getString("titre"));
                    f.setDescription(rs.getString("description"));
                    f.setDuree(rs.getString("duree"));
                    f.setNiveau(rs.getString("niveau"));
                    f.setImagePath(rs.getString("imagePath"));
                    return f;
                }
            }
        }
        return null;
    }
}
