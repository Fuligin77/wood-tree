import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private TextField pizzaAmountField;
    @FXML private Label pizzaAmountError;
    @FXML private RadioButton smallSize;
    @FXML private RadioButton mediumSize;
    @FXML private RadioButton largeSize;
    @FXML private TextField hydrationField;
    @FXML private Label hydrationError;

    @FXML private Label flourResult;
    @FXML private Label waterResult;
    @FXML private Label saltResult;
    @FXML private Label freshYeastResult;
    @FXML private Label dryYeastResult;

    @FXML private Button copyButton;
    @FXML private Button whatsappButton;
    @FXML private Label copyConfirmation;
    @FXML private Label whatsappConfirmation;

    private ToggleGroup sizeGroup;

    // Base recipe ratios (per 1000g flour)
    private static final double BASE_FLOUR = 1000.0;
    private static final double BASE_WATER = 600.0;
    private static final double BASE_SALT = 30.0;
    private static final double BASE_FRESH_YEAST = 2.0;
    private static final double BASE_DRY_YEAST = 0.6;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup toggle group for pizza sizes
        sizeGroup = new ToggleGroup();
        smallSize.setToggleGroup(sizeGroup);
        mediumSize.setToggleGroup(sizeGroup);
        largeSize.setToggleGroup(sizeGroup);

        // Set medium as default
        mediumSize.setSelected(true);

        // Add listeners for real-time calculation
        pizzaAmountField.textProperty().addListener((obs, oldText, newText) -> {
            validatePizzaAmount();
            calculateIngredients();
        });

        hydrationField.textProperty().addListener((obs, oldText, newText) -> {
            validateHydration();
            calculateIngredients();
        });

        sizeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            calculateIngredients();
        });

        // Set default hydration
        hydrationField.setText("60");

        // Initialize confirmation labels
        copyConfirmation.setText("");
        whatsappConfirmation.setText("");

        // Initial calculation
        calculateIngredients();
    }

    private void validatePizzaAmount() {
        String text = pizzaAmountField.getText().trim();
        pizzaAmountError.setText("");

        if (text.isEmpty()) {
            return;
        }

        try {
            int amount = Integer.parseInt(text);
            if (amount < 1) {
                pizzaAmountError.setText("So no Pizza today? :(");
                pizzaAmountError.setStyle("-fx-text-fill: lightgray;");
            } else if (amount > 100) {
                pizzaAmountError.setText("That's too many Pizzas! Get help!");
                pizzaAmountError.setStyle("-fx-text-fill: lightgray;");
            }
        } catch (NumberFormatException e) {
            // Invalid input - will be handled in calculation
        }
    }

    private void validateHydration() {
        String text = hydrationField.getText().trim();
        hydrationError.setText("");

        if (text.isEmpty()) {
            return;
        }

        try {
            double hydration = Double.parseDouble(text);
            if (hydration < 50 || hydration > 90) {
                hydrationError.setText("Please enter a value between 50% and 90%");
                hydrationError.setStyle("-fx-text-fill: red;");
            }
        } catch (NumberFormatException e) {
            hydrationError.setText("Please enter a valid number");
            hydrationError.setStyle("-fx-text-fill: red;");
        }
    }

    private void calculateIngredients() {
        try {
            // Get input values
            String amountText = pizzaAmountField.getText().trim();
            String hydrationText = hydrationField.getText().trim();

            if (amountText.isEmpty() || hydrationText.isEmpty()) {
                clearResults();
                return;
            }

            int pizzaAmount = Integer.parseInt(amountText);
            double hydration = Double.parseDouble(hydrationText);

            // Validate ranges
            if (pizzaAmount < 1 || pizzaAmount > 100 || hydration < 50 || hydration > 90) {
                clearResults();
                return;
            }

            // Get selected pizza size weight
            int doughBallWeight = getDoughBallWeight();

            // Calculate total dough weight needed
            double totalDoughWeight = pizzaAmount * doughBallWeight;

            // Calculate flour amount (flour is the base ingredient)
            // Total dough = flour + water + salt + yeast
            // We need to solve for flour amount considering the hydration percentage
            double waterPercentage = hydration / 100.0;
            double saltPercentage = BASE_SALT / BASE_FLOUR;
            double freshYeastPercentage = BASE_FRESH_YEAST / BASE_FLOUR;
            double dryYeastPercentage = BASE_DRY_YEAST / BASE_FLOUR;

            // flour * (1 + waterPercentage + saltPercentage + yeastPercentage) = totalDoughWeight
            double flourAmount = totalDoughWeight / (1 + waterPercentage + saltPercentage + freshYeastPercentage);

            // Calculate other ingredients based on flour amount
            double waterAmount = flourAmount * waterPercentage;
            double saltAmount = flourAmount * saltPercentage;
            double freshYeastAmount = flourAmount * freshYeastPercentage;
            double dryYeastAmount = flourAmount * dryYeastPercentage;

            // Update results
            flourResult.setText(String.format("%.1f g", flourAmount));
            waterResult.setText(String.format("%.1f ml", waterAmount));
            saltResult.setText(String.format("%.1f g", saltAmount));
            freshYeastResult.setText(String.format("%.1f g", freshYeastAmount));
            dryYeastResult.setText(String.format("%.1f g", dryYeastAmount));

        } catch (NumberFormatException e) {
            clearResults();
        }
    }

    private int getDoughBallWeight() {
        if (smallSize.isSelected()) {
            return 200;
        } else if (largeSize.isSelected()) {
            return 300;
        } else {
            return 250; // medium (default)
        }
    }

    private void clearResults() {
        flourResult.setText("- g");
        waterResult.setText("- ml");
        saltResult.setText("- g");
        freshYeastResult.setText("- g");
        dryYeastResult.setText("- g");
    }

    @FXML
    private void copyRecipeToClipboard() {
        try {
            // Get current recipe values
            String flourText = flourResult.getText();
            String waterText = waterResult.getText();
            String saltText = saltResult.getText();
            String freshYeastText = freshYeastResult.getText();
            String dryYeastText = dryYeastResult.getText();

            // Check if we have valid results to copy
            if (flourText.equals("- g")) {
                return; // No recipe to copy
            }

            // Get input parameters for context
            String pizzaAmount = pizzaAmountField.getText().trim();
            String hydration = hydrationField.getText().trim();
            String sizeText = getSelectedSizeText();

            // Build recipe text
            StringBuilder recipeText = new StringBuilder();
            recipeText.append("Pizza Dough Recipe\n");
            recipeText.append("==================\n\n");
            recipeText.append("Recipe for: ").append(pizzaAmount).append(" ").append(sizeText).append(" pizzas\n");
            recipeText.append("Hydration: ").append(hydration).append("%\n\n");
            recipeText.append("Ingredients:\n");
            recipeText.append("- Flour: ").append(flourText).append("\n");
            recipeText.append("- Water: ").append(waterText).append("\n");
            recipeText.append("- Salt: ").append(saltText).append("\n");
            recipeText.append("- Fresh Yeast: ").append(freshYeastText).append("\n");
            recipeText.append("- Dry Yeast: ").append(dryYeastText).append("\n\n");
            recipeText.append("Tips for preparing the dough:\n");
            recipeText.append("============================\n");
            recipeText.append("Let the dough rise in closed container until double in size. Then divide the dough into portions and form dough balls.\n\n");
            recipeText.append("Afterwards, let it ferment for 16 - 48 Hours in the fridge. Take out of the fridge 30 mins before forming the Pizza.\n\n");
            recipeText.append("It might take a few tries to get it perfect, but you will get the hang of it!");

            // Copy to clipboard
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(recipeText.toString());
            clipboard.setContent(content);

            // Show confirmation
            showCopyConfirmation();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void shareOnWhatsApp() {
        try {
            // Get current recipe values
            String flourText = flourResult.getText();
            String waterText = waterResult.getText();
            String saltText = saltResult.getText();
            String freshYeastText = freshYeastResult.getText();
            String dryYeastText = dryYeastResult.getText();

            // Check if we have valid results to share
            if (flourText.equals("- g")) {
                return; // No recipe to share
            }

            // Get input parameters for context
            String pizzaAmount = pizzaAmountField.getText().trim();
            String hydration = hydrationField.getText().trim();
            String sizeText = getSelectedSizeText();

            // Build WhatsApp message text
            StringBuilder messageText = new StringBuilder();
            messageText.append("Tasty Pizza Dough Recipe\n\n");
            messageText.append("Recipe for: ").append(pizzaAmount).append(" ").append(sizeText).append(" pizzas\n");
            messageText.append("Hydration: ").append(hydration).append("%\n\n");
            messageText.append("Ingredients:\n");
            messageText.append("• Flour: ").append(flourText).append("\n");
            messageText.append("• Water: ").append(waterText).append("\n");
            messageText.append("• Salt: ").append(saltText).append("\n");
            messageText.append("• Fresh Yeast: ").append(freshYeastText).append("\n");
            messageText.append("• Dry Yeast: ").append(dryYeastText).append("\n\n");
            messageText.append("Happy baking! 🍕");

            // Encode the message for URL
            String encodedMessage = URLEncoder.encode(messageText.toString(), StandardCharsets.UTF_8);

            // Try to open WhatsApp
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();

                // First try WhatsApp desktop app
                String whatsappDesktopURI = "whatsapp://send?text=" + encodedMessage;
                boolean desktopOpened = false;

                try {
                    desktop.browse(new URI(whatsappDesktopURI));
                    desktopOpened = true;
                    showWhatsAppConfirmation("Opening WhatsApp Desktop...");
                } catch (IOException | URISyntaxException e) {
                    // Desktop app failed, will try web version
                    desktopOpened = false;
                }

                // If desktop app didn't open, try WhatsApp Web
                if (!desktopOpened) {
                    try {
                        String whatsappWebURI = "https://web.whatsapp.com/send?text=" + encodedMessage;
                        desktop.browse(new URI(whatsappWebURI));
                        showWhatsAppConfirmation("Opening WhatsApp Web...");
                    } catch (IOException | URISyntaxException e) {
                        showWhatsAppConfirmation("Unable to open WhatsApp. Please check your internet connection.");
                    }
                }
            } else {
                showWhatsAppConfirmation("Desktop integration not supported");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showWhatsAppConfirmation("Error sharing to WhatsApp");
        }
    }

    private String getSelectedSizeText() {
        if (smallSize.isSelected()) {
            return "small";
        } else if (largeSize.isSelected()) {
            return "large";
        } else {
            return "medium";
        }
    }

    private void showCopyConfirmation() {
        copyConfirmation.setText("Copied to clipboard");
        copyConfirmation.setStyle("-fx-text-fill: lightgray;");

        // Create timeline to hide the confirmation after 5 seconds
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            copyConfirmation.setText("");
        }));
        timeline.play();
    }

    private void showWhatsAppConfirmation(String message) {
        whatsappConfirmation.setText(message);
        whatsappConfirmation.setStyle("-fx-text-fill: lightgray;");

        // Create timeline to hide the confirmation after 5 seconds
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            whatsappConfirmation.setText("");
        }));
        timeline.play();
    }
}