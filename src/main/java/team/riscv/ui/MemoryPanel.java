package team.riscv.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import team.riscv.model.MemoryAccess;

/** One row per word; the four byte columns make byte addresses visible. */
public final class MemoryPanel extends JPanel {
    private final MemoryTable model = new MemoryTable();
    private final JTable table = new JTable(model);
    private final JLabel status = new JLabel("No stores in this cycle.");
    private final JTextField addressInput = new JTextField("0x0000", 9);
    private Set<Integer> writes = Set.of();

    public MemoryPanel() {
        super(new BorderLayout(0, 14));
        setBackground(UiTheme.PAGE);
        setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UiTheme.label("Data Memory", 26, true, UiTheme.INK), BorderLayout.NORTH);
        JPanel jump = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        jump.setOpaque(false);
        JButton go = UiTheme.button("Go to address", true);
        addressInput.setFont(UiTheme.mono(13));
        addressInput.setForeground(UiTheme.INK);
        addressInput.setBackground(UiTheme.WHITE);
        addressInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.LINE),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        jump.add(addressInput);
        jump.add(go);
        top.add(jump, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        table.setFont(UiTheme.mono(13));
        table.setForeground(UiTheme.INK);
        table.setBackground(UiTheme.WHITE);
        table.setGridColor(new Color(235, 241, 249));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(198, 220, 252));
        table.setSelectionForeground(UiTheme.INK);
        table.getTableHeader().setBackground(UiTheme.TINT);
        table.getTableHeader().setForeground(UiTheme.INK);
        table.getTableHeader().setFont(UiTheme.font(12, true));
        table.getTableHeader().setPreferredSize(new Dimension(100, 42));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setDefaultRenderer((t, value, selected, focused, row, column) -> {
            JLabel label = new JLabel(String.valueOf(value), SwingConstants.CENTER);
            label.setOpaque(true);
            label.setBackground(UiTheme.TINT);
            label.setForeground(UiTheme.INK);
            label.setFont(UiTheme.font(12, true));
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, column == 4 || column == 5 ? UiTheme.LINE : new Color(232, 239, 248)),
                    BorderFactory.createEmptyBorder(0, 8, 0, 8)));
            return label;
        });
        table.setRowHeight(32);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        int[] widths = {90, 65, 65, 65, 65, 120, 115};
        for (int column = 0; column < widths.length; column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
        }
        table.getColumnModel().getColumn(5).setMinWidth(100);
        table.getColumnModel().getColumn(6).setMinWidth(95);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                    boolean focus, int row, int column) {
                Component cell = super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                if (selected) {
                    cell.setBackground(new Color(198, 220, 252));
                    cell.setForeground(UiTheme.INK);
                } else {
                    cell.setBackground(writes.contains(row * 4) ? new Color(173, 207, 250)
                            : (row % 2 == 0 ? Color.WHITE : UiTheme.PAGE));
                    cell.setForeground(UiTheme.INK);
                }
                if (cell instanceof JLabel label) {
                    // Match the header: byte columns centered; address and words left aligned.
                    label.setHorizontalAlignment(column >= 1 && column <= 4
                            ? SwingConstants.CENTER : SwingConstants.LEFT);
                    Color separator = new Color(235, 241, 249);
                    int right = column == 0 || column == 4 || column == 5 ? 1 : 0;
                    int leftAccent = column == 0 && selected ? 3 : 0;
                    label.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, leftAccent, 1, right,
                                    leftAccent > 0 ? UiTheme.BLUE : separator),
                            BorderFactory.createEmptyBorder(0, column >= 1 && column <= 4 ? 2 : 10, 0, 6)));
                    // Draw the right-hand group divider separately when a selected row has an accent.
                    if (leftAccent > 0) label.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 3, 1, right, UiTheme.BLUE),
                            BorderFactory.createEmptyBorder(0, 7, 0, 6)));
                }
                return cell;
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        UiTheme.styleScroll(scroll);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        // A single rounded surface surrounds the table; cells stay aligned and readable.
        JPanel tableCard = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(UiTheme.WHITE);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g.setColor(UiTheme.LINE);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        tableCard.setOpaque(false);
        tableCard.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        tableCard.add(scroll, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(UiTheme.label("4096 bytes  |  Little-endian  |  Blue highlight = SW this cycle",
                12, false, UiTheme.MUTED), BorderLayout.NORTH);
        status.setFont(UiTheme.font(12, false));
        status.setForeground(UiTheme.BLUE);
        bottom.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        bottom.add(status, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
        go.addActionListener(e -> jumpToInput());
        addressInput.addActionListener(e -> jumpToInput());
    }

    public void refresh(byte[] bytes, Map<Integer, Integer> memoryWrites) {
        if (bytes == null || bytes.length != MemoryAccess.SIZE_BYTES) {
            throw new IllegalArgumentException("Supply all 4096 memory bytes.");
        }
        for (int address : memoryWrites.keySet()) {
            if (address < 0 || address > MemoryAccess.SIZE_BYTES - 4 || address % 4 != 0) {
                throw new IllegalArgumentException("Invalid store address: " + address);
            }
        }
        writes = Set.copyOf(memoryWrites.keySet());
        model.bytes = bytes.clone();
        model.fireTableDataChanged();
        status.setText(writes.isEmpty() ? "No stores in this cycle."
                : "Stored at " + writes.stream().sorted().map(a -> String.format("0x%04X", a))
                    .reduce((a, b) -> a + ", " + b).orElse(""));
    }

    private void jumpToInput() {
        try {
            String value = addressInput.getText().trim();
            int address = value.startsWith("0x") || value.startsWith("0X")
                    ? Integer.parseInt(value.substring(2), 16) : Integer.parseInt(value);
            if (address < 0 || address >= MemoryAccess.SIZE_BYTES) {
                throw new NumberFormatException();
            }
            int row = address / 4;
            table.setRowSelectionInterval(row, row);
            table.scrollRectToVisible(table.getCellRect(row, 0, true));
            status.setText(String.format("Byte 0x%04X is in this row, column +%d.", address, address % 4));
        } catch (NumberFormatException ex) {
            status.setText("Enter an address from 0 to 4095, in decimal or 0x hexadecimal.");
        }
    }

    private static final class MemoryTable extends AbstractTableModel {
        private final String[] headings = {"Address", "+0 (hex)", "+1 (hex)", "+2 (hex)",
                "+3 (hex)", "Word (hex)", "Word (signed)"};
        private byte[] bytes = new byte[MemoryAccess.SIZE_BYTES];

        @Override public int getRowCount() { return MemoryAccess.SIZE_BYTES / 4; }
        @Override public int getColumnCount() { return headings.length; }
        @Override public String getColumnName(int column) { return headings[column]; }

        @Override
        public Object getValueAt(int row, int column) {
            int address = row * 4;
            if (column == 0) return String.format("0x%04X", address);
            if (column <= 4) return String.format("%02X", Byte.toUnsignedInt(bytes[address + column - 1]));
            int word = 0;
            for (int i = 0; i < 4; i++) word |= Byte.toUnsignedInt(bytes[address + i]) << (8 * i);
            return column == 5 ? String.format("0x%08X", word) : Integer.toString(word);
        }
    }
}
