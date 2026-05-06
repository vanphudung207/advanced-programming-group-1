package client.controller;
import client.service.AuthService;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Label; // ĐÃ THÊM: Import thư viện Label
import javafx.scene.layout.StackPane; 
import javafx.util.Duration;

public class LoginController {

    @FXML private StackPane rootPane; 
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    
    // ĐÃ THÊM: Biến liên kết với dòng chữ báo lỗi trên giao diện
    @FXML private Label lblError; 

    @FXML
    public void initialize() {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), rootPane);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    @FXML
    private void handleLoginAction(ActionEvent event) {
        String enteredEmail = txtEmail.getText(); 
        String enteredPassword = txtPassword.getText(); 
        
        // Reset lại chữ trống mỗi khi người dùng bấm nút mới
        lblError.setText("");

        // TÀI KHOẢN ADMIN CỐ ĐỊNH
        if (
            enteredEmail.equals("admin@gmail.com")
            && enteredPassword.equals("123")
        ) {

            try {

                javafx.fxml.FXMLLoader loader =
                    new javafx.fxml.FXMLLoader(
                        getClass().getResource(
                            "/client/view/Admin.fxml"
                        )
                    );

                javafx.scene.Parent root = loader.load();

                javafx.stage.Stage stage =
                    (javafx.stage.Stage)
                    ((javafx.scene.Node)
                    event.getSource())
                    .getScene()
                    .getWindow();

                stage.getScene().setRoot(root);

                stage.setTitle(
                    "Online Auction System - Admin"
                );

            } catch (Exception e) {

                e.printStackTrace();
            }

        }

        // ĐĂNG NHẬP USER BÌNH THƯỜNG
        else if (
            client.service.AuthService.login(
                enteredEmail,
                enteredPassword
            )
        ) {
            
            try {
                javafx.fxml.FXMLLoader loader =
                    new javafx.fxml.FXMLLoader(
                        getClass().getResource(
                            "/client/view/ProductList.fxml"
                        )
                    );

                javafx.scene.Parent root = loader.load();

                javafx.stage.Stage stage =
                    (javafx.stage.Stage)
                    ((javafx.scene.Node)
                    event.getSource())
                    .getScene()
                    .getWindow();

                stage.getScene().setRoot(root);

                stage.setTitle(
                    "Online Auction System - Danh sách sản phẩm"
                );

            } catch (Exception e) {

                e.printStackTrace();
            }
        }

        else {

            // HIỆN LỖI MÀU ĐỎ TRỰC TIẾP TRÊN GIAO DIỆN
            lblError.setText(
                "Sai email hoặc mật khẩu! Vui lòng thử lại."
            );
        }
    }

    @FXML
    private void switchToRegister(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/view/Register.fxml"));
            javafx.scene.Parent registerRoot = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(registerRoot);
            stage.setTitle("Online Auction System - Register");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}