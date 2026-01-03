package net.thorioum.gui.components;

import net.thorioum.gui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class MultiSelectDropdown extends JButton {
    private final JPopupMenu menu = new JPopupMenu();
    private final JPanel listPanel = new JPanel();
    private final JScrollPane scrollPane;

    private final String baseLabel;
    private final List<OptionRow> rows = new ArrayList<>();

    private final Color normalBg   = Theme.FIELD_BG;
    private final Color hoverBg    = new Color(0x1A2436);
    private final Color selectedBg = new Color(0x22324B);
    private final Color rowBorder  = new Color(
            Theme.BORDER.getRed(), Theme.BORDER.getGreen(), Theme.BORDER.getBlue(), 120
    );

    public MultiSelectDropdown(String label, String... options) {
        super(label);
        this.baseLabel = label;

        setFont(Theme.font(12, Font.PLAIN));
        setForeground(Theme.TEXT_PRIMARY);
        setBackground(Theme.FIELD_BG);
        setFocusPainted(false);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
        setContentAreaFilled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        menu.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1, true));
        menu.setOpaque(true);
        menu.setBackground(Theme.FIELD_BG);

        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Theme.FIELD_BG);
        listPanel.setOpaque(true);

        scrollPane = new JScrollPane(listPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBorder(null);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(Theme.FIELD_BG);
        scrollPane.setOpaque(false);

        scrollPane.setPreferredSize(new Dimension(198, 180));
        scrollPane.getVerticalScrollBar().setUnitIncrement(24);

        styleScrollBar(scrollPane);

        menu.add(scrollPane);

        addActionListener(e -> {
            refreshLabel();
            menu.show(this, 0, getHeight());
        });

        setOptions(options);
    }

    public void setOptions(String... options) {
        rows.clear();
        listPanel.removeAll();

        for (String opt : options) {
            OptionRow row = new OptionRow(opt);
            rows.add(row);
            listPanel.add(row);
        }

        refreshLabel();
        listPanel.revalidate();
        listPanel.repaint();
    }

    public List<String> getSelected() {
        List<String> out = new ArrayList<>();
        for (OptionRow r : rows) {
            if (r.selected) out.add(r.text);
        }
        return out;
    }

    private void refreshLabel() {
        List<String> sel = getSelected();
        setText(baseLabel + ": " + sel.size() + "/" + rows.size());
    }

    private void styleScrollBar(JScrollPane sp) {
        JScrollBar vb = sp.getVerticalScrollBar();
        vb.setOpaque(false);
        vb.setBackground(Theme.FIELD_BG);
        vb.setPreferredSize(new Dimension(10, Integer.MAX_VALUE));

        vb.setUI(new BasicScrollBarUI() {
            private final Color track = Theme.FIELD_BG;
            private final Color thumb = new Color(
                    Theme.ACCENT_BLUE.getRed(),
                    Theme.ACCENT_BLUE.getGreen(),
                    Theme.ACCENT_BLUE.getBlue(),
                    140
            );

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(track);
                g2.fillRoundRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height, 10, 10);

                g2.setColor(new Color(Theme.BORDER.getRed(), Theme.BORDER.getGreen(), Theme.BORDER.getBlue(), 120));
                g2.drawRoundRect(trackBounds.x, trackBounds.y, trackBounds.width - 1, trackBounds.height - 1, 10, 10);

                g2.dispose();
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int arc = 10;
                int x = thumbBounds.x + 2;
                int y = thumbBounds.y + 2;
                int w = thumbBounds.width - 4;
                int h = thumbBounds.height - 4;

                g2.setColor(thumb);
                g2.fillRoundRect(x, y, w, h, arc, arc);

                g2.setColor(new Color(255, 255, 255, 40));
                g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);

                g2.dispose();
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return zeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return zeroButton();
            }

            private JButton zeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                b.setOpaque(false);
                b.setContentAreaFilled(false);
                b.setBorderPainted(false);
                return b;
            }
        });
    }

    private final class OptionRow extends JPanel {
        private final String text;
        private boolean selected = false;

        private final JLabel label = new JLabel();

        private OptionRow(String text) {
            this.text = text;

            setLayout(new BorderLayout());
            setOpaque(true);
            setBackground(normalBg);

            label.setText(text);
            label.setForeground(Theme.TEXT_PRIMARY);
            label.setFont(Theme.font(12, Font.PLAIN));
            label.setBorder(new EmptyBorder(8, 10, 8, 10));

            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, rowBorder));

            add(label, BorderLayout.CENTER);
            setPreferredSize(new Dimension(10, 18));

            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    if (!selected) setBackground(hoverBg);
                }

                @Override public void mouseExited(MouseEvent e) {
                    if (!selected) setBackground(normalBg);
                }

                @Override public void mousePressed(MouseEvent e) {
                    setSelected(!selected);
                    refreshLabel();
                }
            };

            addMouseListener(ma);
            label.addMouseListener(ma);
        }

        void setSelected(boolean sel) {
            this.selected = sel;
            setBackground(sel ? selectedBg : normalBg);
        }
    }
}
