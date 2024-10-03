package laptrinhmang.phatamthanhbangTCP;
import javax.sound.sampled.*; // Cung cấp các lớp cho việc xử lý âm thanh
import javax.swing.*; // Giao diện người dùng đồ họa (GUI)
import java.awt.*; // Cung cấp các lớp về giao diện đồ họa
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket; // Sử dụng để kết nối TCP
public class AudioClient extends JFrame{
    private JButton startButton; // Nút bắt đầu ghi âm và gửi âm thanh
    private JButton stopButton; // Nút dừng ghi âm
    private JButton saveButton; // Nút lưu âm thanh
    private JTextArea logArea; // Khu vực hiển thị các thông báo trạng thái
    private boolean running = false; // Trạng thái của việc ghi âm có đang chạy không
    private TargetDataLine microphone; // Đối tượng microphone để lấy dữ liệu âm thanh
    private ByteArrayOutputStream audioStream; // Lưu trữ âm thanh tạm thời dưới dạng byte

    public AudioClient() {
        setTitle("Audio Client"); // Tiêu đề cửa sổ
        setSize(400, 300); // Kích thước cửa sổ
        setDefaultCloseOperation(EXIT_ON_CLOSE); // Đóng chương trình khi cửa sổ tắt
        setLayout(new BorderLayout()); // Sử dụng bố cục viền cho giao diện

        // Panel chứa các nút điều khiển
        JPanel controlPanel = new JPanel();
        startButton = new JButton("Start Client"); // Nút bắt đầu gửi âm thanh
        stopButton = new JButton("Stop Client"); // Nút dừng gửi âm thanh
        saveButton = new JButton("Save Audio"); // Nút lưu âm thanh vào file
        stopButton.setEnabled(false); // Chưa cho phép dừng khi chưa bắt đầu
        saveButton.setEnabled(false); // Chưa cho phép lưu khi chưa có dữ liệu âm thanh

        // Thêm các nút vào panel điều khiển
        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(saveButton);

        logArea = new JTextArea(); // Khu vực hiển thị trạng thái
        logArea.setEditable(false); // Không cho phép chỉnh sửa
        JScrollPane scrollPane = new JScrollPane(logArea); // Thêm thanh cuộn cho khu vực trạng thái

        // Thêm các thành phần giao diện vào cửa sổ
        add(controlPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Thêm hành động cho các nút
        startButton.addActionListener(e -> startClient());
        stopButton.addActionListener(e -> stopClient());
        saveButton.addActionListener(e -> saveAudioToFile());

        setVisible(true); // Hiển thị cửa sổ
    }

    // Ghi nhật ký vào khu vực log
    private void log(String message) {
        logArea.append(message + "\n");
    }

    // Phương thức bắt đầu client, ghi âm và gửi âm thanh đến server
    private void startClient() {
        startButton.setEnabled(false); // Vô hiệu hóa nút bắt đầu khi đang chạy
        stopButton.setEnabled(true); // Kích hoạt nút dừng
        saveButton.setEnabled(false); // Vô hiệu hóa nút lưu cho đến khi có âm thanh
        running = true; // Bắt đầu ghi âm

        log("Connecting to server...");

        new Thread(() -> { // Khởi tạo luồng để xử lý việc ghi âm
            try (Socket socket = new Socket("localhost", 5000)) { // Kết nối tới server qua cổng 5000
                log("Connected to server!");

                OutputStream out = socket.getOutputStream(); // Lấy OutputStream để gửi dữ liệu

                AudioFormat format = new AudioFormat(44100, 16, 2, true, false); // Định dạng âm thanh: 44100Hz, 16-bit, 2 kênh (stereo)
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                microphone = (TargetDataLine) AudioSystem.getLine(info); // Lấy dòng dữ liệu từ microphone
                microphone.open(format); // Mở microphone
                microphone.start(); // Bắt đầu ghi âm

                audioStream = new ByteArrayOutputStream(); // Khởi tạo luồng byte để lưu âm thanh
                byte[] buffer = new byte[4096]; // Tạo buffer 4KB để lưu trữ tạm thời dữ liệu âm thanh
                int bytesRead;

                // Vòng lặp ghi âm và gửi âm thanh đến server
                while (running && (bytesRead = microphone.read(buffer, 0, buffer.length)) != -1) {
                    out.write(buffer, 0, bytesRead); // Gửi dữ liệu đến server
                    audioStream.write(buffer, 0, bytesRead); // Ghi dữ liệu vào bộ nhớ tạm để lưu lại sau
                }

                microphone.close(); // Đóng microphone sau khi dừng ghi âm
                log("Stopped transmitting.");
                saveButton.setEnabled(true); // Kích hoạt nút lưu khi đã ghi âm xong
            } catch (Exception e) {
                log("Error: " + e.getMessage());
            }
        }).start();
    }

    // Phương thức dừng client
    private void stopClient() {
        running = false; // Dừng ghi âm
        startButton.setEnabled(true); // Kích hoạt lại nút bắt đầu
        stopButton.setEnabled(false); // Vô hiệu hóa nút dừng
        log("Client stopped.");
    }

    // Phương thức lưu âm thanh vào file WAV
    private void saveAudioToFile() {
        try {
            byte[] audioData = audioStream.toByteArray(); // Lấy dữ liệu âm thanh từ bộ nhớ
            InputStream byteArrayInputStream = new ByteArrayInputStream(audioData); // Tạo luồng đầu vào từ mảng byte
            AudioFormat format = new AudioFormat(44100, 16, 2, true, false); // Định dạng âm thanh
            AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, format, audioData.length / format.getFrameSize());

            // Ghi âm thanh vào file WAV trên ổ D
            File wavFile = new File("D:/LapTrinhMang/demo.wav");
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, wavFile);

            log("Đã lưu vào 'D:/LapTrinhMang/demo.wav'."); // Thông báo đã lưu thành công
        } catch (Exception e) {
            log("Lưu bị lỗi!!!" + e.getMessage()); // Thông báo lỗi nếu xảy ra
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AudioClient::new); // Khởi chạy giao diện GUI
    }
}
