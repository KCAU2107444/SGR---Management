package ui.gui;

import db.DatabaseException;
import db.DatabaseManager;
import model.Admin;
import model.Passenger;
import model.User;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class GuiMain {
    private static final Color SIDEBAR = new Color(49, 49, 49);
    private static final Color HEADER = new Color(38, 38, 38);
    private static final Color PAPER = new Color(222, 222, 222);
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final JFrame window = new JFrame("SGR Rail Management System");
    private final CardLayout screens = new CardLayout();
    private final JPanel content = new JPanel(screens);
    private final JPanel stepPage = new JPanel(new BorderLayout());
    private final JPanel body = new JPanel(new BorderLayout());
    private final JPanel navigation = new JPanel(new GridLayout(0, 1, 0, 5));
    private final JPanel page = new JPanel(new BorderLayout(0, 14));
    private final JLabel heading = new JLabel();
    private final JLabel account = new JLabel();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            javax.swing.plaf.metal.MetalLookAndFeel.setCurrentTheme(new javax.swing.plaf.metal.DefaultMetalTheme() {
                @Override protected javax.swing.plaf.ColorUIResource getPrimary1() {
                    return new javax.swing.plaf.ColorUIResource(75, 75, 75);
                }

                @Override protected javax.swing.plaf.ColorUIResource getPrimary2() {
                    return new javax.swing.plaf.ColorUIResource(155, 155, 155);
                }

                @Override protected javax.swing.plaf.ColorUIResource getPrimary3() {
                    return new javax.swing.plaf.ColorUIResource(210, 210, 210);
                }
            });
            try {
                UIManager.setLookAndFeel(new javax.swing.plaf.metal.MetalLookAndFeel());
            } catch (UnsupportedLookAndFeelException ignored) {
                // Use the installed look and feel if Metal is unavailable.
            }
            Color selection = new Color(195, 195, 195);
            for (String key : new String[] {"List.selectionBackground", "Table.selectionBackground",
                    "ComboBox.selectionBackground", "TextField.selectionBackground",
                    "PasswordField.selectionBackground", "textHighlight"}) {
                UIManager.put(key, selection);
            }
            UIManager.put("ProgressBar.foreground", new Color(105, 105, 105));
            UIManager.put("Panel.background", PAPER);
            new GuiMain().open();
        });
    }

    private void open() {
        InlineForm.attach(this);
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));
        JLabel brand = new JLabel("Madaraka Express");
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 22f));
        brand.setForeground(Color.WHITE);
        account.setForeground(Color.WHITE);
        header.add(brand, BorderLayout.WEST);
        header.add(account, BorderLayout.EAST);

        window.add(header, BorderLayout.NORTH);
        content.add(body, "main");
        content.add(stepPage, "step");
        window.add(content, BorderLayout.CENTER);
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        window.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) {
                InlineForm.cancelActiveStep();
            }

            @Override public void windowClosed(WindowEvent event) {
                db.disconnect();
            }
        });
        window.setMinimumSize(new Dimension(690, 490));
        window.setSize(940, 610);
        window.setLocationRelativeTo(null);
        try {
            db.connect();
            showAuth(false, "");
        } catch (DatabaseException e) {
            JTextArea error = textArea();
            error.setText("Could not open the database.\n\n" + e.getMessage());
            body.add(error, BorderLayout.CENTER);
        }
        window.setVisible(true);
    }

    private void showAuth(boolean register, String message) {
        account.setText("");
        body.removeAll();
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(PAPER);
        JPanel form = new JPanel(new BorderLayout(0, 14));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));
        JLabel title = new JLabel(register ? "Create passenger account" : "Log in");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        form.add(title, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridLayout(0, 2, 10, 8));
        fields.setOpaque(false);
        JTextField name = new JTextField(18);
        JTextField id = new JTextField(18);
        JTextField phone = new JTextField(18);
        JTextField username = new JTextField(18);
        JPasswordField password = new JPasswordField(18);
        if (register) {
            addField(fields, "Full name", name);
            addField(fields, "National ID / Passport", id);
            addField(fields, "Phone number", phone);
        }
        addField(fields, "Username", username);
        addField(fields, "Password", password);
        form.add(fields, BorderLayout.CENTER);

        JLabel feedback = new JLabel(message.isEmpty() ? " " : message);
        JButton submit = new JButton(register ? "Register" : "Log in");
        submit.addActionListener(event -> {
            String userName = username.getText().trim();
            char[] characters = password.getPassword();
            String secret = new String(characters).trim();
            java.util.Arrays.fill(characters, '\0');
            String fullName = name.getText().trim();
            String idNumber = id.getText().trim();
            String phoneNumber = phone.getText().trim();
            if (userName.isEmpty() || secret.isEmpty()
                    || (register && (fullName.isEmpty() || idNumber.isEmpty() || phoneNumber.isEmpty()))) {
                feedback.setText("Fill in all fields.");
                return;
            }
            try {
                if (register) {
                    if (secret.length() < 6) {
                        feedback.setText("Use at least 6 characters for the password.");
                        return;
                    }
                    db.saveRecord(new Passenger(0, fullName, idNumber, phoneNumber, userName, User.hashPassword(secret)));
                    showAuth(false, "Account created. You can now log in.");
                } else {
                    User user = db.authenticate(userName, secret);
                    if (user instanceof Admin admin) {
                        showDashboard(admin.getUsername());
                        AdminMenu.open(this, admin);
                    } else if (user instanceof Passenger passenger) {
                        showDashboard(passenger.getUsername());
                        PassengerMenu.open(this, passenger);
                    } else {
                        feedback.setText("Wrong username or password.");
                    }
                }
            } catch (RuntimeException e) {
                feedback.setText(e.getMessage());
                form.revalidate();
            }
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setOpaque(false);
        buttons.add(button(register ? "Back to login" : "Register", () -> showAuth(!register, "")));
        buttons.add(submit);
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        footer.add(feedback, BorderLayout.NORTH);
        footer.add(buttons, BorderLayout.SOUTH);
        form.add(footer, BorderLayout.SOUTH);
        center.add(form);
        body.add(center);
        window.getRootPane().setDefaultButton(submit);
        refreshWindow();
    }

    private static void addField(JPanel fields, String title, JTextField input) {
        JLabel label = new JLabel(title);
        label.setLabelFor(input);
        fields.add(label);
        fields.add(input);
    }

    private void showDashboard(String username) {
        window.getRootPane().setDefaultButton(null);
        account.setText("Welcome back, " + username);
        body.removeAll();
        navigation.removeAll();
        navigation.setOpaque(false);
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(SIDEBAR);
        sidebar.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));
        sidebar.setPreferredSize(new Dimension(180, 0));
        sidebar.add(navigation, BorderLayout.NORTH);
        sidebar.add(button("Log out", () -> showAuth(false, "")), BorderLayout.SOUTH);
        page.setBackground(PAPER);
        page.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 23f));
        body.add(sidebar, BorderLayout.WEST);
        body.add(page, BorderLayout.CENTER);
    }

    void showStep(JPanel panel, JButton submit) {
        stepPage.add(panel, BorderLayout.CENTER);
        screens.show(content, "step");
        window.getRootPane().setDefaultButton(submit);
    }

    void endStep() {
        window.getRootPane().setDefaultButton(null);
        stepPage.removeAll();
        screens.show(content, "main");
    }

    boolean isOpen() {
        return window.isDisplayable();
    }

    void nav(String title, Runnable action) {
        navigation.add(button(title, action));
    }

    void showPage(String title, String emptyText, Supplier<RecordTable> load, String[] labels, Runnable... actions) {
        page.removeAll();
        heading.setText(title);
        page.add(heading, BorderLayout.NORTH);
        JTextArea records = textArea();
        JScrollPane scroll = new JScrollPane();
        page.add(scroll, BorderLayout.CENTER);

        JLabel status = new JLabel(" ");
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        controls.setOpaque(false);
        JLabel summary = new JLabel();
        Runnable refresh = () -> {
            try {
                RecordTable data = load.get();
                summary.setText(data.summary());
                if (data.rows().length == 0) {
                    records.setText(emptyText);
                    scroll.setViewportView(records);
                } else {
                    scroll.setViewportView(createTable(data));
                }
            } catch (RuntimeException e) {
                summary.setText("");
                records.setText("Error: " + e.getMessage());
                scroll.setViewportView(records);
            }
        };
        JButton back = new JButton("Back to " + title.toLowerCase(java.util.Locale.ROOT));
        back.addActionListener(event -> {
            back.setVisible(false);
            controls.setVisible(true);
            status.setText(" ");
            refresh.run();
        });
        back.setVisible(false);
        for (int i = 0; i < labels.length; i++) {
            Runnable action = actions[i];
            controls.add(button(labels[i], () -> {
                String result = runAction(action);
                if (!isOpen()) return;
                status.setText(" ");
                if (result.contains("\n")) {
                    summary.setText("");
                    scroll.setViewportView(records);
                    records.setText(result);
                    records.setCaretPosition(0);
                    controls.setVisible(false);
                    back.setVisible(true);
                } else {
                    status.setText(result.isEmpty() ? " " : result);
                    refresh.run();
                }
            }));
        }
        JPanel footer = new JPanel(new BorderLayout(0, 5));
        footer.setOpaque(false);
        footer.add(controls, BorderLayout.NORTH);
        footer.add(back, BorderLayout.WEST);
        footer.add(summary, BorderLayout.EAST);
        footer.add(status, BorderLayout.SOUTH);
        page.add(footer, BorderLayout.SOUTH);
        refresh.run();
        refreshWindow();
    }

    static JTable createTable(RecordTable data) {
        JTable table = new JTable(new DefaultTableModel(data.rows(), data.columns()) {
            @Override public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override public Class<?> getColumnClass(int column) {
                for (Object[] row : data.rows()) {
                    if (row[column] != null) return row[column].getClass();
                }
                return Object.class;
            }
        });
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setRowHeight(30);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override protected void setValue(Object value) {
                setHorizontalAlignment(value instanceof Number ? RIGHT : LEFT);
                setText(formatCell(value));
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            }
        };
        for (int i = 0; i < data.columns().length; i++) {
            var column = table.getColumnModel().getColumn(i);
            column.setCellRenderer(renderer);
            int width = table.getFontMetrics(table.getTableHeader().getFont()).stringWidth(data.columns()[i]) + 32;
            for (Object[] row : data.rows()) {
                width = Math.max(width, table.getFontMetrics(table.getFont()).stringWidth(formatCell(row[i])) + 24);
            }
            column.setPreferredWidth(Math.max(55, width));
        }
        return table;
    }

    private static String formatCell(Object value) {
        if (value instanceof LocalDate date) return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        if (value instanceof LocalTime time) return time.format(DateTimeFormatter.ofPattern("HH:mm"));
        if (value instanceof LocalDateTime time) return time.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
        if (value instanceof Double number) return String.format("%,.2f", number);
        return value == null ? "—" : value.toString();
    }

    private static String runAction(Runnable action) {
        try {
            action.run();
        } catch (InlineForm.Cancelled ignored) {
            // Canceling an action returns to the current page.
        } catch (RuntimeException e) {
            InlineForm.println("Error: " + e.getMessage());
        }
        return InlineForm.takeOutput();
    }

    private static JTextArea textArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setMargin(new java.awt.Insets(12, 12, 12, 12));
        return area;
    }

    private static JButton button(String label, Runnable action) {
        JButton button = new JButton(label);
        button.addActionListener(event -> action.run());
        return button;
    }

    private void refreshWindow() {
        body.revalidate();
        body.repaint();
    }
}
