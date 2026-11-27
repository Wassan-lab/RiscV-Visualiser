package team.riscv.ui;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import team.riscv.model.ProcessorSnapshot;

/** Read-only register display. Call updateSnapshot on Swing's EDT. */
public final class RegisterPanel extends JPanel {
    private int[] registers = new int[32];
    private Map<Integer, Integer> writes = Map.of();
    private final RegisterModel low = new RegisterModel(0);
    private final RegisterModel high = new RegisterModel(16);

    public RegisterPanel() {
        super(new BorderLayout(0, 12));
        setOpaque(false);
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(UiTheme.label("Registers", 16, true, UiTheme.INK), BorderLayout.WEST);
        heading.add(UiTheme.label("Blue row = written at WB", 12, false, UiTheme.BLUE), BorderLayout.EAST);
        add(heading, BorderLayout.NORTH);
        JPanel tables = UiTheme.card();
        tables.setLayout(new GridLayout(1, 2, 24, 0));
        tables.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        tables.add(table(low, "registers-low"));
        tables.add(table(high, "registers-high"));
        add(tables, BorderLayout.CENTER);
    }

    public void updateSnapshot(ProcessorSnapshot snapshot) {
        registers = snapshot.registers();
        writes = snapshot.registerWrites();
        low.fireTableDataChanged();
        high.fireTableDataChanged();
    }

    private JScrollPane table(RegisterModel model, String name) {
        JTable table = new JTable(model);
        table.setName(name);
        table.setFont(UiTheme.mono(12));
        table.setRowHeight(20);
        table.setBackground(UiTheme.WHITE);
        table.setPreferredScrollableViewportSize(new Dimension(440, 16 * 20));
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(239, 244, 250));
        table.setRowSelectionAllowed(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(100, 28));
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(t, value, false, false, row, column);
                setFont(UiTheme.font(11, true));
                setBackground(UiTheme.WHITE);
                setForeground(UiTheme.MUTED);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.LINE),
                        BorderFactory.createEmptyBorder(0, 10, 0, 10)));
                setHorizontalAlignment(column == 2 ? RIGHT : LEFT);
                return this;
            }
        });
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(t, value, false, false, row, column);
                int register = model.offset + row;
                setBackground(writes.containsKey(register) ? UiTheme.WRITE
                        : row % 2 == 0 ? Color.WHITE : new Color(250, 252, 255));
                setForeground(register == 0 ? UiTheme.MUTED : UiTheme.INK);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                setHorizontalAlignment(column == 2 ? RIGHT : LEFT);
                return this;
            }
        });
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        JScrollPane scroll = new JScrollPane(table);
        UiTheme.styleScroll(scroll);
        return scroll;
    }

    private final class RegisterModel extends AbstractTableModel {
        private final int offset;
        RegisterModel(int offset) { this.offset = offset; }
        public int getRowCount() { return 16; }
        public int getColumnCount() { return 3; }
        public String getColumnName(int column) {
            return switch (column) {
                case 0 -> "Register";
                case 1 -> "Hexadecimal";
                default -> "Signed decimal";
            };
        }
        public Object getValueAt(int row, int column) {
            int register = offset + row;
            return switch (column) {
                case 0 -> "x" + register + (register == 0 ? " (zero)" : "");
                case 1 -> String.format("0x%08X", registers[register]);
                default -> Integer.toString(registers[register]);
            };
        }
    }
}
