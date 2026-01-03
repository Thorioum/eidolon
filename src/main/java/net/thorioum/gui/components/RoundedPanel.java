package net.thorioum.gui.components;

import net.thorioum.gui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

public class RoundedPanel extends JPanel {
    private final int radius;
    private final Color bg;

    private final boolean roundTL;
    private final boolean roundTR;
    private final boolean roundBR;
    private final boolean roundBL;

    public RoundedPanel(Color bg, int radius, boolean tl, boolean tr, boolean br, boolean bl) {
        this.bg = bg;
        this.radius = Math.max(0, radius);
        this.roundTL = tl;
        this.roundTR = tr;
        this.roundBR = br;
        this.roundBL = bl;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        Shape shape = createCornerShape(w, h, radius, roundTL, roundTR, roundBR, roundBL);

        g2.setComposite(AlphaComposite.Src);
        g2.setColor(new Color(0, 0, 0, 0));
        g2.fillRect(0, 0, w, h);
        g2.setComposite(AlphaComposite.SrcOver);

        g2.setColor(bg);
        g2.fill(shape);

        g2.setColor(new Color(
                Theme.BORDER.getRed(),
                Theme.BORDER.getGreen(),
                Theme.BORDER.getBlue(),
                140
        ));
        g2.draw(shape);

        g2.dispose();
    }

    @Override
    protected void paintChildren(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Shape shape = createCornerShape(getWidth(), getHeight(), radius, roundTL, roundTR, roundBR, roundBL);
        g2.clip(shape);

        super.paintChildren(g2);
        g2.dispose();
    }

    private static Shape createCornerShape(int w, int h, int r,
                                           boolean tl, boolean tr, boolean br, boolean bl) {
        r = Math.min(r, Math.min(w, h) / 2);

        Path2D.Float p = new Path2D.Float();

        p.moveTo(0, tl ? r : 0);

        if (tl) p.quadTo(0, 0, r, 0);
        else    p.lineTo(0, 0);

        p.lineTo(w - (tr ? r : 0), 0);

        if (tr) p.quadTo(w, 0, w, r);
        else    p.lineTo(w, 0);

        p.lineTo(w, h - (br ? r : 0));

        if (br) p.quadTo(w, h, w - r, h);
        else    p.lineTo(w, h);

        p.lineTo(bl ? r : 0, h);

        if (bl) p.quadTo(0, h, 0, h - r);
        else    p.lineTo(0, h);

        p.closePath();
        return p;
    }
}
