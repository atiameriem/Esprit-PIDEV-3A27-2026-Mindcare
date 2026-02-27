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

-- Table formation
CREATE TABLE IF NOT EXISTS formation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    duree VARCHAR(100),
    niveau VARCHAR(50),
    categorie VARCHAR(100),
    image_path VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table module (liée à formation)
CREATE TABLE IF NOT EXISTS module (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    formation_id INT NOT NULL,
    CONSTRAINT fk_module_formation FOREIGN KEY (formation_id) REFERENCES formation(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table contenu (liée à module)
CREATE TABLE IF NOT EXISTS contenu (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    chemin VARCHAR(500) NOT NULL,
    module_id INT NOT NULL,
    CONSTRAINT fk_contenu_module FOREIGN KEY (module_id) REFERENCES module(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Vérifier que tout fonctionne
SELECT * FROM users;

SHOW TABLES;
DESCRIBE users;
DESCRIBE formation;
DESCRIBE module;
DESCRIBE contenu;
