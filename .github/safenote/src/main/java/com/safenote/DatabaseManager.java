package com.safenote;

import java.sql.*;
import java.io.File;

public class DatabaseManager {
    private static final String DB_NAME = "safenote.db";
    private Connection connection;
    private String dbPath;

    public DatabaseManager() throws SQLException {
        // Create database in user home directory
        String userHome = System.getProperty("user.home");
        File safeNoteDir = new File(userHome, ".safenote");
        if (!safeNoteDir.exists()) {
            safeNoteDir.mkdirs();
        }

        dbPath = new File(safeNoteDir, DB_NAME).getAbsolutePath();
        initializeDatabase();
    }

    private void initializeDatabase() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        createTables();
    }

    private void createTables() throws SQLException {
        String createNotebooksTable = """
            CREATE TABLE IF NOT EXISTS notebooks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createSectionsTable = """
            CREATE TABLE IF NOT EXISTS sections (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                notebook_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (notebook_id) REFERENCES notebooks(id) ON DELETE CASCADE
            )
        """;

        String createPagesTable = """
            CREATE TABLE IF NOT EXISTS pages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                section_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                content TEXT,
                created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE CASCADE
            )
        """;

        String createSearchIndex = """
            CREATE VIRTUAL TABLE IF NOT EXISTS pages_fts USING fts5(
                title, content, page_id
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createNotebooksTable);
            stmt.execute(createSectionsTable);
            stmt.execute(createPagesTable);
            stmt.execute(createSearchIndex);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    // com.safenote.Notebook operations
    public long createNotebook(String name) throws SQLException {
        connection.setAutoCommit(false);
        try {
            String sql = "INSERT INTO notebooks (name) VALUES (?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.executeUpdate();
            }

            // Get the generated ID using last_insert_rowid()
            long generatedId;
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    generatedId = rs.getLong(1);
                } else {
                    throw new SQLException("Failed to get generated notebook ID");
                }
            }

            connection.commit();
            return generatedId;

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public ResultSet getAllNotebooks() throws SQLException {
        String sql = "SELECT * FROM notebooks ORDER BY name";
        PreparedStatement stmt = connection.prepareStatement(sql);
        return stmt.executeQuery();
    }

    public void updateNotebook(long id, String name) throws SQLException {
        String sql = "UPDATE notebooks SET name = ?, modified_date = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public void deleteNotebook(long id) throws SQLException {
        String sql = "DELETE FROM notebooks WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    // com.safenote.Section operations
    public long createSection(long notebookId, String name) throws SQLException {
        connection.setAutoCommit(false);
        try {
            String sql = "INSERT INTO sections (notebook_id, name) VALUES (?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, notebookId);
                stmt.setString(2, name);
                stmt.executeUpdate();
            }

            // Get the generated ID using last_insert_rowid()
            long generatedId;
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    generatedId = rs.getLong(1);
                } else {
                    throw new SQLException("Failed to get generated section ID");
                }
            }

            connection.commit();
            return generatedId;

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public ResultSet getSectionsByNotebook(long notebookId) throws SQLException {
        String sql = "SELECT * FROM sections WHERE notebook_id = ? ORDER BY name";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setLong(1, notebookId);
        return stmt.executeQuery();
    }

    public void updateSection(long id, String name) throws SQLException {
        String sql = "UPDATE sections SET name = ?, modified_date = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public void deleteSection(long id) throws SQLException {
        String sql = "DELETE FROM sections WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    // Page operations
    public long createPage(long sectionId, String title) throws SQLException {
        connection.setAutoCommit(false);
        try {
            String sql = "INSERT INTO pages (section_id, title, content) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, sectionId);
                stmt.setString(2, title);
                stmt.setString(3, "");
                stmt.executeUpdate();
            }

            // Get the generated ID using last_insert_rowid()
            long generatedId;
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    generatedId = rs.getLong(1);
                } else {
                    throw new SQLException("Failed to get generated page ID");
                }
            }

            // Update search index within the same transaction
            updateSearchIndex(generatedId, title, "");

            connection.commit();
            return generatedId;

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public ResultSet getPagesBySection(long sectionId) throws SQLException {
        String sql = "SELECT * FROM pages WHERE section_id = ? ORDER BY title";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setLong(1, sectionId);
        return stmt.executeQuery();
    }

    public ResultSet getPage(long pageId) throws SQLException {
        String sql = "SELECT * FROM pages WHERE id = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setLong(1, pageId);
        return stmt.executeQuery();
    }

    public void updatePage(long id, String title, String content) throws SQLException {
        connection.setAutoCommit(false);
        try {
            String sql = "UPDATE pages SET title = ?, content = ?, modified_date = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, title);
                stmt.setString(2, content);
                stmt.setLong(3, id);
                stmt.executeUpdate();
            }

            updateSearchIndex(id, title, content);
            connection.commit();

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public void deletePage(long id) throws SQLException {
        connection.setAutoCommit(false);
        try {
            String sql = "DELETE FROM pages WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, id);
                stmt.executeUpdate();
            }

            // Remove from search index
            String deleteFromIndex = "DELETE FROM pages_fts WHERE page_id = ?";
            try (PreparedStatement indexStmt = connection.prepareStatement(deleteFromIndex)) {
                indexStmt.setLong(1, id);
                indexStmt.executeUpdate();
            }

            connection.commit();

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    // Search operations
    private void updateSearchIndex(long pageId, String title, String content) throws SQLException {
        // Remove existing entry
        String delete = "DELETE FROM pages_fts WHERE page_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(delete)) {
            stmt.setLong(1, pageId);
            stmt.executeUpdate();
        }

        // Add new entry
        String insert = "INSERT INTO pages_fts (title, content, page_id) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setString(1, title);
            stmt.setString(2, content);
            stmt.setLong(3, pageId);
            stmt.executeUpdate();
        }
    }

    public ResultSet searchPages(String query) throws SQLException {
        String sql = """
            SELECT p.*, highlight(pages_fts, 0, '<mark>', '</mark>') as title_highlight,
                   highlight(pages_fts, 1, '<mark>', '</mark>') as content_highlight
            FROM pages_fts 
            JOIN pages p ON p.id = pages_fts.page_id
            WHERE pages_fts MATCH ?
            ORDER BY rank
        """;

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, query);
        return stmt.executeQuery();
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}