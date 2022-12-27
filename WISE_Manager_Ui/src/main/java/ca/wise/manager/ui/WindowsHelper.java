package ca.wise.manager.ui;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.WString;

public class WindowsHelper {
    static {
        try {
            Native.register("shell32");
        }
        catch (UnsatisfiedLinkError e) {
        }
    }

    private static native NativeLong SetCurrentProcessExplicitAppUserModelID(WString appID);

    /**
     * Set the current processes ID. This allows multiple instances to be combined into one
     * taskbar icon in versions of Windows that support it.
     * 
     * @param appID
     */
    public static void setCurrentProcessExplicitAppUserModelID(final String appID) {
        String os = System.getProperty("os.name");
        if (!os.contains("Windows"))
            return;
        String split[] = os.split(" ");
        if (split.length < 2)
            return;
        int ver;
        try {
            ver = Integer.parseInt(split[1]);
            if (ver < 7)
                return;
        }
        catch (NumberFormatException e) {
            return;
        }
        try {
            SetCurrentProcessExplicitAppUserModelID(new WString(appID));
        }
        catch (UnsatisfiedLinkError e) {
        }
    }
}
