
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

public class VideoStreamCapture {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private VideoCapture camera;
    private Mat frame;

    public VideoStreamCapture() {
        camera = new VideoCapture(0); // 0 for default webcam
        frame = new Mat();
        if (!camera.isOpened()) {
            System.out.println("Error: Camera not found.");
        }
    }

    public Mat getFrame() {
        camera.read(frame);
        return frame;
    }

    public void release() {
        camera.release();
    }
}
