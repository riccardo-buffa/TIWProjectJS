CREATE DATABASE aste_online;
USE aste_online;

CREATE TABLE utenti (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    nome VARCHAR(50) NOT NULL,
    cognome VARCHAR(50) NOT NULL,
    indirizzo TEXT NOT NULL
);

CREATE TABLE articoli (
    id INT PRIMARY KEY AUTO_INCREMENT,
    codice VARCHAR(20) UNIQUE NOT NULL,
    nome VARCHAR(100) NOT NULL,
    descrizione TEXT NOT NULL,
    immagine VARCHAR(255),
    prezzo DECIMAL(10,2) NOT NULL,
    proprietario_id INT,
    venduto BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (proprietario_id) REFERENCES utenti(id)
);

CREATE TABLE aste (
    id INT PRIMARY KEY AUTO_INCREMENT,
    prezzo_iniziale DECIMAL(10,2) NOT NULL,
    rialzo_minimo INT NOT NULL,
    scadenza DATETIME NOT NULL,
    chiusa BOOLEAN DEFAULT FALSE,
    venditore_id INT,
    vincitore_id INT,
    prezzo_finale DECIMAL(10,2),
    FOREIGN KEY (venditore_id) REFERENCES utenti(id),
    FOREIGN KEY (vincitore_id) REFERENCES utenti(id)
);

CREATE TABLE asta_articoli (
    asta_id INT,
    articolo_id INT,
    PRIMARY KEY (asta_id, articolo_id),
    FOREIGN KEY (asta_id) REFERENCES aste(id),
    FOREIGN KEY (articolo_id) REFERENCES articoli(id)
);

CREATE TABLE offerte (
    id INT PRIMARY KEY AUTO_INCREMENT,
    asta_id INT,
    offerente_id INT,
    importo DECIMAL(10,2) NOT NULL,
    data_offerta DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (asta_id) REFERENCES aste(id),
    FOREIGN KEY (offerente_id) REFERENCES utenti(id)
);

-- Dati di test
INSERT INTO utenti (username, password, nome, cognome, indirizzo) VALUES 
('admin', 'admin123', 'Mario', 'Rossi', 'Via Roma 1, Milano'),
('utente1', 'pass123', 'Luigi', 'Bianchi', 'Via Verdi 2, Roma');