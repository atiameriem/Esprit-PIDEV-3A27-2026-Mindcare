-- Script de création de la base de données pour MindCare
-- Exécutez ce script dans MySQL pour configurer la base de données

-- Créer la base de données
CREATE DATABASE IF NOT EXISTS studentmangment;
USE studentmangment;

-- Créer la table users
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    age INT NOT NULL,
    firstName VARCHAR(255) NOT NULL,
    lastName VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Ajouter quelques données de test (optionnel)
INSERT INTO users (age, firstName, lastName) VALUES
(25, 'Jean', 'Dupont'),
(30, 'Marie', 'Martin'),
(28, 'Pierre', 'Bernard');

-- Vérifier que tout fonctionne
SELECT * FROM users;

SHOW TABLES;
DESCRIBE users;
