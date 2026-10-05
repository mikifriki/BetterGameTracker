package com.bettergametracker.play;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayTimeEntryNotesMigrationTests {

    @Test
    void upgradesExistingEntriesWithoutLosingDataOrConstraints(@TempDir Path directory) throws Exception {
        String url = "jdbc:sqlite:" + directory.resolve("migration.db");
        Flyway.configure().dataSource(url, null, null).placeholders(Map.of("uuidType", "BLOB"))
                .target("1").load().migrate();
        try (var connection = DriverManager.getConnection(url); var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("INSERT INTO games (id, game_title) VALUES (X'01', 'Existing game')");
            statement.execute("INSERT INTO play_entries (id, game_id) VALUES (X'02', X'01')");
            statement.execute("""
                    INSERT INTO play_time_entries (id, play_entry_id, date, duration_minutes, notes)
                    VALUES (X'03', X'02', '2026-09-08', 30, 'Existing notes'),
                           (X'04', X'02', '2026-09-08', 45, NULL)
                    """);
        }

        Flyway.configure().dataSource(url, null, null).placeholders(Map.of("uuidType", "BLOB"))
                .initSql("PRAGMA foreign_keys = ON").load().migrate();
        try (var connection = DriverManager.getConnection(url); var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            try (var rows = statement.executeQuery("""
                    SELECT hex(id), hex(play_entry_id), date, duration_minutes, notes
                    FROM play_time_entries ORDER BY id
                    """)) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("03");
                assertThat(rows.getString(2)).isEqualTo("02");
                assertThat(rows.getString(3)).isEqualTo("2026-09-08");
                assertThat(rows.getInt(4)).isEqualTo(30);
                assertThat(rows.getString(5)).isEqualTo("Existing notes");
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("04");
                assertThat(rows.getString(2)).isEqualTo("02");
                assertThat(rows.getString(3)).isEqualTo("2026-09-08");
                assertThat(rows.getInt(4)).isEqualTo(45);
                assertThat(rows.getString(5)).isNull();
                assertThat(rows.next()).isFalse();
            }
            try (var rows = statement.executeQuery("SELECT type FROM pragma_table_info('play_time_entries') WHERE name = 'notes'")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("VARCHAR(5000)");
            }
            try (var rows = statement.executeQuery("SELECT name FROM pragma_index_list('play_time_entries')")) {
                boolean hasParentIndex = false;
                while (rows.next()) {
                    hasParentIndex |= rows.getString(1).equals("idx_play_time_entries_play_entry_id");
                }
                assertThat(hasParentIndex).isTrue();
            }
            assertThatThrownBy(() -> statement.execute("UPDATE play_time_entries SET duration_minutes = 0"))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> statement.execute("UPDATE play_time_entries SET play_entry_id = X'FF'"))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> statement.execute("DELETE FROM play_entries"))
                    .isInstanceOf(SQLException.class);
        }
    }
}
