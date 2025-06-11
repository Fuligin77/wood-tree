package com.safenote;

import javafx.scene.control.TreeItem;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotebookManager {
    private DatabaseManager dbManager;
    private Map<Long, Notebook> notebooksCache;
    private Map<Long, Section> sectionsCache;
    private Map<Long, NotePage> pagesCache;
    
    public NotebookManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.notebooksCache = new HashMap<>();
        this.sectionsCache = new HashMap<>();
        this.pagesCache = new HashMap<>();
    }
    
    // com.safenote.Notebook operations
    public Notebook createNotebook(String name) {
        try {
            long id = dbManager.createNotebook(name);
            Notebook notebook = new Notebook(id, name, LocalDateTime.now(), LocalDateTime.now());
            notebooksCache.put(id, notebook);
            return notebook;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create notebook: " + e.getMessage(), e);
        }
    }
    
    public List<Notebook> getAllNotebooks() {
        List<Notebook> notebooks = new ArrayList<>();
        try (ResultSet rs = dbManager.getAllNotebooks()) {
            while (rs.next()) {
                long id = rs.getLong("id");
                Notebook notebook = notebooksCache.get(id);
                if (notebook == null) {
                    notebook = new Notebook(
                        id,
                        rs.getString("name"),
                        rs.getTimestamp("created_date").toLocalDateTime(),
                        rs.getTimestamp("modified_date").toLocalDateTime()
                    );
                    notebooksCache.put(id, notebook);
                }
                notebooks.add(notebook);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load notebooks: " + e.getMessage(), e);
        }
        return notebooks;
    }
    
    public void updateNotebook(Notebook notebook) {
        try {
            dbManager.updateNotebook(notebook.getId(), notebook.getName());
            notebook.setModifiedDate(LocalDateTime.now());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update notebook: " + e.getMessage(), e);
        }
    }
    
    public void deleteNotebook(long notebookId) {
        try {
            dbManager.deleteNotebook(notebookId);
            notebooksCache.remove(notebookId);
            
            // Remove from cache all sections and pages that belonged to this notebook
            sectionsCache.entrySet().removeIf(entry -> {
                Section section = entry.getValue();
                if (section.getNotebookId() == notebookId) {
                    // Remove all pages in this section
                    pagesCache.entrySet().removeIf(pageEntry -> 
                        pageEntry.getValue().getSectionId() == section.getId());
                    return true;
                }
                return false;
            });
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete notebook: " + e.getMessage(), e);
        }
    }
    
    // com.safenote.Section operations
    public Section createSection(long notebookId, String name) {
        try {
            long id = dbManager.createSection(notebookId, name);
            Section section = new Section(id, notebookId, name, LocalDateTime.now(), LocalDateTime.now());
            sectionsCache.put(id, section);
            
            // Add to notebook if cached
            Notebook notebook = notebooksCache.get(notebookId);
            if (notebook != null) {
                notebook.addSection(section);
            }
            
            return section;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create section: " + e.getMessage(), e);
        }
    }
    
    public List<Section> getSectionsByNotebook(long notebookId) {
        List<Section> sections = new ArrayList<>();
        try (ResultSet rs = dbManager.getSectionsByNotebook(notebookId)) {
            while (rs.next()) {
                long id = rs.getLong("id");
                Section section = sectionsCache.get(id);
                if (section == null) {
                    section = new Section(
                        id,
                        rs.getLong("notebook_id"),
                        rs.getString("name"),
                        rs.getTimestamp("created_date").toLocalDateTime(),
                        rs.getTimestamp("modified_date").toLocalDateTime()
                    );
                    sectionsCache.put(id, section);
                }
                sections.add(section);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load sections: " + e.getMessage(), e);
        }
        return sections;
    }
    
    public void updateSection(Section section) {
        try {
            dbManager.updateSection(section.getId(), section.getName());
            section.setModifiedDate(LocalDateTime.now());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update section: " + e.getMessage(), e);
        }
    }
    
    public void deleteSection(long sectionId) {
        try {
            dbManager.deleteSection(sectionId);
            Section section = sectionsCache.remove(sectionId);
            
            // Remove from parent notebook if cached
            if (section != null) {
                Notebook notebook = notebooksCache.get(section.getNotebookId());
                if (notebook != null) {
                    notebook.removeSection(section);
                }
            }
            
            // Remove all pages in this section from cache
            pagesCache.entrySet().removeIf(entry -> 
                entry.getValue().getSectionId() == sectionId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete section: " + e.getMessage(), e);
        }
    }
    
    // Page operations
    public NotePage createPage(long sectionId, String title) {
        try {
            long id = dbManager.createPage(sectionId, title);
            NotePage page = new NotePage(id, sectionId, title, "", LocalDateTime.now(), LocalDateTime.now());
            pagesCache.put(id, page);
            
            // Add to section if cached
            Section section = sectionsCache.get(sectionId);
            if (section != null) {
                section.addPage(page);
            }
            
            return page;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create page: " + e.getMessage(), e);
        }
    }
    
    public List<NotePage> getPagesBySection(long sectionId) {
        List<NotePage> pages = new ArrayList<>();
        try (ResultSet rs = dbManager.getPagesBySection(sectionId)) {
            while (rs.next()) {
                long id = rs.getLong("id");
                NotePage page = pagesCache.get(id);
                if (page == null) {
                    page = new NotePage(
                        id,
                        rs.getLong("section_id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getTimestamp("created_date").toLocalDateTime(),
                        rs.getTimestamp("modified_date").toLocalDateTime()
                    );
                    pagesCache.put(id, page);
                }
                pages.add(page);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load pages: " + e.getMessage(), e);
        }
        return pages;
    }
    
    public NotePage getPage(long pageId) {
        NotePage page = pagesCache.get(pageId);
        if (page == null) {
            try (ResultSet rs = dbManager.getPage(pageId)) {
                if (rs.next()) {
                    page = new NotePage(
                        rs.getLong("id"),
                        rs.getLong("section_id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getTimestamp("created_date").toLocalDateTime(),
                        rs.getTimestamp("modified_date").toLocalDateTime()
                    );
                    pagesCache.put(pageId, page);
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load page: " + e.getMessage(), e);
            }
        }
        return page;
    }
    
    public void savePage(NotePage page) {
        try {
            dbManager.updatePage(page.getId(), page.getTitle(), page.getContent());
            page.setModifiedDate(LocalDateTime.now());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save page: " + e.getMessage(), e);
        }
    }
    
    public void deletePage(long pageId) {
        try {
            dbManager.deletePage(pageId);
            NotePage page = pagesCache.remove(pageId);
            
            // Remove from parent section if cached
            if (page != null) {
                Section section = sectionsCache.get(page.getSectionId());
                if (section != null) {
                    section.removePage(page);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete page: " + e.getMessage(), e);
        }
    }
    
    // Search operations
    public List<SearchResult> searchPages(String query) {
        List<SearchResult> results = new ArrayList<>();
        try (ResultSet rs = dbManager.searchPages(query)) {
            while (rs.next()) {
                long pageId = rs.getLong("id");
                NotePage page = getPage(pageId); // This will cache it if not already cached
                
                String titleHighlight = rs.getString("title_highlight");
                String contentHighlight = rs.getString("content_highlight");
                
                SearchResult result = new SearchResult(page, titleHighlight, contentHighlight);
                results.add(result);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search pages: " + e.getMessage(), e);
        }
        return results;
    }
    
    // Tree building for UI
    public TreeItem<NotebookItem> loadNotebookTree() {
        TreeItem<NotebookItem> root = new TreeItem<>();
        
        List<Notebook> notebooks = getAllNotebooks();
        for (Notebook notebook : notebooks) {
            TreeItem<NotebookItem> notebookItem = new TreeItem<>(notebook);
            
            List<Section> sections = getSectionsByNotebook(notebook.getId());
            for (Section section : sections) {
                TreeItem<NotebookItem> sectionItem = new TreeItem<>(section);
                
                List<NotePage> pages = getPagesBySection(section.getId());
                for (NotePage page : pages) {
                    TreeItem<NotebookItem> pageItem = new TreeItem<>(page);
                    sectionItem.getChildren().add(pageItem);
                }
                
                sectionItem.setExpanded(true);
                notebookItem.getChildren().add(sectionItem);
            }
            
            notebookItem.setExpanded(true);
            root.getChildren().add(notebookItem);
        }
        
        return root;
    }
    
    // Cache management
    public void clearCache() {
        notebooksCache.clear();
        sectionsCache.clear();
        pagesCache.clear();
    }
    
    public void refreshCache() {
        clearCache();
        getAllNotebooks(); // This will rebuild the cache
    }
    
    // Statistics
    public int getTotalNotebooks() {
        return getAllNotebooks().size();
    }
    
    public int getTotalSections() {
        int count = 0;
        for (Notebook notebook : getAllNotebooks()) {
            count += getSectionsByNotebook(notebook.getId()).size();
        }
        return count;
    }
    
    public int getTotalPages() {
        int count = 0;
        for (Notebook notebook : getAllNotebooks()) {
            for (Section section : getSectionsByNotebook(notebook.getId())) {
                count += getPagesBySection(section.getId()).size();
            }
        }
        return count;
    }
}