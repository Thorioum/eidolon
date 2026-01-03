package net.thorioum.gui.components;

import net.thorioum.gui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class CancelButton extends JButton {

    public CancelButton(String text) {
        super(text);
        setFont(Theme.font(12, Font.BOLD));
        setForeground(Theme.TEXT_PRIMARY);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(10, 16, 10, 16));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { repaint(); }
            @Override public void mouseExited(MouseEvent e) { repaint(); }
            @Override public void mousePressed(MouseEvent e) { repaint(); }
            @Override public void mouseReleased(MouseEvent e) { repaint(); }
        });
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        boolean hover = getModel().isRollover();
        boolean pressed = getModel().isPressed();
        boolean enabled = isEnabled();

        Color base = enabled ? new Color(0xC62828) : new Color(Theme.BORDER.getRGB());
        Color top  = enabled ? new Color(0xFF5252) : new Color(Theme.BORDER.getRGB());

        int arc = 16;
        Shape rr = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc);

        Color fill = blend(top, base, 0.55f);
        if (hover) fill = blend(top, base, 0.40f);
        if (pressed) fill = blend(top, base, 0.70f);

        g2.setColor(fill);
        g2.fill(rr);

        g2.setColor(new Color(255, 255, 255, enabled ? 40 : 25));
        g2.draw(rr);

        g2.dispose();
        super.paintComponent(g);
    }

    private static Color blend(Color a, Color b, float t) {
        int r = (int) (a.getRed() * (1 - t) + b.getRed() * t);
        int gr = (int) (a.getGreen() * (1 - t) + b.getGreen() * t);
        int bl = (int) (a.getBlue() * (1 - t) + b.getBlue() * t);
        return new Color(r, gr, bl);
    }
}
