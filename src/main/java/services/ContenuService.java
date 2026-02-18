package services;

import models.Contenu;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ContenuService implements IService<Contenu> {

    private final Connection connection;

    public ContenuService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public int create(Contenu contenu) throws SQLException {
        String query = "INSERT INTO contenu (type, chemin, module_id) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, contenu.getType());
            statement.setString(2, contenu.getChemin());
            statement.setInt(3, contenu.getModuleId());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating contenu failed, no rows affected.");
            }
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    contenu.setId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Creating contenu failed, no ID obtained.");
                }
            }
        }
    }

    @Override
    public void update(Contenu contenu) throws SQLException {
        String query = "UPDATE contenu SET type = ?, chemin = ?, module_id = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, contenu.getType());
            statement.setString(2, contenu.getChemin());
            statement.setInt(3, contenu.getModuleId());
            statement.setInt(4, contenu.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM contenu WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    @Override
    public List<Contenu> read() throws SQLException {
        List<Contenu> contenus = new ArrayList<>();
        String query = "SELECT * FROM contenu";
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                contenus.add(mapResultSetToContenu(resultSet));
            }
        }
        return contenus;
    }

    public List<Contenu> findByModuleId(int moduleId) throws SQLException {
        List<Contenu> contenus = new ArrayList<>();
        String query = "SELECT * FROM contenu WHERE module_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, moduleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    contenus.add(mapResultSetToContenu(resultSet));
                }
            }
        }
        return contenus;
    }

    private Contenu mapResultSetToContenu(ResultSet rs) throws SQLException {
        Contenu c = new Contenu();
        c.setId(rs.getInt("id"));
        c.setType(rs.getString("type"));
        c.setChemin(rs.getString("chemin"));
        c.setModuleId(rs.getInt("module_id"));
        return c;
    }
}
