package net.thorioum.gui.components;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.DecimalFormat;

public class NumericFields {
    public static JFormattedTextField intField(int initial, int min, int max) {
        NumberFormatter nf = new NumberFormatter(new DecimalFormat("#"));
        nf.setValueClass(Integer.class);
        nf.setMinimum(min);
        nf.setMaximum(max);
        nf.setAllowsInvalid(true);
        nf.setCommitsOnValidEdit(true);

        JFormattedTextField f = new JFormattedTextField(nf);
        f.setValue(initial);
        f.setColumns(10);
        return f;
    }

    public static JFormattedTextField doubleField(double initial, double min, double max) {
        NumberFormatter nf = new NumberFormatter(new DecimalFormat("#0.########"));
        nf.setValueClass(Double.class);
        nf.setMinimum(min);
        nf.setMaximum(max);
        nf.setAllowsInvalid(true);
        nf.setCommitsOnValidEdit(true);

        JFormattedTextField f = new JFormattedTextField(nf);
        f.setValue(initial);
        f.setColumns(10);
        return f;
    }

    public static JSpinner intSpinner(int initial, int min, int max, int step) {
        SpinnerNumberModel model = new SpinnerNumberModel(initial, min, max, step);
        JSpinner sp = new JSpinner(model);

        JSpinner.NumberEditor ed = new JSpinner.NumberEditor(sp, "#");
        sp.setEditor(ed);

        JFormattedTextField tf = ed.getTextField();
        ((AbstractDocument) tf.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string != null && string.matches("\\d+")) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text == null || text.isEmpty() || text.matches("\\d+")) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });

        Runnable snap = () -> {
            try {
                sp.commitEdit();
            } catch (Exception ignored) {}

            int v = ((Number) sp.getValue()).intValue();
            int snapped = snapToStep(v, min, max, step);
            if (snapped != v) sp.setValue(snapped);
        };

        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { snap.run(); }
        });

        sp.addChangeListener(e -> {
            int v = ((Number) sp.getValue()).intValue();
            int snapped = snapToStep(v, min, max, step);
            if (snapped != v) sp.setValue(snapped);
        });

        tf.setColumns(10);

        return sp;
    }

    private static int snapToStep(int value, int min, int max, int step) {
        int clamped = Math.max(min, Math.min(max, value));
        int snapped = ((clamped + step / 2) / step) * step;
        return Math.max(min, Math.min(max, snapped));
    }
}
