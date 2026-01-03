package net.thorioum.gui;

import net.thorioum.gui.components.ThemedSpinnerUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;

public class Theme {
    public static final Color OBSIDIAN = new Color(0x080012);
    public static final Color CARD = new Color(0x17152C);
    public static final Color FIELD_BG = new Color(0x121215);
    public static final Color BORDER = new Color(0x241E49);

    public static final Color TEXT_PRIMARY = new Color(0xE7ECF5);
    public static final Color TEXT_MUTED = new Color(0xAEB7C6);

    public static final Color ACCENT_BLUE = new Color(0x6A4DD1);
    public static final Color ACCENT_PURPLE = new Color(0x9660EC);

    public static void apply() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        UIManager.put("control", CARD);
        UIManager.put("info", CARD);
        UIManager.put("nimbusBase", new Color(0x22324B));
        UIManager.put("nimbusBlueGrey", new Color(0x1A2436));
        UIManager.put("nimbusLightBackground", OBSIDIAN);
        UIManager.put("text", TEXT_PRIMARY);

        UIManager.put("ScrollBar.thumb", new Color(0x22324B));
        UIManager.put("ScrollBar.thumbDarkShadow", new Color(0x22324B));
        UIManager.put("ScrollBar.thumbHighlight", new Color(0x22324B));
        UIManager.put("ScrollBar.thumbShadow", new Color(0x22324B));
    }

    public static Font font(int size, int style) {
        Font base = UIManager.getFont("Label.font");
        if (base == null) base = new Font("SansSerif", Font.PLAIN, 12);
        return base.deriveFont(style, (float) size);
    }





    public static void styleTextField(JTextField f) {
        f.setFont(Theme.font(12, Font.PLAIN));
        f.setForeground(Theme.TEXT_PRIMARY);
        f.setBackground(Theme.FIELD_BG);
        f.setCaretColor(Theme.ACCENT_BLUE);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));
    }

    public static void styleFormattedTextField(JFormattedTextField f) {
        f.setFont(Theme.font(12, Font.PLAIN));
        f.setForeground(Theme.TEXT_PRIMARY);
        f.setBackground(Theme.FIELD_BG);
        f.setCaretColor(Theme.ACCENT_BLUE);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));
        f.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
    }

    public static void styleSpinner(JSpinner sp) {
        sp.setUI(new ThemedSpinnerUI());
        sp.setOpaque(false);

        sp.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                new EmptyBorder(0, 0, 0, 0)
        ));

        JComponent editor = sp.getEditor();
        if (editor instanceof JSpinner.DefaultEditor de) {
            JFormattedTextField tf = de.getTextField();
            tf.setFont(Theme.font(12, Font.PLAIN));
            tf.setForeground(Theme.TEXT_PRIMARY);
            tf.setBackground(Theme.FIELD_BG);
            tf.setCaretColor(Theme.ACCENT_BLUE);
            tf.setBorder(new EmptyBorder(8, 10, 8, 10));
            tf.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
        }
    }

    public static void styleScrollPane(JScrollPane sp) {
        if (sp == null) return;

        sp.setBorder(null);
        sp.setOpaque(false);

        if (sp.getViewport() != null) {
            sp.getViewport().setOpaque(true);
            sp.getViewport().setBackground(Theme.FIELD_BG);
        }

        JScrollBar vb = sp.getVerticalScrollBar();
        if (vb != null) {
            vb.setOpaque(false);
            vb.setBackground(Theme.FIELD_BG);
            vb.setPreferredSize(new Dimension(10, Integer.MAX_VALUE));
            vb.setUnitIncrement(24);

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
                protected JButton createDecreaseButton(int orientation) { return zeroButton(); }

                @Override
                protected JButton createIncreaseButton(int orientation) { return zeroButton(); }

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
    }

    public static void styleComboBox(JComboBox<?> cb) {
        cb.setFont(Theme.font(12, Font.PLAIN));
        cb.setForeground(Theme.TEXT_PRIMARY);
        cb.setBackground(Theme.FIELD_BG);

        cb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));

        cb.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton b = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);

                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        int w = getWidth();
                        int h = getHeight();
                        int s = 6;
                        int cx = w / 2;
                        int cy = h / 2;

                        g2.setColor(Theme.TEXT_MUTED);
                        Polygon tri = new Polygon(
                                new int[]{cx - s, cx + s, cx},
                                new int[]{cy - 2, cy - 2, cy + s},
                                3
                        );
                        g2.fillPolygon(tri);

                        g2.dispose();
                    }
                };

                b.setOpaque(false);
                b.setContentAreaFilled(false);
                b.setBorderPainted(false);
                b.setFocusPainted(false);
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                b.setPreferredSize(new Dimension(28, 0));
                return b;
            }

            @Override
            protected ComboPopup createPopup() {
                return new BasicComboPopup(comboBox) {{
                    list.setBackground(Theme.FIELD_BG);
                    list.setForeground(Theme.TEXT_PRIMARY);
                    list.setSelectionBackground(new Color(0x22324B));
                    list.setSelectionForeground(Theme.TEXT_PRIMARY);
                }

                    @Override
                    protected JScrollPane createScroller() {
                        JScrollPane sc = super.createScroller();
                        sc.getViewport().setOpaque(true);
                        sc.getViewport().setBackground(Theme.FIELD_BG);
                        Theme.styleScrollPane(sc);
                        return sc;
                    }
                };
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(cb.getBackground());
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });
    }
}
