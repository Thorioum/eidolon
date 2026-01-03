package net.thorioum.gui.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSpinnerUI;
import java.awt.*;

public class ThemedSpinnerUI extends BasicSpinnerUI {

    @Override
    protected Component createNextButton() {
        return createArrowButton(+1);
    }

    @Override
    protected Component createPreviousButton() {
        return createArrowButton(-1);
    }

    private JButton createArrowButton(int dir) {
        ThemedArrowButton b = new ThemedArrowButton(dir);

        b.addActionListener(e -> {
            try {
                spinner.commitEdit();
            } catch (Exception ignored) {}
            Object v = (dir > 0) ? spinner.getNextValue() : spinner.getPreviousValue();
            if (v != null) spinner.setValue(v);
        });

        b.setBorder(null);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);

        return b;
    }

    @Override
    protected void installNextButtonListeners(Component c) {
        super.installNextButtonListeners(c);
    }

    @Override
    protected void installPreviousButtonListeners(Component c) {
        super.installPreviousButtonListeners(c);
    }
}
