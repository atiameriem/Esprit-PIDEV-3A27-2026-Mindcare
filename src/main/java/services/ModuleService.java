package services;

import models.Module;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ModuleService implements IService<Module> {

    private final Connection connection;

    public ModuleService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public int create(Module module) throws SQLException {
        String query = "INSERT INTO module (titre, description, id_formation) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, module.getTitre());
            statement.setString(2, module.getDescription());
            statement.setInt(3, module.getFormationId());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating module failed, no rows affected.");
            }
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    module.setId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Creating module failed, no ID obtained.");
                }
            }
        }
    }

    @Override
    public void update(Module module) throws SQLException {
        String query = "UPDATE module SET titre = ?, description = ?, id_formation = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, module.getTitre());
            statement.setString(2, module.getDescription());
            statement.setInt(3, module.getFormationId());
            statement.setInt(4, module.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM module WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    @Override
    public List<Module> read() throws SQLException {
        List<Module> modules = new ArrayList<>();
        String query = "SELECT * FROM module";
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                Module m = mapResultSetToModule(resultSet);
                modules.add(m);
            }
        }
        return modules;
    }

    public List<Module> findByFormationId(int formationId) throws SQLException {
        List<Module> modules = new ArrayList<>();
        String query = "SELECT * FROM module WHERE id_formation= ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, formationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    modules.add(mapResultSetToModule(resultSet));
                }
            }
        }
        return modules;
    }

    private Module mapResultSetToModule(ResultSet rs) throws SQLException {
        Module m = new Module();
        m.setId(rs.getInt("id"));
        m.setTitre(rs.getString("titre"));
        m.setDescription(rs.getString("description"));
        m.setFormationId(rs.getInt("id_formation"));
        return m;
    }
}
