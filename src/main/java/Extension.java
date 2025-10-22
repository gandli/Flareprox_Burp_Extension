import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.http.message.HttpRequestResponse;
import javax.swing.JMenuItem;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
// import java.awt.datatransfer.StringSelection;
// import java.awt.datatransfer.Clipboard;
import java.net.URI;
// import java.net.http.HttpClient;
// import java.net.http.HttpRequest;
// import java.net.http.HttpResponse;
import java.util.List;
import java.util.ArrayList;
// import java.util.UUID;

public class Extension implements BurpExtension {
    private static final String PREF_API_TOKEN = "cloudflare_api_token";
    private static final String PREF_ACCOUNT_ID = "cloudflare_account_id";

    // ---------------------- Helpers: preferences & layout ----------------------
    private static String getPreference(MontoyaApi api, String key) {
        var prefs = api.persistence().preferences();
        if (prefs.stringKeys().contains(key)) {
            String v = prefs.getString(key);
            return v != null ? v : "";
        }
        return "";
    }

    private static void addFormRow(JPanel form, int row, String label, JComponent field) {
        Insets insets = new Insets(4, 4, 4, 4);
    
        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        labelGbc.insets = insets;
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.weightx = 0;
        form.add(new JLabel(label), labelGbc);
    
        GridBagConstraints fieldGbc = new GridBagConstraints();
        fieldGbc.gridx = 1;
        fieldGbc.gridy = row;
        fieldGbc.insets = insets;
        fieldGbc.anchor = GridBagConstraints.WEST;
        fieldGbc.fill = GridBagConstraints.NONE;
        fieldGbc.weightx = 0;
        form.add(field, fieldGbc);
    
        GridBagConstraints fillerGbc = new GridBagConstraints();
        fillerGbc.gridx = 2;
        fillerGbc.gridy = row;
        fillerGbc.insets = insets;
        fillerGbc.weightx = 1.0;
        fillerGbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(Box.createHorizontalGlue(), fillerGbc);
    }

