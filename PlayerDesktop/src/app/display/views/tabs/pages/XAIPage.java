package app.display.views.tabs.pages;

import app.DesktopApp;
import app.PlayerApp;
import app.display.views.tabs.TabPage;
import app.display.views.tabs.TabView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import other.context.Context;

/**
 * Tab for displaying a chat page.
 */
public class XAIPage extends TabPage
{
    private JTextField inputField;       // Input field for user messages
    private JButton sendButton;          // Button to send the message
    private JEditorPane textArea;        // Area to display chat history
    private JScrollPane scrollPane;      // Scroll pane to contain the chat area
    private JPanel inputPanel;           // Panel to hold input components
    private String chatHistory = "";     // Store chat history as HTML

    //-------------------------------------------------------------------------

    public XAIPage(final PlayerApp app, final Rectangle rect, final String title, final String text, final int pageIndex, final TabView parent)
    {
        super(app, rect, title, text, pageIndex, parent);

        // Set up the text area to display chat history
        textArea = new JEditorPane();
        textArea.setContentType("text/html");
        textArea.setEditable(false);
        textArea.setVisible(true);
        textArea.setText("<b>Welcome to the chat!</b><br>"); // Initial message

        // Set up the scroll pane for the chat area
        scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(null);

        // Set up the input field for the user to type messages
        inputField = new JTextField();
        inputField.setFont(new Font("Arial", Font.PLAIN, 14));
        inputField.setVisible(true);

        // Set up the send button
        sendButton = new JButton("Send");
        sendButton.setFont(new Font("Arial", Font.PLAIN, 14));
        sendButton.setBackground(Color.LIGHT_GRAY);
        sendButton.setVisible(true);

        // Panel to hold the input field and send button
        inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.setBounds(rect.x, rect.y+rect.height - 30, rect.width, 30); // Set bounds at the bottom of the screen

        // Add the scroll pane and input panel to the desktop view
//        DesktopApp.view().setLayout(null); // Use null layout to manually position components
        DesktopApp.view().add(scrollPane);
        DesktopApp.view().add(inputPanel);

        // Adjust the bounds of scrollPane to fill the remaining space above the inputPanel
        scrollPane.setBounds(rect.x, rect.y, rect.width, rect.height - 30);

        // Set up the send button to append the user's input to the chat area
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        // Add a KeyListener to the input field to send message when Enter is pressed
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });
    }

    //-------------------------------------------------------------------------

    @Override
    public void updatePage(final Context context)
    {
        // Any updates related to the context (game state) should be handled here.
    }

    //-------------------------------------------------------------------------

    @Override
    public void reset()
    {
        // Clear the chat history when the page is reset.
        clear();
    }

    //-------------------------------------------------------------------------

    /**
     * Handles sending a message in the chat.
     */
    private void sendMessage()
    {
        String message = inputField.getText();

        if (message != null && !message.trim().isEmpty())
        {
            try
            {
                chatHistory += "<b>You:</b> " + message + "<br>"; // Append the new message to chat history
                textArea.setText(chatHistory);  // Update the text area
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            // Scroll to the bottom of the chat
            textArea.setCaretPosition(textArea.getDocument().getLength());
            inputField.setText("");  // Clear the input field
        }
    }

    //-------------------------------------------------------------------------

    /**
     * Paint the page and its components.
     */
    @Override
    public void paint(final Graphics2D g2d)
    {
        super.paint(g2d);  // Paint inherited components first

        // Custom painting logic (if any) goes here. For chat, Swing handles most of the UI work.
    }

    //-------------------------------------------------------------------------

    /**
     * Clear the chat area.
     */
    @Override
    public void clear()
    {
        chatHistory = "<b>Welcome to the chat!</b><br>";  // Reset chat history
        textArea.setText(chatHistory);  // Clear the text area
    }

    //-------------------------------------------------------------------------

    /**
     * Shows or hides the XAIPage tab content.
     */
    @Override
    public void show(final boolean show)
    {
        textArea.setVisible(show);
        scrollPane.setVisible(show);
        inputPanel.setVisible(show);
    }

    //-------------------------------------------------------------------------
}
