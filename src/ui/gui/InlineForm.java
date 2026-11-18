package ui.gui;

import model.PrintableReceipt;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.swing.JList;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.ListSelectionModel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

final class InlineForm {
    private static final StringBuilder output = new StringBuilder();
    private static GuiMain app;
    private static Runnable cancelStep;
    private static boolean backEnabled;

    static void setBackEnabled(boolean enabled) {
        backEnabled = enabled;
    }

    private InlineForm() { }

    static void attach(GuiMain window) {
        app = window;
    }

    static void cancelActiveStep() {
        if (cancelStep != null) cancelStep.run();
    }

    static void println(Object value) {
        output.append(value).append(System.lineSeparator());
    }

    static String takeOutput() {
        String value = output.toString().strip();
        output.setLength(0);
        return value;
    }

    static void processing(String prompt, int delayMillis) {
        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        step(prompt, progress, null, "Continue", delayMillis, true);
    }

    static void acknowledge(String prompt) {
        step(prompt, null, null, "OK", 0, false);
    }

    static int selectRow(String prompt, RecordTable data, String submitLabel) {
        return selectRow(prompt, data, submitLabel, 0);
    }

    static int selectRow(String prompt, RecordTable data, String submitLabel, int previousRow) {
        javax.swing.JTable table = GuiMain.createTable(data);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int rowToSelect = Math.max(0, previousRow);
        table.setRowSelectionInterval(rowToSelect, rowToSelect);
        table.scrollRectToVisible(table.getCellRect(rowToSelect, 0, true));
        JScrollPane scroll = new JScrollPane(table);
        step(prompt, scroll, table, submitLabel);
        int row = table.getSelectedRow();
        if (row < 0) throw new Cancelled();
        return table.convertRowIndexToModel(row);
    }

    static void reviewTrip(Object[][] details) {
        JPanel fields = new JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints cell = new java.awt.GridBagConstraints();
        cell.anchor = java.awt.GridBagConstraints.WEST;
        cell.insets = new java.awt.Insets(5, 0, 5, 18);
        for (int row = 0; row < details.length; row++) {
            cell.gridy = row;
            cell.gridx = 0;
            JLabel label = new JLabel(details[row][0].toString());
            label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
            fields.add(label, cell);
            cell.gridx = 1;
            fields.add(new JLabel(details[row][1].toString()), cell);
        }
        step("Review your booking", fields, null, "Proceed to payment");
    }

    static String selectSeat(List<String> seats, String previous) {
        JComboBox<String> choices = new JComboBox<>(seats.toArray(String[]::new));
        if (previous != null && seats.contains(previous)) choices.setSelectedItem(previous);
        step("Select an available seat", choices, choices, "Continue");
        return (String) choices.getSelectedItem();
    }

