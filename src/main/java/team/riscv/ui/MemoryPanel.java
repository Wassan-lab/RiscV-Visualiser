package team.riscv.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
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
        super(new BorderLayout(8, 8));
        UiStyle.pad(this);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UiStyle.title("Data memory"), BorderLayout.NORTH);
        JPanel jump = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        jump.setOpaque(false);
        JButton go = new JButton("Go to address");
        jump.add(addressInput);
        jump.add(go);
        top.add(jump, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        table.setRowHeight(26);
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
                if (!selected) {
                    cell.setBackground(writes.contains(row * 4) ? UiStyle.WRITE
                            : (row % 2 == 0 ? Color.WHITE : UiStyle.LIGHT));
                    cell.setForeground(UiStyle.NAVY);
                }
                return cell;
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        add(scroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(new JLabel("4096 bytes | Little-endian | Yellow = SW this cycle"), BorderLayout.NORTH);
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
