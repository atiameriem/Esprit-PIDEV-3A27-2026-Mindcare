package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import models.LocalRelaxation;
import services.LocalRelaxationService;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;

public class LocalFormController {

    @FXML private Label titleLabel;
    @FXML private TextField nomField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField capaciteField;
    @FXML private TextArea equipementsArea;
    @FXML private TextField etageField;
    @FXML private TextField dureeField;
    @FXML private TextField tarifField;
    @FXML private ComboBox<String> etatCombo;
    @FXML private CheckBox disponibleCheck;
    @FXML private Label errorLabel;

    // Image
    @FXML private Button btnChooseImage;
    @FXML private Label imageNameLabel;
    @FXML private ImageView imagePreview;

    private final LocalRelaxationService localService = new LocalRelaxationService();

    private int editingId = 0;
    private Runnable onDone;

    private File selectedImageFile;            // image choisie dans le PC
    private String currentImageName = "default.png"; // image actuelle (en édition)

    public void setOnDone(Runnable onDone) {
        this.onDone = onDone;
    }

    @FXML
    private void initialize() {
        if (typeCombo != null && typeCombo.getItems().isEmpty()) {
            typeCombo.getItems().setAll("MEDITATION", "THERAPIE_GROUPE", "YOGA", "RESPIRATION", "AUTRE");
            typeCombo.getSelectionModel().selectFirst();
        }

        if (etatCombo != null && etatCombo.getItems().isEmpty()) {
            etatCombo.getItems().setAll("ACTIF", "MAINTENANCE", "HORS_SERVICE");
            etatCombo.getSelectionModel().selectFirst();
        }

        if (disponibleCheck != null) {
            disponibleCheck.setSelected(true);
        }

        // preview image par défaut
        if (imagePreview != null) {
            var url = getClass().getResource("/images/default.png");
            if (url != null) imagePreview.setImage(new Image(url.toExternalForm()));
        }
        if (imageNameLabel != null) {
            imageNameLabel.setText("");
        }
    }

    public void setEditingId(int idLocal) {
        this.editingId = idLocal;

        if (idLocal > 0) {
            if (titleLabel != null) titleLabel.setText("✏️ Modifier un local");

            try {
                LocalRelaxation l = localService.getById(idLocal);
                if (l != null) {
                    nomField.setText(nvl(l.getNom()));
                    descriptionArea.setText(nvl(l.getDescription()));
                    typeCombo.getSelectionModel().select(nvl(l.getType()));
                    capaciteField.setText(String.valueOf(l.getCapacite()));
                    equipementsArea.setText(nvl(l.getEquipements()));
                    etageField.setText(String.valueOf(l.getEtage()));
                    dureeField.setText(String.valueOf(l.getDureeMaxSession()));
                    tarifField.setText(l.getTarifHoraire() == null ? "0" : l.getTarifHoraire().toPlainString());
                    etatCombo.getSelectionModel().select(nvl(l.getEtat()));
                    disponibleCheck.setSelected(l.isDisponible());

                    currentImageName = (l.getImage() == null || l.getImage().isBlank()) ? "default.png" : l.getImage();
                    selectedImageFile = null; // pas d'image choisie tant que l'utilisateur ne change pas

                    if (imageNameLabel != null) imageNameLabel.setText(currentImageName);

                    // Preview depuis uploads si existe, sinon default
                    if (imagePreview != null) {
                        File f = new File("uploads/locaux/" + currentImageName);
                        if (f.exists()) imagePreview.setImage(new Image(f.toURI().toString()));
                        else {
                            var url = getClass().getResource("/images/default.png");
                            if (url != null) imagePreview.setImage(new Image(url.toExternalForm()));
                        }
                    }
                }
            } catch (SQLException ex) {
                if (errorLabel != null) errorLabel.setText("Erreur BD: " + ex.getMessage());
            }

        } else {
            if (titleLabel != null) titleLabel.setText("➕ Ajouter un local");

            editingId = 0;
            selectedImageFile = null;
            currentImageName = "default.png";

            if (nomField != null) nomField.clear();
            if (descriptionArea != null) descriptionArea.clear();
            if (capaciteField != null) capaciteField.clear();
            if (equipementsArea != null) equipementsArea.clear();
            if (etageField != null) etageField.clear();
            if (dureeField != null) dureeField.clear();
            if (tarifField != null) tarifField.clear();

            if (typeCombo != null) typeCombo.getSelectionModel().selectFirst();
            if (etatCombo != null) etatCombo.getSelectionModel().selectFirst();
            if (disponibleCheck != null) disponibleCheck.setSelected(true);

            if (imageNameLabel != null) imageNameLabel.setText("");
            if (imagePreview != null) {
                var url = getClass().getResource("/images/default.png");
                if (url != null) imagePreview.setImage(new Image(url.toExternalForm()));
            }
        }
    }

