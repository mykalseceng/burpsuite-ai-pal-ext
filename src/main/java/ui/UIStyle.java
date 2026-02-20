package ui;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

public final class UIStyle {
    private static final Color FALLBACK_BORDER_COLOR = new Color(130, 130, 130);

    private UIStyle() {
    }

    public static Border createFieldBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(resolveBorderColor(), 1),
                BorderFactory.createEmptyBorder(1, 1, 1, 1)
        );
    }

    public static Border createSectionBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(resolveBorderColor(), 1),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP
        );
    }

    public static void applyTextInputPadding(JTextArea area) {
        area.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
    }

    private static Color resolveBorderColor() {
        Color color = UIManager.getColor("Component.borderColor");
        if (color != null) {
            return color;
        }
        color = UIManager.getColor("Separator.foreground");
        if (color != null) {
            return color;
        }
        return FALLBACK_BORDER_COLOR;
    }
}
