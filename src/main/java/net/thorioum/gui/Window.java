package net.thorioum.gui;

import com.github.felipeucelli.javatube.Youtube;
import net.thorioum.DatapackWriter;
import net.thorioum.Eidolon;
import net.thorioum.MinecraftVersion;
import net.thorioum.gui.components.*;
import net.thorioum.gui.components.Button;
import net.thorioum.sound.ConverterContext;
import net.thorioum.sound.SoundFilesGrabber;
import net.thorioum.sound.SoundPlaybackDevice;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.prefs.Preferences;

import static net.thorioum.Eidolon.*;
import static net.thorioum.gui.Theme.*;
import static net.thorioum.sound.Util.filterString;
import static net.thorioum.sound.Util.formatTime;

public class Window extends JFrame {

    private static Thread audioExec;
    public static void playback(Runnable runnable) {
        if(audioExec != null) {
            SoundPlaybackDevice.stopPlaybackNow();
            audioExec.interrupt();
            audioExec = null;
            return;
        }
        if(runnable == null) return;
        info("Playing back current audio.");

        audioExec = new Thread(runnable);
        audioExec.setDaemon(false);
        audioExec.start();
    }
    private final Preferences prefs = Preferences.userNodeForPackage(Window.class);

    private final LoadingBar loadingBar = new LoadingBar();
    private final JButton runButton = new Button("Run");
    private final JButton cancelButton = new CancelButton("Cancel");
    private final CardLayout runCancelCards = new CardLayout();
    private final JPanel runCancelSlot = new JPanel(runCancelCards);

    private final JButton saveButton = new Button("Save");
    private final JButton listenButton = new Button("Listen");
    private final JPanel actionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

    private Timer runTimer = null;

    private final MultiSelectDropdown blacklistedSounds = new MultiSelectDropdown("Blacklisted Sounds", "");

    private final JComboBox<String> versionDropdown;

    private final JFormattedTextField soundCountField = NumericFields.intField(Integer.parseInt(prefs.get("soundCount","60")), 0, Integer.MAX_VALUE);
    private final JSpinner frameLengthField = NumericFields.intSpinner(Integer.parseInt(prefs.get("frameLength","50")), 20,1000,10);
    private final JFormattedTextField pitchesPerSoundField = NumericFields.intField(Integer.parseInt(prefs.get("pitchesPerSound","128")), 0, Integer.MAX_VALUE);
    private final JFormattedTextField highpassField = NumericFields.doubleField(Double.parseDouble(prefs.get("highpass","350.0")), 0.0, Double.MAX_VALUE);
    private final JFormattedTextField brightnessField = NumericFields.doubleField(Double.parseDouble(prefs.get("brightness","0.9")), 0.0, 1.0);
    private final ToggleSwitch tryUseGpuField = new ToggleSwitch(Boolean.parseBoolean(prefs.get("tryUseGpu", String.valueOf(true))));
    private final JSpinner numProcessorsField = NumericFields.intSpinner(Integer.parseInt(prefs.get("numProcessors", String.valueOf(Runtime.getRuntime().availableProcessors()/2))), 1, Runtime.getRuntime().availableProcessors(),1);

    public static final JTextArea consoleArea = new JTextArea();

    private final JButton selectFileButton = new Button("Select Input");
    private File selectedAudioFile = null;
    private String selectedYoutubeUrl = null;

    private boolean hasInputSelected() {
        return selectedAudioFile != null || (selectedYoutubeUrl != null && !selectedYoutubeUrl.isBlank());
    }

    private void clearInputSelection() {
        selectedAudioFile = null;
        selectedYoutubeUrl = null;
    }

    private MinecraftVersion currentVersion() {
        return MinecraftVersion.get((String) versionDropdown.getSelectedItem());
    }

