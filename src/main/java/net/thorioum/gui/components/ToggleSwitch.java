package net.thorioum.gui.components;

import net.thorioum.gui.Theme;

import javax.swing.*;
import java.awt.*;

public class ToggleSwitch extends JToggleButton {
    public ToggleSwitch(boolean initial) {
        setSelected(initial);
        setOpaque(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(Theme.font(12, Font.BOLD));
        setForeground(Theme.TEXT_PRIMARY);
        setPreferredSize(new Dimension(10, 30));

        addActionListener(e -> repaint());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int trackH = getHeight();
        int trackW = getWidth();
        int trackX = 0;
        int trackY = 0;

        int arc = 4;

        boolean on = isSelected();

        Color trackOff = Theme.FIELD_BG;
        Color trackOn = Theme.ACCENT_PURPLE;

        g2.setColor(on ? trackOn : trackOff);
        g2.fillRoundRect(trackX, trackY, trackW, trackH, arc, arc);

        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(trackX, trackY, trackW - 1, trackH - 1, arc, arc);

        int knob = trackH - 6;
        int knobY = trackY + 3;
        int knobX = on ? (trackX + trackW - knob - 3) : (trackX + 3);

        g2.setColor(Theme.CARD);
        int knobArc = 6;
        g2.fillRoundRect(knobX, knobY, knob, knob, knobArc, knobArc);

        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(knobX, knobY, knob - 1, knob - 1, knobArc, knobArc);

        String s = on ? "ON" : "OFF";
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int tx = trackX + (trackW - fm.stringWidth(s)) / 2;
        int ty = trackY + (trackH - fm.getHeight()) / 2 + fm.getAscent();
        g2.setColor(Theme.TEXT_PRIMARY);
        g2.drawString(s, tx, ty);

        g2.dispose();
    }
}
