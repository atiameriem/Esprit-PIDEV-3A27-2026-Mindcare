# Guide d'utilisation - PIDEV3A8

## ✨ Nouveautés - Système de Navigation

### 🎯 Système de navigation dynamique
L'interface possède maintenant un **système de navigation complet** qui permet de charger différentes vues dans la zone de contenu centrale en cliquant sur les boutons du menu.

### 📂 Vues disponibles
- 🏠 **Accueil** - Dashboard avec statistiques
- 📅 **Rendez-vous** - Gestion des rendez-vous
- 📄 **Compte-rendu** - Comptes-rendus de consultation
- 💬 **Forum** - Fil d'actualité et discussions
- 🤖 **Chatbot** - Assistant virtuel
- ✍️ **Passer tests** - Tests psychologiques
- 📈 **Suivie** - Suivi des patients
- 👁️ **Profil** - Consultation du profil
- ⚠️ **Réclamation** - Gestion des réclamations
- 📚 **Formation** - Réservation de formations
- 📖 **Support** - Consultation du support
- 📍 **Locaux** - Localisation

**📖 Guide complet disponible**: Consultez `GUIDE_NAVIGATION.md` pour apprendre à ajouter vos propres vues !

---

## Problème résolu
Le fichier manquant `UserService.java` a été créé dans `src/main/java/services/UserService.java`.

## Prérequis
1. **Java JDK 17** ou supérieur
2. **Maven 3.x**
3. **MySQL** (base de données)
4. **JavaFX 21** (inclus dans les dépendances Maven)

## Configuration de la base de données

Avant de lancer l'application, vous devez créer la base de données MySQL :

```sql
CREATE DATABASE studentmangment;
USE studentmangment;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    age INT NOT NULL,
    firstName VARCHAR(255) NOT NULL,
    lastName VARCHAR(255) NOT NULL
);
```

**Configuration de connexion** (dans `MyDatabase.java`) :
- URL: `jdbc:mysql://localhost:3306/studentmangment`
- Utilisateur: `root`
- Mot de passe: `` (vide)

Si vos identifiants MySQL sont différents, modifiez le fichier `src/main/java/utils/MyDatabase.java`.

## Compilation

Pour compiler le projet, exécutez :

```bash
mvn clean compile
```

## Exécution

Pour lancer l'interface JavaFX :

```bash
mvn javafx:run
```

Ou si vous préférez créer un JAR :

```bash
mvn package
java -jar target/pidev3a8-1.0-SNAPSHOT.jar
```

## Exécution des tests

Pour exécuter les tests unitaires :

```bash
mvn test
```

## Structure du projet

```
PIDEV3A8_FIXED/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── controllers/
│   │   │   │   └── MindCareLayoutController.java
│   │   │   ├── models/
│   │   │   │   └── User.java
│   │   │   ├── services/
│   │   │   │   ├── IService.java
│   │   │   │   └── UserService.java (NOUVEAU - CRÉÉ)
│   │   │   ├── test/
│   │   │   │   ├── Main.java
│   │   │   │   └── MainFx.java
│   │   │   ├── utils/
│   │   │   │   └── MyDatabase.java
│   │   │   └── module-info.java
│   │   └── resources/
│   │       ├── MindCareLayout.fxml
│   │       └── mindcare.css
│   └── test/
│       └── java/
│           └── UserServiceTest.java
└── pom.xml
```

## Fonctionnalités

L'application **MindCare** est une interface JavaFX pour la gestion psychologique avec :
- Gestion des utilisateurs (CRUD)
- Interface utilisateur complète avec menu de navigation
- Tests psychologiques
- Forum et chatbot
- Gestion des formations
- Système de réclamations

## Dépannage

### Erreur de connexion MySQL
Si vous obtenez une erreur de connexion :
1. Vérifiez que MySQL est démarré
2. Vérifiez les identifiants dans `MyDatabase.java`
3. Assurez-vous que la base de données `studentmangment` existe

### Erreur JavaFX
Si JavaFX ne se lance pas :
1. Vérifiez que Java 17+ est installé
2. Utilisez `mvn javafx:run` au lieu de `java -jar`

### Erreur Maven
Si Maven ne télécharge pas les dépendances :
1. Vérifiez votre connexion internet
2. Essayez `mvn clean install -U` pour forcer la mise à jour

## Support

Pour toute question ou problème, vérifiez :
- La configuration de la base de données
- Les versions de Java et Maven
- Les logs d'erreur dans la console
