package bc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Standalone & launchable Modrinth-style Mod and Resource Pack Manager for Blockcraft.
 * Can be opened externally via {@code Blockcraft-mods.bat} or from the in-game Mods screen.
 */
public final class ModManagerMain {
   private static final Color BG_DARK = new Color(0x16, 0x18, 0x1C);
   private static final Color BG_CARD = new Color(0x22, 0x25, 0x2B);
   private static final Color BG_HEADER = new Color(0x11, 0x13, 0x16);
   private static final Color FG_TEXT = new Color(0xEC, 0xF0, 0xF6);
   private static final Color FG_MUTED = new Color(0x9A, 0xA2, 0xB1);
   private static final Color ACCENT_GREEN = new Color(0x1B, 0xD9, 0x6A);
   private static final Color ACCENT_RED = new Color(0xE0, 0x4F, 0x5F);

   private static JFrame activeFrame;

   private ModManagerMain() {
   }

   public static void main(String[] args) {
      ModLoader.init();
      ResourcePacks.refresh();
      for (String a : args) {
         if ("--status".equals(a) || "--list".equals(a)) {
            System.out.println("Blockcraft Mod Manager | Mods=" + ModLoader.allMods().size()
               + " | Active=" + ModLoader.activeCount()
               + " | Packs=" + ResourcePacks.list().size());
            return;
         }
      }
      if (GraphicsEnvironment.isHeadless()) {
         System.out.println("Headless environment: use --status for CLI output.");
         return;
      }
      SwingUtilities.invokeLater(() -> showWindow(null));
   }

   public static void openInProcess(Game g) {
      if (GraphicsEnvironment.isHeadless()) {
         return;
      }
      SwingUtilities.invokeLater(() -> showWindow(g));
   }

