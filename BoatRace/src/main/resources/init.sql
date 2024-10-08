-- Stocke les informations sur le joueur
CREATE TABLE IF NOT EXISTS br_joueur (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    username VARCHAR(16) NOT NULL
);

-- Records
CREATE TABLE IF NOT EXISTS br_record (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    date_record TIMESTAMP NOT NULL,
    nom_circuit VARCHAR(32) DEFAULT('default'),
    joueur INTEGER,
    nom_kiosque VARCHAR(64) -- dans le cas du Mode Kiosque
);

-- Temps
CREATE TABLE IF NOT EXISTS br_temps (
    id INTEGER,
    no_segment INTEGER DEFAULT(0),
    temps INTEGER NOT NULL,

    PRIMARY KEY (id, no_segment)
);

-- Vues
CREATE OR REPLACE VIEW br_leaderboard AS
SELECT j.username username, j.uuid uuid, r.date_record date_record, r.nom_circuit nom_circuit,
       t.no_segment no_segment, t.temps temps
FROM br_record r
    INNER JOIN br_joueur j ON j.id = r.joueur
    INNER JOIN br_temps  t ON t.id = r.id
ORDER BY t.temps ASC;