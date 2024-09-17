import GUI.DesktopAppXAI;
import app.DesktopApp;

public class Main {
    private static DesktopApp desktopApp = null;

    public static void main(final String[] args)
    {
        // The actual launching
        if (args.length == 0)
        {
            desktopApp = new DesktopAppXAI();
            desktopApp.createDesktopApp();
        }
//        else
//        {
//            PlayerCLI.runCommand(args);
//        }
    }

    // Used in case any agents need DesktopApp functions.
    public static DesktopApp desktopApp()
    {
        return desktopApp;
    }
}