    private static JPanel createSection(String title, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(6, 6, 0, 6),
                BorderFactory.createTitledBorder(title)));
        panel.add(content, BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, content.getPreferredSize().height + 28));
        return panel;
    }

    // ---------------------- Java translation of flareprox.py
    // ----------------------

    // ---------------------- Initialize Burp extension ----------------------
    @Override
    public void initialize(MontoyaApi montoyaApi) {
        montoyaApi.extension().setName("FlareProx");

        // 注册一个上下文菜单项提供者
        montoyaApi.userInterface().registerContextMenuItemsProvider(new ContextMenuItemsProvider() {
            @Override
            public List<Component> provideMenuItems(ContextMenuEvent event) {
                JMenuItem item = new JMenuItem("Rotating IP Proxy");
                item.addActionListener(l -> {
                    event.messageEditorRequestResponse().ifPresent(messageEditorReqRes -> {
                        HttpRequestResponse reqRes = messageEditorReqRes.requestResponse();
                        montoyaApi.logging().logToOutput("Request URL: " + reqRes.request().url());
                        montoyaApi.logging().raiseInfoEvent("Request URL: " + reqRes.request().url());
                    });
                });
                return List.of(item);
            }
        });

        // 添加 Cloudflare 凭证设置选项卡（优化布局）
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(6, 6, 6, 6));

        // 说明区域（HTML，可点击链接）
        String helpHtml = "<html >"
                + "<h3 style='margin:0 0 8px 0;'>How to get Cloudflare API Token and Account ID</h3>"
                + "<ol style='margin:0 0 8px 20px;'>"
                + "<li>Sign in to <a href='https://dash.cloudflare.com/'>Cloudflare Dashboard</a></li>"
                + "<li>Go to API Tokens, click Create Token, choose 'Edit Cloudflare Workers' template</li>"
                + "<li>Allow account/zone resources and create the token; find and copy Account ID on Dashboard</li>"
                + "<li>Paste the API Token and Account ID below</li>"
                + "</ol>"
                + "</html>";
        JEditorPane helpPane = new JEditorPane("text/html", helpHtml);
        helpPane.setEditable(false);
        helpPane.setOpaque(false);
        helpPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(new URI(e.getURL().toString()));
                    }
                } catch (Exception ex) {
                    montoyaApi.logging().logToError("Failed to open browser: " + ex.getMessage());
                }
            }
        });
        JPanel helpSection = createSection("Help", helpPane);
        root.add(helpSection);

        // 表单区域（两列对齐）
        JPanel form = new JPanel(new GridBagLayout());

        // 读取已保存的值
        String existingToken = getPreference(montoyaApi, PREF_API_TOKEN);
        String existingAccountId = getPreference(montoyaApi, PREF_ACCOUNT_ID);

        // 行1：API Token
        JPasswordField tokenField = new JPasswordField(existingToken, 24);
        tokenField.setEchoChar('*');
        tokenField.setToolTipText("Create an API Token on Cloudflare and paste here");
        JToggleButton tokenEye = new JToggleButton("👁");
        tokenEye.setMargin(new Insets(0, 6, 0, 6));
        tokenEye.setFocusable(false);
        tokenEye.setToolTipText("Show API Token");
        final char tokenEcho = tokenField.getEchoChar();
        tokenEye.addItemListener(ev -> {
            boolean on = tokenEye.isSelected();
            tokenField.setEchoChar(on ? (char)0 : tokenEcho);
            tokenEye.setToolTipText(on ? "Hide API Token" : "Show API Token");
        });
        JPanel tokenFieldPanel = new JPanel(new BorderLayout(4, 0));
        tokenFieldPanel.add(tokenField, BorderLayout.CENTER);
        tokenFieldPanel.add(tokenEye, BorderLayout.EAST);
        addFormRow(form, 0, "Cloudflare API Token", tokenFieldPanel);

        // 行2：Account ID
        JPasswordField accountIdField = new JPasswordField(existingAccountId, 24);
        accountIdField.setEchoChar('*');
        accountIdField.setToolTipText("Find on Dashboard top-right or Account settings");
        JToggleButton accountEye = new JToggleButton("👁");
        accountEye.setMargin(new Insets(0, 6, 0, 6));
        accountEye.setFocusable(false);
        accountEye.setToolTipText("Show Account ID");
        final char accountEcho = accountIdField.getEchoChar();
        accountEye.addItemListener(ev -> {
            boolean on = accountEye.isSelected();
            accountIdField.setEchoChar(on ? (char)0 : accountEcho);
            accountEye.setToolTipText(on ? "Hide Account ID" : "Show Account ID");
        });
        JPanel accountFieldPanel = new JPanel(new BorderLayout(4, 0));
        accountFieldPanel.add(accountIdField, BorderLayout.CENTER);
        accountFieldPanel.add(accountEye, BorderLayout.EAST);
        addFormRow(form, 1, "Cloudflare Account ID", accountFieldPanel);

        JPanel formSection = createSection("Cloudflare Credentials", form);
        root.add(formSection);

        // 按钮区域
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

        // 追加：部署计数与按钮
        actions.add(Box.createRigidArea(new Dimension(8, 0)));
        actions.add(new JLabel("Count:"));
        JSpinner deployCountSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
        ((JSpinner.DefaultEditor) deployCountSpinner.getEditor()).getTextField().setColumns(3);
        actions.add(deployCountSpinner);
        actions.add(Box.createRigidArea(new Dimension(8, 0)));
        JButton deployBtn = new JButton("Deploy proxy endpoints");
        actions.add(deployBtn);
        actions.add(Box.createRigidArea(new Dimension(8, 0)));
        JButton listBtn = new JButton("List URLs");
        actions.add(listBtn);
        actions.add(Box.createRigidArea(new Dimension(8, 0)));
        JButton deleteBtn = new JButton("Clean up all deployed endpoints");
        deleteBtn.setForeground(new Color(180, 0, 0));
        actions.add(deleteBtn);

        // 状态标签：显示执行中/部署中/获取中/清理中等
        actions.add(Box.createRigidArea(new Dimension(16, 0)));
        actions.add(new JLabel("Status:"));
        final JLabel statusLabel = new JLabel("Idle");
        actions.add(statusLabel);

        JPanel actionsSection = createSection("Operations", actions);
        root.add(actionsSection);

        // 端点列表 UI（使用 JTable 展示 Name/URL/IP）
        final javax.swing.table.DefaultTableModel endpointsTableModel =
                new javax.swing.table.DefaultTableModel(new Object[] { "No.", "Name", "URL", "IP" }, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) { return false; }
                };
        final List<FlareProx.Endpoint> createdEndpointsCache = new ArrayList<>();
        JTable endpointsTable = new JTable(endpointsTableModel);
        endpointsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        endpointsTable.setFillsViewportHeight(true);
        // 启用列排序（点击表头切换升序/降序），并为 IP 列设置数值比较
        javax.swing.table.TableRowSorter<javax.swing.table.DefaultTableModel> sorter =
                new javax.swing.table.TableRowSorter<>(endpointsTableModel);
        java.util.Comparator<Object> serialComparator = (a, b) -> {
            int ia = 0;
            int ib = 0;
            try { ia = (a instanceof Number) ? ((Number) a).intValue() : Integer.parseInt(a == null ? "0" : a.toString().trim()); } catch (Exception ignore) {}
            try { ib = (b instanceof Number) ? ((Number) b).intValue() : Integer.parseInt(b == null ? "0" : b.toString().trim()); } catch (Exception ignore) {}
            return Integer.compare(ia, ib);
        };
        java.util.Comparator<String> ipComparator = (a, b) -> {
            String sa = a == null ? "" : a.trim();
            String sb = b == null ? "" : b.trim();
            boolean aSpecial = sa.isEmpty() || "pending".equalsIgnoreCase(sa) || "n/a".equalsIgnoreCase(sa);
            boolean bSpecial = sb.isEmpty() || "pending".equalsIgnoreCase(sb) || "n/a".equalsIgnoreCase(sb);
            if (aSpecial && bSpecial) return sa.compareToIgnoreCase(sb);
            if (aSpecial) return 1;
            if (bSpecial) return -1;
            long va = parseIPv4ToLong(sa);
            long vb = parseIPv4ToLong(sb);
            if (va >= 0 && vb >= 0) {
                return Long.compare(va, vb);
            }
            return sa.compareToIgnoreCase(sb);
        };
        sorter.setComparator(0, serialComparator);
        sorter.setComparator(3, ipComparator);
        endpointsTable.setRowSorter(sorter);

        // 快捷键复制选中行
        javax.swing.KeyStroke copyKs = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        endpointsTable.getInputMap(JComponent.WHEN_FOCUSED).put(copyKs, "copySelected");
        endpointsTable.getActionMap().put("copySelected", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int[] rows = endpointsTable.getSelectedRows();
                if (rows != null && rows.length > 0) {
                    java.util.List<String> lines = new java.util.ArrayList<>();
                    for (int r : rows) {
                        int mr = endpointsTable.convertRowIndexToModel(r);
                        String name = String.valueOf(endpointsTableModel.getValueAt(mr, 1));
                        String url = String.valueOf(endpointsTableModel.getValueAt(mr, 2));
                        String ip = String.valueOf(endpointsTableModel.getValueAt(mr, 3));
                        lines.add(name + " -> " + url + " [IP: " + ip + "]");
                    }
                    String text = String.join("\n", lines);
                    java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(text);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
                    montoyaApi.logging().logToOutput("[Results] Copied selected endpoints to clipboard.");
                    montoyaApi.logging().raiseInfoEvent("Copied selected endpoints to clipboard.");
                }
            }
        });

        // 右键菜单：复制整行、仅 URL、仅 IP
        JPopupMenu endpointsPopup = new JPopupMenu();
        JMenuItem copyRowItem = new JMenuItem("Copy Row");
        JMenuItem copyUrlItem = new JMenuItem("Copy URL");
        JMenuItem copyIpItem = new JMenuItem("Copy IP");
        endpointsPopup.add(copyRowItem);
        endpointsPopup.add(copyUrlItem);
        endpointsPopup.add(copyIpItem);
        endpointsTable.setComponentPopupMenu(endpointsPopup);

        copyRowItem.addActionListener(ev -> {
            int[] rows = endpointsTable.getSelectedRows();
            if (rows != null && rows.length > 0) {
                java.util.List<String> lines = new java.util.ArrayList<>();
                for (int r : rows) {
                    int mr = endpointsTable.convertRowIndexToModel(r);
                    String name = String.valueOf(endpointsTableModel.getValueAt(mr, 1));
                    String url = String.valueOf(endpointsTableModel.getValueAt(mr, 2));
                    String ip = String.valueOf(endpointsTableModel.getValueAt(mr, 3));
                    lines.add(name + " -> " + url + " [IP: " + ip + "]");
                }
                String text = String.join("\n", lines);
                java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(text);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
                montoyaApi.logging().raiseInfoEvent("Copied row(s) to clipboard");
            }
        });

        copyUrlItem.addActionListener(ev -> {
            int[] rows = endpointsTable.getSelectedRows();
            if (rows != null && rows.length > 0) {
                java.util.List<String> urls = new java.util.ArrayList<>();
                for (int r : rows) {
                    int mr = endpointsTable.convertRowIndexToModel(r);
                    String url = String.valueOf(endpointsTableModel.getValueAt(mr, 2));
                    urls.add(url);
                }
                String text = String.join("\n", urls);
                java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(text);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
                montoyaApi.logging().raiseInfoEvent("Copied URL(s) to clipboard");
            }
        });

        copyIpItem.addActionListener(ev -> {
            int[] rows = endpointsTable.getSelectedRows();
            if (rows != null && rows.length > 0) {
                java.util.List<String> ips = new java.util.ArrayList<>();
                for (int r : rows) {
                    int mr = endpointsTable.convertRowIndexToModel(r);
                    String ip = String.valueOf(endpointsTableModel.getValueAt(mr, 3));
                    ips.add(ip);
                }
                String text = String.join("\n", ips);
                java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(text);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
                montoyaApi.logging().raiseInfoEvent("Copied IP(s) to clipboard");
            }
        });

        // 双击复制 URL
        endpointsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = endpointsTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        int mr = endpointsTable.convertRowIndexToModel(row);
                        String url = String.valueOf(endpointsTableModel.getValueAt(mr, 2));
                        java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(url);
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
                        montoyaApi.logging().raiseInfoEvent("URL copied to clipboard");
                    }
                }
            }
        });

        JScrollPane endpointsScroll = new JScrollPane(endpointsTable);
        JPanel resultsSection = createSection("Deployed Endpoints", endpointsScroll);
        resultsSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 228));
        root.add(resultsSection);

        // 自动保存逻辑：字段失去焦点时保存
        java.awt.event.FocusAdapter autoSaveListener = new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                String token = new String(tokenField.getPassword()).trim();
                String accountId = new String(accountIdField.getPassword()).trim();

                // 基础校验：Token 允许字母数字/下划线/短横线，长度≥20；Account ID 为32位十六进制
                final boolean tokenOk = !token.isBlank() && token.matches("^[A-Za-z0-9_-]{20,80}$");
                final boolean accountOk = !accountId.isBlank() && accountId.matches("(?i)^[a-f0-9]{32}$");
                if (!tokenOk || !accountOk) {
                    StringBuilder sb = new StringBuilder();
                    if (!tokenOk) sb.append("Invalid token format (alphanumeric/_/-, length ≥ 20)");
                    if (!accountOk) {
                        if (sb.length() > 0) sb.append("，");
                        sb.append("Invalid Account ID format (32 hex characters)");
                    }
                    statusLabel.setText("Validation failed: " + sb.toString());
                    montoyaApi.logging().raiseErrorEvent("Invalid Cloudflare credentials format");
                    return;
                }

                // 保存并提示
                montoyaApi.persistence().preferences().setString(PREF_API_TOKEN, token);
                montoyaApi.persistence().preferences().setString(PREF_ACCOUNT_ID, accountId);
                montoyaApi.logging().logToOutput("[Settings] Auto-saved Cloudflare credentials.");
                montoyaApi.logging().raiseInfoEvent("Cloudflare credentials auto-saved");
            
                // 两者均有效时自动执行“显示 URLs”
                final String tokenFinal = token;
                final String accountIdFinal = accountId;
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (!tokenFinal.isBlank() && !accountIdFinal.isBlank()) {
                        statusLabel.setText("Fetching...");
                        listBtn.doClick();
                    }
                });
            }
        };
        tokenField.addFocusListener(autoSaveListener);
        accountIdField.addFocusListener(autoSaveListener);

        // 部署逻辑：Java 版 create_proxies
        deployBtn.addActionListener(e -> {
            int count = (int) deployCountSpinner.getValue();
            String token = new String(tokenField.getPassword()).trim();
            String accountId = new String(accountIdField.getPassword()).trim();
            final String tokenFinal = token.isBlank() ? getPreference(montoyaApi, PREF_API_TOKEN) : token;
            final String accountIdFinal = accountId.isBlank() ? getPreference(montoyaApi, PREF_ACCOUNT_ID) : accountId;

            // 详细部署日志
            montoyaApi.logging()
                    .logToOutput("[Deploy] Requested deploy count=" + count + ", Account ID=" + accountIdFinal);
            montoyaApi.logging()
                    .raiseInfoEvent("[Deploy] Requested deploy count=" + count + ", Account ID=" + accountIdFinal);

            // 状态更新与禁用按钮
            statusLabel.setText("Deploying...");
            deployBtn.setEnabled(false);
            listBtn.setEnabled(false);
            deleteBtn.setEnabled(false);

            new Thread(() -> {
                CloudflareService cf = new SimpleCloudflareService(tokenFinal, accountIdFinal, montoyaApi);
                FlareProx prox = new FlareProx(cf);
                try {
                    FlareProx.Result res = prox.createProxies(count, montoyaApi);
                    montoyaApi.logging().logToOutput(
                            "Deploy completed. Created: " + res.created.size() + ", Failed: " + res.failed);
                    montoyaApi.logging().raiseInfoEvent(
                            "Deploy completed. Created: " + res.created.size() + ", Failed: " + res.failed);
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        if (!res.created.isEmpty()) {
                            // 在部署成功后，直接将新端点填入表格并进行 IP 探测
                            java.util.concurrent.ExecutorService execCreated = java.util.concurrent.Executors
                                    .newFixedThreadPool(Math.min(4, Math.max(1, res.created.size())));
                            for (FlareProx.Endpoint ep : res.created) {
                                final int rowIndex = endpointsTableModel.getRowCount();
                                endpointsTableModel.addRow(new Object[] { rowIndex + 1, ep.name, ep.url, "pending" });
                                createdEndpointsCache.add(ep);
                                execCreated.submit(() -> {
                                    int maxAttempts = 3;
                                    long baseDelayMs = 500;
                                    String ip = "n/a";
                                    boolean parsed = false;
                                    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                                        try {
                                            String probeUrl = ep.url + "/https://httpbin.org/ip";
                                            burp.api.montoya.http.message.requests.HttpRequest req = burp.api.montoya.http.message.requests.HttpRequest
                                                    .httpRequestFromUrl(probeUrl)
                                                    .withMethod("GET");
                                            burp.api.montoya.http.message.HttpRequestResponse rr = montoyaApi.http()
                                                    .sendRequest(req);
                                            int status = (rr != null && rr.response() != null) ? rr.response().statusCode()
                                                    : -1;
                                            String respBody = (rr != null && rr.response() != null)
                                                    ? rr.response().bodyToString()
                                                    : "";

                                            if (status == 200 && respBody != null && !respBody.isBlank()) {
                                                java.util.regex.Matcher m = java.util.regex.Pattern
                                                        .compile("\"origin\"\\s*:\\s*\"([^\"]+)\"")
                                                        .matcher(respBody);
                                                if (m.find()) {
                                                    String origin = m.group(1);
                                                    String[] parts = origin.split(",");
                                                    String chosen = parts[parts.length - 1].trim();
                                                    ip = chosen;
                                                    parsed = true;
                                                } else {
                                                    java.util.regex.Matcher ipv4 = java.util.regex.Pattern
                                                            .compile("(?:\\b\\d{1,3}\\.){3}\\d{1,3}\\b")
                                                            .matcher(respBody);
                                                    if (ipv4.find()) {
                                                        ip = ipv4.group();
                                                        parsed = true;
                                                    } else if (attempt == maxAttempts) {
                                                        String preview = respBody.length() > 200
                                                                ? respBody.substring(0, 200) + "..."
                                                                : respBody;
                                                        montoyaApi.logging().logToOutput("[Deploy] IP parse failed for "
                                                                + ep.name + " body preview: " + preview);
                                                    }
                                                }
                                            } else {
                                                montoyaApi.logging().logToOutput(
                                                        "[Deploy] IP probe failed status=" + status + " for " + ep.name + " attempt " + attempt + "/" + maxAttempts);
                                                if (respBody != null && !respBody.isBlank() && attempt == maxAttempts) {
                                                    String preview = respBody.length() > 200
                                                            ? respBody.substring(0, 200) + "..."
                                                            : respBody;
                                                    montoyaApi.logging()
                                                            .logToOutput("[Deploy] IP probe body preview: " + preview);
                                                }
                                            }

                                            if (parsed) {
                                                break;
                                            }

                                            if (attempt < maxAttempts && "n/a".equals(ip)) {
                                                try {
                                                    Thread.sleep(baseDelayMs * attempt);
                                                } catch (InterruptedException ie) {
                                                    Thread.currentThread().interrupt();
                                                    break;
                                                }
                                            }
                                        } catch (Exception exIp) {
                                            montoyaApi.logging().logToOutput(
                                                    "[Deploy] IP probe error for " + ep.name + " attempt " + attempt + "/" + maxAttempts + ": " + exIp.getMessage());
                                        }
                                    }
                                    final String ipFinal = ip;
                                    javax.swing.SwingUtilities.invokeLater(() -> {
                                        endpointsTableModel.setValueAt(ipFinal, rowIndex, 3);
                                    });
                                });
                            }
                            execCreated.shutdown();
                        }
                        statusLabel.setText("Deploy finished: created " + res.created.size() + " , failed " + res.failed);
                    });
                } catch (FlareProx.FlareProxException ex) {
                    montoyaApi.logging().logToError("Deploy failed: " + ex.getMessage());
                    javax.swing.SwingUtilities.invokeLater(() -> statusLabel.setText("Deploy failed: " + ex.getMessage()));
                } finally {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        deployBtn.setEnabled(true);
                        listBtn.setEnabled(true);
                        deleteBtn.setEnabled(true);
                    });
                }
            }).start();
        });

        // 刷新/显示 URLs：从 Cloudflare 拉取 flareprox-* 并显示
        listBtn.addActionListener(e -> {
            String token = new String(tokenField.getPassword()).trim();
            String accountId = new String(accountIdField.getPassword()).trim();
            final String tokenFinal = token.isBlank() ? getPreference(montoyaApi, PREF_API_TOKEN) : token;
            final String accountIdFinal = accountId.isBlank() ? getPreference(montoyaApi, PREF_ACCOUNT_ID) : accountId;

            statusLabel.setText("Fetching...");
            listBtn.setEnabled(false);
            deployBtn.setEnabled(false);
            deleteBtn.setEnabled(false);

            new Thread(() -> {
                CloudflareService cf = new SimpleCloudflareService(tokenFinal, accountIdFinal, montoyaApi);
                try {
                    List<FlareProx.Endpoint> eps = cf.listEndpoints();
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        endpointsTableModel.setRowCount(0);
                        createdEndpointsCache.clear();
                        if (eps.isEmpty()) {
                            statusLabel.setText("Fetched 0 URLs");
                        } else {
                            // 在后台线程中进行 IP 探测，避免在 EDT 进行网络请求
                            java.util.concurrent.ExecutorService exec = java.util.concurrent.Executors
                                    .newFixedThreadPool(Math.min(4, Math.max(1, eps.size())));

                            for (FlareProx.Endpoint ep : eps) {
                                final int rowIndex = endpointsTableModel.getRowCount();
                                endpointsTableModel.addRow(new Object[] { rowIndex + 1, ep.name, ep.url, "pending" });
                                createdEndpointsCache.add(ep);
                                exec.submit(() -> {
                                    int maxAttempts = 3;
                                    long baseDelayMs = 500;
                                    String ip = "n/a";
                                    boolean parsed = false;
                                    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                                        try {
                                            String probeUrl = ep.url + "/https://httpbin.org/ip";
                                            burp.api.montoya.http.message.requests.HttpRequest req = burp.api.montoya.http.message.requests.HttpRequest
                                                    .httpRequestFromUrl(probeUrl)
                                                    .withMethod("GET");
                                            burp.api.montoya.http.message.HttpRequestResponse rr = montoyaApi.http()
                                                    .sendRequest(req);
                                            int status = (rr != null && rr.response() != null) ? rr.response().statusCode()
                                                    : -1;
                                            String respBody = (rr != null && rr.response() != null)
                                                    ? rr.response().bodyToString()
                                                    : "";

                                            if (status == 200 && respBody != null && !respBody.isBlank()) {
                                                java.util.regex.Matcher m = java.util.regex.Pattern
                                                        .compile("\"origin\"\\s*:\\s*\"([^\"]+)\"")
                                                        .matcher(respBody);
                                                if (m.find()) {
                                                    String origin = m.group(1);
                                                    String[] parts = origin.split(",");
                                                    String chosen = parts[parts.length - 1].trim();
                                                    ip = chosen;
                                                    parsed = true;
                                                } else {
                                                    java.util.regex.Matcher ipv4 = java.util.regex.Pattern
                                                            .compile("(?:\\b\\d{1,3}\\.){3}\\d{1,3}\\b")
                                                            .matcher(respBody);
                                                    if (ipv4.find()) {
                                                        ip = ipv4.group();
                                                        parsed = true;
                                                    } else if (attempt == maxAttempts) {
                                                        String preview = respBody.length() > 200
                                                                ? respBody.substring(0, 200) + "..."
                                                                : respBody;
                                                        montoyaApi.logging().logToOutput("[URLs] IP parse failed for "
                                                                + ep.name + " body preview: " + preview);
                                                    }
                                                }
                                            } else {
                                                montoyaApi.logging().logToOutput(
                                                        "[URLs] IP probe failed status=" + status + " for " + ep.name + " attempt " + attempt + "/" + maxAttempts);
                                                if (respBody != null && !respBody.isBlank() && attempt == maxAttempts) {
                                                    String preview = respBody.length() > 200
                                                            ? respBody.substring(0, 200) + "..."
                                                            : respBody;
                                                    montoyaApi.logging()
                                                            .logToOutput("[URLs] IP probe body preview: " + preview);
                                                }
                                            }

                                            if (parsed) {
                                                break;
                                            }

                                            if (attempt < maxAttempts && "n/a".equals(ip)) {
                                                try {
                                                    Thread.sleep(baseDelayMs * attempt);
                                                } catch (InterruptedException ie) {
                                                    Thread.currentThread().interrupt();
                                                    break;
                                                }
                                            }
                                        } catch (Exception exIp) {
                                            montoyaApi.logging().logToOutput(
                                                    "[URLs] IP probe error for " + ep.name + " attempt " + attempt + "/" + maxAttempts + ": " + exIp.getMessage());
                                        }
                                    }
                                    final String ipFinal = ip;
                                    javax.swing.SwingUtilities.invokeLater(() -> {
                                        endpointsTableModel.setValueAt(ipFinal, rowIndex, 3);
                                    });
                                });
                            }
                            exec.shutdown();

                            statusLabel.setText("Fetched " + eps.size() + " URLs");
                        }
                    });

                    montoyaApi.logging().logToOutput("[URLs] Listed " + eps.size() + " endpoints.");
                    montoyaApi.logging().raiseInfoEvent("Listed " + eps.size() + " endpoints");
                } catch (Exception ex1) {
                    montoyaApi.logging().logToError("List URLs failed: " + ex1.getMessage());
                    javax.swing.SwingUtilities.invokeLater(() -> statusLabel.setText("Fetch failed: " + ex1.getMessage()));
                } finally {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        listBtn.setEnabled(true);
                        deployBtn.setEnabled(true);
                        deleteBtn.setEnabled(true);
                    });
                }
            }).start();
        });

        deleteBtn.addActionListener(e -> {
            int size = endpointsTableModel.getRowCount();
            if (size == 0) {
                montoyaApi.logging().logToOutput("[Cleanup] No endpoints to clean.");
                montoyaApi.logging().raiseInfoEvent("[Cleanup] No endpoints to clean.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(root, "Clean up all deployed endpoints?", "Confirm Cleanup",
                    JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                montoyaApi.logging().logToOutput("[Cleanup] Cancelled.");
                return;
            }

            String token = new String(tokenField.getPassword()).trim();
            String accountId = new String(accountIdField.getPassword()).trim();
            final String tokenFinal = token.isBlank() ? getPreference(montoyaApi, PREF_API_TOKEN) : token;
            final String accountIdFinal = accountId.isBlank() ? getPreference(montoyaApi, PREF_ACCOUNT_ID) : accountId;

            montoyaApi.logging().logToOutput("[Cleanup] Requested cleanup. Account ID=" + accountIdFinal);
            montoyaApi.logging().raiseInfoEvent("[Cleanup] Requested cleanup. Account ID=" + accountIdFinal);

            statusLabel.setText("Cleaning up...");
            deleteBtn.setEnabled(false);
            deployBtn.setEnabled(false);
            listBtn.setEnabled(false);

            new Thread(() -> {
                CloudflareService cf = new SimpleCloudflareService(tokenFinal, accountIdFinal, montoyaApi);
                try {
                    cf.cleanupAll();
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        endpointsTableModel.setRowCount(0);
                        createdEndpointsCache.clear();
                        statusLabel.setText("Cleanup completed");
                    });
                    montoyaApi.logging().logToOutput("Cleanup completed.");
                    montoyaApi.logging().raiseInfoEvent("Cleanup completed.");
                } catch (Exception ex) {
                    montoyaApi.logging().logToError("Cleanup failed: " + ex.getMessage());
                    javax.swing.SwingUtilities.invokeLater(() -> statusLabel.setText("Cleanup failed: " + ex.getMessage()));
                } finally {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        deleteBtn.setEnabled(true);
                        deployBtn.setEnabled(true);
                        listBtn.setEnabled(true);
                    });
                }
            }).start();
        });

        montoyaApi.userInterface().registerSuiteTab("Flareprox Settings", root);
        javax.swing.SwingUtilities.invokeLater(listBtn::doClick);
    }

    private static long parseIPv4ToLong(String ip) {
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) return -1;
            long res = 0;
            for (String p : parts) {
                int v = Integer.parseInt(p);
                if (v < 0 || v > 255) return -1;
                res = (res << 8) | v;
            }
            return res;
        } catch (Exception e) {
            return -1;
        }
    }
}