    static void showReceipt(PrintableReceipt receipt) {
        JPanel paper = new JPanel();
        paper.setLayout(new javax.swing.BoxLayout(paper, javax.swing.BoxLayout.Y_AXIS));
        paper.setBackground(java.awt.Color.WHITE);
        paper.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));

        receiptLine(paper, "KENYA RAILWAYS  /  MADARAKA EXPRESS", 12, true);
        receiptLine(paper, "Ticket receipt", 24, true);
        receiptLine(paper, receipt.getReceiptNumber(), 13, false);
        receiptDivider(paper);
        receiptLine(paper, "Journey", 14, true);
        receiptLine(paper, receipt.getPassengerName(), 13, false);
        receiptLine(paper, receipt.getTravelDetails(), 13, false);
        receiptDivider(paper);
        receiptLine(paper, "Payment", 14, true);
        receiptLine(paper, String.format("KES %,.2f paid", receipt.getAmountPaid()), 22, true);
        receiptLine(paper, receipt.getPaymentMethod() + "  ·  "
                + receipt.getPaymentDate().format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")), 13, false);
        receiptLine(paper, "Reference  " + receipt.getReferenceCode(), 13, false);
        receiptDivider(paper);
        receiptLine(paper, "Boarding code", 14, true);
        receiptLine(paper, receipt.getQrCodeData(), 12, false);

        JScrollPane scroll = new JScrollPane(paper);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        step("Receipt preview", scroll, null, "Done", 0, false);
    }

    private static void receiptLine(JPanel paper, String value, int size, boolean bold) {
        JTextArea text = new JTextArea(value);
        text.setEditable(false);
        text.setOpaque(false);
        text.setForeground(new java.awt.Color(42, 42, 42));
        text.setFont(text.getFont().deriveFont(bold ? java.awt.Font.BOLD : java.awt.Font.PLAIN, (float) size));
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setSize(460, Short.MAX_VALUE);
        text.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, text.getPreferredSize().height));
        text.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        text.setCaretPosition(0);
        paper.add(text);
    }

    private static void receiptDivider(JPanel paper) {
        javax.swing.JSeparator divider = new javax.swing.JSeparator();
        divider.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        divider.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 1));
        paper.add(javax.swing.Box.createVerticalStrut(10));
        paper.add(divider);
        paper.add(javax.swing.Box.createVerticalStrut(10));
    }

    private static void step(String prompt, JComponent input, JComponent focus, String submitLabel) {
        step(prompt, input, focus, submitLabel, 0, true);
    }

    private static void step(String prompt, JComponent input, JComponent focus, String submitLabel,
                             int delayMillis, boolean showCancel) {
        JPanel form = new JPanel(new BorderLayout(0, 12));
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JPanel header = new JPanel(new BorderLayout(0, 8));
        JLabel label = new JLabel(prompt);
        label.setLabelFor(focus);
        header.add(label, BorderLayout.SOUTH);

        String details = takeOutput();
        if (!details.isEmpty() && !details.contains("\n")) {
            header.add(new JLabel(details), BorderLayout.CENTER);
        } else if (!details.isEmpty()) {
            JTextArea text = new JTextArea(details, 4, 44);
            text.setEditable(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setCaretPosition(0);
            text.setOpaque(false);
            header.add(text, BorderLayout.CENTER);
        }
        form.add(header, BorderLayout.NORTH);
        if (input instanceof JScrollPane) {
            form.add(input, BorderLayout.CENTER);
        } else if (input != null) {
            JPanel fields = new JPanel(new BorderLayout());
            fields.add(input, BorderLayout.NORTH);
            form.add(fields, BorderLayout.CENTER);
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        JButton submit = new JButton(submitLabel);
        JButton back = new JButton("Back");
        if (showCancel) buttons.add(cancel);
        if (showCancel && backEnabled) buttons.add(back);
        if (delayMillis == 0) buttons.add(submit);
        form.add(buttons, BorderLayout.SOUTH);

        SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
        boolean[] accepted = {false};
        boolean[] wentBack = {false};
        back.addActionListener(event -> {
            wentBack[0] = true;
            cancelActiveStep();
        });
        cancel.addActionListener(event -> cancelActiveStep());
        submit.addActionListener(event -> {
            accepted[0] = true;
            loop.exit();
        });
        Timer timer = new Timer(delayMillis, event -> {
            accepted[0] = true;
            loop.exit();
        });
        timer.setRepeats(false);
        cancelStep = () -> {
            timer.stop();
            loop.exit();
        };
        app.showStep(form, delayMillis == 0 ? submit : null);
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (focus != null) focus.requestFocusInWindow();
            else if (delayMillis == 0) submit.requestFocusInWindow();
            else cancel.requestFocusInWindow();
        });
        // Keep the current workflow on the stack while Swing handles input on this page.
        try {
            if (delayMillis > 0) timer.start();
            loop.enter();
        } finally {
            timer.stop();
            cancelStep = null;
            app.endStep();
        }
        if (!app.isOpen()) throw new Cancelled();
        if (wentBack[0]) throw new Back();
        if (!accepted[0]) throw new Cancelled();
    }

    static int select(String title, String... options) {
        JComboBox<String> choices = new JComboBox<>(options);
        step(title, choices, choices, "Continue");
        return choices.getSelectedIndex() + 1;
    }

    static <T> T choose(String prompt, List<T> choices, Function<T, String> label) {
        return choose(prompt, choices, label, choices.size());
    }

    static <T> T choose(String prompt, List<T> choices, Function<T, String> label, int selectableCount) {
        return choose(prompt, choices, label, selectableCount, null);
    }

    static <T> T choose(String prompt, List<T> choices, Function<T, String> label, int selectableCount, T previous) {
        String[] labels = choices.subList(0, selectableCount).stream().map(label).toArray(String[]::new);
        JList<String> list = new JList<>(labels);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(Math.min(8, labels.length));
        int previousIndex = previous == null ? -1 : choices.subList(0, selectableCount).indexOf(previous);
        list.setSelectedIndex(Math.max(0, previousIndex));
        JScrollPane scroll = new JScrollPane(list);
        JPanel options = new JPanel(new BorderLayout(0, 6));
        if (selectableCount < choices.size()) {
            options.add(list, BorderLayout.CENTER);
            JPanel unavailable = new JPanel(new java.awt.GridLayout(0, 1, 0, 4));
            for (T choice : choices.subList(selectableCount, choices.size())) {
                JLabel row = new JLabel(label.apply(choice) + " — Unavailable for booking now");
                row.setForeground(UIManager.getColor("Label.disabledForeground") == null
                        ? java.awt.Color.GRAY : UIManager.getColor("Label.disabledForeground"));
                unavailable.add(row);
            }
            options.add(unavailable, BorderLayout.SOUTH);
        }
        step(prompt, selectableCount < choices.size() ? options : scroll, list, "Continue");
        int selected = list.getSelectedIndex();
        if (selected < 0) throw new Cancelled();
        return choices.get(selected);
    }

    static String optional(String prompt) {
        JTextField field = new JTextField(32);
        step(prompt, field, field, "Continue");
        return field.getText().trim();
    }

    static String text(String prompt) {
        while (true) {
            String value = optional(prompt);
            if (!value.isEmpty()) return value;
            println("Please enter a value.");
        }
    }

    static String validatedText(String prompt, String previous, Predicate<String> valid, String error) {
        JTextField field = new JTextField(previous, 32);
        JLabel feedback = new JLabel(" ");
        feedback.setForeground(new java.awt.Color(75, 75, 75));
        JPanel input = new JPanel(new BorderLayout(0, 6));
        input.add(field, BorderLayout.NORTH);
        input.add(feedback, BorderLayout.SOUTH);
        while (true) {
            step(prompt, input, field, "Continue");
            String value = field.getText().trim();
            if (valid.test(value)) return value;
            feedback.setText(error);
        }
    }

    static String password(String prompt) {
        while (true) {
            JPasswordField field = new JPasswordField(20);
            step(prompt, field, field, "Continue");
            char[] characters = field.getPassword();
            String value = new String(characters).trim();
            Arrays.fill(characters, '\0');
            if (!value.isEmpty()) return value;
            println("Please enter a value.");
        }
    }

    static int integer(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(optional(prompt));
            } catch (NumberFormatException e) {
                println("Please enter a whole number.");
            }
        }
    }

    static double number(String prompt) {
        while (true) {
            try {
                return Double.parseDouble(optional(prompt));
            } catch (NumberFormatException e) {
                println("Please enter a number.");
            }
        }
    }

    static LocalTime time(String prompt) {
        while (true) {
            try {
                return LocalTime.parse(optional(prompt));
            } catch (DateTimeParseException e) {
                println("Use the 24-hour format HH:MM, e.g. 08:00.");
            }
        }
    }

    static boolean confirm(String prompt) {
        try {
            step(prompt, null, null, "Confirm");
            return true;
        } catch (Cancelled e) {
            if (!app.isOpen()) throw e;
            return false;
        }
    }

    static final class Back extends RuntimeException { }

    static final class Cancelled extends RuntimeException { }
}
