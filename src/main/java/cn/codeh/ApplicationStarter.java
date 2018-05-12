package cn.codeh;

import cn.codeh.controller.MainController;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApplicationStarter extends javafx.application.Application {

    private ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Override
    public void start(Stage primaryStage) throws Exception {

        URL location = getClass().getResource("/main.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(location);
        fxmlLoader.setBuilderFactory(new JavaFXBuilderFactory());
        Parent root = fxmlLoader.load();

        MainController mainController = fxmlLoader.getController();
        mainController.primaryStage = primaryStage;
        mainController.executorService = executorService;

        primaryStage.setTitle("Audio Clipper");
        primaryStage.setScene(new Scene(root, 600, 600));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() throws Exception {
        System.out.println("application stop");
        executorService.shutdown();
        if (!executorService.isShutdown()) {
            Thread.sleep(1000 * 60);
            executorService.shutdownNow();
        }
        super.stop();
    }
}
