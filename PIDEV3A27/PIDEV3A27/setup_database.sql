-- Script de création de la base de données pour MindCare
-- Exécutez ce script dans MySQL pour configurer la base de données

-- Créer la base de données
CREATE DATABASE IF NOT EXISTS studentmangment;
USE studentmangment;

-- Créer la table users
CREATE TABLE IF NOT EXISTS users (
    id_users INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    prenom VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    telephone VARCHAR(20),
    date_inscription DATE NOT NULL,
    mot_de_passe VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    badge_image VARCHAR(255),
    date_naissance DATE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Créer la table reclamation
CREATE TABLE IF NOT EXISTS reclamation (
    id_reclamation INT AUTO_INCREMENT PRIMARY KEY,
    id_users INT NOT NULL,
    objet VARCHAR(255) NOT NULL,
    urgence VARCHAR(50) NOT NULL,
    description TEXT,
    statut VARCHAR(50) DEFAULT 'EN_ATTENTE',
    date DATETIME NOT NULL,
    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Ajouter un compte Admin par défaut
INSERT INTO users (nom, prenom, email, telephone, date_inscription, mot_de_passe, role) 
VALUES ('Admin', 'System', 'admin@mindcare.tn', '00000000', CURDATE(), 'admin123', 'Admin');

-- Vérifier que tout fonctionne
SHOW TABLES;
DESCRIBE users;
DESCRIBE reclamation;
