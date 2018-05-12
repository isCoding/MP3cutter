package cn.codeh.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;

import java.io.File;
import java.util.concurrent.ExecutorService;

import static cn.codeh.cutter.Mp3Cutter.generateMp3ByTimeAndCBR;

public class MainController {

    @FXML
    private Slider timeSlider;

    private MediaPlayer mediaPlayer;

    public Stage primaryStage;
    public ExecutorService executorService;

    private File sourceFile;
    private File targetFile;
    private static String sourceFilePath;
    private static String targetFilePath;

    private long cutStartTime;
    private long cutEndTime;
    private long sourceTotalTime;

    @FXML
    private Text sourceCurrentTimeText;
    @FXML
    private Text sourceTotalTimeText;
    @FXML
    private TextField cutStartTimeTF;
    @FXML
    private TextField cutEndTimeTF;
    @FXML
    private Text alertZone;
    @FXML
    private Text outputPathText;
    @FXML
    private Button playBtn;
    @FXML
    private Text sourceFilePathText;

    @FXML
    public void loadSourceSong() {
        initCutTimeValidator();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Source File.");
//        fileChooser.setInitialDirectory(new File("E:\\ku\\music"));
        sourceFile = fileChooser.showOpenDialog(primaryStage);
        if (sourceFileNotChoose()) {
            return;
        }
        stopSourceSong();
        sourceFilePath = sourceFile.getPath();
        sourceFilePathText.setText(sourceFilePath);
    }

    @FXML
    public void chooseOutputDict() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("choose Output File");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Mp3 Files", "*.mp3"));
        targetFile = fileChooser.showSaveDialog(primaryStage);
        if (targetFileNotChoose()) {
            return;
        }
        targetFilePath = targetFile.getPath();
        updateOutputDisplay("output path is:" + targetFilePath);
    }

    @FXML
    public void getStartTime4Cut() {
        cutStartTime = (long) mediaPlayer.getCurrentTime().toMillis();
        cutStartTimeTF.setText(String.valueOf(cutStartTime));
    }

    @FXML
    public void getEndTime4Cut() {
        cutEndTime = (long) mediaPlayer.getCurrentTime().toMillis();
        cutEndTimeTF.setText(String.valueOf(cutEndTime));
    }

    @FXML
    public void playSouceSong() {
        if (sourceFileNotChoose()) {
            return;
        }
        stopSourceSong();
        initMediaPlayer();
        executorService.submit(() -> mediaPlayer.play());
    }

    private void initMediaPlayer(){
        playBtn.setText("replay");
        Media media = new Media(sourceFile.toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.currentTimeProperty().addListener((observableValue, oldDuration, newDuration) -> {
            Duration duration = mediaPlayer.getTotalDuration();
            double currentTime = mediaPlayer.getCurrentTime().toMillis();
            //update currentTime text display
            sourceCurrentTimeText.setText(String.valueOf((long) currentTime));
            Platform.runLater(() -> {
                timeSlider.setDisable(duration.isUnknown());
                if (!timeSlider.isDisabled()
                        && duration.greaterThan(Duration.ZERO)
                        && !timeSlider.isValueChanging()) {
                    timeSlider.setValue(currentTime);
                }
            });
        });
        mediaPlayer.setOnEndOfMedia(this::stopSourceSong);
        mediaPlayer.setOnReady(() -> {
            sourceTotalTime = (long) mediaPlayer.getTotalDuration().toMillis();
            sourceTotalTimeText.setText(" / " + String.valueOf(sourceTotalTime));
            timeSlider.setMax(mediaPlayer.getTotalDuration().toMillis());
        });
        timeSlider.valueProperty().addListener(observable -> {
            if (timeSlider.isValueChanging()) {
                mediaPlayer.seek(new Duration(timeSlider.getValue()));
            }
        });
    }

    @FXML
    public void stopSourceSong() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            playBtn.setText("play");
        }
    }

    @FXML
    public void cut() throws Exception {
        if (sourceFileNotChoose() || targetFileNotChoose()) {
            return;
        }
        String validateResult = validateCutTime();
        if (!validateResult.equals("ok")) {
            updateAlert(validateResult);
            return;
        }
        MP3File file = new MP3File(sourceFilePath);
        MP3AudioHeader mp3AudioHeader = file.getMP3AudioHeader();

        sourceFile = file.getFile();

        executorService.submit(() -> {
            generateMp3ByTimeAndCBR(mp3AudioHeader, targetFilePath, cutStartTime, cutEndTime, sourceFile);
            updateAlert("cut ok.");
        });
    }

    private String validateCutTime(){
        if (cutStartTimeTF.getText().equals("") || cutEndTimeTF.getText().equals("")) {
            return "select cutStartTime or cutEndTime pls.";
        }
        cutStartTime = Long.parseLong(cutStartTimeTF.getText());
        cutEndTime = Long.parseLong(cutEndTimeTF.getText());
        if (cutStartTime >= cutEndTime) {
            return "cutStartTime >= cutEndTime.";
        }
        if (cutStartTime > sourceTotalTime || cutEndTime > sourceTotalTime) {
            return "cutStartTime or cutEndTime > sourceTotalTime";
        }
        return "ok";
    }

    private boolean sourceFileNotChoose() {
        if (sourceFile == null) {
            updateAlert("choose source file pls.");
            return true;
        }
        return false;
    }

    private boolean targetFileNotChoose() {
        if (targetFile == null) {
            updateAlert("choose output file pls.");
            return true;
        }
        return false;
    }

    private void updateAlert(String alertInfo){
        alertZone.setText(alertInfo);
    }

    private void updateOutputDisplay(String info){
        outputPathText.setText(info);
    }

    private void initCutTimeValidator(){
        // force the field to be numeric only
        cutStartTimeTF.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                cutStartTimeTF.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        // force the field to be numeric only
        cutEndTimeTF.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                cutEndTimeTF.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }
}
