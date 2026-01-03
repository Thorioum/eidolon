package net.thorioum.gui.components;

import net.thorioum.gui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;

public class ThemedArrowButton extends JButton {
    private final int dir; //+1 up, -1 down

    public ThemedArrowButton(int dir) {
        this.dir = dir;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(26, 14));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { repaint(); }
            @Override public void mouseExited(MouseEvent e)  { repaint(); }
            @Override public void mousePressed(MouseEvent e) { repaint(); }
            @Override public void mouseReleased(MouseEvent e){ repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        boolean hover = getModel().isRollover();
        boolean pressed = getModel().isPressed();
        boolean enabled = isEnabled();

        Color bg = Theme.FIELD_BG;
        Color border = Theme.BORDER;

        Color fill = bg;
        if (enabled && hover)   fill = new Color(0x1A2436);
        if (enabled && pressed) fill = new Color(0x22324B);

        int w = getWidth();
        int h = getHeight();
        int arc = 6;

        g2.setColor(fill);
        g2.fillRoundRect(0, 0, w, h, arc, arc);

        g2.setColor(new Color(border.getRed(), border.getGreen(), border.getBlue(), 160));
        g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

        g2.setColor(enabled ? Theme.TEXT_PRIMARY : Theme.TEXT_MUTED);

        int cx = w / 2;
        int cy = h / 2;
        int size = Math.max(2, Math.min(w, h) / 7);

        Path2D p = new Path2D.Float();
        if (dir > 0) {
            p.moveTo(cx - size, cy + size / 2f);
            p.lineTo(cx,        cy - size);
            p.lineTo(cx + size, cy + size / 2f);
        } else {
            p.moveTo(cx - size, cy - size / 2f);
            p.lineTo(cx,        cy + size);
            p.lineTo(cx + size, cy - size / 2f);
        }

        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(p);

        g2.dispose();
    }
}
