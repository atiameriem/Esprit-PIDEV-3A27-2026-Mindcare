package utils;
//permet de lire un fichier (ici config.properties qui contient des cles
import java.io.InputStream;
import java.util.Properties;

/**
 * Charge les propriétés depuis config.properties (src/main/resources/).
 * La clé API ne doit jamais être commitée dans le dépôt Git.
 */
public class AppConfig {
//props = objet qui va contenir toutes les clés/valeurs du fichier
    //static = partagé par toute l’application
    private static final Properties props = new Properties();
//Ce bloc s’exécute une seule fois quand la classe est chargée en mémoire.
    static {
        //Cherche config.properties dans src/main/resources
        try (InputStream in = AppConfig.class.getResourceAsStream("/config.properties")) {
            //Si le fichier existe
            //On charge toutes les propriétés dans props
            if (in != null) {
                props.load(in);
            } else {
                System.err.println("[AppConfig] config.properties introuvable dans les resources.");
            }
        } catch (Exception e) {
            System.err.println("[AppConfig] Erreur chargement config.properties : " + e.getMessage());
        }
    }

    public static String get(String key) {
        return props.getProperty(key, "");
    }
}
