package net.thorioum.gui.components;

import net.thorioum.gui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class HelpDot extends JComponent {
    private final String tooltipText;
    private boolean hover = false;

    public HelpDot(String tooltipText) {
        this.tooltipText = tooltipText;

        Dimension d = new Dimension(18, 18);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(tooltipText);

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
        });
    }

    @Override public String getToolTipText(MouseEvent event) {
        if (tooltipText == null) return null;
        return "<html><div style='width:150px;'>" + escapeHtml(tooltipText) + "</div></html>";
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(hover ? Theme.ACCENT_BLUE : Theme.BORDER);
        g2.fillOval(0, 0, getWidth(), getHeight());

        g2.setColor(new Color(255, 255, 255, 50));
        g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);

        g2.setFont(Theme.font(12, Font.BOLD));
        FontMetrics fm = g2.getFontMetrics();
        String q = "?";
        int x = (getWidth() - fm.stringWidth(q) / 2) / 2 - 1;
        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

        g2.setColor(Theme.TEXT_PRIMARY);
        g2.drawString(q, x, y);

        g2.dispose();
    }
}
