package services;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import java.io.File;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.CV_32SC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;

public class FaceAuthService {

    private CascadeClassifier faceDetector;
    private LBPHFaceRecognizer recognizer;
    private static final String FACE_IMAGES_DIR = "face_data/";
    private static final String TRAINED_MODEL_PATH = "face_data/trained_model.xml";

    public FaceAuthService() throws Exception {
        InputStream in = getClass().getResourceAsStream("/haarcascade_frontalface_default.xml");
        if (in == null) {
            throw new Exception("haarcascade_frontalface_default.xml introuvable.");
        }
        File tmpFile = File.createTempFile("haarcascade", ".xml");
        tmpFile.deleteOnExit();
        Files.copy(in, tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        faceDetector = new CascadeClassifier(tmpFile.getAbsolutePath());

        recognizer = LBPHFaceRecognizer.create();
        new File(FACE_IMAGES_DIR).mkdirs();
        loadModel();
    }

    private void loadModel() {
        File modelFile = new File(TRAINED_MODEL_PATH);
        if (modelFile.exists()) {
            recognizer.read(TRAINED_MODEL_PATH);
        } else {
            trainModel();
        }
    }

    public void trainModel() {
        File faceDataDir = new File(FACE_IMAGES_DIR);
        File[] files = faceDataDir.listFiles((dir, name) -> name.startsWith("user_") && name.endsWith(".jpg"));

        if (files == null || files.length == 0)
            return;

        MatVector images = new MatVector(files.length);
        Mat labels = new Mat(files.length, 1, CV_32SC1);
        IntBuffer labelsBuf = labels.createBuffer();

        for (int i = 0; i < files.length; i++) {
            Mat img = imread(files[i].getAbsolutePath(), IMREAD_GRAYSCALE);
            images.put(i, img);
            String fileName = files[i].getName();
            int userId = Integer.parseInt(fileName.substring(5, fileName.lastIndexOf(".")));
            labelsBuf.put(i, userId);
        }

        if (files.length > 0) {
            recognizer.train(images, labels);
            recognizer.save(TRAINED_MODEL_PATH);
        }
    }

    public boolean enrollFace(int userId) throws Exception {
        Mat face = captureAndDetectFace();
        if (face == null)
            return false;

        String path = FACE_IMAGES_DIR + "user_" + userId + ".jpg";
        imwrite(path, face);
        trainModel();
        return true;
    }

    public int identifyUser() throws Exception {
        Mat currentFace = captureAndDetectFace();
        if (currentFace == null)
            return -1;

        int[] label = new int[1];
        double[] confidence = new double[1];
        recognizer.predict(currentFace, label, confidence);

        System.out.println("Identification: User=" + label[0] + " Conf=" + confidence[0]);
        // Seuil typique pour LBPH: < 50-80 est un bon match
        if (label[0] != -1 && confidence[0] < 80) {
            return label[0];
        }
        return -1;
    }

    public boolean verifyFace(int userId) throws Exception {
        Mat currentFace = captureAndDetectFace();
        if (currentFace == null)
            return false;

        int[] label = new int[1];
        double[] confidence = new double[1];
        recognizer.predict(currentFace, label, confidence);

        return label[0] == userId && confidence[0] < 80;
    }

    public void renameFaceData(int fromId, int toId) {
        File fromFile = new File(FACE_IMAGES_DIR + "user_" + fromId + ".jpg");
        File toFile = new File(FACE_IMAGES_DIR + "user_" + toId + ".jpg");

        if (fromFile.exists()) {
            if (fromFile.renameTo(toFile)) {
                trainModel();
            }
        }
    }

    private Mat captureAndDetectFace() throws Exception {
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        grabber.start();
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
        Mat face = null;

        for (int i = 0; i < 50; i++) {
            Frame frame = grabber.grab();
            if (frame == null)
                continue;

            Mat colorMat = converter.convert(frame);
            Mat grayMat = new Mat();
            cvtColor(colorMat, grayMat, COLOR_BGR2GRAY);

            RectVector faces = new RectVector();
            faceDetector.detectMultiScale(grayMat, faces);

            if (faces.size() > 0) {
                face = new Mat(grayMat, faces.get(0));
                break;
            }
        }
        grabber.stop();
        return face;
    }
}
