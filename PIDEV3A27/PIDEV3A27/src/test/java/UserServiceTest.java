import models.User;
import org.junit.jupiter.api.*;
import services.UserService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserServiceTest {
    static UserService us;
    private int idUser = -1;

    @BeforeAll
    public static void setup() {
        us = new UserService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (idUser != -1) {
            us.delete(idUser);
            System.out.println("[DEBUG_LOG] Cleanup: Deleted User with ID: " + idUser);
            idUser = -1;
        }
    }

    @Test
    @Order(1)
    public void testCreateUser() {
        User u = new User();
        u.setNom("foulen");
        u.setPrenom("ben foulen");
        u.setEmail("foulen.test@esprit.tn");
        u.setTelephone("12345678");
        u.setMotDePasse("password123");
        u.setRole(User.Role.Patient);
        u.setDateInscription(LocalDate.now());

        try {
            int id = us.create(u);
            this.idUser = id;
            System.out.println("[DEBUG_LOG] Created User with ID: " + id);
            assertTrue(id > 0, "User ID should be greater than 0");
            List<User> users = us.read();
            assertFalse(users.isEmpty());
            boolean found = users.stream().anyMatch(pers -> pers.getId() == id && pers.getNom().equals("foulen"));
            if (found) {
                System.out.println("[DEBUG_LOG] Verified: User with the generated ID and name 'foulen' exists.");
            }
            assertTrue(found, "User with the generated ID and name 'foulen' should exist");
        } catch (SQLException e) {
            System.out.println("exception in test : " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    public void testUpdate() throws SQLException {
        User u = new User();
        u.setNom("foulen");
        u.setPrenom("ben foulen");
        u.setEmail("foulen.update@esprit.tn");
        u.setTelephone("87654321");
        u.setMotDePasse("password123");
        u.setRole(User.Role.Patient);
        u.setDateInscription(LocalDate.now());

        int id = us.create(u);
        this.idUser = id;
        System.out.println("[DEBUG_LOG] Created User with ID: " + id);

        User updateInfo = new User();
        updateInfo.setId(id);
        updateInfo.setNom("after clean");
        updateInfo.setPrenom("ben foulen");
        updateInfo.setEmail("foulen.update@esprit.tn");
        updateInfo.setTelephone("87654321");
        updateInfo.setMotDePasse("password123");
        updateInfo.setRole(User.Role.Patient);
        updateInfo.setDateInscription(LocalDate.now());
        updateInfo.setDateNaissance(LocalDate.of(2000, 1, 1));

        us.update(updateInfo);
        System.out.println("[DEBUG_LOG] Updated User ID " + id + " to name 'after clean'");

        List<User> users = us.read();
        assertFalse(users.isEmpty());
        boolean found = users.stream().anyMatch(pers -> pers.getId() == id && pers.getNom().equals("after clean"));
        if (found) {
            System.out.println("[DEBUG_LOG] Verified: User with ID " + id + " has updated name 'after clean'");
        }
        assertTrue(found, "User with ID " + id + " should have updated name 'after clean'");
    }
}