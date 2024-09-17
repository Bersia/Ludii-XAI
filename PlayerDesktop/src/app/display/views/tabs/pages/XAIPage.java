package app.display.views.tabs.pages;

import app.DesktopApp;
import app.PlayerApp;
import app.display.views.tabs.TabPage;
import app.display.views.tabs.TabView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import other.context.Context;

/**
 * Tab for displaying a chat page.
 *
 * @author Matthew.Stephenson
 */
public class XAIPage extends TabPage
{
    private JTextField inputField;
    private JButton sendButton;
    private HTMLEditorKit editorKit;

    //-------------------------------------------------------------------------

    public XAIPage(final PlayerApp app, final Rectangle rect, final String title, final String text, final int pageIndex, final TabView parent)
    {
        super(app, rect, title, text, pageIndex, parent);

        // Set the layout to null for absolute positioning
        DesktopApp.view().setLayout(null);

        // Initialize input field
        inputField = new JTextField();
        inputField.setBounds(placement.x, placement.y + placement.height - 40, placement.width - 90, 30); // Position it near the bottom
        inputField.setFont(new Font("Arial", Font.PLAIN, 14));
        inputField.setVisible(true);  // Ensure it's visible

        // Initialize send button
        sendButton = new JButton("Send");
        sendButton.setBounds(placement.x + placement.width - 80, placement.y + placement.height - 40, 80, 30); // Position next to input field
        sendButton.setFont(new Font("Arial", Font.PLAIN, 14));
        sendButton.setBackground(Color.LIGHT_GRAY);  // Set background color
        sendButton.setVisible(true);  // Ensure it's visible

        // Ensure chat text area uses HTML formatting
        editorKit = new HTMLEditorKit();
        textArea.setEditorKit(editorKit);

        // Set initial content in chat pane
        textArea.setContentType("text/html");
        textArea.setText("<b>Welcome to the chat!</b><br>");
        textArea.setEditable(false);  // Make sure the text area isn't editable
        textArea.setVisible(true);    // Ensure the text area is visible
        textArea.setBounds(placement.x, placement.y, placement.width, placement.height - 50); // Take the rest of the space above the input

        // Add the input field, send button, and chat area to the view
        DesktopApp.view().add(textArea);
        DesktopApp.view().add(inputField);
        DesktopApp.view().add(sendButton);

        // Add ActionListener to send button
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
    }

    //-------------------------------------------------------------------------

    @Override
    public void updatePage(final Context context) {
        // Any updates that are required per context
    }

    //-------------------------------------------------------------------------

    @Override
    public void reset() {
        clear();
        updatePage(app.contextSnapshot().getContext(app));
    }

    //-------------------------------------------------------------------------

    /**
     * Handles sending messages in the chat.
     */
    private void sendMessage() {
        // Get text from input field
        String message = inputField.getText();

        if (message != null && !message.trim().isEmpty()) {
            // Add the message to the chat area
            try {
                HTMLDocument doc = (HTMLDocument) textArea.getDocument();
                editorKit.insertHTML(doc, doc.getLength(), "<b>You:</b> " + message + "<br>", 0, 0, null);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Scroll to the latest message
            textArea.setCaretPosition(textArea.getDocument().getLength());

            // Clear the input field
            inputField.setText("");
        }
    }
}
