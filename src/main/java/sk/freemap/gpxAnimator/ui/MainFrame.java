package sk.freemap.gpxAnimator.ui;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import sk.freemap.gpxAnimator.Configuration;
import sk.freemap.gpxAnimator.Constants;
import sk.freemap.gpxAnimator.FileXmlAdapter;
import sk.freemap.gpxAnimator.Preferences;
import sk.freemap.gpxAnimator.Renderer;
import sk.freemap.gpxAnimator.RenderingContext;
import sk.freemap.gpxAnimator.TrackConfiguration;
import sk.freemap.gpxAnimator.UserException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public final class MainFrame extends JFrame {

    private static final String PROJECT_FILENAME_SUFFIX = ".ga.xml";
    private static final String UNSAVED_MSG = "There are unsaved changes. Continue?";
    private static final String WARNING_TITLE = "Warning";
    private static final String ERROR_TITLE = "Error";
    private static final String TITLE = "GPX Animator " + Constants.VERSION;
    private static final long serialVersionUID = 190371886979948114L;
    private static final int FIXED_TABS = 1; // TODO set to 2 for MapPanel

    private final transient Random random = new Random();
    private final transient File defaultConfigFile = new File(Preferences.getConfigurationDir()
            + Preferences.FILE_SEPARATOR + "defaultConfig.ga.xml");
    private final transient JTabbedPane tabbedPane;
    private final transient JButton renderButton;
    private final transient JMenu openRecent;
    private final transient JFileChooser fileChooser = new JFileChooser();
    private final transient GeneralSettingsPanel generalSettingsPanel;

    private transient SwingWorker<Void, String> swingWorker;
    private transient File file;
    private transient boolean changed;

    @SuppressWarnings("checkstyle:MethodLength") // TODO Refactor when doing the redesign task https://github.com/zdila/gpx-animator/issues/60
    public MainFrame() {
        final ActionListener addTrackActionListener = new ActionListener() {
            private float hue = random.nextFloat();

            @Override
            public void actionPerformed(final ActionEvent e) {
                addTrackSettingsTab(TrackConfiguration
                        .createBuilder()
                        .color(Color.getHSBColor(hue, 0.8f, 0.8f))
                        .build());
                hue += 0.275f;
                while (hue >= 1f) {
                    hue -= 1f;
                }
            }
        };

        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(new FileFilter() {
            @Override
            public String getDescription() {
                return "GPX Animator Configuration Files";
            }

            @Override
            public boolean accept(final File f) {
                return f.isDirectory() || f.getName().endsWith(PROJECT_FILENAME_SUFFIX);
            }
        });

        setTitle(TITLE);
        setIconImages(
                Arrays.asList(
                        new ImageIcon(getClass().getResource("/icon_16.png")).getImage(),
                        new ImageIcon(getClass().getResource("/icon_32.png")).getImage()
                )
        );
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setBounds(100, 100, 800, 750);

        final JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        final JMenu mnFile = new JMenu("File");
        menuBar.add(mnFile);

        final JMenuItem mntmNew = new JMenuItem("New");
        mntmNew.addActionListener(e -> {
            if (!changed || JOptionPane.showConfirmDialog(MainFrame.this, UNSAVED_MSG, WARNING_TITLE,
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                loadDefaults();
            }
        });
        mnFile.add(mntmNew);

        final JMenuItem mntmOpen = new JMenuItem("Open...");
        mntmOpen.addActionListener(e -> {
            if (!changed || JOptionPane.showConfirmDialog(MainFrame.this, UNSAVED_MSG, WARNING_TITLE,
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {

                final String lastCwd = Preferences.getLastWorkingDir();
                fileChooser.setCurrentDirectory(new File(lastCwd == null ? System.getProperty("user.dir") : lastCwd));
                if (fileChooser.showOpenDialog(MainFrame.this) == JFileChooser.APPROVE_OPTION) {
                    final File fileToOpen = fileChooser.getSelectedFile();
                    Preferences.setLastWorkingDir(fileToOpen.getParent());

                    openFile(fileToOpen);
                }
            }

        });
        mnFile.add(mntmOpen);

        openRecent = new JMenu("Open Recent");
        pupulateOpenRecentMenu();
        mnFile.add(openRecent);

        mnFile.addSeparator();

        final JMenuItem mntmSave = new JMenuItem("Save");
        mntmSave.addActionListener(e -> {
            if (file == null) {
                saveAs();
            } else {
                save(file);
            }
        });
        mnFile.add(mntmSave);

        final JMenuItem mntmSaveAs = new JMenuItem("Save As...");
        mntmSaveAs.addActionListener(e -> saveAs());
        mnFile.add(mntmSaveAs);

        mnFile.addSeparator();

        final JMenuItem mntmSaveAsDefault = new JMenuItem("Save As Default");
        mntmSaveAsDefault.addActionListener(e -> saveAsDefault());
        mnFile.add(mntmSaveAsDefault);

        final JMenuItem mntmResetDefaults = new JMenuItem("Reset To Factory Defaults");
        mntmResetDefaults.addActionListener(e -> resetDefaults());
        mnFile.add(mntmResetDefaults);

        mnFile.addSeparator();

        final JMenuItem preferencesMenu = new JMenuItem("Preferences");
        preferencesMenu.addActionListener(e -> SwingUtilities.invokeLater(() -> new PreferencesDialog(this).setVisible(true)));
        mnFile.add(preferencesMenu);

        mnFile.addSeparator();

        final JMenuItem mntmExit = new JMenuItem("Exit");
        mntmExit.addActionListener(this::exitApplication);
        mnFile.add(mntmExit);

        final JMenu mnTrack = new JMenu("Track");
        menuBar.add(mnTrack);

        final JMenuItem mntmAddTrack = new JMenuItem("Add");
        mntmAddTrack.addActionListener(addTrackActionListener);
        mnTrack.add(mntmAddTrack);

        final JMenuItem mntmRemoveTrack = new JMenuItem("Remove");
        mntmRemoveTrack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final int index = tabbedPane.getSelectedIndex();
                if (index >= FIXED_TABS) {
                    tabbedPane.remove(index);
                    afterRemove();
                }
            }
        });
        mnTrack.add(mntmRemoveTrack);

        final JMenu mnHelp = new JMenu("Help");
        menuBar.add(mnHelp);

        final JMenuItem mntmAbout = new JMenuItem("About");
        mntmAbout.addActionListener(e -> {
            final AboutDialog aboutDialog = new AboutDialog();
            aboutDialog.setLocationRelativeTo(MainFrame.this);
            aboutDialog.setVisible(true);
        });
        mnHelp.add(mntmAbout);

        final JMenuItem mntmUsage = new JMenuItem("Usage");
        mntmUsage.addActionListener(e -> {
            final UsageDialog usageDialog = new UsageDialog();
            usageDialog.setLocationRelativeTo(MainFrame.this);
            usageDialog.setVisible(true);
        });
        mnHelp.add(mntmUsage);

        final JMenuItem mntmFAQ = new JMenuItem("FAQ");
        mntmFAQ.addActionListener(e -> {
            final String url = "https://gpx-animator.app/#faq";
            try {
                final String os = System.getProperty("os.name").toLowerCase(Locale.getDefault());
                final Runtime rt = Runtime.getRuntime();

                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                } else if (os.contains("win")) {

                    // this doesn't support showing urls in the form of "page.html#nameLink"
                    rt.exec("rundll32 url.dll,FileProtocolHandler " + url);

                } else if (os.contains("mac")) {

                    rt.exec("open " + url);

                } else if (os.contains("nix") || os.contains("nux")) {

                    // Do a best guess on unix until we get a platform independent way
                    // Build a list of browsers to try, in this order.
                    final String[] browsers = {"chrome", "firefox", "mozilla", "konqueror",
                            "epiphany", "netscape", "opera", "links", "lynx"};

                    // Build a command string which looks like "browser1 "url" || browser2 "url" ||..."
                    final StringBuilder cmd = new StringBuilder();
                    for (int i = 0; i < browsers.length; i++) {
                        cmd.append(i == 0 ? "" : " || ").append(browsers[i]).append(" \"").append(url).append("\" ");
                    }
                    rt.exec(new String[]{"sh", "-c", cmd.toString()});
                }
            } catch (final IOException | URISyntaxException ex) {
                JOptionPane.showMessageDialog(
                        null,
                        "I was not able to start a web browser for the FAQ:\n" + url,
                        "Sorry",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        });
        mnHelp.add(mntmFAQ);

        final JMenuItem changelogMenu = new JMenuItem("Changelog");
        changelogMenu.addActionListener(e -> SwingUtilities.invokeLater(this::showChangelog));
        mnHelp.add(changelogMenu);

        final JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        final GridBagLayout gblContentPane = new GridBagLayout();
        gblContentPane.columnWidths = new int[]{438, 0};
        gblContentPane.rowHeights = new int[]{264, 0, 0};
        gblContentPane.columnWeights = new double[]{1.0, Double.MIN_VALUE};
        gblContentPane.rowWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
        contentPane.setLayout(gblContentPane);

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        final GridBagConstraints gbcTabbedPane = new GridBagConstraints();
        gbcTabbedPane.insets = new Insets(0, 0, 5, 0);
        gbcTabbedPane.fill = GridBagConstraints.BOTH;
        gbcTabbedPane.gridx = 0;
        gbcTabbedPane.gridy = 0;
        contentPane.add(tabbedPane, gbcTabbedPane);

        tabbedPane.addChangeListener(e -> mntmRemoveTrack.setEnabled(tabbedPane.getSelectedIndex() > 0));

        final JScrollPane generalScrollPane = new JScrollPane();
        tabbedPane.addTab("General", generalScrollPane);

        generalSettingsPanel = new GeneralSettingsPanel() {
            private static final long serialVersionUID = 9088070803139334820L;

            @Override
            protected void configurationChanged() {
                setChanged(true);
            }
        };

        generalScrollPane.setViewportView(generalSettingsPanel);

        // TODO tabbedPane.addTab("Map", new MapPanel());

        final JPanel panel = new JPanel();
        final GridBagConstraints gbcPanel = new GridBagConstraints();
        gbcPanel.fill = GridBagConstraints.BOTH;
        gbcPanel.gridx = 0;
        gbcPanel.gridy = 1;
        contentPane.add(panel, gbcPanel);
        final GridBagLayout gblPanel = new GridBagLayout();
        gblPanel.columnWidths = new int[]{174, 49, 0, 32, 0};
        gblPanel.rowHeights = new int[]{27, 0};
        gblPanel.columnWeights = new double[]{1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
        gblPanel.rowWeights = new double[]{0.0, Double.MIN_VALUE};
        panel.setLayout(gblPanel);

        final JProgressBar progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        final GridBagConstraints gbcProgressBar = new GridBagConstraints();
        gbcProgressBar.fill = GridBagConstraints.HORIZONTAL;
        gbcProgressBar.insets = new Insets(0, 0, 0, 5);
        gbcProgressBar.gridx = 0;
        gbcProgressBar.gridy = 0;
        panel.add(progressBar, gbcProgressBar);

        final JButton addTrackButton = new JButton("Add Track");
        final GridBagConstraints gbcAddTrackButton = new GridBagConstraints();
        gbcAddTrackButton.anchor = GridBagConstraints.NORTHWEST;
        gbcAddTrackButton.insets = new Insets(0, 0, 0, 5);
        gbcAddTrackButton.gridx = 1;
        gbcAddTrackButton.gridy = 0;
        panel.add(addTrackButton, gbcAddTrackButton);
        addTrackButton.addActionListener(addTrackActionListener);

// final JButton btnComputeBbox = new JButton("Compute BBox");
// final GridBagConstraints gbc_btnComputeBbox = new GridBagConstraints();
// gbc_btnComputeBbox.anchor = GridBagConstraints.NORTHWEST;
// gbc_btnComputeBbox.insets = new Insets(0, 0, 0, 5);
// gbc_btnComputeBbox.gridx = 2;
// gbc_btnComputeBbox.gridy = 0;
// panel.add(btnComputeBbox, gbc_btnComputeBbox);
// btnComputeBbox.addActionListener(new ActionListener() {
//     @Override
//     public void actionPerformed(final ActionEvent e) {
//         // TODO Auto-generated method stub
//     }
// });

        renderButton = new JButton("Render");
        renderButton.setEnabled(false);
        final GridBagConstraints gbcRenderButton = new GridBagConstraints();
        gbcRenderButton.anchor = GridBagConstraints.NORTHWEST;
        gbcRenderButton.gridx = 3;
        gbcRenderButton.gridy = 0;
        panel.add(renderButton, gbcRenderButton);
        renderButton.addActionListener(e -> {
            if (swingWorker != null) {
                swingWorker.cancel(false);
                return;
            }

            final Configuration cfg = createConfiguration(true, true);
            if (cfg.getOutput().exists()) {
                final String message = String.format(
                        "A file with the name \"%s\" already exists.%nDo you really want to overwrite this file?", cfg.getOutput());
                final int result = JOptionPane.showConfirmDialog(MainFrame.this,
                        message, WARNING_TITLE, JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.NO_OPTION) {
                    return;
                }
            }

            swingWorker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    new Renderer(cfg).render(new RenderingContext() {
                        @Override
                        public void setProgress1(final int pct, final String message) {
                            System.out.printf("[%3d%%] %s%n", pct, message);
                            setProgress(pct);
                            publish(message + " (" + pct + "%)");
                        }

                        @Override
                        public boolean isCancelled1() {
                            return isCancelled();
                        }
                    });

                    return null;
                }

                @Override
                protected void process(final List<String> chunks) {
                    if (!chunks.isEmpty()) {
                        progressBar.setString(chunks.get(chunks.size() - 1));
                    }
                }

                @Override
                protected void done() {
                    swingWorker = null; // NOPMD -- dereference the SwingWorker to make it available for garbage collection
                    progressBar.setVisible(false);
                    renderButton.setText("Render");

                    try {
                        get();
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "Rendering has finished successfully.", "Finished", JOptionPane.INFORMATION_MESSAGE);
                    } catch (final InterruptedException e) {
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "Rendering has been interrupted.", "Interrupted", JOptionPane.ERROR_MESSAGE);
                    } catch (final ExecutionException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "Error while rendering:\n" + e.getCause().getMessage(), ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
                    } catch (final CancellationException e) {
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "Rendering has been cancelled.", "Cancelled", JOptionPane.WARNING_MESSAGE);
                    }
                }
            };

            swingWorker.addPropertyChangeListener(evt -> {
                if ("progress".equals(evt.getPropertyName())) {
                    progressBar.setValue((Integer) evt.getNewValue());
                }
            });

            progressBar.setVisible(true);
            renderButton.setText("Cancel");
            swingWorker.execute();
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                if (!changed || JOptionPane.showConfirmDialog(MainFrame.this,
                        "There are unsaved changes. Close anyway?", "Unsaved changes", JOptionPane.YES_NO_OPTION
                ) == JOptionPane.YES_OPTION) {
                    System.exit(0); // NOPMD -- Exit on user request
                }

                if (swingWorker != null && !swingWorker.isDone()) {
                    swingWorker.cancel(false);
                }
            }
        });

        SwingUtilities.invokeLater(this::loadDefaults);
        SwingUtilities.invokeLater(this::showChangelogOnce);
    }

    @SuppressWarnings("PMD.DoNotCallSystemExit") // Exit the application on user request
    @SuppressFBWarnings(value = "DM_EXIT", justification = "Exit the application on user request")
    private void exitApplication(final ActionEvent e) {
        if (!changed || JOptionPane.showConfirmDialog(MainFrame.this, UNSAVED_MSG, WARNING_TITLE,
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    public Configuration createConfiguration(final boolean includeTracks, final boolean replacePlaceholders) {
        final Configuration.Builder builder = Configuration.createBuilder();

        generalSettingsPanel.buildConfiguration(builder, replacePlaceholders);

        if (includeTracks) {
            for (int i = FIXED_TABS, n = tabbedPane.getTabCount(); i < n; i++) {
                final TrackSettingsPanel tsp = (TrackSettingsPanel) ((JScrollPane) tabbedPane.getComponentAt(i)).getViewport().getView();
                builder.addTrackConfiguration(tsp.createConfiguration());
            }
        }

        return builder.build();
    }

    public void setConfiguration(final Configuration c) {
        generalSettingsPanel.setConfiguration(c);

        // remove all track tabs
        for (int i = tabbedPane.getTabCount() - 1; i >= FIXED_TABS; i--) {
            tabbedPane.remove(i);
        }
        afterRemove();

        for (final TrackConfiguration tc : c.getTrackConfigurationList()) {
            addTrackSettingsTab(tc);
        }

        setChanged(false);
    }

    private void pupulateOpenRecentMenu() {
        openRecent.removeAll();
        Preferences.getRecentFiles().forEach(recentFile -> {
                    JMenuItem item = new JMenuItem(recentFile.getName());
                    openRecent.add(item);
                    item.addActionListener(e -> openFile(recentFile));
                }
        );
    }

    private void openFile(final File fileToOpen) {
        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(Configuration.class);
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            unmarshaller.setAdapter(new FileXmlAdapter(fileToOpen.getParentFile()));
            setConfiguration((Configuration) unmarshaller.unmarshal(fileToOpen));
            MainFrame.this.file = fileToOpen;
            addRecentFile(fileToOpen);
            setChanged(false);
        } catch (final JAXBException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(MainFrame.this, "Error opening configuration: " + e1.getMessage(), ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
        }
    }


    private void addRecentFile(final File recentFile) {
        Preferences.addRecentFile(recentFile);
        pupulateOpenRecentMenu();
    }


    private void setChanged(final boolean changed) {
        this.changed = changed;
        updateTitle();
    }

    private void updateTitle() {
        final String filename = file != null ? file.getName() : "[unnamed]";
        setTitle(TITLE + " - " + filename + (changed ? " (*)" : ""));
    }

    private void addTrackSettingsTab(final TrackConfiguration tc) {
        final JScrollPane trackScrollPane = new JScrollPane();
        final TrackSettingsPanel trackSettingsPanel = new TrackSettingsPanel() {
            private static final long serialVersionUID = 308660875202822183L;

            @Override
            protected void remove() {
                tabbedPane.remove(trackScrollPane);
                afterRemove();
            }

            @Override
            protected void configurationChanged() {
                setChanged(true);
            }

            @Override
            protected void labelChanged(final String label) {
                tabbedPane.setTitleAt(tabbedPane.indexOfComponent(trackScrollPane), label == null || label.isEmpty() ? "Track" : label);
            }
        };

        tabbedPane.addTab("Track", trackScrollPane);
        trackScrollPane.setViewportView(trackSettingsPanel);
        tabbedPane.setSelectedComponent(trackScrollPane);
        trackSettingsPanel.setConfiguration(tc);

        renderButton.setEnabled(true);

        setChanged(true);
    }

    private void afterRemove() {
        if (tabbedPane.getTabCount() == 1) { // NOPMD -- Ignore magic number literal
            renderButton.setEnabled(false);
        }
        setChanged(true);
    }

    private void saveAs() {
        final String lastCwd = Preferences.getLastWorkingDir();
        fileChooser.setCurrentDirectory(new File(lastCwd));
        fileChooser.setSelectedFile(new File("")); // to forget previous file name
        if (fileChooser.showSaveDialog(MainFrame.this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            Preferences.setLastWorkingDir(fileToSave.getParent());

            if (!fileToSave.getName().endsWith(PROJECT_FILENAME_SUFFIX)) {
                fileToSave = new File(fileToSave.getPath() + PROJECT_FILENAME_SUFFIX);
            }
            save(fileToSave);
        }
    }

    private void save(final File fileToSave) {
        try {
            try {
                final JAXBContext jaxbContext = JAXBContext.newInstance(Configuration.class);
                final Marshaller marshaller = jaxbContext.createMarshaller();
                marshaller.setAdapter(new FileXmlAdapter(fileToSave.getParentFile()));
                marshaller.marshal(createConfiguration(true, false), fileToSave);
                MainFrame.this.file = fileToSave;
                setChanged(false);
                addRecentFile(fileToSave);
            } catch (final JAXBException e) {
                throw new UserException(e.getMessage(), e);
            }
        } catch (final UserException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving configuration: " + e.getMessage(), ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveAsDefault() {
        try {
            try {
                final JAXBContext jaxbContext = JAXBContext.newInstance(Configuration.class);
                final Marshaller marshaller = jaxbContext.createMarshaller();
                marshaller.setAdapter(new FileXmlAdapter(null));
                marshaller.marshal(createConfiguration(false, false), defaultConfigFile);
            } catch (final JAXBException e) {
                throw new UserException(e.getMessage(), e);
            }
        } catch (final UserException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving default configuration: " + e.getMessage(), ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadDefaults() {
        file = null; // NOPMD -- Loading defaults = resetting everything means unsetting the filename, too
        setChanged(false);

        if (defaultConfigFile == null || !defaultConfigFile.exists()) {
            setConfiguration(Configuration.createBuilder().build());
            return;
        }

        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(Configuration.class);
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            unmarshaller.setAdapter(new FileXmlAdapter(null));
            setConfiguration((Configuration) unmarshaller.unmarshal(defaultConfigFile));
        } catch (final JAXBException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(MainFrame.this, "Error loading default configuration: " + e1.getMessage(),
                    ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetDefaults() {
        if (defaultConfigFile != null) {
            if (defaultConfigFile.delete()) {
                loadDefaults();
            } else {
                JOptionPane.showMessageDialog(MainFrame.this, "Can't reset default configuration!",
                        ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showChangelog() {
        SwingUtilities.invokeLater(() -> new ChangelogDialog(this).setVisible(true));
    }

    private void showChangelogOnce() {
        if (!Preferences.getChangelogVersion().equals(Constants.VERSION)) {
            showChangelog();
            Preferences.setChangelogVersion(Constants.VERSION);
        }
    }

}