    // ✅ Bouton Choisir une image
    @FXML
    private void chooseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File f = fc.showOpenDialog(btnChooseImage.getScene().getWindow());
        if (f != null) {
            selectedImageFile = f;
            currentImageName = f.getName();

            if (imageNameLabel != null) imageNameLabel.setText(f.getName());
            if (imagePreview != null) imagePreview.setImage(new Image(f.toURI().toString()));
            if (errorLabel != null) errorLabel.setText("");
        }
    }

    @FXML
    private void save() {
        if (errorLabel != null) errorLabel.setText("");

        try {
            String nom = txt(nomField);
            if (nom.isEmpty()) throw new IllegalArgumentException("Le nom est obligatoire.");

            int capacite = parseInt(txt(capaciteField), "Capacité invalide.");
            int etage = parseInt(txt(etageField), "Étage invalide.");
            int duree = parseInt(txt(dureeField), "Durée invalide.");
            BigDecimal tarif = parseBigDecimal(txt(tarifField), "Tarif invalide.");

            String type = (typeCombo == null) ? null : typeCombo.getValue();
            String etat = (etatCombo == null) ? null : etatCombo.getValue();

            if (type == null || type.isBlank()) throw new IllegalArgumentException("Veuillez choisir un type.");
            if (etat == null || etat.isBlank()) throw new IllegalArgumentException("Veuillez choisir un état.");

            boolean isEdit = (editingId > 0);

            // ✅ image obligatoire à l'ajout
            if (!isEdit && selectedImageFile == null) {
                throw new IllegalArgumentException("Veuillez choisir une image pour le local.");
            }

            LocalRelaxation l = new LocalRelaxation();
            l.setIdLocal(editingId);
            l.setNom(nom);
            l.setDescription(txt(descriptionArea));
            l.setType(type);
            l.setCapacite(capacite);
            l.setEquipements(txt(equipementsArea));
            l.setEtage(etage);
            l.setDureeMaxSession(duree);
            l.setTarifHoraire(tarif);
            l.setEtat(etat);
            l.setDisponible(disponibleCheck != null && disponibleCheck.isSelected());

            // ✅ Copier l'image si l'utilisateur en a choisi une
            if (selectedImageFile != null) {
                ensureUploadsFolder();

                String original = selectedImageFile.getName();
                String ext = "";
                int dot = original.lastIndexOf('.');
                if (dot >= 0) ext = original.substring(dot);

                String uniqueName = "local_" + System.currentTimeMillis() + ext;
                Path dest = Path.of("uploads", "locaux", uniqueName);

                try {
                    Files.copy(selectedImageFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ioe) {
                    throw new IllegalArgumentException("Erreur copie image: " + ioe.getMessage());
                }

                l.setImage(uniqueName);
                currentImageName = uniqueName;
            } else {
                // ✅ En édition sans changement d'image -> garder l'ancienne
                l.setImage(currentImageName);
            }

            if (isEdit) localService.update(l);
            else localService.add(l);

            showPopup(isEdit ? "Modification avec succès" : "Ajout avec succès");
            if (onDone != null) onDone.run();

        } catch (IllegalArgumentException ex) {
            if (errorLabel != null) errorLabel.setText(ex.getMessage());
        } catch (SQLException ex) {
            if (errorLabel != null) errorLabel.setText("Erreur BD: " + ex.getMessage());
        }
    }

    @FXML
    private void cancel() {
        if (onDone != null) onDone.run();
    }

    private void showPopup(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succès");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void ensureUploadsFolder() {
        try {
            Files.createDirectories(Path.of("uploads", "locaux"));
        } catch (IOException ignored) { }
    }

    private String txt(TextInputControl c) {
        return c == null || c.getText() == null ? "" : c.getText().trim();
    }

    // ✅ manquant sinon compilation
    private String txt(TextArea a) {
        return a == null || a.getText() == null ? "" : a.getText().trim();
    }

    private String nvl(String s) { return s == null ? "" : s; }

    private int parseInt(String s, String msg) {
        try { return Integer.parseInt(s); }
        catch (Exception e) { throw new IllegalArgumentException(msg); }
    }

    private BigDecimal parseBigDecimal(String s, String msg) {
        try { return new BigDecimal(s); }
        catch (Exception e) { throw new IllegalArgumentException(msg); }
    }
}