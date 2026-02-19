import models.User;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import services.UserService;

/**
 * Tests désactivés par défaut car ils dépendent d'une base MySQL locale.
 * Si tu veux les activer: enlève @Disabled et assure-toi que la DB tourne.
 */
@Disabled("DB locale requise")
public class UserServiceTest {

    @Test
    void dummyCompilationTest() {
        UserService us = new UserService();
        User u = new User("Nom", "Prenom", "email@test.com", "1234", "patient");
        // Juste pour vérifier la compilation.
        assert us != null && u != null;
    }
}
