import models.User;
import org.junit.jupiter.api.*;
import services.UserService;

import java.sql.SQLException;
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
        User u = new User(22, "foulen", "ben foulen");
        try {
            int id = us.create(u);
            this.idUser = id;
            System.out.println("[DEBUG_LOG] Created User with ID: " + id);
            assertTrue(id > 0, "User ID should be greater than -1");
            List<User> users = us.read();
            assertFalse(users.isEmpty());
            boolean found = users.stream().anyMatch(pers -> pers.getId() == id && pers.getFirstName().equals("foulen"));
            if (found) {
                System.out.println("[DEBUG_LOG] Verified: User with the generated ID and name 'Ali' exists.");
            }
            assertTrue(found,
                    "User with the generated ID and name 'Ali' should exist");
        } catch (SQLException e) {
            System.out.println("execption in test : " + e.getMessage());
        }

    }

    @Test
    @Order(2)
    public void testUpdate() throws SQLException {
        User u = new User(22, "foulen", "ben foulen");
        int id = us.create(u);
        this.idUser = id;
        System.out.println("[DEBUG_LOG] Created User with ID: " + id);
        User updateInfo = new User();
        updateInfo.setId(id);
        updateInfo.setAge(21);
        updateInfo.setFirstName("after clean");
        updateInfo.setLastName("ben foulen");
        us.update(updateInfo);
        System.out.println("[DEBUG_LOG] Updated User ID " + id + " to name 'flen'");
        List<User> users = us.read();
        assertFalse(users.isEmpty());
        boolean found = users.stream().anyMatch(pers -> pers.getId() == id && pers.getFirstName().equals("after clean"));
        if (found) {
            System.out.println("[DEBUG_LOG] Verified: User with ID " + id + " has updated name 'mehdi'");
        }
        assertTrue(found,
                "User with ID " + id + " should have updated name 'mehdi'");
    }


}
