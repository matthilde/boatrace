package de.matthil.boatRace;

import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;

/**
 * Class to handle all the database queries to do.
 */
public class Leaderboard {
    private final String connectUrl;
    private final String username, password;
    private final String race;

    /**
     * @param host MySQL host
     * @param port MySQL port
     * @param database database to use
     * @param user Username
     * @param password Password
     */
    public Leaderboard(String race, String host, int port, String database, String user, String password) {
        this.connectUrl = String.format("jdbc:mysql://%s:%d/%s", host, port, database);
        this.username = user;
        this.password = password;

        this.race = race;
    }

    /**
     * Binding for the MySQL LAST_INSERT_ID() function.
     */
    public int lastInsertId(Connection conn) throws SQLException {
        ResultSet rs = conn.createStatement().executeQuery("SELECT LAST_INSERT_ID() id");
        rs.next();
        return rs.getInt("id");
    }

    /**
     * Gets the database ID of a player.
     *
     * @param player player
     * @return ID of the player, -1 if it does not exist
     */
    public int getPlayerId(Player player) throws SQLException {
        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT id FROM br_joueur WHERE uuid = ?");
            stmt.setString(1, player.getUniqueId().toString());

            ResultSet result = stmt.executeQuery();
            if (result.next()) {
                return result.getInt("id");
            } else {
                return -1;
            }
        }
    }

    /**
     * Adds or updates a player row based on a Player object's information
     *
     * @return database ID of the player row
     */
    public int setPlayer(Player player) throws SQLException {
        try (Connection conn = getConnection()) {
            int playerId = getPlayerId(player);

            PreparedStatement stmt;
            if (playerId > 0) {
                stmt = conn.prepareStatement("UPDATE br_joueur SET username = ? WHERE id = ?");
                stmt.setString(1, player.getName());
                stmt.setInt(2, playerId);
            } else {
                stmt = conn.prepareStatement("INSERT INTO br_joueur(username, uuid) VALUES (?, ?)");
                stmt.setString(1, player.getName());
                stmt.setString(2, player.getUniqueId().toString());
            }

            stmt.execute();
            return playerId == -1 ? lastInsertId(conn) : playerId;
        }
    }

    /**
     * Adds a new record in the database based on a PlayerState object's information.
     *
     * @param nomKiosque nullable, user name to give in Kiosk Mode
     * @return record ID
     */
    public int newRecord(PlayerState state, String nomKiosque) throws SQLException {
        PreparedStatement stmt;

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            stmt = conn.prepareStatement(
                    "INSERT INTO br_record(date_record, joueur, nom_kiosque, nom_circuit) VALUES (?, ?, ?, ?)"
            );
            stmt.setTimestamp(1, new Timestamp(state.start().getEpochSecond()));
            if (nomKiosque != null) {
                stmt.setNull(2, Types.INTEGER);
                stmt.setString(3, nomKiosque);
            } else {
                stmt.setInt(2, setPlayer(state.player()));
                stmt.setNull(3, Types.VARCHAR);
            }
            stmt.setString(4, race);

            stmt.execute();

            int id = lastInsertId(conn);
            for (int i = 0; i <= state.getNbSegments(); ++i) {
                stmt = conn.prepareStatement("INSERT INTO br_temps(id, no_segment, temps) VALUES (?, ?, ?)");
                stmt.setInt(1, id);
                stmt.setInt(2, i);
                stmt.setLong(3, i == 0 ? state.totalTime() : state.segmentTime(i));

                stmt.execute();
            }

            conn.commit();
            return id;
        }
    }

    /**
     * Get the best segment of a player.
     *
     * @param segment 0 for the total record, otherwise segment number
     * @return best record in milliseconds, null if the record does not exist
     */
    public Long getBestRecord(Player player, int segment) throws SQLException {
        try (Connection conn = getConnection()) {
            /*
            PreparedStatement query = conn.prepareStatement(
                "SELECT t.temps FROM br_joueur j, br_record r, br_temps t " +
                "WHERE t.no_segment = ? AND j.id = r.joueur AND t.id = r.id AND j.uuid = ? AND r.nom_circuit = ? ORDER BY t.temps ASC LIMIT 1"
            ); */
            PreparedStatement query = conn.prepareStatement(
                    "SELECT temps FROM br_leaderboard WHERE no_segment = ? AND uuid = ? AND nom_circuit = ? LIMIT 1"
            );

            query.setInt(1, segment);
            query.setString(2, player.getUniqueId().toString());
            query.setString(3, race);

            ResultSet rs = query.executeQuery();
            if (rs.next()) {
                return rs.getLong("temps");
            } else {
                return null;
            }
        }
    }

    /**
     * Obtain the list of the global top records.
     *
     * @return list of LeaderboardEntry records containing the best records
     */
    public ArrayList<LeaderboardEntry> getLeaderboard() throws SQLException {
        ArrayList<LeaderboardEntry> result = new ArrayList<>();

        try (Connection conn = getConnection()) {
            /*
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT j.username username, t.temps temps, r.nom_circuit course, r.date_record start " +
                "FROM br_joueur j, br_record r, br_temps t " +
                "        WHERE " +
                "t.no_segment = 0 " +
                "AND t.id = r.id " +
                "AND j.id = r.joueur " +
                "AND r.nom_circuit = ?" +
                "ORDER BY t.temps ASC " +
                "LIMIT 10");
             */
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT username, temps, nom_circuit course, date_record start " +
                        "FROM br_leaderboard WHERE nom_circuit = ? AND no_segment = 0 LIMIT 10");

            stmt.setString(1, race);
            ResultSet query = stmt.executeQuery();

            while (query.next()) {
                LeaderboardEntry entry = new LeaderboardEntry(
                        query.getLong("temps"),
                        Instant.ofEpochSecond(query.getTimestamp("start").getTime()),
                        query.getString("course"),
                        query.getString("username")
                );
                result.add(entry);
            }
        }

        return result;
    }

    /**
     * Shortcut function to connect to the database.
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(connectUrl, username, password);
    }

    /**
     * Initalize the database with necessary tables and configuration.
     */
    public void initDatabase() throws IOException, SQLException {
        try (
            InputStream in = getClass().getResourceAsStream("/init.sql")
        ) {
            if (in == null) throw new IOException("input stream is null");

            String script = new String(in.readAllBytes());
            Connection conn = getConnection();
            conn.setAutoCommit(false);

            Statement s = conn.createStatement();
            for (String line : script.split(";")) {
                if (line.trim().length() == 0) continue;
                s.addBatch(line);
            }

            s.executeBatch();
            conn.commit();
        }
    }
}