    private ConverterContext generateContext() {
        return new ConverterContext(
                currentVersion()
                , (Integer) frameLengthField.getValue()
                , (Integer) pitchesPerSoundField.getValue()
                , (Double) highpassField.getValue()
                , (Double) brightnessField.getValue()
        );
    }
    public Window(List<MinecraftVersion> supportedVersions) {
        super("Eidolon");
        versionDropdown = new JComboBox<>(supportedVersions
                .stream()
                .sorted()
                .map(MinecraftVersion::str)
                .toList()
                .reversed()
                .toArray(new String[0]));

        ToolTipManager.sharedInstance().setDismissDelay(999999);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 620));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.OBSIDIAN);
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        setContentPane(root);

        root.add(buildTopBar(), BorderLayout.NORTH);

        root.add(buildLeftSettings(), BorderLayout.WEST);

        root.add(buildConsoleArea(), BorderLayout.CENTER);

        root.add(buildBottomBar(), BorderLayout.SOUTH);

        wireActions();
        setupTick();
    }

    private JComponent buildTopBar() {
        RoundedPanel top = new RoundedPanel(Theme.CARD, 18, true, true, false, false);
        top.setLayout(new BorderLayout(12, 0));
        top.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        ImageIcon raw = new ImageIcon(Window.class.getResource("/assets/icons/app-icon.png"));
        Image scaled = raw.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
        JLabel icon = new JLabel(new ImageIcon(scaled));

        JLabel title = new JLabel("Eidolon");
        title.setForeground(Theme.TEXT_PRIMARY);
        title.setFont(Theme.font(18, Font.BOLD));

        left.add(icon);
        left.add(title);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        styleComboBox(versionDropdown);

        versionDropdown.setPreferredSize(new Dimension(170, 34));
        blacklistedSounds.setPreferredSize(new Dimension(200, 34));
        selectFileButton.setPreferredSize(new Dimension(130, 36));

        right.add(blacklistedSounds);
        right.add(versionDropdown);
        right.add(selectFileButton);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        return top;
    }

    private JComponent buildLeftSettings() {
        RoundedPanel card = new RoundedPanel(Theme.CARD, 18, false, false, false, false);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));
        card.setPreferredSize(new Dimension(330, 10));

        JLabel header = new JLabel("Settings");
        header.setForeground(Theme.TEXT_PRIMARY);
        header.setFont(Theme.font(14, Font.BOLD));

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new GridBagLayout());
        stack.setBorder(new EmptyBorder(2, 0, 0, 0));

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.weightx = 0.5;

        JComponent soundsPerFrame = settingRow(
                "Sounds Per Frame",
                soundCountField,
                "This is max amount of sounds the algorithm can match to a single frame. Important to keep frame length in mind when choosing this as the maximum amount of sounds minecraft will allow to be played at once is 255 (247 for most). Due to various ingame factors, the most this number should be is 1/3 the max sound limit. Numbers 45-60 should work fine for normal framelengths. The higher this number, the higher the quality. Processing time increases linearly with this number."
        );

        JComponent frameLength = settingRow(
                "Frame Length",
                frameLengthField,
                "Controls how often groups of sounds are played. At frame length 50, the input audio will be split into frames of 50ms, and the first 50ms of minecraft sound effects will be matched against each frame. Then for each frame the datapack will play the matched sounds for 50ms, /stopsound, and then play the next frame. 50ms is the lowest, and recommended setting if you cant use /tick. If you can use /tick, 20 is the recommended frame length.\n While the frequency you can use stopsound is effected by server /tick rate, the client side sound cap is still going at the rate of normal client side tick rate - if you have a hack module like Timer, try using that. Negligible effect on processing speed."
        );

        JComponent pitchesPerSound = settingRow(
                "Pitches Per Sound",
                pitchesPerSoundField,
                "When matching sound effects to input audio, the algorithm matches a frame against a sound at a {this_number} of frequencies from 0.5->2.0. Higher number will increase quality, but slow processing. 128 recommended"
        );

        JComponent highpass = settingRow(
                "Highpass",
                highpassField,
                "The higher the number, the less impactful the deep bass of input audio will be."
        );

        JComponent brightness = settingRow(
                "Brightness",
                brightnessField,
                "Value between 0.0->1.0. The lower this value, the stricter the filter for how wide the range of frequencies a minecraft sound effect is. Think the lower the number, the more '8-bit' it sounds"
        );

        JComponent tryUseGpu = settingRow(
                "Try Use GPU",
                tryUseGpuField,
                "Attempts to use GPU acceleration if available. Falls back to CPU if unsupported."
        );

        JComponent numProcessors = settingRow(
                "Allowed Cores",
                numProcessorsField,
                "The amount of cores this program can try to use when matching. With each increment, you CPU and or GPU usage will begin to climb AKA more computer lag, or heat. The more you give, the faster the processing."
        );

        int y = 0;

        // Row 1
        gc.gridy = y;
        gc.gridx = 0; gc.insets = new Insets(0, 0, 0, 10);
        stack.add(soundsPerFrame, gc);
        gc.gridx = 1; gc.insets = new Insets(0, 10, 0, 0);
        stack.add(frameLength, gc);
        y++;

        // Row 2
        gc.gridy = y;
        gc.gridx = 0; gc.insets = new Insets(0, 0, 0, 10);
        stack.add(pitchesPerSound, gc);
        gc.gridx = 1; gc.insets = new Insets(0, 10, 0, 0);
        stack.add(highpass, gc);
        y++;

        // Row 3
        gc.gridy = y;
        gc.gridx = 0; gc.insets = new Insets(0, 0, 0, 10);
        stack.add(brightness, gc);
        gc.gridx = 1; gc.insets = new Insets(0, 10, 0, 0);
        stack.add(tryUseGpu, gc);
        y++;

        // Row 4
        gc.gridy = y;
        gc.gridx = 0; gc.insets = new Insets(0, 0, 0, 10);
        stack.add(numProcessors, gc);


        y++;

        // push content to top
        gc.gridy = y;
        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.weighty = 1.0;
        gc.insets = new Insets(0, 0, 0, 0);
        stack.add(Box.createVerticalGlue(), gc);

        card.add(header, BorderLayout.NORTH);
        card.add(stack, BorderLayout.CENTER);

        return card;
    }


    private JComponent buildConsoleArea() {
        RoundedPanel center = new RoundedPanel(new Color(0x0D121A), 18, false, false, false, false);
        center.setLayout(new BorderLayout());
        center.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel header = new JLabel("Console");
        header.setForeground(Theme.TEXT_PRIMARY);
        header.setFont(Theme.font(13, Font.BOLD));

        consoleArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        consoleArea.setForeground(new Color(0xDCE3F2));
        consoleArea.setBackground(new Color(0x070B10));
        consoleArea.setCaretColor(Theme.ACCENT_BLUE);
        consoleArea.setLineWrap(true);
        consoleArea.setWrapStyleWord(true);
        consoleArea.setEditable(false);
        consoleArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(consoleArea);
        styleScrollPane(scroll);

        scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                new EmptyBorder(0, 0, 0, 0)
        ));
        scroll.getViewport().setBackground(consoleArea.getBackground());
        scroll.setBackground(center.getBackground());

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(header, BorderLayout.WEST);

        center.add(top, BorderLayout.NORTH);
        center.add(Box.createVerticalStrut(10), BorderLayout.CENTER);
        center.add(scroll, BorderLayout.SOUTH);

        center.remove(scroll);
        center.add(scroll, BorderLayout.CENTER);

        return center;
    }

    private void showCancel() {
        runCancelCards.show(runCancelSlot, "CANCEL");
        runCancelSlot.revalidate();
        runCancelSlot.repaint();
    }

    private void showRun() {
        runCancelCards.show(runCancelSlot, "RUN");
        runCancelSlot.revalidate();
        runCancelSlot.repaint();
    }

    public void setExtraButtonsVisible(boolean visible) {
        saveButton.setVisible(visible);
        listenButton.setVisible(visible);

        actionButtonsPanel.revalidate();
        actionButtonsPanel.repaint();
    }

    private JComponent buildBottomBar() {
        RoundedPanel bottom = new RoundedPanel(Theme.CARD, 18, false, false, true, true);
        bottom.setLayout(new BorderLayout(10, 0));
        bottom.setBorder(new EmptyBorder(12, 14, 12, 14));

        loadingBar.setPreferredSize(new Dimension(10, 18));
        loadingBar.setText("Ready");

        runButton.setPreferredSize(new Dimension(110, 36));
        cancelButton.setPreferredSize(new Dimension(110, 36));
        saveButton.setPreferredSize(new Dimension(110, 36));
        listenButton.setPreferredSize(new Dimension(110, 36));

        runCancelSlot.setOpaque(false);
        runCancelSlot.add(runButton, "RUN");
        runCancelSlot.add(cancelButton, "CANCEL");
        runCancelCards.show(runCancelSlot, "RUN");

        actionButtonsPanel.setOpaque(false);
        actionButtonsPanel.removeAll();
        actionButtonsPanel.add(runCancelSlot);
        actionButtonsPanel.add(saveButton);
        actionButtonsPanel.add(listenButton);

        bottom.add(loadingBar, BorderLayout.CENTER);
        bottom.add(actionButtonsPanel, BorderLayout.EAST);

        return bottom;
    }

    private JPanel settingRow(String name, JFormattedTextField field, String helpText) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(10, 0, 10, 0));

        JLabel label = new JLabel(name);
        label.setForeground(Theme.TEXT_PRIMARY);
        label.setFont(Theme.font(13, Font.PLAIN));

        styleFormattedTextField(field);
        HelpDot helpDot = new HelpDot(helpText);

        JPanel topLine = new JPanel(new BorderLayout());
        topLine.setOpaque(false);
        topLine.add(label, BorderLayout.WEST);
        topLine.add(helpDot, BorderLayout.EAST);

        row.add(topLine, BorderLayout.NORTH);

        JPanel fieldWrap = new JPanel(new BorderLayout());
        fieldWrap.setOpaque(false);
        fieldWrap.setBorder(new EmptyBorder(8, 0, 0, 0));
        fieldWrap.add(field, BorderLayout.CENTER);

        row.add(fieldWrap, BorderLayout.CENTER);

        return row;
    }
    private JPanel settingRow(String name, JComponent field, String helpText) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(10, 0, 10, 0));

        JLabel label = new JLabel(name);
        label.setForeground(Theme.TEXT_PRIMARY);
        label.setFont(Theme.font(13, Font.PLAIN));

        if (field instanceof JFormattedTextField tf) {
            styleFormattedTextField(tf);
        } else if (field instanceof JSpinner sp) {
            styleSpinner(sp);
        }

        HelpDot helpDot = new HelpDot(helpText);

        JPanel topLine = new JPanel(new BorderLayout());
        topLine.setOpaque(false);
        topLine.add(label, BorderLayout.WEST);
        topLine.add(helpDot, BorderLayout.EAST);

        row.add(topLine, BorderLayout.NORTH);

        JPanel fieldWrap = new JPanel(new BorderLayout());
        fieldWrap.setOpaque(false);
        fieldWrap.setBorder(new EmptyBorder(8, 0, 0, 0));
        fieldWrap.add(field, BorderLayout.CENTER);

        row.add(fieldWrap, BorderLayout.CENTER);

        return row;
    }

    public void setupTick() {
        Timer uiTick = new Timer(100, e -> {

            setExtraButtonsVisible(processingStatus == Status.MATCHING_AUDIO || processingStatus == Status.COMPLETE);

            prefs.put("soundCount", String.valueOf(soundCountField.getValue()));
            prefs.put("frameLength", String.valueOf(frameLengthField.getValue()));
            prefs.put("pitchesPerSound", String.valueOf(pitchesPerSoundField.getValue()));
            prefs.put("highpass", String.valueOf(highpassField.getValue()));
            prefs.put("brightness", String.valueOf(brightnessField.getValue()));
            prefs.put("tryUseGpu", String.valueOf(tryUseGpuField.isSelected()));
            prefs.put("numProcessors", String.valueOf(numProcessorsField.getValue()));

        });
        uiTick.start();
    }
    private void cancel() {
        playback(null);
        Eidolon.cancelCurrentProcess();

        if (runTimer != null) {
            runTimer.stop();
            runTimer = null;
        }

        loadingBar.setText("Cancelled");
        loadingBar.setProgress(0);
        runButton.setEnabled(true);

        showRun();
    }
    private void wireActions() {
        runButton.addActionListener(e -> {
            showCancel();

            runButton.setEnabled(false);
            loadingBar.setProgress(0);

            runTimer = new Timer(10, null);
            runTimer.addActionListener(e1 -> {
                if (!hasInputSelected()) {
                    error("Select an input audio file or a YouTube link!");
                    cancel();
                    return;
                }


                if(Eidolon.processingStatus == Eidolon.Status.IDLE) {
                    if(!SoundFilesGrabber.soundMap.get(currentVersion()).soundFilesMap().isResolved()) {
                        SoundFilesGrabber.soundMap.get(currentVersion()).soundFilesMap().resolveSounds();
                        refreshBlacklistedSounds();
                    }

                    File audioFile = null;
                    if(selectedAudioFile != null) {
                        audioFile = selectedAudioFile;
                    } else if (selectedYoutubeUrl != null) {
                        try {
                            audioFile = youtubeToFile(selectedYoutubeUrl);
                        } catch (Exception ex) {
                            error(ex.getMessage());
                            ex.printStackTrace();
                        }
                    }

                    if(audioFile == null) {
                        error("Failed to process input audio. Try again, or try something else.");
                        cancel();
                        return;
                    }

                    File finalAudioFile = audioFile;
                    executor.submit(()-> {
                        resetExecutor((Integer) numProcessorsField.getValue());
                        if(!Eidolon.process(generateContext(), (Integer) soundCountField.getValue(), tryUseGpuField.isSelected(), blacklistedSounds.getSelected(), finalAudioFile)) {
                            cancel();
                        }
                    });
                } else {
                    if(Eidolon.processingStatus == Eidolon.Status.PROCESSING_SOUNDS) {
                        loadingBar.setText("Processing Minecraft Sound Effects (" + Eidolon.currentDb.metaProcessedSounds() + " / " + Eidolon.currentDb.metaExpectedSounds + ")");
                        loadingBar.setProgress((int) (((float) currentDb.metaProcessedSounds() / (float) currentDb.metaExpectedSounds) * 100));
                    } else if (Eidolon.processingStatus == Eidolon.Status.MATCHING_AUDIO && Eidolon.currentContext != null && Eidolon.currentDb != null) {

                        long currentTime = (long) Eidolon.getCurrentResult().composition().size() * Eidolon.currentContext.frameLength();
                        long totalTime = (long) Eidolon.getCurrentResult().expectedFrames * Eidolon.currentContext.frameLength();
                        String time = String.format("%s/%s", formatTime(currentTime), formatTime(totalTime));

                        loadingBar.setText("Processing " + filterString(currentAudioName()) + ". . . (" + time + ")");
                        loadingBar.setProgress((int) (((float)currentTime / (float)totalTime) * 100));

                    } else if (processingStatus == Status.COMPLETE) {
                        long currentTime = (long) Eidolon.getCurrentResult().composition().size() * Eidolon.currentContext.frameLength();
                        long totalTime = (long) Eidolon.getCurrentResult().expectedFrames * Eidolon.currentContext.frameLength();
                        String time = String.format("%s/%s", formatTime(currentTime), formatTime(totalTime));

                        loadingBar.setProgress(100);
                        loadingBar.setText("Complete Processing " + filterString(currentAudioName()) + ". . . (" + time + ")");

                    }
                }
            });
            runTimer.start();
        });

        cancelButton.addActionListener(e -> {
            cancel();
        });

        saveButton.addActionListener(e -> {
            info("Attempting to save as datapack.");
            File output = promptSaveFile("Save As Datapack",filterString(currentAudioName()) + "-" + currentContext.frameLength() + ".zip");
            if(output != null) {
                if(output.getName().endsWith(".zip")) {
                    output = new File(output.getParentFile(), output.getName().replace(".zip",""));
                }
                DatapackWriter.createAudioPack(currentContext,output,getCurrentResult());
            }
        });

        listenButton.addActionListener(e -> {

            playback(()->{
                SoundPlaybackDevice.play(currentContext, getCurrentResult());
            });
        });

        versionDropdown.addActionListener(e -> {
            refreshBlacklistedSounds();
        });
        versionDropdown.setSelectedItem(versionDropdown.getItemAt(0));

        selectFileButton.addActionListener(e -> openInputChooser());
    }
    private void refreshBlacklistedSounds() {
        MinecraftVersion ver = currentVersion();
        blacklistedSounds.setOptions(
                SoundFilesGrabber.soundMap.get(ver)
                        .soundFilesMap().fileMap().keySet().stream().sorted().toList()
                        .toArray(new String[0])
        );
    }
    private File youtubeToFile(String link) throws Exception {
        Youtube yt = new Youtube(link);

        Path cacheDir = Path.of("").toAbsolutePath().resolve("cache");
        cacheDir.toFile().mkdirs();

        yt.streams().getOnlyAudio().download("cache/","vidtmp");
        return new File("cache/vidtmp.mp4");
    }

    private void openInputChooser() {
        JButton fileButton   = new Button("Audio File");
        JButton ytButton     = new Button("YouTube");
        JButton cancelButton = new CancelButton("Cancel");

        fileButton.setPreferredSize(new Dimension(120,30));
        ytButton.setPreferredSize(new Dimension(120,30));
        cancelButton.setPreferredSize(new Dimension(120,30));

        Object[] options = { fileButton, ytButton, cancelButton };

        JOptionPane pane = new JOptionPane(
                "Choose an input source:",
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                options,
                fileButton
        );

        JDialog dialog = pane.createDialog(this, "Select Input");

        fileButton.addActionListener(e -> { pane.setValue(fileButton); dialog.dispose(); });
        ytButton.addActionListener(e -> { pane.setValue(ytButton); dialog.dispose(); });
        cancelButton.addActionListener(e -> { pane.setValue(cancelButton); dialog.dispose(); });

        dialog.setVisible(true);

        Object v = pane.getValue();

        if (v == fileButton) {
            openAudioFileChooser();
        } else if (v == ytButton) {
            promptYoutubeLink();
        }
    }

    String currentYoutubeTitle = "";
    private String currentAudioName() {
        if(selectedAudioFile != null) {
            return selectedAudioFile.getName();
        } else {
            return currentYoutubeTitle;
        }
    }

    private void promptYoutubeLink() {
        JTextField field = new JTextField();
        styleTextField(field);
        field.setPreferredSize(new Dimension(420, 34));

        JButton okBtn = new Button("OK");
        JButton cancelBtn = new CancelButton("Cancel");

        okBtn.setPreferredSize(new Dimension(120, 30));
        cancelBtn.setPreferredSize(new Dimension(120, 30));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BorderLayout(0, 10));

        JLabel label = new JLabel("Paste a YouTube link:");
        label.setForeground(Theme.TEXT_PRIMARY);
        label.setFont(Theme.font(12, Font.PLAIN));

        content.add(label, BorderLayout.NORTH);
        content.add(field, BorderLayout.CENTER);

        Object[] options = { okBtn, cancelBtn };

        JOptionPane pane = new JOptionPane(
                content,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                options,
                okBtn
        );

        JDialog dialog = pane.createDialog(this, "YouTube Link");

        okBtn.addActionListener(e -> { pane.setValue(okBtn); dialog.dispose(); });
        cancelBtn.addActionListener(e -> { pane.setValue(cancelBtn); dialog.dispose(); });

        field.addActionListener(e -> { pane.setValue(okBtn); dialog.dispose(); });
        dialog.getRootPane().registerKeyboardAction(
                e -> { pane.setValue(cancelBtn); dialog.dispose(); },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        dialog.addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) {
                field.requestFocusInWindow();
                field.selectAll();
            }
        });

        dialog.setVisible(true);

        Object v = pane.getValue();
        if (v != okBtn) return;

        String url = field.getText();
        if (url == null) return;

        url = url.trim();
        if (url.isBlank()) return;

        boolean looksLikeYoutube = url.contains("youtu");
        if (!looksLikeYoutube) {
            error("That doesn't look like a YouTube link.");
            return;
        }

        selectedYoutubeUrl = url;
        selectedAudioFile = null;

        try {
            Youtube yt = new Youtube(url);
            currentYoutubeTitle = yt.getTitle();
        } catch (Exception e) {
            currentYoutubeTitle = url;
        }

        loadingBar.setText("YouTube: " + shorten(currentYoutubeTitle));
    }

    private static String shorten(String s) {
        if (s == null) return "";
        if (s.length() <= 60) return s;
        return s.substring(0, Math.max(0, 57)) + "...";
    }

    public File promptSaveFile(String title, String defaultFileName) {
        FileDialog dialog = new FileDialog(this, title, FileDialog.SAVE);

        if (defaultFileName != null && !defaultFileName.isBlank()) {
            dialog.setFile(defaultFileName);
        }

        dialog.setVisible(true);

        String file = dialog.getFile();
        String dir  = dialog.getDirectory();

        if (file == null || dir == null) {
            return null;
        }

        return new File(dir, file);
    }

    private void openAudioFileChooser() {
        FileDialog fd = new FileDialog(this, "Select an audio file", FileDialog.LOAD);

        fd.setFilenameFilter((dir, name) -> {
            if (name == null) return false;

            File f = new File(dir, name);
            if (f.isDirectory()) return true;

            String n = name.toLowerCase();
            return n.endsWith(".wav")
                    || n.endsWith(".mp3")
                    || n.endsWith(".flac")
                    || n.endsWith(".ogg")
                    || n.endsWith(".m4a")
                    || n.endsWith(".aiff")
                    || n.endsWith(".aif")
                    || n.endsWith(".aac")
                    || n.endsWith(".wma");
        });

        fd.setVisible(true);

        String file = fd.getFile();
        String dir = fd.getDirectory();

        if (file != null && dir != null) {
            selectedAudioFile = new File(dir, file);
            selectedYoutubeUrl = null;
            loadingBar.setText("Selected File: " + selectedAudioFile.getName());
        }
    }

}
