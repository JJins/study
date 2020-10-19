import javax.swing.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

enum Gender {male, female}

class UiUserInfo extends JFrame {
    public UiUserInfo(UserInfo userInfo) {
        JPanel panel = new JPanel();
        setContentPane(panel);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel userIdLabel = new JLabel("User ID: " + userInfo.getUserID());
        JLabel usernameLabel = new JLabel("Username: " + userInfo.getUserName());
        JLabel phoneNumberLabel = new JLabel("Phone Number: " + userInfo.getPhoneNumber());
        JLabel emailLabel = new JLabel("Email: " + userInfo.getEmail());
        JLabel genderLabel = new JLabel("Gender: " + userInfo.getGender());
        JButton setNewGenderButton = new JButton("Set");
        setNewGenderButton.addActionListener(ActionEvent -> {
            int rs = JOptionPane.showOptionDialog(
                    null,
                    "Set new gender to: (male or female)",
                    "Set Gender",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    new String[]{"male", "female", "Cancel"},
                    "male"
            );
            if (rs == 0) {
                if (userInfo.setGender(Gender.male)) {
                    JOptionPane.showMessageDialog(null,
                            "user \"" + userInfo.getUserName() + "\" set to male successfully.");
                }
            }
            if (rs == 1) {
                if (userInfo.setGender(Gender.female)) {
                    JOptionPane.showMessageDialog(null,
                            "user \"" + userInfo.getUserName() + "\" set to female successfully.");
                }
            }
            genderLabel.setText("Gender: " + userInfo.getGender());
        });

        panel.add(userIdLabel);
        panel.add(usernameLabel);
        panel.add(phoneNumberLabel);
        panel.add(emailLabel);
        panel.add(genderLabel);
        panel.add(setNewGenderButton);
    }
}

class UserManager {
    static boolean deleteUserByUserName(String _uname) {
        if (!DbHelper.isUserNameExisted(_uname)) {
            return false;
        }
        Connection c;
        Statement stmt;
        String sql;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:db.sqlite");
            c.setAutoCommit(false);
            stmt = c.createStatement();
            sql = "delete from user_info where userName='" + _uname + "';";
            stmt.executeUpdate(sql);
            sql = "delete from user_login where userName='" + _uname + "';";
            stmt.executeUpdate(sql);
            c.commit();
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        return true;
    }
}

class DbHelper {
    static boolean isUserNameExisted(String _uname) {
        Connection c;
        Statement stmt;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:db.sqlite");
            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("select userName from user_login where userName='" + _uname + "';");
            if (rs.next()) {
                rs.close();
                stmt.close();
                c.close();
                return true;
            } else {
                rs.close();
                stmt.close();
                c.close();
                return false;
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
            return false;
        }
    }
}

class UserLogin {
    private final String userName;
    private UserInfo userInfo;

    public UserLogin(String userName) {
        this.userName = userName;
    }

    public boolean login(String password) {
        Connection c;
        Statement stmt;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:db.sqlite");
            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("select * from user_login where userName='" + userName +
                    "' and password='" + password + "';");
            if (!rs.next()) {
                rs.close();
                stmt.close();
                c.close();
                return false;
            }
            // construct UserInfo object
            rs = stmt.executeQuery("select * from user_info where userName='" + userName + "';");
            Gender gender;
            if (rs.getInt("gender") == 1) {
                gender = Gender.male;
            } else {
                gender = Gender.female;
            }
            userInfo = new UserInfo(rs.getInt("userId"), rs.getString("userName"), rs.getString("email"),
                    rs.getString("phoneNumber"), gender);
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        return true;
    }

    public boolean register(String password) {
        if (DbHelper.isUserNameExisted(userName)) {
            System.out.println("User name is already existed.");
            return false;
        }
        // init vars
        Connection c;
        Statement stmt;
        String sql;
        // commit change to db
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:db.sqlite");
            c.setAutoCommit(false);
            stmt = c.createStatement();
            sql = "INSERT INTO user_login (userName,password) " +
                    "VALUES ('" + userName + "', '" + password + "');";
            stmt.executeUpdate(sql);
            sql = "INSERT INTO user_info (userName,gender,phoneNumber,email) " +
                    "VALUES ('" + userName + "', " + 1 + ", '+61488164886', 'admin@example.com');";
            stmt.executeUpdate(sql);
            c.commit();
            sql = "select userId from user_info where userName = '" + userName + "';";
            ResultSet rs = stmt.executeQuery(sql);
            userInfo = new UserInfo(rs.getInt("userId"), userName, "admin@example.com", "+61488164886", Gender.male);
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        return true;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }
}

class UserInfo {
    private final int userID;
    private final String userName;
    private final String phoneNumber;
    private final String email;
    private Gender gender;

    public UserInfo(int userID, String userName, String phoneNumber, String email, Gender gender) {
        this.userID = userID;
        this.userName = userName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.gender = gender;
    }

    public boolean setGender(Gender _gender) {
        if (this.gender == _gender) {
            return true;
        }
        int gender;
        if (_gender == Gender.male) {
            gender = 1;
        } else if (_gender == Gender.female) {
            gender = 2;
        } else {
            return false;
        }
        try {
            Class.forName("org.sqlite.JDBC");
            Connection c = DriverManager.getConnection("jdbc:sqlite:db.sqlite");
            c.setAutoCommit(false);
            Statement stmt = c.createStatement();
            String sql = "update user_info set gender=" + gender + " where userId=" + this.userID + ";";
            stmt.executeUpdate(sql);
            c.commit();
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        this.gender = _gender;
        return true;
    }

    public int getUserID() {
        return this.userID;
    }

    public String getUserName() {
        return this.userName;
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public String getEmail() {
        return this.email;
    }

    public Gender getGender() {
        return this.gender;
    }
}

public class Home {
    private JPanel panel1;
    private JTextField textField1;
    private JPasswordField passwordField1;
    private JButton loginButton;
    private JButton registerButton;
    private JButton deleteUserButton;

    public Home() {
        loginButton.addActionListener(actionEvent -> {
            if (textField1.getText().equals("") || new String(passwordField1.getPassword()).equals("")) {
                return;
            }
            UserLogin user_login = new UserLogin(textField1.getText());
            if (user_login.login(new String(passwordField1.getPassword()))) {
                JOptionPane.showMessageDialog(null, "Login success.");
                UiUserInfo ui = new UiUserInfo(user_login.getUserInfo());
                ui.pack();
                ui.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(null, "Wrong password.");
            }
        });

        registerButton.addActionListener(actionEvent -> {
            if (textField1.getText().equals("") || new String(passwordField1.getPassword()).equals("")) {
                return;
            }
            UserLogin user_login = new UserLogin(textField1.getText());
            if (user_login.register(new String(passwordField1.getPassword()))) {
                JOptionPane.showMessageDialog(null, "Registration success.");
                UiUserInfo ui = new UiUserInfo(user_login.getUserInfo());
                ui.pack();
                ui.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(null, "Failed. Username existed.");
            }
        });

        deleteUserButton.addActionListener(actionEvent -> {
            String rs = (String) JOptionPane.showInputDialog(
                    null,
                    "Username:",
                    "Delete user",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    ""
            );
            if (rs == null || rs.equals("")) {
                return;
            }
            if (UserManager.deleteUserByUserName(rs)) {
                JOptionPane.showMessageDialog(null, "Deleted.");
            } else {
                JOptionPane.showMessageDialog(null, "User not found.");
            }
        });
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Home");
        frame.setLocationRelativeTo(null);
        frame.setContentPane(new Home().panel1);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
