package net.thorioum.gui.components;

import net.thorioum.gui.Theme;

import javax.swing.*;
import java.awt.*;

public class LoadingBar extends JProgressBar {
    public LoadingBar() {
        super(0, 100);
        setOpaque(false);
        setBorderPainted(false);
        setStringPainted(true);
        setFont(Theme.font(11, Font.BOLD));
        setForeground(Theme.ACCENT_BLUE);
        setBackground(new Color(0x0E1522));
        setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override protected void paintDeterminate(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = c.getWidth(), h = c.getHeight();
                int arc = h/2;

                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                int fill = (int) (w * (getPercentComplete()));
                g2.setColor(Theme.ACCENT_PURPLE);
                g2.fillRoundRect(0, 0, fill, h, arc, arc);

                g2.setColor(new Color(255, 255, 255, 40));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

                if (isStringPainted()) {
                    g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics();
                    String s = getString();
                    int tx = (w - fm.stringWidth(s)) / 2;
                    int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                    g2.setColor(Theme.TEXT_PRIMARY);
                    g2.drawString(s, tx, ty);
                }

                g2.dispose();
            }

            @Override protected void paintIndeterminate(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = c.getWidth(), h = c.getHeight();

                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, w, h, h, h);
                g2.dispose();

                super.paintIndeterminate(g, c);
            }
        });
    }

    public void setProgress(int percent) {
        setIndeterminate(false);
        setValue(Math.max(0, Math.min(100, percent)));
    }

    public void setText(String text) {
        setString(text == null ? "" : text);
    }
}
