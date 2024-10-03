package laptrinhmang.phatamthanhbangTCP;
import javax.sound.sampled.*; // Cung cấp các lớp xử lý âm thanh
import javax.swing.*; // Giao diện người dùng đồ họa (GUI)
import java.awt.*; // Cung cấp các lớp về giao diện đồ họa
import java.io.InputStream; // Lớp xử lý luồng dữ liệu đầu vào
import java.net.ServerSocket; // Lớp hỗ trợ cho việc lắng nghe kết nối TCP
import java.net.Socket; // Lớp hỗ trợ kết nối TCP

public class AudioServer extends JFrame{
    private JButton startButton; // Nút bắt đầu server
    private JButton stopButton; // Nút dừng server
    private JTextArea logArea; // Khu vực hiển thị trạng thái
    private boolean running = false; // Trạng thái server có đang chạy không
    private SourceDataLine speakers; // Đối tượng phát âm thanh

    public AudioServer() {
        setTitle("Audio Server"); // Tiêu đề cửa sổ
        setSize(400, 300); // Kích thước cửa sổ
        setDefaultCloseOperation(EXIT_ON_CLOSE); // Đóng chương trình khi cửa sổ tắt
        setLayout(new BorderLayout()); // Sử dụng bố cục viền cho giao diện

        // Panel chứa các nút điều khiển
        JPanel controlPanel = new JPanel();
        startButton = new JButton("Start Server"); // Nút bắt đầu server
        stopButton = new JButton("Stop Server"); // Nút dừng server
        stopButton.setEnabled(false); // Vô hiệu hóa nút dừng khi chưa bắt đầu

        // Thêm các nút vào panel điều khiển
        controlPanel.add(startButton);
        controlPanel.add(stopButton);

        logArea = new JTextArea(); // Khu vực hiển thị trạng thái
        logArea.setEditable(false); // Không cho phép chỉnh sửa
        JScrollPane scrollPane = new JScrollPane(logArea); // Thêm thanh cuộn cho khu vực trạng thái

        // Thêm các thành phần giao diện vào cửa sổ
        add(controlPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Thêm hành động cho các nút
        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());

        setVisible(true); // Hiển thị cửa sổ
    }

    // Ghi nhật ký vào khu vực log
    private void log(String message) {
        logArea.append(message + "\n");
    }

    // Phương thức bắt đầu server, nhận âm thanh và phát lại
    private void startServer() {
        startButton.setEnabled(false); // Vô hiệu hóa nút bắt đầu khi đang chạy
        stopButton.setEnabled(true); // Kích hoạt nút dừng
        running = true; // Bắt đầu server

        log("Server started, waiting for connection...");

        new Thread(() -> { // Khởi tạo luồng để xử lý việc nhận âm thanh
            try (ServerSocket serverSocket = new ServerSocket(5000)) { // Mở server lắng nghe trên cổng 5000
                Socket socket = serverSocket.accept(); // Chờ client kết nối
                log("Client connected!");

                InputStream in = socket.getInputStream(); // Lấy luồng đầu vào để nhận dữ liệu từ client

                AudioFormat format = new AudioFormat(44100, 16, 2, true, false); // Định dạng âm thanh: 44100Hz, 16-bit, 2 kênh (stereo)
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                speakers = (SourceDataLine) AudioSystem.getLine(info); // Lấy dòng phát âm thanh
                speakers.open(format); // Mở dòng phát âm thanh
                speakers.start(); // Bắt đầu phát âm thanh

                byte[] buffer = new byte[4096]; // Tạo buffer 4KB để nhận dữ liệu
                int bytesRead;

                // Vòng lặp nhận dữ liệu âm thanh từ client và phát lại
                while (running && (bytesRead = in.read(buffer)) != -1) {
                    speakers.write(buffer, 0, bytesRead); // Phát âm thanh qua loa
                }

                speakers.drain(); // Đảm bảo tất cả âm thanh được phát hết
                speakers.close(); // Đóng dòng phát âm thanh
                socket.close(); // Đóng kết nối socket
                log("Connection closed.");
            } catch (Exception e) {
                log("Error: " + e.getMessage()); // Ghi lại lỗi nếu xảy ra
            }
        }).start();
    }

    // Phương thức dừng server
    private void stopServer() {
        running = false; // Dừng server
        startButton.setEnabled(true); // Kích hoạt lại nút bắt đầu
        stopButton.setEnabled(false); // Vô hiệu hóa nút dừng
        log("Server stopped.");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AudioServer::new); // Khởi chạy giao diện GUI
    }
}
