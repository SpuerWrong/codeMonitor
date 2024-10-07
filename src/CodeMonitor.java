import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
/*
  @author 编程起步-孤洲工作室
 */
public class CodeMonitor extends JFrame {
    private static final String STATS_FILE = "code_stats.dat";
    private static final String SETTINGS_FILE = "settings.dat";

    private Map<String, Integer> directoryTotals = new HashMap<>(); // Directory to its total lines
    private Map<String, Map<String, Integer>> directoryFileLines = new HashMap<>(); // Directory to (File to lines)
    private Map<String, Integer> baselineTotals = new HashMap<>(); // Date to baseline total lines
    private List<DailyStat> dailyStats = new ArrayList<>();

    private int totalLines = 0;
    private int todayBaseline = 0;
    private String lastUpdateDate;

    private JLabel totalLabel;
    private JLabel dailyLabel;
    private JLabel directoryLabel; // 显示当前监控目录
    private JButton changeDirButton;
    private Path monitoredDir;

    public CodeMonitor() {
        // 初始化窗口
        setTitle("代码行数统计");
        setSize(500, 290);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 设置字体和对齐方式
        Font font = new Font("宋体", Font.BOLD, 26); // 字体：宋体，加粗，26pt

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 1));

        // 显示当前监控目录
        Font dirFont = new Font("黑体", Font.PLAIN, 14); // 字体：黑体，20px
        directoryLabel = new JLabel("当前监控目录：", SwingConstants.LEFT);
        directoryLabel.setFont(dirFont);
        directoryLabel.setHorizontalAlignment(SwingConstants.LEFT);

        totalLabel = new JLabel("累计代码行数: 0", SwingConstants.CENTER);
        totalLabel.setFont(font);
        dailyLabel = new JLabel("今日新增代码行数: 0", SwingConstants.CENTER);
        dailyLabel.setFont(font);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        changeDirButton = new JButton("修改Java项目");
        changeDirButton.setFont(font);
        changeDirButton.addActionListener(this::handleChangeDir);

        JButton dailyStatsButton = new JButton("每日统计");
        dailyStatsButton.setFont(font);
        dailyStatsButton.addActionListener(this::showDailyStats);

        JButton exitButton = new JButton("退出");
        exitButton.setFont(font);
        exitButton.addActionListener(e -> System.exit(0));

        buttonPanel.add(changeDirButton);
        buttonPanel.add(dailyStatsButton);
        buttonPanel.add(exitButton);

        panel.add(directoryLabel);
        panel.add(totalLabel);
        panel.add(dailyLabel);

        add(panel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        loadSettings();
        loadStats();

        refreshLabels();

        if (monitoredDir == null) {
            selectDirectory();
        } else {
            initializeMonitoring(monitoredDir);
        }

        setVisible(true);

        // 启动每日重置线程
        startDailyResetThread();
    }

    private void loadSettings() {
        File file = new File(SETTINGS_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                monitoredDir = Paths.get((String) ois.readObject());
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveSettings() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SETTINGS_FILE))) {
            oos.writeObject(monitoredDir.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadStats() {
        File file = new File(STATS_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                directoryTotals = (Map<String, Integer>) ois.readObject();
                directoryFileLines = (Map<String, Map<String, Integer>>) ois.readObject();
                baselineTotals = (Map<String, Integer>) ois.readObject();
                dailyStats = (List<DailyStat>) ois.readObject();
                totalLines = directoryTotals.values().stream().mapToInt(Integer::intValue).sum();

                todayBaseline = ois.readInt();
                lastUpdateDate = (String) ois.readObject();

                updateDailyLines();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                initializeStats();
            }
        } else {
            initializeStats();
        }
    }

    private void initializeStats() {
        directoryTotals = new HashMap<>();
        directoryFileLines = new HashMap<>();
        baselineTotals = new HashMap<>();
        dailyStats = new ArrayList<>();
        totalLines = 0;
        todayBaseline = 0;
        lastUpdateDate = getTodayDate();
    }

    private void saveStats() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(STATS_FILE))) {
            oos.writeObject(directoryTotals);
            oos.writeObject(directoryFileLines);
            oos.writeObject(baselineTotals);
            oos.writeObject(dailyStats);
            oos.writeInt(todayBaseline);
            oos.writeObject(lastUpdateDate);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void selectDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            monitoredDir = chooser.getSelectedFile().toPath();
            directoryLabel.setText("当前监控目录：" + monitoredDir.toString());
            saveSettings();
            initializeMonitoring(monitoredDir);
        } else {
            JOptionPane.showMessageDialog(this, "未选择目录，程序将退出。");
            System.exit(0);
        }
    }

    private void initializeMonitoring(Path dir) {
        if (!directoryTotals.containsKey(dir.toString())) {
            directoryTotals.put(dir.toString(), 0);
            directoryFileLines.put(dir.toString(), new HashMap<>());
            try {
                Files.walk(dir).filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                    try {
                        int lines = countCodeLines(p);
                        directoryFileLines.get(dir.toString()).put(p.toString(), lines);
                        directoryTotals.put(dir.toString(), directoryTotals.get(dir.toString()) + lines);
                        totalLines += lines;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                saveStats();
                updateDailyLines();
                refreshLabels();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        directoryLabel.setText("当前监控目录：" + dir.toString());
        startMonitoring(dir);
    }

    private void handleChangeDir(ActionEvent e) {
        selectDirectory();
    }

    private void refreshLabels() {
        totalLabel.setText("累计代码行数: " + totalLines);
        int todayLines = totalLines - todayBaseline;
        dailyLabel.setText("今日新增代码行数: " + todayLines);
    }

    private void updateDailyLines() {
        String today = getTodayDate();
        if (!today.equals(lastUpdateDate)) {
            // 保存前一天的统计数据
            int todayLines = totalLines - todayBaseline;
            dailyStats.add(new DailyStat(lastUpdateDate, todayLines, totalLines));
            // 重置当天基准
            todayBaseline = totalLines;
            lastUpdateDate = today;
            baselineTotals.put(today, todayBaseline);
            saveStats();
        } else {
            saveStats(); // 确保当日数据也被保存
        }
    }

    private void showDailyStats(ActionEvent e) {
        JDialog dialog = new JDialog(this, "每日统计", true);
        dialog.setSize(600, 400);
        dialog.setLayout(new BorderLayout());

        String[] columnNames = {"日期", "当日打代码行数", "累计打代码行数"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);

        for (DailyStat stat : dailyStats) {
            tableModel.addRow(new Object[]{stat.date, stat.dailyLines, stat.totalLines});
        }

        // 添加今天的统计数据
        int todayLines = totalLines - todayBaseline;
        tableModel.addRow(new Object[]{getTodayDate(), todayLines, totalLines});

        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void startMonitoring(Path dir) {
        new Thread(() -> {
            try {
                WatchService watchService = FileSystems.getDefault().newWatchService();
                Map<WatchKey, Path> keyDirMap = new HashMap<>();
                registerAll(dir, watchService, keyDirMap);

                while (true) {
                    WatchKey key = watchService.take();
                    Path eventDir = keyDirMap.get(key);

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }

                        Path name = (Path) event.context();
                        Path child = eventDir.resolve(name);

                        if (Files.isDirectory(child)) {
                            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                registerAll(child, watchService, keyDirMap);
                            }
                            continue;
                        }

                        if (child.toString().endsWith(".java")) {
                            if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                int newLines = countCodeLines(child);
                                Map<String, Integer> fileLines = directoryFileLines.get(eventDir.toString());
                                int previousLines = fileLines.getOrDefault(child.toString(), 0);
                                int delta = newLines - previousLines;

                                fileLines.put(child.toString(), newLines);
                                directoryTotals.put(eventDir.toString(), directoryTotals.get(eventDir.toString()) + delta);
                                totalLines += delta;

                                updateDailyLines();
                                refreshLabels();
                            } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                Map<String, Integer> fileLines = directoryFileLines.get(eventDir.toString());
                                int removedLines = fileLines.getOrDefault(child.toString(), 0);
                                fileLines.remove(child.toString());
                                directoryTotals.put(eventDir.toString(), directoryTotals.get(eventDir.toString()) - removedLines);
                                totalLines -= removedLines;

                                updateDailyLines();
                                refreshLabels();
                            }
                        }
                    }

                    boolean valid = key.reset();
                    if (!valid) {
                        keyDirMap.remove(key);
                        if (keyDirMap.isEmpty()) {
                            break;
                        }
                    }
                }
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void registerAll(final Path start, final WatchService ws, final Map<WatchKey, Path> keyDirMap) throws IOException {
        Files.walk(start).filter(Files::isDirectory).forEach(path -> {
            try {
                WatchKey key = path.register(ws, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
                keyDirMap.put(key, path);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    private int countCodeLines(Path file) throws IOException {
        int lines = 0;
        boolean inBlockComment = false;
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (inBlockComment) {
                    if (line.contains("*/")) {
                        inBlockComment = false;
                    }
                    continue;
                }
                if (line.startsWith("/*")) {
                    if (!line.contains("*/")) {
                        inBlockComment = true;
                    }
                    continue;
                }
                if (!line.startsWith("//") && !line.isEmpty()) {
                    lines++;
                }
            }
        }
        return lines;
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    private void startDailyResetThread() {
        // 启动一个线程，每分钟检查一次日期是否变化
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // 每分钟检查一次
                    String today = getTodayDate();
                    if (!today.equals(lastUpdateDate)) {
                        updateDailyLines();
                        refreshLabels();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CodeMonitor::new);
    }

    // 用于存储每日统计数据的类
    static class DailyStat implements Serializable {
        private static final long serialVersionUID = 1L;
        String date;
        int dailyLines;
        int totalLines;

        public DailyStat(String date, int dailyLines, int totalLines) {
            this.date = date;
            this.dailyLines = dailyLines;
            this.totalLines = totalLines;
        }
    }
}
