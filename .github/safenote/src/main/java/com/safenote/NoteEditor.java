package com.safenote;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.input.Clipboard;
import javafx.scene.image.Image;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import java.nio.file.Files;
import javafx.embed.swing.SwingFXUtils;

public class NoteEditor {

    private boolean isDarkTheme = false;
    private BorderPane editorPane;
    private TextField titleField;
    private HTMLEditor contentEditor;
    private NotePage currentPage;
    private Runnable onTextChangedCallback;
    private boolean isLoading = false;

    public NoteEditor() {
        initializeEditor();
    }

    public void setDarkTheme(boolean darkTheme) {
        this.isDarkTheme = darkTheme;
        applyEditorTheme();
    }

    private String getDarkThemeCSS() {
        return "body { " +
                "background-color: #2b2b2b !important; " +
                "color: #ffffff !important; " +
                "font-family: -fx-font-family; " +
                "} " +
                "* { " +
                "background-color: #2b2b2b !important; " +
                "color: #ffffff !important; " +
                "} " +
                "a { color: #0078d4 !important; } " +
                "h1, h2, h3, h4, h5, h6 { color: #ffffff !important; } " +
                "p { color: #ffffff !important; } " +
                "div { background-color: #2b2b2b !important; color: #ffffff !important; } " +
                "span { background-color: transparent !important; color: #ffffff !important; }";
    }

    private String getLightThemeCSS() {
        return "body { " +
                "background-color: #ffffff !important; " +
                "color: #323130 !important; " +
                "font-family: -fx-font-family; " +
                "} " +
                "* { " +
                "background-color: #ffffff !important; " +
                "color: #323130 !important; " +
                "} " +
                "a { color: #0078d4 !important; } " +
                "h1, h2, h3, h4, h5, h6 { color: #323130 !important; } " +
                "p { color: #323130 !important; } " +
                "div { background-color: #ffffff !important; color: #323130 !important; } " +
                "span { background-color: transparent !important; color: #323130 !important; }";
    }

    private void applyEditorTheme() {
        Platform.runLater(() -> {
            WebView webView = (WebView) contentEditor.lookup("WebView");
            if (webView != null && webView.getEngine().getDocument() != null) {
                String css = isDarkTheme ? getDarkThemeCSS() : getLightThemeCSS();

                // Inject CSS into the HTML document
                webView.getEngine().executeScript(
                        "var style = document.getElementById('theme-style');" +
                                "if (style) style.remove();" +
                                "var head = document.head || document.getElementsByTagName('head')[0];" +
                                "var style = document.createElement('style');" +
                                "style.id = 'theme-style';" +
                                "style.type = 'text/css';" +
                                "style.innerHTML = '" + css.replace("'", "\\'") + "';" +
                                "head.appendChild(style);"
                );
            }
        });
    }

    private void initializeEditor() {
        editorPane = new BorderPane();
        editorPane.setPadding(new Insets(10));

        // Title section
        VBox titleSection = createTitleSection();
        editorPane.setTop(titleSection);

        // Content editor
        contentEditor = new HTMLEditor();
        contentEditor.setPrefHeight(600);

        // Add change listeners
        setupChangeListeners();

        // Add keyboard shortcuts
        setupKeyboardShortcuts();

        editorPane.setCenter(contentEditor);

        // Initially show empty state
        showEmptyState();
    }

    private VBox createTitleSection() {
        VBox titleSection = new VBox(10);
        titleSection.setPadding(new Insets(0, 0, 10, 0));

        Label titleLabel = new Label("Page Title:");
        titleLabel.setStyle("-fx-font-weight: bold;");

        titleField = new TextField();
        titleField.setPromptText("Enter page title...");
        titleField.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        titleField.textProperty().addListener((obs, oldText, newText) -> {
            if (!isLoading && currentPage != null) {
                currentPage.setTitle(newText);
                notifyTextChanged();
            }
        });

        titleSection.getChildren().addAll(titleLabel, titleField);
        return titleSection;
    }

