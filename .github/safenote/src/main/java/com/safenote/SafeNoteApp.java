package com.safenote;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import java.util.Timer;
import java.util.TimerTask;
import javafx.stage.FileChooser;
import java.io.File;

public class SafeNoteApp extends Application {
    private DatabaseManager dbManager;
    private NotebookManager notebookManager;
    private NoteEditor noteEditor;
    private TreeView<NotebookItem> notebookTree;
    private TextField searchField;
    private boolean isDarkTheme = false;
    private Timer autoSaveTimer;
    private boolean hasUnsavedChanges = false;
    private Scene scene;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize database and managers
            dbManager = new DatabaseManager();
            notebookManager = new NotebookManager(dbManager);
            noteEditor = new NoteEditor();

            // Setup auto-save timer
            setupAutoSave();

            // Create UI
            BorderPane root = createMainLayout();
            scene = new Scene(root, 1200, 800);

            // Apply default theme
            applyTheme();

            // Setup stage
            primaryStage.setTitle("SafeNote");
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(e -> {
                saveCurrentNote();
                dbManager.close();
                if (autoSaveTimer != null) {
                    autoSaveTimer.cancel();
                }
                Platform.exit();
            });

            // Load notebooks and notes
            loadNotebooks();

            primaryStage.show();

        } catch (Exception e) {
            showError("Failed to start SafeNote", e.getMessage());
        }
    }

    private BorderPane createMainLayout() {
        BorderPane root = new BorderPane();

        // Create menu bar
        MenuBar menuBar = createMenuBar();
        root.setTop(menuBar);

        // Create toolbar
        ToolBar toolBar = createToolBar();
        VBox topContainer = new VBox(menuBar, toolBar);
        root.setTop(topContainer);

        // Create left sidebar with notebooks
        VBox leftSidebar = createLeftSidebar();
        root.setLeft(leftSidebar);

        // Create main content area
        root.setCenter(noteEditor.getEditorPane());

        return root;
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem newNotebook = new MenuItem("New com.safenote.Notebook");
        MenuItem newSection = new MenuItem("New com.safenote.Section");
        MenuItem newPage = new MenuItem("New Page");
        MenuItem exit = new MenuItem("Exit");

        newNotebook.setOnAction(e -> createNewNotebook());
        newSection.setOnAction(e -> createNewSection());
        newPage.setOnAction(e -> createNewPage());
        exit.setOnAction(e -> Platform.exit());

        fileMenu.getItems().addAll(newNotebook, newSection, newPage,
                                  new SeparatorMenuItem(), exit);

        // Edit menu
        Menu editMenu = new Menu("Edit");
        MenuItem cut = new MenuItem("Cut");
        MenuItem copy = new MenuItem("Copy");
        MenuItem paste = new MenuItem("Paste");
        MenuItem find = new MenuItem("Find");

        cut.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN));
        copy.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
        paste.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN));
        find.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN));

        find.setOnAction(e -> showSearchDialog());

        editMenu.getItems().addAll(cut, copy, paste, new SeparatorMenuItem(), find);

        // View menu
        Menu viewMenu = new Menu("View");
        MenuItem toggleTheme = new MenuItem("Toggle Dark Theme");
        toggleTheme.setOnAction(e -> toggleTheme());
        viewMenu.getItems().add(toggleTheme);

        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu);
        return menuBar;
    }

    private ToolBar createToolBar() {
        ToolBar toolBar = new ToolBar();

        Button newNotebookButton = new Button("New Notebook");
        Button newPageBtn = new Button("New Page");
        Button newSectionButton = new Button("New Section");

        // Add image insertion buttons
        Button insertImageBtn = new Button("Insert Image");
        Button pasteImageBtn = new Button("Paste Image");

        newPageBtn.setOnAction(e -> createNewPage());
        newNotebookButton.setOnAction(e -> createNewNotebook());
        newSectionButton.setOnAction(e -> createNewSection());
        insertImageBtn.setOnAction(e -> insertImageFromFile());
        pasteImageBtn.setOnAction(e -> insertImageFromClipboard());

        toolBar.getItems().addAll(newNotebookButton, newSectionButton,
                newPageBtn, new Separator(), insertImageBtn, pasteImageBtn);

        return toolBar;
    }

    private void insertImageFromFile() {
        if (noteEditor.getCurrentPage() == null) {
            showInfo("Please select a page first");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("PNG Files", "*.png"),
                new FileChooser.ExtensionFilter("JPEG Files", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("GIF Files", "*.gif"),
                new FileChooser.ExtensionFilter("BMP Files", "*.bmp")
        );

        File selectedFile = fileChooser.showOpenDialog(scene.getWindow());
        if (selectedFile != null) {
            noteEditor.insertImageFromFile(selectedFile);
        }
    }

    private void insertImageFromClipboard() {
        if (noteEditor.getCurrentPage() == null) {
            showInfo("Please select a page first");
            return;
        }

        noteEditor.insertImageFromClipboard();
    }

    private VBox createLeftSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(10));
        sidebar.setPrefWidth(250);

        // Search box
        searchField = new TextField();
        searchField.setPromptText("Search notes...");
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                performSearch();
            }
        });

        Button searchBtn = new Button("Search");
        searchBtn.setOnAction(e -> performSearch());

        HBox searchBox = new HBox(5, searchField, searchBtn);

        // com.safenote.Notebook tree
        notebookTree = new TreeView<>();
        notebookTree.setShowRoot(false);
        notebookTree.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null && newSelection.getValue() instanceof NotePage) {
                    loadNote((NotePage) newSelection.getValue());
                }
            });

        VBox.setVgrow(notebookTree, Priority.ALWAYS);

        sidebar.getChildren().addAll(searchBox, notebookTree);
        return sidebar;
    }

    private void setupAutoSave() {
        autoSaveTimer = new Timer(true);
        autoSaveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (hasUnsavedChanges) {
                    Platform.runLater(() -> saveCurrentNote());
                }
            }
        }, 10000, 10000); // Save every 10 seconds
    }

    private void loadNotebooks() {
        Task<TreeItem<NotebookItem>> loadTask = new Task<TreeItem<NotebookItem>>() {
            @Override
            protected TreeItem<NotebookItem> call() throws Exception {
                return notebookManager.loadNotebookTree();
            }
        };

        loadTask.setOnSucceeded(e -> {
            notebookTree.setRoot(loadTask.getValue());
        });

        loadTask.setOnFailed(e -> {
            showError("Failed to load notebooks", loadTask.getException().getMessage());
        });

        new Thread(loadTask).start();
    }

    private void createNewNotebook() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New com.safenote.Notebook");
        dialog.setHeaderText("Create New com.safenote.Notebook");
        dialog.setContentText("com.safenote.Notebook name:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                notebookManager.createNotebook(name.trim());
                loadNotebooks();
            }
        });
    }

    private void createNewSection() {
        TreeItem<NotebookItem> selected = notebookTree.getSelectionModel().getSelectedItem();
        if (selected == null || !(selected.getValue() instanceof Notebook)) {
            showInfo("Please select a notebook first");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New com.safenote.Section");
        dialog.setHeaderText("Create New com.safenote.Section");
        dialog.setContentText("com.safenote.Section name:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                Notebook notebook = (Notebook) selected.getValue();
                notebookManager.createSection(notebook.getId(), name.trim());
                loadNotebooks();
            }
        });
    }

    private void createNewPage() {
        TreeItem<NotebookItem> selected = notebookTree.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Please select a section first");
            return;
        }

        // Find parent section
        Section section;
        if (selected.getValue() instanceof Section) {
            section = (Section) selected.getValue();
        } else if (selected.getValue() instanceof NotePage && selected.getParent() != null) {
            section = (Section) selected.getParent().getValue();
        } else {
            section = null;
        }

        if (section == null) {
            showInfo("Please select a section first");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Page");
        dialog.setHeaderText("Create New Page");
        dialog.setContentText("Page title:");

        dialog.showAndWait().ifPresent(title -> {
            if (!title.trim().isEmpty()) {
                notebookManager.createPage(section.getId(), title.trim());
                loadNotebooks();
            }
        });
    }

    private void loadNote(NotePage page) {
        saveCurrentNote(); // Save previous note
        noteEditor.loadNote(page);
        noteEditor.setOnTextChanged(() -> hasUnsavedChanges = true);
    }

    private void saveCurrentNote() {
        if (noteEditor.getCurrentPage() != null && hasUnsavedChanges) {
            NotePage page = noteEditor.getCurrentPage();
            page.setContent(noteEditor.getContent());
            notebookManager.savePage(page);
            hasUnsavedChanges = false;
        }
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        if (!query.isEmpty()) {
            // Implement search functionality
            showInfo("Search functionality will be implemented");
        }
    }

    private void showSearchDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Find");
        dialog.setHeaderText("Find text in current note");

        TextField findField = new TextField();
        findField.setPromptText("Enter text to find...");

        dialog.getDialogPane().setContent(findField);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return findField.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(searchText -> {
            if (!searchText.trim().isEmpty()) {
                noteEditor.findText(searchText);
            }
        });
    }

    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        applyTheme();

        // Apply theme to the note editor as well
        if (noteEditor != null) {
            noteEditor.setDarkTheme(isDarkTheme);
        }
    }

    private void applyTheme() {
        scene.getStylesheets().clear();
        try {
            if (isDarkTheme) {
                scene.getStylesheets().add(getClass().getResource("/styles/dark-theme.css").toExternalForm());
            } else {
                scene.getStylesheets().add(getClass().getResource("/styles/light-theme.css").toExternalForm());
            }

            // Apply theme to note editor
            if (noteEditor != null) {
                noteEditor.setDarkTheme(isDarkTheme);
            }
        } catch (Exception e) {
            System.out.println("Could not load CSS files: " + e.getMessage());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}