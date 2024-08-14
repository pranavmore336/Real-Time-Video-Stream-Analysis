
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.core.MatOfRect;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;

import javax.swing.*;
import java.awt.image.BufferedImage;

public class VideoStreamViewer extends Application {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private VideoCapture camera;
    private CascadeClassifier faceDetector;
    private Label infoLabel;
    private LineChart<Number, Number> faceChart;
    private XYChart.Series<Number, Number> faceSeries;
    private int frameCount = 0;
    private VideoWriter videoWriter; // Add VideoWriter instance variable

    @Override
    public void start(Stage primaryStage) {
        camera = new VideoCapture(0);
        if (!camera.isOpened()) {
            System.out.println("Error: Camera not found.");
            return;
        }

        String xmlPath = new java.io.File("src/haarcascade_frontalface_default.xml").getAbsolutePath();
        faceDetector = new CascadeClassifier(xmlPath);

        ImageView imageView = new ImageView();
        infoLabel = new Label("Faces Detected: 0");

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Frames");
        yAxis.setLabel("Faces Detected");
        faceChart = new LineChart<>(xAxis, yAxis);
        faceSeries = new XYChart.Series<>();
        faceSeries.setName("Face Detection Over Time");
        faceChart.getData().add(faceSeries);

        VBox root = new VBox(imageView, infoLabel, faceChart);
        Scene scene = new Scene(root, 640, 480);

        primaryStage.setTitle("Video Stream");
        primaryStage.setScene(scene);
        primaryStage.show();


        videoWriter = new VideoWriter("output.mp4", VideoWriter.fourcc('X', '2', '6', '4'), 30, new Size(640, 480)); //mjpg for avi file
        if (!videoWriter.isOpened()) {
            System.out.println("Error: Could not open VideoWriter.");
            return;
        }

        new Thread(() -> {
            Mat frame = new Mat();
            while (true) {
                if (!camera.read(frame) || frame.empty()) break;

                Mat grayFrame = new Mat();
                Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
                MatOfRect faces = new MatOfRect();
                faceDetector.detectMultiScale(grayFrame, faces, 1.1, 3, 0, new Size(30, 30), new Size());

                for (Rect rect : faces.toArray()) {
                    Imgproc.rectangle(frame, rect.tl(), rect.br(), new Scalar(0, 255, 0), 2);
                }

                int faceCount = faces.toArray().length;

                Platform.runLater(() -> {
                    infoLabel.setText("Faces Detected: " + faceCount);
                    faceSeries.getData().add(new XYChart.Data<>(frameCount++, faceCount));

                    if (faceCount > 5) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Alert");
                        alert.setHeaderText(null);
                        alert.setContentText("High number of faces detected!");
                        alert.show();
                    }

                    BufferedImage bufferedImage = new BufferedImage(frame.width(), frame.height(), BufferedImage.TYPE_3BYTE_BGR);
                    byte[] data = new byte[frame.width() * frame.height() * (int) frame.elemSize()];
                    frame.get(0, 0, data);
                    bufferedImage.getRaster().setDataElements(0, 0, frame.width(), frame.height(), data);

                    Image image = SwingFXUtils.toFXImage(bufferedImage, null);
                    imageView.setImage(image);
                });

                // Write the frame to the video file
                videoWriter.write(frame);
            }

            // Release resources
            camera.release();
            videoWriter.release();
            Platform.runLater(() -> primaryStage.close());
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
