package GUI;

import app.DesktopApp;
import app.JFrameListener;
import app.PlayerApp;
import app.display.MainWindowDesktop;
import app.display.views.OverlayView;
import app.display.views.tabs.TabPage;
import app.display.views.tabs.TabView;
import app.display.views.tabs.pages.InfoPage;
import app.loading.FileLoading;
import app.util.SettingsDesktop;
import app.util.UserPreferences;
import app.utils.AnimationVisualsType;
import app.utils.MVCSetup;
import app.utils.SettingsExhibition;
import app.views.BoardView;
import app.views.View;
import app.views.players.PlayerView;
import app.views.tools.ToolView;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;

public class DesktopAppXAI extends DesktopApp {
    protected void createFrame() throws SQLException
    {
        super.createFrame();
        view = new MainWindowDesktopXAI(this);
        frame.setContentPane(view);
        frame.setSize(SettingsDesktop.defaultWidth, SettingsDesktop.defaultHeight);

        if (SettingsExhibition.exhibitionVersion)
        {
            frame.setUndecorated(true);
            frame.setResizable(false);
            frame.setSize(SettingsExhibition.exhibitionDisplayWidth, SettingsExhibition.exhibitionDisplayHeight);
        }

        try
        {
            if (settingsPlayer().defaultX() == -1 || settingsPlayer().defaultY() == -1)
                frame.setLocationRelativeTo(null);
            else
                frame.setLocation(settingsPlayer().defaultX(), settingsPlayer().defaultY());

            if (settingsPlayer().frameMaximised())
                frame.setExtendedState(frame.getExtendedState() | Frame.MAXIMIZED_BOTH);
        }
        catch (final Exception e)
        {
            frame.setLocationRelativeTo(null);
        }

        frame.setVisible(true);
        frame.setMinimumSize(new Dimension(minimumViewWidth, minimumViewHeight));

        FileLoading.createFileChoosers();
        setCurrentGraphicsDevice(frame.getGraphicsConfiguration().getDevice());

        // gets called when the app is closed (save preferences and trial)
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                appClosedTasks();
            }
        });

        loadInitialGame(true);
    }

    private class MainWindowDesktopXAI extends MainWindowDesktop {

        private DesktopApp app;

        /**
         * Constructor.
         *
         * @param app
         */
        public MainWindowDesktopXAI(DesktopApp app) {
            super(app);
            this.app = app;
        }

        /**
         * Create UI panels.
         */
        public void createPanels()
        {
            MVCSetup.setMVC(app);
            panels.clear();
            removeAll();

            final boolean portraitMode = width < height;

            // Create board panel
            boardPanel = new BoardView(app, false);
            panels.add(boardPanel);

            // create the player panel
            playerPanel = new PlayerView(app, portraitMode, false);
            panels.add(playerPanel);

            // Create tool panel
            toolPanel = new ToolView(app, portraitMode);
            panels.add(toolPanel);

            // Create tab panel
            if (!app.settingsPlayer().isPerformingTutorialVisualisation())
            {
                tabPanel = new TabViewXAI(app, portraitMode);
                panels.add(tabPanel);
            }

            // Create overlay panel
            overlayPanel = new OverlayView(app);
            panels.add(overlayPanel());

            if (SettingsExhibition.exhibitionVersion)
                app.settingsPlayer().setAnimationType(AnimationVisualsType.Single);
        }
    }

    private class TabViewXAI extends TabView{

        public static final int PanelXAI = 7;

        /**
         * Constructor.
         *
         * @param app
         * @param portraitMode
         */
        public TabViewXAI(PlayerApp app, boolean portraitMode) {
            super(app, portraitMode);

            final Rectangle tabPagePlacement = new Rectangle(placement.x + 10, placement.y + TabView.fontSize + 6, placement.width - 16, placement.height - TabView.fontSize - 20);
            final TabPage xaiPage     = new InfoPage(app, tabPagePlacement, " XAI  ",    "", PanelXAI, this);
            pages.add(xaiPage);

            resetTabs();

            select(app.settingsPlayer().tabSelected());

            if (SettingsExhibition.exhibitionVersion)
                select(5);

            for (final View view : pages)
                DesktopApp.view().getPanels().add(view);
        }
    }
}


