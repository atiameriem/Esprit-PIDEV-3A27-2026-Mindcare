/*package test;

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
    private static int idUser = -1;

    @BeforeAll
    public static void setup() {
        us = new UserService();
    }

    @AfterAll
    public static void cleanUp() {
        try {
            if (idUser != -1) {
                us.delete(idUser);
                System.out.println("[DEBUG_LOG] Cleanup: Deleted User with ID: " + idUser);
                idUser = -1;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR_LOG] Cleanup failed: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    public void testCreateUser() {

        User u = new User(
                25,
                "Foulen",
                "Ben Foulen",
                "foulen@test.com",
                "12345678",
                LocalDate.of(2024, 1, 1),
                "password123",
                User.Role.Patient
        );

        try {
            int id = us.create(u);
            idUser = id;

            System.out.println("[DEBUG_LOG] Created User with ID: " + id);
            assertTrue(id > 0, "User ID should be greater than 0");

            List<User> users = us.getAll();
            assertFalse(users.isEmpty(), "User list should not be empty");

            boolean found = users.stream()
                    .anyMatch(p -> p.getId() == id && p.getNom().equals("Foulen"));

            assertTrue(found, "User with the generated ID and name 'Foulen' should exist");
            System.out.println("[DEBUG_LOG] Verified: User exists with ID " + id);

        } catch (SQLException e) {
            e.printStackTrace();
            Assertions.fail("Exception occurred during testCreateUser: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    public void testUpdate() {

        try {
            if (idUser == -1) {
                User u = new User(
                        30,
                        "ToUpdate",
                        "User",
                        "update@test.com",
                        "00000000",
                        LocalDate.of(2024, 1, 1),
                        "pass",
                        User.Role.Patient
                );
                idUser = us.create(u);
            }

            User updateInfo = new User();
            updateInfo.setId(idUser);
            updateInfo.setAge(26);
            updateInfo.setNom("UpdatedName");
            updateInfo.setPrenom("Ben Foulen");
            updateInfo.setEmail("updated@test.com");
            updateInfo.setTelephone("87654321");
            updateInfo.setDateInscription(LocalDate.of(2024, 1, 2));
            updateInfo.setMotDePasse("newpass");
            updateInfo.setRole(User.Role.Patient);

            us.update(updateInfo);

            System.out.println("[DEBUG_LOG] Updated User ID " + idUser);

            List<User> users = us.getAll();
            assertFalse(users.isEmpty(), "User list should not be empty after update");

            boolean found = users.stream()
                    .anyMatch(p -> p.getId() == idUser && p.getNom().equals("UpdatedName"));

            assertTrue(found, "User with ID " + idUser + " should have updated name 'UpdatedName'");
            System.out.println("[DEBUG_LOG] Verified: User ID " + idUser + " updated successfully");

        } catch (SQLException e) {
            e.printStackTrace();
            Assertions.fail("Exception occurred during testUpdate: " + e.getMessage());
        }
    }
}
*/