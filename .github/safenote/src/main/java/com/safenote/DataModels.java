package com.safenote;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Base interface for all notebook items
interface NotebookItem {
    long getId();
    String getName();
    LocalDateTime getCreatedDate();
    LocalDateTime getModifiedDate();
}

// com.safenote.Notebook class
class Notebook implements NotebookItem {
    private long id;
    private String name;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private List<Section> sections;
    
    public Notebook(long id, String name, LocalDateTime createdDate, LocalDateTime modifiedDate) {
        this.id = id;
        this.name = name;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.sections = new ArrayList<>();
    }
    
    @Override
    public long getId() { return id; }
    
    @Override
    public String getName() { return name; }
    
    public void setName(String name) { this.name = name; }
    
    @Override
    public LocalDateTime getCreatedDate() { return createdDate; }
    
    @Override
    public LocalDateTime getModifiedDate() { return modifiedDate; }
    
    public void setModifiedDate(LocalDateTime modifiedDate) { this.modifiedDate = modifiedDate; }
    
    public List<Section> getSections() { return sections; }
    
    public void addSection(Section section) { sections.add(section); }
    
    public void removeSection(Section section) { sections.remove(section); }
    
    @Override
    public String toString() { return name; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Notebook notebook = (Notebook) obj;
        return id == notebook.id;
    }
    
    @Override
    public int hashCode() { return Long.hashCode(id); }
}

// com.safenote.Section class
class Section implements NotebookItem {
    private long id;
    private long notebookId;
    private String name;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private List<NotePage> pages;
    
    public Section(long id, long notebookId, String name, LocalDateTime createdDate, LocalDateTime modifiedDate) {
        this.id = id;
        this.notebookId = notebookId;
        this.name = name;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.pages = new ArrayList<>();
    }
    
    @Override
    public long getId() { return id; }
    
    public long getNotebookId() { return notebookId; }
    
    @Override
    public String getName() { return name; }
    
    public void setName(String name) { this.name = name; }
    
    @Override
    public LocalDateTime getCreatedDate() { return createdDate; }
    
    @Override
    public LocalDateTime getModifiedDate() { return modifiedDate; }
    
    public void setModifiedDate(LocalDateTime modifiedDate) { this.modifiedDate = modifiedDate; }
    
    public List<NotePage> getPages() { return pages; }
    
    public void addPage(NotePage page) { pages.add(page); }
    
    public void removePage(NotePage page) { pages.remove(page); }
    
    @Override
    public String toString() { return name; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Section section = (Section) obj;
        return id == section.id;
    }
    
    @Override
    public int hashCode() { return Long.hashCode(id); }
}

// com.safenote.NotePage class
class NotePage implements NotebookItem {
    private long id;
    private long sectionId;
    private String title;
    private String content;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    
    public NotePage(long id, long sectionId, String title, String content, 
                   LocalDateTime createdDate, LocalDateTime modifiedDate) {
        this.id = id;
        this.sectionId = sectionId;
        this.title = title;
        this.content = content != null ? content : "";
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
    }
    
    @Override
    public long getId() { return id; }
    
    public long getSectionId() { return sectionId; }
    
    @Override
    public String getName() { return title; }
    
    public String getTitle() { return title; }
    
    public void setTitle(String title) { this.title = title; }
    
    public String getContent() { return content; }
    
    public void setContent(String content) { this.content = content != null ? content : ""; }
    
    @Override
    public LocalDateTime getCreatedDate() { return createdDate; }
    
    @Override
    public LocalDateTime getModifiedDate() { return modifiedDate; }
    
    public void setModifiedDate(LocalDateTime modifiedDate) { this.modifiedDate = modifiedDate; }
    
    public boolean isEmpty() {
        return (title == null || title.trim().isEmpty()) && 
               (content == null || content.trim().isEmpty());
    }
    
    @Override
    public String toString() { return title; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        NotePage page = (NotePage) obj;
        return id == page.id;
    }
    
    @Override
    public int hashCode() { return Long.hashCode(id); }
}

// Search result class
class SearchResult {
    private NotePage page;
    private String titleHighlight;
    private String contentHighlight;
    private double relevanceScore;
    
    public SearchResult(NotePage page, String titleHighlight, String contentHighlight) {
        this.page = page;
        this.titleHighlight = titleHighlight;
        this.contentHighlight = contentHighlight;
        this.relevanceScore = 1.0;
    }
    
    public NotePage getPage() { return page; }
    
    public String getTitleHighlight() { return titleHighlight; }
    
    public String getContentHighlight() { return contentHighlight; }
    
    public double getRelevanceScore() { return relevanceScore; }
    
    public void setRelevanceScore(double score) { this.relevanceScore = score; }
    
    @Override
    public String toString() {
        return page.getTitle() + " (Score: " + String.format("%.2f", relevanceScore) + ")";
    }
}