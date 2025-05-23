package Planner;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

public class Controller {

    @FXML
    private HBox TitleBar;

    @FXML
    private Label TitleLabel;

    @FXML
    private Button MinimiseButton;

    @FXML
    private Button MaximiseButton;

    @FXML
    private Button CloseButton;

    @FXML
    private TextArea balanceDisplay;

    @FXML
    private TextField amountInput;

    @FXML
    private TextField incomeInput;

    // Input field for withdrawal amount

    private double balance; // Holds the current balance

    private double xOffset = 0; // For draggable title bar
    private double yOffset = 0;
    private Stage stage;
    private Scene scene;

    private static final String BALANCE_FILE_PATH = "Balance.txt"; // Path ot the Balance.txt file

    /**
     * Reads the balance from a file.
     */
    private void loadBalance() {
        try {
            // Check if the balance file exists; if not, create it with a default value
            File file = new File(BALANCE_FILE_PATH);
            if (!file.exists()) {
                file.createNewFile();
                balance = 0.0;
                saveBalance(); // Save the initial balance if file was newly created
            }

            // Read the balance from the file
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();
                balance = Optional.ofNullable(line).map(Double::parseDouble).orElse(0.0);
            }
        } catch (IOException e) {
            System.err.println("Error reading balance: " + e.getMessage());
            balance = 0.0; // Default to 0.0 if an error occurs
        }
    }

    /**
     * Writes the balance to the file.
     */
    private void saveBalance() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(BALANCE_FILE_PATH))) {
            writer.write(String.format("%.2f", balance)); // Write balance with 2 decimal precision
        } catch (IOException e) {
            System.err.println("Error saving balance: " + e.getMessage());
        }
    }

    @FXML
    private void handleDecreaseBalance() { //Class for decreasing the balance
        try {
            double amount = Double.parseDouble(amountInput.getText().trim());

            if (amount < 0) {
                balanceDisplay.setText("Error: Cannot withdraw a negative amount.");
                return;
            }

            if (amount > balance) {
                balanceDisplay.setText("Error: Insufficient balance.");
                return;
            }

            // Update the balance
            balance -= amount;

            // Save the updated balance to the file
            saveBalance();

            // Update the display
            balanceDisplay.setText("Current Balance: " + String.format("%.2f", balance));
            amountInput.clear(); // Clear the input field after processing

        } catch (NumberFormatException e) {
            balanceDisplay.setText("Error: Invalid amount entered.");

        }
    }
    private static final String TYPE_TOTALS_FILE_PATH = "TypeTotals.txt"; // Path for type-specific totals file

    private void updateTypeTotals(String type, double amount) {
        try {
            // Initialize totals
            double workTotal = 0.0;
            double otherTotal = 0.0;

            // Read existing totals if file exists
            if (Files.exists(Paths.get(TYPE_TOTALS_FILE_PATH))) {
                try (BufferedReader reader = new BufferedReader(new FileReader(TYPE_TOTALS_FILE_PATH))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(":");
                        if (parts.length == 2) {
                            if (parts[0].trim().equalsIgnoreCase("work")) {
                                workTotal = Double.parseDouble(parts[1].trim());
                            } else if (parts[0].trim().equalsIgnoreCase("other")) {
                                otherTotal = Double.parseDouble(parts[1].trim());
                            }
                        }
                    }
                }
            }

            // Update the appropriate total
            if (type.equals("work")) {
                workTotal += amount;
            } else if (type.equals("other")) {
                otherTotal += amount;
            }

            // Write the updated totals back to the file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(TYPE_TOTALS_FILE_PATH))) {
                writer.write("work:" + workTotal + System.lineSeparator());
                writer.write("other:" + otherTotal + System.lineSeparator());
            }
        } catch (IOException e) {
            System.err.println("Error updating type totals: " + e.getMessage());
        }
    }

    private double[] readTypeTotals() {
        double workTotal = 0.0;
        double otherTotal = 0.0;

        try {
            if (Files.exists(Paths.get(TYPE_TOTALS_FILE_PATH))) {
                try (BufferedReader reader = new BufferedReader(new FileReader(TYPE_TOTALS_FILE_PATH))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(":");
                        if (parts.length == 2) {
                            if (parts[0].trim().equalsIgnoreCase("work")) {
                                workTotal = Double.parseDouble(parts[1].trim());
                            } else if (parts[0].trim().equalsIgnoreCase("other")) {
                                otherTotal = Double.parseDouble(parts[1].trim());
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading type totals: " + e.getMessage());
        }

        return new double[]{workTotal, otherTotal};
    }

    @FXML
    private void handleIncreaseBalance() {
        try {
            String[] inputParts = incomeInput.getText().trim().split("\\s+"); // Split input by spaces
            if (inputParts.length != 2) {
                balanceDisplay.setText("Error: Enter in format '<amount> <type>' (e.g., '200 work').");
                return;
            }

            double amount = Double.parseDouble(inputParts[0]);
            String type = inputParts[1].toLowerCase();

            if (amount <= 0 || (!type.equals("work") && !type.equals("other"))) {
                balanceDisplay.setText("Error: Invalid amount or type. Type must be 'work' or 'other'.");
                return;
            }

            // Update balance
            balance += amount;

            // Update the type-specific totals in the file
            updateTypeTotals(type, amount);

            // Save balance to file
            saveBalance();

            // Get the updated totals
            double[] totals = readTypeTotals();
            double workTotal = totals[0];
            double otherTotal = totals[1];

            // Update display
            balanceDisplay.setText(
                    "Current Balance: " + String.format("%.2f", balance) + "\n" +
                            "Work Total: " + String.format("%.2f", workTotal) + "\n" +
                            "Other Total: " + String.format("%.2f", otherTotal)
            );
            incomeInput.clear();
        } catch (NumberFormatException e) {
            balanceDisplay.setText("Error: Invalid input. Enter in format '<amount> <type>'.");
        }
    }




    // Initializes the controller.

    @FXML
    private void initialize() {
        loadBalance(); // Load the balance on startup
        if (balanceDisplay != null) {
            balanceDisplay.setText("Current Balance: " + String.format("%.2f", balance));
        }

        // Make the title bar draggable
        TitleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        TitleBar.setOnMouseDragged(event -> {
            Stage currentStage = (Stage) TitleBar.getScene().getWindow();
            currentStage.setX(event.getScreenX() - xOffset);
            currentStage.setY(event.getScreenY() - yOffset);
        });
    }

    // Switches to the home screen (for the back buttons)
    public void switchToScene1(ActionEvent event) throws IOException {
        loadBalance(); // Load balance before switching
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Financial Planner Home.fxml"));
        Parent root = loader.load();

        // Get the controller of the Financial Planner Home scene
        Controller homeController = loader.getController();

        // Pass the current balance to the home controller
        homeController.setBalance(balance);

        // Switch to the new scene
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    //Sets the balance
    public void setBalance(double newBalance) {
        this.balance = newBalance;

        // Update the TextArea display
        if (balanceDisplay != null) {
            balanceDisplay.setText("Current Balance: " + String.format("%.2f", balance));
        }
    }

   //Switches to the Payments.fxml file
    public void switchToScene2(ActionEvent event) throws IOException {
        loadBalance(); // Load balance before switching
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Payments.fxml"));
        Parent root = loader.load();

        // Get the controller of the Payments scene
        Controller paymentsController = loader.getController();

        // Pass the current balance to the Payments controller
        paymentsController.setBalance(balance);

        // Switch scenes
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
//Switches to the Payment Categories Screen
    public void switchToScene3(ActionEvent event) throws IOException {
        loadBalance(); // Load balance before switching
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/PaymentCategories.fxml"));
        Parent root = loader.load();

        // Get the controller of the Payments scene
        Controller paymentsController = loader.getController();

        // Pass the current balance to the Payments controller
        paymentsController.setBalance(balance);

        // Switch scenes
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
    public void switchToScene4(ActionEvent event) throws IOException {
        loadBalance(); // Load balance before switching
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Income.fxml"));
        Parent root = loader.load();

        // Get the controller of the Payments scene
        Controller paymentsController = loader.getController();

        // Pass the current balance to the Payments controller
        paymentsController.setBalance(balance);

        // Switch scenes
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Minimize the current window.
     */
    @FXML
    private void handleMinimize() {
        stage = (Stage) MinimiseButton.getScene().getWindow();
        stage.setIconified(true);
    }

    /**
     * Maximize or restore the current window.
     */
    @FXML
    private void handleMaximize() {
        stage = (Stage) MaximiseButton.getScene().getWindow();

        // Toggle maximize state
        boolean isMaximized = stage.isMaximized();
        stage.setMaximized(!isMaximized);

        // Get the root node of the scene
        Parent root = stage.getScene().getRoot();


    }

    /**
     * Close the current window.
     */
    @FXML
    private void handleClose() {
        stage = (Stage) CloseButton.getScene().getWindow();
        stage.close();
    }
}
