package fi.helsinki.cs.tmc.intellij.ui.login;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.openapi.application.ApplicationManager;
import fi.helsinki.cs.tmc.core.domain.Organization;
import fi.helsinki.cs.tmc.core.utilities.TmcServerAddressNormalizer;
import fi.helsinki.cs.tmc.intellij.holders.ProjectListManagerHolder;
import fi.helsinki.cs.tmc.intellij.io.SettingsTmc;
import fi.helsinki.cs.tmc.intellij.services.login.LoginManager;
import fi.helsinki.cs.tmc.intellij.services.persistence.PersistentTmcSettings;
import fi.helsinki.cs.tmc.intellij.ui.courseselection.CourseListWindow;
import fi.helsinki.cs.tmc.intellij.ui.organizationselection.OrganizationListWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Objects;

public class LoginDialog extends JDialog {
    private static LoginDialog instance;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel serverAddress;
    private JButton changeServerAddressButton;
    private SettingsTmc settingsTmc;
    private TmcServerAddressNormalizer addressNormalizer;
    private Organization previousOrganization;

    private static final Logger logger = LoggerFactory.getLogger(LoginManager.class);

    public LoginDialog() {
        setContentPane(contentPane);
        setModal(false);
        getRootPane().setDefaultButton(buttonOK);

        settingsTmc = PersistentTmcSettings.getInstance().getSettingsTmc();
        previousOrganization = settingsTmc.getOrganization().orNull();

        serverAddress.setText(settingsTmc.getServerAddress());

        this.setTitle("TMC Login");
        this.pack();

        buttonOK.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        onOK();
                    }
                });

        buttonCancel.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        onCancel();
                    }
                });

        changeServerAddressButton.addActionListener(createActionListenerChangeServerAddress());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(
                new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        onCancel();
                    }
                });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        onCancel();
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    public TmcServerAddressNormalizer getAddressNormalizer() {
        if (this.addressNormalizer == null) {
            this.addressNormalizer = new TmcServerAddressNormalizer();
        }
        return this.addressNormalizer;
    }

    public static void display() {
        logger.info("Showing Login window. @LoginDialog");
        if (instance != null && instance.isDisplayable()) {
            instance.toFront();
            return;
        }
        instance = new LoginDialog();
        instance.setLocationRelativeTo(null);
        instance.setVisible(true);
    }

    private void onOK() {
        final PersistentTmcSettings saveSettings = PersistentTmcSettings.getInstance();

        settingsTmc.setUsername(usernameField.getText());

        saveSettings.setSettingsTmc(settingsTmc);

        char[] password = passwordField.getPassword();
        buttonOK.setEnabled(false);
        buttonCancel.setEnabled(false);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            boolean loggedIn;
            try {
                loggedIn = new LoginManager().login(new String(password));
            } finally {
                Arrays.fill(password, '\0');
            }
            boolean loginSucceeded = loggedIn;
            ApplicationManager.getApplication().invokeLater(() -> finishLogin(loginSucceeded));
        });
    }

    private void finishLogin(boolean loggedIn) {
        if (!isDisplayable()) {
            return;
        }
        buttonOK.setEnabled(true);
        buttonCancel.setEnabled(true);
        if (!loggedIn) {
            return;
        }

        dispose();
        instance = null;
        addressNormalizer = getAddressNormalizer();
        addressNormalizer.selectOrganizationAndCourse();
        try {
            if (!settingsTmc.getOrganization().isPresent()) {
                OrganizationListWindow.display();
            } else if (!settingsTmc.getCurrentCourse().isPresent()
                    || !Objects.equals(previousOrganization, settingsTmc.getOrganization().get())) {
                CourseListWindow.display();
            } else {
                ProjectListManagerHolder.get().refreshAllCourses();
            }
        } catch (Exception e) {
            logger.warn("Could not show organization or course selection.", e);
        }
    }

    private void onCancel() {
        dispose();
        instance = null;
    }

    private ActionListener createActionListenerChangeServerAddress() {
        return actionEvent -> {
            String newAddress =
                    JOptionPane.showInputDialog(
                            this, "Server address", this.serverAddress.getText());

            if (newAddress != null && !newAddress.trim().isEmpty()) {
                this.serverAddress.setText(newAddress.trim());
                settingsTmc.setServerAddress(newAddress);
                addressNormalizer = getAddressNormalizer();

                addressNormalizer.normalize();
            }
        };
    }

    public static void main(String[] args) {
        LoginDialog dialog = new LoginDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.setOpaque(true);
        contentPane.setPreferredSize(new Dimension(400, 175));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.setToolTipText("LOGIN PLS");
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("Login");
        panel2.add(buttonOK, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel2.add(buttonCancel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(3, 4, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(223, 252), null, 0, false));
        changeServerAddressButton = new JButton();
        changeServerAddressButton.setText("Change");
        panel3.add(changeServerAddressButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        serverAddress = new JLabel();
        serverAddress.setText("Label");
        panel3.add(serverAddress, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Server");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel3.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Username");
        panel3.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Password");
        panel3.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        usernameField = new JTextField();
        usernameField.setText("");
        panel3.add(usernameField, new GridConstraints(1, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        passwordField = new JPasswordField();
        passwordField.setText("");
        panel3.add(passwordField, new GridConstraints(2, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
