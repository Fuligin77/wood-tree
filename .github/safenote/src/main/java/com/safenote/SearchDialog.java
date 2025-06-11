package com.safenote;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class SearchDialog {
    private Stage stage;
    private TextField searchField;
    private ListView<SearchResult> resultsListView;
    private NotebookManager notebookManager;
    private NoteEditor noteEditor;
    private Label statusLabel;
    
    public SearchDialog(Stage parent, NotebookManager notebookManager, NoteEditor noteEditor) {
        this.notebookManager = notebookManager;
        this.noteEditor = noteEditor;
        initializeDialog(parent);
    }
    
    private void initializeDialog(Stage parent) {
        stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parent);
        stage.setTitle("Search Notes");
        stage.setResizable(true);
        stage.setWidth(600);
        stage.setHeight(500);
        
        VBox root = createContent();
        Scene scene = new Scene(root);
        
        // Apply same theme as parent
        if (parent.getScene().getStylesheets().size() > 0) {
            scene.getStylesheets().addAll(parent.getScene().getStylesheets());
        }
        
        stage.setScene(scene);
        setupEventHandlers();
    }
    
    private VBox createContent() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        
        // Search section
        VBox searchSection = createSearchSection();
        
        // Results section
        VBox resultsSection = createResultsSection();
        
        // Status section
        HBox statusSection = createStatusSection();
        
        // Button section
        HBox buttonSection = createButtonSection();
        
        VBox.setVgrow(resultsSection, Priority.ALWAYS);
        
        root.getChildren().addAll(searchSection, resultsSection, statusSection, buttonSection);
        return root;
    }
    
    private VBox createSearchSection() {
        VBox searchSection = new VBox(8);
        
        Label searchLabel = new Label("Search for:");
        searchLabel.setStyle("-fx-font-weight: bold;");
        
        HBox searchBox = new HBox(8);
        searchField = new TextField();
        searchField.setPromptText("Enter search terms...");
        searchField.setPrefWidth(400);
        
        Button searchButton = new Button("Search");
        searchButton.setDefaultButton(true);
        searchButton.setOnAction(e -> performSearch());
        
        Button clearButton = new Button("Clear");
        clearButton.setOnAction(e -> clearSearch());
        
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchBox.getChildren().addAll(searchField, searchButton, clearButton);
        
        searchSection.getChildren().addAll(searchLabel, searchBox);
        return searchSection;
    }
    
    private VBox createResultsSection() {
        VBox resultsSection = new VBox(8);
        
        Label resultsLabel = new Label("Search Results:");
        resultsLabel.setStyle("-fx-font-weight: bold;");
        
        resultsListView = new ListView<>();
        resultsListView.setPrefHeight(300);
        resultsListView.setCellFactory(listView -> new SearchResultCell());
        
        // Handle double-click to open note
        resultsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                openSelectedResult();
            }
        });
        
        resultsSection.getChildren().addAll(resultsLabel, resultsListView);
        return resultsSection;
    }
    
    private HBox createStatusSection() {
        HBox statusSection = new HBox();
        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #666666;");
        statusSection.getChildren().add(statusLabel);
        return statusSection;
    }
    
    private HBox createButtonSection() {
        HBox buttonSection = new HBox(8);
        buttonSection.setStyle("-fx-alignment: center-right;");
        
        Button openButton = new Button("Open");
        openButton.setOnAction(e -> openSelectedResult());
        
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> stage.close());
        
        buttonSection.getChildren().addAll(openButton, closeButton);
        return buttonSection;
    }
    
    private void setupEventHandlers() {
        // Search on Enter key
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                performSearch();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                stage.close();
            }
        });
        
        // Navigate results with arrow keys
        resultsListView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                openSelectedResult();
            }
        });
        
        // Focus search field when dialog opens
        stage.setOnShown(e -> searchField.requestFocus());
    }
    
    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            statusLabel.setText("Please enter search terms");
            return;
        }
        
        try {
            List<SearchResult> results = notebookManager.searchPages(query);
            resultsListView.getItems().clear();
            resultsListView.getItems().addAll(results);
            
            if (results.isEmpty()) {
                statusLabel.setText("No results found for \"" + query + "\"");
            } else {
                statusLabel.setText("Found " + results.size() + " result(s) for \"" + query + "\"");
            }
        } catch (Exception e) {
            statusLabel.setText("Search error: " + e.getMessage());
        }
    }
    
    private void clearSearch() {
        searchField.clear();
        resultsListView.getItems().clear();
        statusLabel.setText("");
        searchField.requestFocus();
    }
    
    private void openSelectedResult() {
        SearchResult selected = resultsListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            noteEditor.loadNote(selected.getPage());
            
            // Highlight the search terms in the editor
            String searchTerms = searchField.getText().trim();
            if (!searchTerms.isEmpty()) {
                noteEditor.findAndHighlightAll(searchTerms);
            }
            
            stage.close();
        }
    }
    
    public void show() {
        stage.show();
    }
    
    public void showAndWait() {
        stage.showAndWait();
    }
    
    // Custom cell for displaying search results
    private static class SearchResultCell extends ListCell<SearchResult> {
        @Override
        protected void updateItem(SearchResult result, boolean empty) {
            super.updateItem(result, empty);
            
            if (empty || result == null) {
                setText(null);
                setGraphic(null);
            } else {
                VBox content = new VBox(4);
                content.setPadding(new Insets(8));
                
                // Page title
                Label titleLabel = new Label(result.getPage().getTitle());
                titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                
                // Content preview with highlights
                String contentPreview = result.getContentHighlight();
                if (contentPreview == null || contentPreview.trim().isEmpty()) {
                    contentPreview = getPlainTextPreview(result.getPage().getContent(), 100);
                }
                
                Label contentLabel = new Label(stripHtmlTags(contentPreview));
                contentLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px;");
                contentLabel.setWrapText(true);
                contentLabel.setMaxWidth(500);
                
                // Modified date
                Label dateLabel = new Label("Modified: " + 
                    result.getPage().getModifiedDate().toLocalDate().toString());
                dateLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 11px;");
                
                content.getChildren().addAll(titleLabel, contentLabel, dateLabel);
                setGraphic(content);
            }
        }
        
        private String getPlainTextPreview(String htmlContent, int maxLength) {
            if (htmlContent == null) return "";
            
            // Remove HTML tags
            String plainText = htmlContent.replaceAll("<[^>]+>", "").trim();
            
            if (plainText.length() <= maxLength) {
                return plainText;
            }
            
            return plainText.substring(0, maxLength) + "...";
        }
        
        private String stripHtmlTags(String html) {
            if (html == null) return "";
            return html.replaceAll("<[^>]+>", "");
        }
    }
}