   private static void showWindow(Game g) {
      if (activeFrame != null && activeFrame.isDisplayable()) {
         activeFrame.toFront();
         activeFrame.requestFocus();
         return;
      }

      ModLoader.refresh();
      ResourcePacks.refresh();

      JFrame frame = new JFrame("Blockcraft Mod & Resource Pack Manager (Modrinth Hub)");
      activeFrame = frame;
      frame.setDefaultCloseOperation(g == null ? JFrame.EXIT_ON_CLOSE : JFrame.DISPOSE_ON_CLOSE);
      frame.setSize(860, 620);
      frame.setLocationRelativeTo(null);
      frame.getContentPane().setBackground(BG_DARK);
      frame.setLayout(new BorderLayout(0, 0));

      JPanel header = new JPanel(new BorderLayout(12, 0));
      header.setBackground(BG_HEADER);
      header.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

      JLabel title = new JLabel("BLOCKCRAFT MODRINTH HUB");
      title.setForeground(ACCENT_GREEN);
      title.setFont(new Font("SansSerif", Font.BOLD, 19));
      JLabel sub = new JLabel("Browse, install & manage mods (.jar / .bcmod) and Resource Packs");
      sub.setForeground(FG_MUTED);
      sub.setFont(new Font("SansSerif", Font.PLAIN, 12));

      JPanel titleBox = new JPanel();
      titleBox.setOpaque(false);
      titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
      titleBox.add(title);
      titleBox.add(Box.createVerticalStrut(3));
      titleBox.add(sub);
      header.add(titleBox, BorderLayout.WEST);

      JLabel statusLbl = new JLabel();
      statusLbl.setForeground(FG_TEXT);
      statusLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
      header.add(statusLbl, BorderLayout.EAST);

      frame.add(header, BorderLayout.NORTH);

      JTabbedPane tabs = new JTabbedPane();
      tabs.setBackground(BG_DARK);
      tabs.setForeground(FG_TEXT);

      JPanel catalogList = new JPanel();
      catalogList.setBackground(BG_DARK);
      catalogList.setLayout(new BoxLayout(catalogList, BoxLayout.Y_AXIS));

      JPanel installedList = new JPanel();
      installedList.setBackground(BG_DARK);
      installedList.setLayout(new BoxLayout(installedList, BoxLayout.Y_AXIS));

      JPanel packList = new JPanel();
      packList.setBackground(BG_DARK);
      packList.setLayout(new BoxLayout(packList, BoxLayout.Y_AXIS));

      JTextField searchField = new JTextField(20);
      JComboBox<String> catBox = new JComboBox<>(new String[]{
         "All Categories", "Utility / HUD", "Crafting", "Gameplay", "Building", "Survival", "Caving", "Custom Mod", "JAR Mod"
      });

      Runnable rebuildAll = () -> {
         String q = searchField.getText().trim().toLowerCase(Locale.ROOT);
         String cat = (String)catBox.getSelectedItem();

         catalogList.removeAll();
         installedList.removeAll();
         packList.removeAll();

         List<ModLoader.ModEntry> all = ModLoader.allMods();
         for (ModLoader.ModEntry m : all) {
            boolean matchesQ = q.isEmpty()
               || m.name.toLowerCase(Locale.ROOT).contains(q)
               || m.summary.toLowerCase(Locale.ROOT).contains(q)
               || m.author.toLowerCase(Locale.ROOT).contains(q);
            boolean matchesCat = cat == null || "All Categories".equals(cat) || m.category.equalsIgnoreCase(cat);
            if (matchesQ && matchesCat) {
               catalogList.add(buildModCard(m, false, statusLbl, g, () -> SwingUtilities.invokeLater(tabs::repaint)));
               catalogList.add(Box.createVerticalStrut(8));
            }
            if (m.installed) {
               installedList.add(buildModCard(m, true, statusLbl, g, () -> SwingUtilities.invokeLater(tabs::repaint)));
               installedList.add(Box.createVerticalStrut(8));
            }
         }

         for (ResourcePacks.Pack p : ResourcePacks.list()) {
            packList.add(buildPackCard(p, statusLbl, g));
            packList.add(Box.createVerticalStrut(8));
         }

         statusLbl.setText(ModLoader.activeCount() + " Active Mods  |  Pack: " + ResourcePacks.active().title);
         catalogList.revalidate();
         catalogList.repaint();
         installedList.revalidate();
         installedList.repaint();
         packList.revalidate();
         packList.repaint();
      };

      // Tab 1: Browse Modrinth Catalog
      JPanel browseTab = new JPanel(new BorderLayout(8, 8));
      browseTab.setBackground(BG_DARK);
      browseTab.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

      JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
      filterBar.setBackground(BG_CARD);
      JLabel sLbl = new JLabel("Search Mods:");
      sLbl.setForeground(FG_TEXT);
      filterBar.add(sLbl);
      filterBar.add(searchField);
      filterBar.add(catBox);

      JButton importBtn = styledButton("Import .jar / .bcmod", new Color(0x3B, 0x82, 0xF6));
      importBtn.addActionListener(e -> {
         JFileChooser fc = new JFileChooser();
         if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
               Files.copy(f.toPath(), ModLoader.dir().resolve(f.getName()), StandardCopyOption.REPLACE_EXISTING);
               ModLoader.refresh();
               rebuildAll.run();
            } catch (Exception ex) {
               JOptionPane.showMessageDialog(frame, "Failed to import mod: " + ex.getMessage());
            }
         }
      });
      filterBar.add(importBtn);

      JButton openModsBtn = styledButton("Open mods/ Folder", new Color(0x4B, 0x55, 0x63));
      openModsBtn.addActionListener(e -> openFolder(ModLoader.dir().toFile()));
      filterBar.add(openModsBtn);

      browseTab.add(filterBar, BorderLayout.NORTH);
      JScrollPane browseScroll = new JScrollPane(catalogList);
      browseScroll.setBorder(BorderFactory.createEmptyBorder());
      browseScroll.getVerticalScrollBar().setUnitIncrement(16);
      browseTab.add(browseScroll, BorderLayout.CENTER);

      // Tab 2: Installed Mods
      JPanel instTab = new JPanel(new BorderLayout(8, 8));
      instTab.setBackground(BG_DARK);
      instTab.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
      JScrollPane instScroll = new JScrollPane(installedList);
      instScroll.setBorder(BorderFactory.createEmptyBorder());
      instScroll.getVerticalScrollBar().setUnitIncrement(16);
      instTab.add(instScroll, BorderLayout.CENTER);

      // Tab 3: Resource Packs
      JPanel packsTab = new JPanel(new BorderLayout(8, 8));
      packsTab.setBackground(BG_DARK);
      packsTab.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
      JPanel packTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
      packTop.setBackground(BG_CARD);
      JButton openPacksBtn = styledButton("Open resourcepacks/ Folder", new Color(0x4B, 0x55, 0x63));
      openPacksBtn.addActionListener(e -> openFolder(ResourcePacks.dir().toFile()));
      JButton refreshPacksBtn = styledButton("Refresh Packs", ACCENT_GREEN);
      refreshPacksBtn.addActionListener(e -> {
         ResourcePacks.refresh();
         rebuildAll.run();
      });
      packTop.add(openPacksBtn);
      packTop.add(refreshPacksBtn);
      packsTab.add(packTop, BorderLayout.NORTH);
      JScrollPane packScroll = new JScrollPane(packList);
      packScroll.setBorder(BorderFactory.createEmptyBorder());
      packScroll.getVerticalScrollBar().setUnitIncrement(16);
      packsTab.add(packScroll, BorderLayout.CENTER);

      tabs.addTab("Browse Modrinth Catalog", browseTab);
      tabs.addTab("Installed Mods", instTab);
      tabs.addTab("Resource Packs", packsTab);
      tabs.addChangeListener(e -> rebuildAll.run());

      searchField.getDocument().addDocumentListener(new DocumentListener() {
         @Override
         public void insertUpdate(DocumentEvent e) {
            rebuildAll.run();
         }

         @Override
         public void removeUpdate(DocumentEvent e) {
            rebuildAll.run();
         }

         @Override
         public void changedUpdate(DocumentEvent e) {
            rebuildAll.run();
         }
      });
      catBox.addActionListener(e -> rebuildAll.run());

      rebuildAll.run();
      frame.add(tabs, BorderLayout.CENTER);
      frame.setVisible(true);
   }

   private static JPanel buildModCard(ModLoader.ModEntry m, boolean installedView, JLabel statusLbl, Game g, Runnable onUpdate) {
      JPanel card = new JPanel(new BorderLayout(12, 6));
      card.setBackground(BG_CARD);
      card.setBorder(BorderFactory.createCompoundBorder(
         BorderFactory.createLineBorder(new Color(m.badgeColor & 0xFFFFFF), 1),
         BorderFactory.createEmptyBorder(10, 14, 10, 14)
      ));
      card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));

      JPanel info = new JPanel();
      info.setOpaque(false);
      info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

      JLabel nameLbl = new JLabel(m.name + "  v" + m.version + "  [" + m.category + "]  by " + m.author);
      nameLbl.setForeground(FG_TEXT);
      nameLbl.setFont(new Font("SansSerif", Font.BOLD, 14));

      JLabel descLbl = new JLabel(m.summary);
      descLbl.setForeground(FG_MUTED);
      descLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));

      info.add(nameLbl);
      info.add(Box.createVerticalStrut(4));
      info.add(descLbl);
      card.add(info, BorderLayout.CENTER);

      JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
      actions.setOpaque(false);

      if (!m.installed) {
         JButton instBtn = styledButton("Install", ACCENT_GREEN);
         instBtn.addActionListener(e -> {
            ModLoader.install(m.id);
            statusLbl.setText(ModLoader.activeCount() + " Active Mods  |  Installed " + m.name);
            ContainerRefresh(card);
            onUpdate.run();
         });
         actions.add(instBtn);
      } else {
         JButton toggleBtn = styledButton(m.enabled ? "Enabled (ON)" : "Disabled (OFF)", m.enabled ? ACCENT_GREEN : new Color(0x6B, 0x72, 0x80));
         toggleBtn.addActionListener(e -> {
            ModLoader.toggleEnabled(m.id);
            toggleBtn.setText(m.enabled ? "Enabled (ON)" : "Disabled (OFF)");
            toggleBtn.setBackground(m.enabled ? ACCENT_GREEN : new Color(0x6B, 0x72, 0x80));
            statusLbl.setText(ModLoader.activeCount() + " Active Mods  |  Pack: " + ResourcePacks.active().title);
            onUpdate.run();
         });
         actions.add(toggleBtn);

         if (m.catalogMod) {
            JButton unBtn = styledButton("Uninstall", ACCENT_RED);
            unBtn.addActionListener(e -> {
               ModLoader.uninstall(m.id);
               statusLbl.setText(ModLoader.activeCount() + " Active Mods  |  Uninstalled " + m.name);
               onUpdate.run();
            });
            actions.add(unBtn);
         }
      }

      card.add(actions, BorderLayout.EAST);
      return card;
   }

   private static JPanel buildPackCard(ResourcePacks.Pack p, JLabel statusLbl, Game g) {
      boolean isAct = ResourcePacks.activeId().equalsIgnoreCase(p.id);
      JPanel card = new JPanel(new BorderLayout(12, 6));
      card.setBackground(BG_CARD);
      card.setBorder(BorderFactory.createCompoundBorder(
         BorderFactory.createLineBorder(isAct ? ACCENT_GREEN : new Color(p.badgeColor & 0xFFFFFF), isAct ? 2 : 1),
         BorderFactory.createEmptyBorder(10, 14, 10, 14)
      ));
      card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));

      JPanel info = new JPanel();
      info.setOpaque(false);
      info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

      JLabel nameLbl = new JLabel(p.title + (p.builtIn ? "  [Built-In]" : "  [Custom Pack]"));
      nameLbl.setForeground(FG_TEXT);
      nameLbl.setFont(new Font("SansSerif", Font.BOLD, 14));

      JLabel descLbl = new JLabel(p.subtitle);
      descLbl.setForeground(FG_MUTED);
      descLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));

      info.add(nameLbl);
      info.add(Box.createVerticalStrut(4));
      info.add(descLbl);
      card.add(info, BorderLayout.CENTER);

      JButton useBtn = styledButton(isAct ? "Active Pack" : "Activate Pack", isAct ? ACCENT_GREEN : new Color(0x3B, 0x82, 0xF6));
      useBtn.addActionListener(e -> {
         if (g == null) {
            Options opt = new Options(new File("."));
            opt.load();
            opt.resourcePack = p.id;
            opt.save();
            ResourcePacks.setActiveId(p.id);
         } else {
            ResourcePacks.select(p.id, g);
         }
         statusLbl.setText(ModLoader.activeCount() + " Active Mods  |  Pack: " + ResourcePacks.active().title);
      });
      JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
      actions.setOpaque(false);
      actions.add(useBtn);
      card.add(actions, BorderLayout.EAST);
      return card;
   }

   private static void ContainerRefresh(Component c) {
      c.revalidate();
      c.repaint();
   }

   private static JButton styledButton(String text, Color bg) {
      JButton b = new JButton(text);
      b.setBackground(bg);
      b.setForeground(Color.WHITE);
      b.setFocusPainted(false);
      b.setFont(new Font("SansSerif", Font.BOLD, 12));
      b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      return b;
   }

   private static void openFolder(File dir) {
      try {
         if (!dir.exists()) {
            dir.mkdirs();
         }
         if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(dir);
         }
      } catch (Exception ignored) {
      }
   }
}
