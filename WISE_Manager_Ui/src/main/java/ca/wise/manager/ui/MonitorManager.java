package ca.wise.manager.ui;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class MonitorManager {
    
    private static boolean isInitialized = false;
    private static List<Rectangle> screenBounds = new ArrayList<>();
    
    private static void initialize() {
        if (!isInitialized) {
            isInitialized = true;
            GraphicsDevice[] monitors = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            for (GraphicsDevice monitor : monitors) {
                Rectangle rect = monitor.getDefaultConfiguration().getBounds();
                screenBounds.add(rect);
            }
        }
    }
    
    /**
     * Check to see if a given point is within the valid screen bounds.
     * @param pt The point to check.
     * @return True if the point lies somewhere within the valid screen bounds, false if
     * the point is outside the screen.
     */
    public static boolean screenContains(Point pt) {
        initialize();
        for (Rectangle screen : screenBounds) {
            if (screen.contains(pt))
                return true;
        }
        return false;
    }
}