    private void setupChangeListeners() {
        // Monitor HTML editor content changes
        Platform.runLater(() -> {
            WebView webView = (WebView) contentEditor.lookup("WebView");
            if (webView != null) {
                // Apply theme when document loads
                webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                    if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                        applyEditorTheme();
                    }
                });

                webView.getEngine().documentProperty().addListener((obs, oldDoc, newDoc) -> {
                    if (!isLoading && newDoc != null) {
                        // Apply theme to new document
                        Platform.runLater(() -> {
                            applyEditorTheme();
                            if (currentPage != null) {
                                String content = contentEditor.getHtmlText();
                                currentPage.setContent(content);
                                notifyTextChanged();
                            }
                        });
                    }
                });
            }
        });
    }

    private void setupKeyboardShortcuts() {
        editorPane.setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case B:
                        toggleBold();
                        event.consume();
                        break;
                    case I:
                        toggleItalic();
                        event.consume();
                        break;
                    case U:
                        toggleUnderline();
                        event.consume();
                        break;
                    case S:
                        if (onTextChangedCallback != null) {
                            onTextChangedCallback.run();
                        }
                        event.consume();
                        break;
                }
            }
        });
    }

    public void insertImageFromFile(File imageFile) {
        try {
            // Read the image file and convert to base64
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            String mimeType = Files.probeContentType(imageFile.toPath());
            if (mimeType == null) {
                // Fallback MIME type detection based on file extension
                String fileName = imageFile.getName().toLowerCase();
                if (fileName.endsWith(".png")) mimeType = "image/png";
                else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) mimeType = "image/jpeg";
                else if (fileName.endsWith(".gif")) mimeType = "image/gif";
                else if (fileName.endsWith(".bmp")) mimeType = "image/bmp";
                else mimeType = "image/png"; // default
            }

            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String dataUrl = "data:" + mimeType + ";base64," + base64Image;

            // Insert the image into the editor
            Platform.runLater(() -> {
                WebView webView = (WebView) contentEditor.lookup("WebView");
                if (webView != null) {
                    webView.getEngine().executeScript(
                            "document.execCommand('insertHTML', false, " +
                                    "'<img src=\"" + dataUrl + "\" style=\"max-width: 100%; height: auto;\" />');"
                    );
                }
            });

        } catch (IOException e) {
            System.err.println("Error reading image file: " + e.getMessage());
            // You might want to show an error dialog here
        }
    }

    public void insertImageFromClipboard() {
        Clipboard clipboard = Clipboard.getSystemClipboard();

        if (clipboard.hasImage()) {
            javafx.scene.image.Image fxImage = clipboard.getImage();

            // Convert JavaFX Image to BufferedImage and then to base64
            try {
                BufferedImage bufferedImage = javafx.embed.swing.SwingFXUtils.fromFXImage(fxImage, null);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", baos);
                byte[] imageBytes = baos.toByteArray();

                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                String dataUrl = "data:image/png;base64," + base64Image;

                // Insert the image into the editor
                Platform.runLater(() -> {
                    WebView webView = (WebView) contentEditor.lookup("WebView");
                    if (webView != null) {
                        webView.getEngine().executeScript(
                                "document.execCommand('insertHTML', false, " +
                                        "'<img src=\"" + dataUrl + "\" style=\"max-width: 100%; height: auto;\" />');"
                        );
                    }
                });

            } catch (IOException e) {
                System.err.println("Error processing clipboard image: " + e.getMessage());
            }
        } else {
            // No image in clipboard - you might want to show a message
            System.out.println("No image found in clipboard");
        }
    }

    // Optional: Enhanced insertImage method that accepts base64 data
    public void insertImageFromBase64(String base64Data, String mimeType) {
        String dataUrl = "data:" + mimeType + ";base64," + base64Data;

        Platform.runLater(() -> {
            WebView webView = (WebView) contentEditor.lookup("WebView");
            if (webView != null) {
                webView.getEngine().executeScript(
                        "document.execCommand('insertHTML', false, " +
                                "'<img src=\"" + dataUrl + "\" style=\"max-width: 100%; height: auto;\" />');"
                );
            }
        });
    }

    public BorderPane getEditorPane() {
        return editorPane;
    }

    public void loadNote(NotePage page) {
        isLoading = true;
        this.currentPage = page;

        if (page != null) {
            titleField.setText(page.getTitle());
            contentEditor.setHtmlText(page.getContent());
            showEditorContent();
            // Apply theme after content is loaded
            Platform.runLater(() -> {
                Platform.runLater(() -> applyEditorTheme()); // Double Platform.runLater to ensure DOM is ready
            });
        } else {
            showEmptyState();
        }

        isLoading = false;
    }

    private void showEditorContent() {
        titleField.setDisable(false);
        contentEditor.setDisable(false);
        titleField.setVisible(true);
        contentEditor.setVisible(true);
    }

    private void showEmptyState() {
        titleField.setText("");
        titleField.setPromptText("Select a page to start editing...");
        titleField.setDisable(true);
        contentEditor.setHtmlText("<html><body>Select a page from the notebook tree to start editing.</body></html>");
        contentEditor.setDisable(true);
    }

    public NotePage getCurrentPage() {
        return currentPage;
    }

    public String getContent() {
        return contentEditor.getHtmlText();
    }

    public void setContent(String content) {
        isLoading = true;
        contentEditor.setHtmlText(content != null ? content : "");
        isLoading = false;
    }

    public String getTitle() {
        return titleField.getText();
    }

    public void setTitle(String title) {
        isLoading = true;
        titleField.setText(title != null ? title : "");
        isLoading = false;
    }

    // Formatting methods
    public void toggleBold() {
        executeCommand("bold");
    }

    public void toggleItalic() {
        executeCommand("italic");
    }

    public void toggleUnderline() {
        executeCommand("underline");
    }

    public void toggleHighlight() {
        executeCommand("hiliteColor", "yellow");
    }

    public void setFontSize(String size) {
        executeCommand("fontSize", size);
    }

    public void setFontFamily(String family) {
        executeCommand("fontName", family);
    }

    public void setTextColor(Color color) {
        String colorString = String.format("#%02x%02x%02x",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
        executeCommand("foreColor", colorString);
    }

    public void insertBulletList() {
        executeCommand("insertUnorderedList");
    }

    public void insertNumberedList() {
        executeCommand("insertOrderedList");
    }

    public void alignLeft() {
        executeCommand("justifyLeft");
    }

    public void alignCenter() {
        executeCommand("justifyCenter");
    }

    public void alignRight() {
        executeCommand("justifyRight");
    }

    public void indent() {
        executeCommand("indent");
    }

    public void outdent() {
        executeCommand("outdent");
    }

    private void executeCommand(String command) {
        Platform.runLater(() -> {
            WebView webView = (WebView) contentEditor.lookup("WebView");
            if (webView != null) {
                webView.getEngine().executeScript(
                    "document.execCommand('" + command + "', false, null);"
                );
            }
        });
    }

    private void executeCommand(String command, String value) {
        Platform.runLater(() -> {
            WebView webView = (WebView) contentEditor.lookup("WebView");
            if (webView != null) {
                webView.getEngine().executeScript(
                    "document.execCommand('" + command + "', false, '" + value + "');"
                );
            }
        });
    }

    // Search functionality
    public void findText(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return;
        }

        Platform.runLater(() -> {
            WebView webView = (WebView) contentEditor.lookup("WebView");
            if (webView != null) {
                // Clear previous highlights
                webView.getEngine().executeScript(
                    "if (window.find) {" +
                    "  window.find('" + escapeForJavaScript(searchText) + "', false, false, true, false, true, false);" +
                    "}"
                );
            }
        });
    }

    public void findAndHighlightAll(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return;
        }

        String content = contentEditor.getHtmlText();
        String highlightedContent = highlightSearchTerms(content, searchText);

        if (!highlightedContent.equals(content)) {
            isLoading = true;
            contentEditor.setHtmlText(highlightedContent);
            isLoading = false;
        }
    }

    private String highlightSearchTerms(String content, String searchText) {
        // Remove existing highlights
        content = content.replaceAll("<mark[^>]*>([^<]*)</mark>", "$1");

        // Add new highlights
        Pattern pattern = Pattern.compile(Pattern.quote(searchText), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<mark style='background-color: yellow;'>" + matcher.group() + "</mark>");
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    public void clearHighlights() {
        String content = contentEditor.getHtmlText();
        String clearedContent = content.replaceAll("<mark[^>]*>([^<]*)</mark>", "$1");

        if (!clearedContent.equals(content)) {
            isLoading = true;
            contentEditor.setHtmlText(clearedContent);
            isLoading = false;
        }
    }

    // Text statistics
    public int getWordCount() {
        String text = getPlainText();
        if (text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    public int getCharacterCount() {
        return getPlainText().length();
    }

    public String getPlainText() {
        String html = contentEditor.getHtmlText();
        // Remove HTML tags
        return html.replaceAll("<[^>]+>", "").trim();
    }

    // Utility methods
    private String escapeForJavaScript(String text) {
        return text.replace("\\", "\\\\")
                  .replace("'", "\\'")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r");
    }

    public void setOnTextChanged(Runnable callback) {
        this.onTextChangedCallback = callback;
    }

    private void notifyTextChanged() {
        if (onTextChangedCallback != null) {
            onTextChangedCallback.run();
        }
    }

    // Content manipulation
    public void insertText(String text) {
        executeCommand("insertText", text);
    }

    public void insertImage(String imagePath) {
        executeCommand("insertImage", imagePath);
    }

    public void insertLink(String url, String text) {
        Platform.runLater(() -> {
            WebView webView = (WebView) contentEditor.lookup("WebView");
            if (webView != null) {
                webView.getEngine().executeScript(
                    "document.execCommand('insertHTML', false, " +
                    "'<a href=\"" + escapeForJavaScript(url) + "\">" +
                    escapeForJavaScript(text) + "</a>');"
                );
            }
        });
    }

    public void focus() {
        Platform.runLater(() -> {
            if (!titleField.isDisabled()) {
                contentEditor.requestFocus();
            }
        });
    }
}