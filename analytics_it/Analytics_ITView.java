/*
 * Analytics_ITView.java
 */

package analytics_it;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.sql.SQLException;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import javax.sound.sampled.SourceDataLine;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.table.TableColumn;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYBarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.data.time.Year;
import org.jfree.data.xy.TableXYDataset;

/**
 * The application's main frame.
 */
public class Analytics_ITView extends FrameView {


    public Analytics_ITView(SingleFrameApplication app) throws SQLException {
        
        super(app);
       try {
    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
      UIManager.setLookAndFeel("com.jtattoo.plaf.acryl.AcrylLookAndFeel");
       UIManager.put( "TabbedPane.background" ,new Color(200,255,255));
        //UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
        /*if ("Ocean".equals(info.getName())) {
            UIManager.setLookAndFeel(info.getClassName());
            break;
        }*/
    }
} catch (Exception e) {
    // If Nimbus is not available, fall back to cross-platform
    try {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
    } catch (Exception ex) {
        // not worth my time
    }
}
       // подключение к БД
     mdbc=new DBConnection();
     mdbc.init();
     Connection conn=mdbc.getMyConnection();

     stmt= conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                           ResultSet.CONCUR_READ_ONLY);
    getSourceData();
         //
        initComponents();
        // заполняем таблицу ранжирования рисков комбобоксами
        //  ---- Шкала оценки вероятности осуществления риска
        Object[] valueP =  { "Очень вероятно", "Возможно", "Маловероятно" };
        addComboboxToTable(rangTable.getColumnModel().getColumn(1),valueP );
        //  ---- Шкала оценки воздействия рисков
        Object[] valueVoz =  { "Умеренные", "Критичные", "Катастрофические" };
        addComboboxToTable(rangTable.getColumnModel().getColumn(2),valueVoz );
        

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        // устанавливаем иконку и наименование проекат

            ImageIcon icon = new ImageIcon(this.getClass().getResource("resources/analytics.png"));
        this.getFrame().setIconImage(icon.getImage());
 
      this.getFrame().setTitle("ITAnalytics");
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                } else if ("message".equals(propertyName)) {
                    String text = (String)(evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer)(evt.getNewValue());
                }
            }
        });

    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = Analytics_ITApp.getApplication().getMainFrame();
            aboutBox = new Analytics_ITAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        Analytics_ITApp.getApplication().show(aboutBox);
    }

    @Action
    public void showOpenProjJDialog() {
        if (newProjBox == null) {
            JFrame mainFrame = Analytics_ITApp.getApplication().getMainFrame();
            newProjBox = new newOrOpenProjJDialog(mainFrame,true,this,stmt,false);
            newProjBox.setLocationRelativeTo(mainFrame);
        }
        Analytics_ITApp.getApplication().show(newProjBox);
    }

    @Action
    public void showNewProjJDialog() {
        if (newProjBox == null) {
            JFrame mainFrame = Analytics_ITApp.getApplication().getMainFrame();
            newProjBox = new newOrOpenProjJDialog(mainFrame,true,this,stmt,true);
            newProjBox.setLocationRelativeTo(mainFrame);
        }
        Analytics_ITApp.getApplication().show(newProjBox);

    }

    @Action
    public void showPropJDialog() {
        if (propBox == null) {
            JFrame mainFrame = Analytics_ITApp.getApplication().getMainFrame();
            propBox = new propJDialog(mainFrame,true);
            propBox.setLocationRelativeTo(mainFrame);
        }
        Analytics_ITApp.getApplication().show(propBox);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        mainPanel = new javax.swing.JPanel();
        dataPanel = new javax.swing.JPanel();
        tabs = new javax.swing.JTabbedPane();
        inputPanel = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        periodFinancialLabelP = new javax.swing.JLabel();
        periodFinancialFormattedTextField = new javax.swing.JFormattedTextField();
        userContLabelP = new javax.swing.JLabel();
        userCountTextField = new javax.swing.JFormattedTextField();
        periodFinancialLabel = new javax.swing.JLabel();
        discountRateLabel = new javax.swing.JLabel();
        discountRateFormattedTextField = new javax.swing.JFormattedTextField();
        discountRateLabelP = new javax.swing.JLabel();
        userContLabel = new javax.swing.JLabel();
        aggCoastContractorLabel = new javax.swing.JLabel();
        aggCoastContractorFormattedTextField = new javax.swing.JFormattedTextField();
        coastWorkContractorLabelP = new javax.swing.JLabel();
        coastHarwareFormattedTextField = new javax.swing.JFormattedTextField();
        coastHarwareLabelP = new javax.swing.JLabel();
        aggCoastContractorLabelP = new javax.swing.JLabel();
        coastHarwareLabel = new javax.swing.JLabel();
        coastEducationLabelP = new javax.swing.JLabel();
        workDaysCountLabelP = new javax.swing.JLabel();
        workDaysCountButton = new javax.swing.JButton();
        coastEducationLabel = new javax.swing.JLabel();
        coastUserHourFormattedTextField = new javax.swing.JFormattedTextField();
        coastEducationFormattedTextField = new javax.swing.JFormattedTextField();
        coastUserHourLabel = new javax.swing.JLabel();
        workDaysCountFormattedTextField = new javax.swing.JFormattedTextField();
        workDaysCountLabel = new javax.swing.JLabel();
        coastUserHourLabe = new javax.swing.JLabel();
        annualСostLabel = new javax.swing.JLabel();
        coastSoftwareLabel = new javax.swing.JLabel();
        coastSoftwareFormattedTextField = new javax.swing.JFormattedTextField();
        coastSoftwareLabelP = new javax.swing.JLabel();
        aggCoastSoftwareLabel = new javax.swing.JLabel();
        annualСostFormattedTextField = new javax.swing.JFormattedTextField();
        annualСostLabelP = new javax.swing.JLabel();
        aggCoastSoftwareLabelFormattedTextField = new javax.swing.JFormattedTextField();
        aggCoastSoftwareLabelLabelP = new javax.swing.JLabel();
        coastWorkContractorLabel = new javax.swing.JLabel();
        coastWorkContractorFormattedTextField = new javax.swing.JFormattedTextField();
        jPanel5 = new javax.swing.JPanel();
        periodFinancialLabel6 = new javax.swing.JLabel();
        periodFinancialLabel1 = new javax.swing.JLabel();
        periodFinancialLabel12 = new javax.swing.JLabel();
        periodFinancialLabel13 = new javax.swing.JLabel();
        costsPanel = new javax.swing.JPanel();
        costsYearScrollPane = new javax.swing.JScrollPane();
        costsYearTable = new javax.swing.JTable();
        costsLabel = new javax.swing.JLabel();
        costsPane = new javax.swing.JScrollPane();
        costsTable = new javax.swing.JTable();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        valuePanel = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jToolBar3 = new javax.swing.JToolBar();
        jButton11 = new javax.swing.JButton();
        jButton12 = new javax.swing.JButton();
        jButton13 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        coastUserHourLabe1 = new javax.swing.JLabel();
        coastUserHourLabe2 = new javax.swing.JLabel();
        coastUserHourFormattedTextField1 = new javax.swing.JFormattedTextField();
        periodFinancialLabel5 = new javax.swing.JLabel();
        periodFinancialLabel2 = new javax.swing.JLabel();
        coastUserHourFormattedTextField2 = new javax.swing.JFormattedTextField();
        periodFinancialLabel3 = new javax.swing.JLabel();
        periodFinancialLabelP1 = new javax.swing.JLabel();
        jButton5 = new javax.swing.JButton();
        periodFinancialFormattedTextField1 = new javax.swing.JFormattedTextField();
        periodFinancialLabel4 = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        costsYearScrollPane1 = new javax.swing.JScrollPane();
        costsYearTable1 = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        costsYearScrollPane2 = new javax.swing.JScrollPane();
        costsYearTable2 = new javax.swing.JTable();
        jToolBar2 = new javax.swing.JToolBar();
        jButton6 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        coastUserHourLabe3 = new javax.swing.JLabel();
        coastUserHourFormattedTextField3 = new javax.swing.JFormattedTextField();
        periodFinancialLabel8 = new javax.swing.JLabel();
        periodFinancialLabel9 = new javax.swing.JLabel();
        periodFinancialLabelP2 = new javax.swing.JLabel();
        jButton9 = new javax.swing.JButton();
        periodFinancialFormattedTextField2 = new javax.swing.JFormattedTextField();
        periodFinancialLabel10 = new javax.swing.JLabel();
        jButton10 = new javax.swing.JButton();
        jTextField2 = new javax.swing.JTextField();
        periodFinancialLabel11 = new javax.swing.JLabel();
        coastUserHourFormattedTextField4 = new javax.swing.JFormattedTextField();
        coastUserHourLabe4 = new javax.swing.JLabel();
        costsYearScrollPane3 = new javax.swing.JScrollPane();
        costsYearTable3 = new javax.swing.JTable();
        costsLabel1 = new javax.swing.JLabel();
        risksPanel = new javax.swing.JPanel();
        costsLabel2 = new javax.swing.JLabel();
        costsYearScrollPane4 = new javax.swing.JScrollPane();
        costsYearTable4 = new javax.swing.JTable();
        costsLabel3 = new javax.swing.JLabel();
        costsYearScrollPane5 = new javax.swing.JScrollPane();
        costsYearTable5 = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextPane2 = new javax.swing.JTextPane();
        msfRisksPanel = new javax.swing.JPanel();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        identifPanel = new javax.swing.JPanel();
        costsYearScrollPane6 = new javax.swing.JScrollPane();
        costsYearTable6 = new javax.swing.JTable();
        jToolBar4 = new javax.swing.JToolBar();
        addRiskButton = new javax.swing.JButton();
        delRiskButton = new javax.swing.JButton();
        analitPanel = new javax.swing.JPanel();
        tblRangePanel = new javax.swing.JPanel();
        rangScrollPane = new javax.swing.JScrollPane();
        rangTable = new javax.swing.JTable();
        matixPanel = new javax.swing.JPanel();
        nameFormLabel = new javax.swing.JLabel();
        statusRiskLabel = new javax.swing.JLabel();
        statusRiskTextField = new javax.swing.JTextField();
        metodRiskLabel = new javax.swing.JLabel();
        metodRiskComboBox = new javax.swing.JComboBox();
        catRiskLabel = new javax.swing.JLabel();
        catRiskTextField = new javax.swing.JTextField();
        PLabel = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        PTextPane = new javax.swing.JTextPane();
        SimLabel = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        simTextPane = new javax.swing.JTextPane();
        vozLabel = new javax.swing.JLabel();
        jScrollPane6 = new javax.swing.JScrollPane();
        vozTextPane = new javax.swing.JTextPane();
        triggerRiskLabel = new javax.swing.JLabel();
        triggerRiskTextField = new javax.swing.JTextField();
        printReportRisksButton = new javax.swing.JButton();
        clearCartButton = new javax.swing.JButton();
        saveCartButton = new javax.swing.JButton();
        conclPanel = new javax.swing.JPanel();
        chartPanel = new javax.swing.JPanel();
        calcResultDiagramButton = new javax.swing.JButton();
        chartScrollPane = new javax.swing.JScrollPane();
        jPanel8 = new javax.swing.JPanel();
        costsYearScrollPane7 = new javax.swing.JScrollPane();
        costsYearTable7 = new javax.swing.JTable();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem newMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem openMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem saveMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem optionMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem printMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem aboutMenuItem1 = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();

        mainPanel.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                mainPanelFocusGained(evt);
            }
        });

        tabs.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setTabPlacement(javax.swing.JTabbedPane.LEFT);

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(analytics_it.Analytics_ITApp.class).getContext().getResourceMap(Analytics_ITView.class);
        periodFinancialLabelP.setText(resourceMap.getString("periodFinancialLabelP.text")); // NOI18N
        periodFinancialLabelP.setToolTipText(resourceMap.getString("periodFinancialLabelP.toolTipText")); // NOI18N

        periodFinancialFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(java.text.NumberFormat.getIntegerInstance())));
        periodFinancialFormattedTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        periodFinancialFormattedTextField.setText(resourceMap.getString("periodFinancialFormattedTextField.text")); // NOI18N
        periodFinancialFormattedTextField.setToolTipText(resourceMap.getString("periodFinancialFormattedTextField.toolTipText")); // NOI18N

        userContLabelP.setText(resourceMap.getString("userContLabelP.text")); // NOI18N
        userContLabelP.setToolTipText(resourceMap.getString("userContLabelP.toolTipText")); // NOI18N

        userCountTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(java.text.NumberFormat.getIntegerInstance())));
        userCountTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        userCountTextField.setText(resourceMap.getString("userCountTextField.text")); // NOI18N
        userCountTextField.setToolTipText(resourceMap.getString("userCountTextField.toolTipText")); // NOI18N

        periodFinancialLabel.setText(resourceMap.getString("periodFinancialLabel.text")); // NOI18N
        periodFinancialLabel.setToolTipText(resourceMap.getString("periodFinancialLabel.toolTipText")); // NOI18N

        discountRateLabel.setText(resourceMap.getString("discountRateLabel.text")); // NOI18N
        discountRateLabel.setToolTipText(resourceMap.getString("discountRateLabel.toolTipText")); // NOI18N

        discountRateFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(java.text.NumberFormat.getIntegerInstance())));
        discountRateFormattedTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        discountRateFormattedTextField.setText(resourceMap.getString("discountRateFormattedTextField.text")); // NOI18N
        discountRateFormattedTextField.setToolTipText(resourceMap.getString("discountRateFormattedTextField.toolTipText")); // NOI18N

        discountRateLabelP.setText(resourceMap.getString("discountRateLabelP.text")); // NOI18N
        discountRateLabelP.setToolTipText(resourceMap.getString("discountRateLabelP.toolTipText")); // NOI18N

        userContLabel.setText(resourceMap.getString("userContLabel.text")); // NOI18N
        userContLabel.setToolTipText(resourceMap.getString("userContLabel.toolTipText")); // NOI18N

        aggCoastContractorLabel.setFont(resourceMap.getFont("aggCoastContractorLabel.font")); // NOI18N
        aggCoastContractorLabel.setText(resourceMap.getString("aggCoastContractorLabel.text")); // NOI18N
        aggCoastContractorLabel.setToolTipText(resourceMap.getString("aggCoastContractorLabel.toolTipText")); // NOI18N

        aggCoastContractorFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.00"))));
        aggCoastContractorFormattedTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        aggCoastContractorFormattedTextField.setText(resourceMap.getString("aggCoastContractorFormattedTextField.text")); // NOI18N
        aggCoastContractorFormattedTextField.setToolTipText(resourceMap.getString("aggCoastContractorFormattedTextField.toolTipText")); // NOI18N
        aggCoastContractorFormattedTextField.setEnabled(false);

        coastWorkContractorLabelP.setText(resourceMap.getString("coastWorkContractorLabelP.text")); // NOI18N
        coastWorkContractorLabelP.setToolTipText(resourceMap.getString("coastWorkContractorLabelP.toolTipText")); // NOI18N

        coastHarwareFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.00"))));
        coastHarwareFormattedTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        coastHarwareFormattedTextField.setText(resourceMap.getString("coastHarwareFormattedTextField.text")); // NOI18N
        coastHarwareFormattedTextField.setToolTipText(resourceMap.getString("coastHarwareFormattedTextField.toolTipText")); // NOI18N

        coastHarwareLabelP.setText(resourceMap.getString("coastHarwareLabelP.text")); // NOI18N
        coastHarwareLabelP.setToolTipText(resourceMap.getString("coastHarwareLabelP.toolTipText")); // NOI18N

        aggCoastContractorLabelP.setFont(resourceMap.getFont("aggCoastContractorLabelP.font")); // NOI18N
        aggCoastContractorLabelP.setText(resourceMap.getString("aggCoastContractorLabelP.text")); // NOI18N
        aggCoastContractorLabelP.setToolTipText(resourceMap.getString("aggCoastContractorLabelP.toolTipText")); // NOI18N

        coastHarwareLabel.setText(resourceMap.getString("coastHarwareLabel.text")); // NOI18N
        coastHarwareLabel.setToolTipText(resourceMap.getString("coastHarwareLabel.toolTipText")); // NOI18N

        coastEducationLabelP.setText(resourceMap.getString("coastEducationLabelP.text")); // NOI18N
        coastEducationLabelP.setToolTipText(resourceMap.getString("coastEducationLabelP.toolTipText")); // NOI18N

        workDaysCountLabelP.setText(resourceMap.getString("workDaysCountLabelP.text")); // NOI18N
        workDaysCountLabelP.setToolTipText(resourceMap.getString("workDaysCountLabelP.toolTipText")); // NOI18N

        workDaysCountButton.setText(resourceMap.getString("workDaysCountButton.text")); // NOI18N
        workDaysCountButton.setToolTipText(resourceMap.getString("workDaysCountButton.toolTipText")); // NOI18N

        coastEducationLabel.setText(resourceMap.getString("coastEducationLabel.text")); // NOI18N
        coastEducationLabel.setToolTipText(resourceMap.getString("coastEducationLabel.toolTipText")); // NOI18N

        coastUserHourFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.00"))));
        coastUserHourFormattedTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        coastUserHourFormattedTextField.setText(resourceMap.getString("coastUserHourFormattedTextField.text")); // NOI18N
        coastUserHourFormattedTextField.setToolTipText(resourceMap.getString("coastUserHourFormattedTextField.toolTipText")); // NOI18N

        coastEducationFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.00"))));
        coastEducationFormattedTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        coastEducationFormattedTextField.setText(resourceMap.getString("coastEducationFormattedTextField.text")); // NOI18N
        coastEducationFormattedTextField.setToolTipText(resourceMap.getString("coastEducationFormattedTextField.toolTipText")); // NOI18N

        coastUserHourLabel.setText(resourceMap.getString("coastUserHourLabel.text")); // NOI18N
        coastUserHourLabel.setToolTipText(resourceMap.getString("coastUserHourLabel.toolTipText")); // NOI18N

        workDaysCountFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(java.text.NumberFormat.getIntegerInstance())));
        workDaysCountFormattedTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        workDaysCountFormattedTextField.setText(resourceMap.getString("workDaysCountFormattedTextField.text")); // NOI18N
        workDaysCountFormattedTextField.setToolTipText(resourceMap.getString("workDaysCountFormattedTextField.toolTipText")); // NOI18N

        workDaysCountLabel.setText(resourceMap.getString("workDaysCountLabel.text")); // NOI18N
        workDaysCountLabel.setToolTipText(resourceMap.getString("workDaysCountLabel.toolTipText")); // NOI18N

        coastUserHourLabe.setText(resourceMap.getString("coastUserHourLabe.text")); // NOI18N
        coastUserHourLabe.setToolTipText(resourceMap.getString("coastUserHourLabe.toolTipText")); // NOI18N

        annualСostLabel.setFont(resourceMap.getFont("annualСostLabel.font")); // NOI18N
        annualСostLabel.setText(resourceMap.getString("annualСostLabel.text")); // NOI18N
        annualСostLabel.setToolTipText(resourceMap.getString("annualСostLabel.toolTipText")); // NOI18N

        coastSoftwareLabel.setText(resourceMap.getString("coastSoftwareLabel.text")); // NOI18N
        coastSoftwareLabel.setToolTipText(resourceMap.getString("coastSoftwareLabel.toolTipText")); // NOI18N

        coastSoftwareFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.00"))));
        coastSoftwareFormattedTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        coastSoftwareFormattedTextField.setText(resourceMap.getString("coastSoftwareFormattedTextField.text")); // NOI18N
        coastSoftwareFormattedTextField.setToolTipText(resourceMap.getString("coastSoftwareFormattedTextField.toolTipText")); // NOI18N

        coastSoftwareLabelP.setText(resourceMap.getString("coastSoftwareLabelP.text")); // NOI18N
        coastSoftwareLabelP.setToolTipText(resourceMap.getString("coastSoftwareLabelP.toolTipText")); // NOI18N

        aggCoastSoftwareLabel.setFont(resourceMap.getFont("aggCoastSoftwareLabel.font")); // NOI18N
        aggCoastSoftwareLabel.setText(resourceMap.getString("aggCoastSoftwareLabel.text")); // NOI18N
        aggCoastSoftwareLabel.setToolTipText(resourceMap.getString("aggCoastSoftwareLabel.toolTipText")); // NOI18N

        annualСostFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.00"))));
        annualСostFormattedTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        annualСostFormattedTextField.setText(resourceMap.getString("annualСostFormattedTextField.text")); // NOI18N
        annualСostFormattedTextField.setToolTipText(resourceMap.getString("annualСostFormattedTextField.toolTipText")); // NOI18N
        annualСostFormattedTextField.setEnabled(false);

        annualСostLabelP.setFont(resourceMap.getFont("annualСostLabelP.font")); // NOI18N
        annualСostLabelP.setText(resourceMap.getString("annualСostLabelP.text")); // NOI18N
        annualСostLabelP.setToolTipText(resourceMap.getString("annualСostLabelP.toolTipText")); // NOI18N

        aggCoastSoftwareLabelFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.00"))));
        aggCoastSoftwareLabelFormattedTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        aggCoastSoftwareLabelFormattedTextField.setText(resourceMap.getString("aggCoastSoftwareLabelFormattedTextField.text")); // NOI18N
        aggCoastSoftwareLabelFormattedTextField.setToolTipText(resourceMap.getString("aggCoastSoftwareLabelFormattedTextField.toolTipText")); // NOI18N
        aggCoastSoftwareLabelFormattedTextField.setEnabled(false);

        aggCoastSoftwareLabelLabelP.setFont(resourceMap.getFont("aggCoastSoftwareLabelLabelP.font")); // NOI18N
        aggCoastSoftwareLabelLabelP.setText(resourceMap.getString("aggCoastSoftwareLabelLabelP.text")); // NOI18N
        aggCoastSoftwareLabelLabelP.setToolTipText(resourceMap.getString("aggCoastSoftwareLabelLabelP.toolTipText")); // NOI18N

        coastWorkContractorLabel.setText(resourceMap.getString("coastWorkContractorLabel.text")); // NOI18N
        coastWorkContractorLabel.setToolTipText(resourceMap.getString("coastWorkContractorLabel.toolTipText")); // NOI18N

        coastWorkContractorFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(java.text.NumberFormat.getIntegerInstance())));
        coastWorkContractorFormattedTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        coastWorkContractorFormattedTextField.setText(resourceMap.getString("coastWorkContractorFormattedTextField.text")); // NOI18N
        coastWorkContractorFormattedTextField.setToolTipText(resourceMap.getString("coastWorkContractorFormattedTextField.toolTipText")); // NOI18N

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(userContLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(userCountTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(userContLabelP))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(periodFinancialLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(periodFinancialFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(periodFinancialLabelP))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(discountRateLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(discountRateFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(discountRateLabelP))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(aggCoastContractorLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(aggCoastContractorFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(aggCoastContractorLabelP))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(workDaysCountLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(workDaysCountFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(workDaysCountButton, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(workDaysCountLabelP))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(annualСostLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(annualСostFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(annualСostLabelP))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(aggCoastSoftwareLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(aggCoastSoftwareLabelFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(aggCoastSoftwareLabelLabelP))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(coastWorkContractorLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(coastWorkContractorFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(coastWorkContractorLabelP))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(coastHarwareLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(coastHarwareFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(coastHarwareLabelP))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(coastEducationLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(coastEducationFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(coastEducationLabelP))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(coastUserHourLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(coastUserHourFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(coastSoftwareLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(coastSoftwareFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(coastSoftwareLabelP)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(coastUserHourLabe)))
                .addContainerGap(170, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(periodFinancialLabel)
                        .addComponent(periodFinancialFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(periodFinancialLabelP))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(26, 26, 26)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(discountRateLabel)
                            .addComponent(discountRateFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(discountRateLabelP))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(userContLabel)
                    .addComponent(userCountTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(userContLabelP))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(workDaysCountFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(workDaysCountButton, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(workDaysCountLabelP)
                    .addComponent(workDaysCountLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(coastUserHourLabel)
                    .addComponent(coastUserHourFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(coastUserHourLabe))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(annualСostLabel)
                    .addComponent(annualСostFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(annualСostLabelP))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(coastSoftwareLabel)
                    .addComponent(coastSoftwareFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(coastSoftwareLabelP))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(aggCoastSoftwareLabel)
                    .addComponent(aggCoastSoftwareLabelLabelP)
                    .addComponent(aggCoastSoftwareLabelFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(coastWorkContractorLabel)
                    .addComponent(coastWorkContractorFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(coastWorkContractorLabelP))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(aggCoastContractorLabel)
                    .addComponent(aggCoastContractorFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(aggCoastContractorLabelP))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(coastHarwareLabel)
                    .addComponent(coastHarwareLabelP)
                    .addComponent(coastHarwareFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(coastEducationLabel)
                    .addComponent(coastEducationLabelP)
                    .addComponent(coastEducationFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        periodFinancialLabel6.setFont(resourceMap.getFont("periodFinancialLabel6.font")); // NOI18N
        periodFinancialLabel6.setText(resourceMap.getString("periodFinancialLabel6.text")); // NOI18N
        periodFinancialLabel6.setToolTipText(resourceMap.getString("periodFinancialLabel6.toolTipText")); // NOI18N

        periodFinancialLabel1.setFont(resourceMap.getFont("periodFinancialLabel6.font")); // NOI18N
        periodFinancialLabel1.setText(resourceMap.getString("periodFinancialLabel1.text")); // NOI18N
        periodFinancialLabel1.setToolTipText(resourceMap.getString("periodFinancialLabel1.toolTipText")); // NOI18N

        periodFinancialLabel12.setFont(resourceMap.getFont("periodFinancialLabel12.font")); // NOI18N
        periodFinancialLabel12.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        periodFinancialLabel12.setText(resourceMap.getString("periodFinancialLabel12.text")); // NOI18N
        periodFinancialLabel12.setToolTipText(resourceMap.getString("periodFinancialLabel12.toolTipText")); // NOI18N

        periodFinancialLabel13.setFont(resourceMap.getFont("periodFinancialLabel13.font")); // NOI18N
        periodFinancialLabel13.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        periodFinancialLabel13.setText(resourceMap.getString("periodFinancialLabel13.text")); // NOI18N
        periodFinancialLabel13.setToolTipText(resourceMap.getString("periodFinancialLabel13.toolTipText")); // NOI18N

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(periodFinancialLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(periodFinancialLabel6))
                    .addComponent(periodFinancialLabel12, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 524, Short.MAX_VALUE)
                    .addComponent(periodFinancialLabel13, javax.swing.GroupLayout.DEFAULT_SIZE, 524, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(periodFinancialLabel6)
                    .addComponent(periodFinancialLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 24, Short.MAX_VALUE)
                .addComponent(periodFinancialLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(periodFinancialLabel12)
                .addContainerGap())
        );

        javax.swing.GroupLayout inputPanelLayout = new javax.swing.GroupLayout(inputPanel);
        inputPanel.setLayout(inputPanelLayout);
        inputPanelLayout.setHorizontalGroup(
            inputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, inputPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(inputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        inputPanelLayout.setVerticalGroup(
            inputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(inputPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(90, Short.MAX_VALUE))
        );

        tabs.addTab(resourceMap.getString("inputPanel.TabConstraints.tabTitle"), inputPanel); // NOI18N

        costsYearTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Стоимость лицензии, поддержки", new Float(0.0), new Float(0.0), new Float(0.0), null},
                {"Внедрение силами ИТ", new Float(28000.0), new Float(0.0), new Float(0.0), new Float(0.0)},
                {"Внешние подрядчики", new Float(0.0), new Float(0.0), new Float(0.0), new Float(0.0)},
                {"Оборудование", new Float(0.0), new Float(0.0), new Float(0.0), new Float(0.0)},
                {"Обучение", new Float(0.0), new Float(0.0), new Float(0.0), new Float(0.0)}
            },
            new String [] {
                "Показатель затрат", "Год 0", "Год 1", "Год 2", "Год 3"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        costsYearScrollPane.setViewportView(costsYearTable);

        costsLabel.setFont(resourceMap.getFont("costsLabel.font")); // NOI18N
        costsLabel.setText(resourceMap.getString("costsLabel.text")); // NOI18N

        costsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Стоимость лицензии, поддержки", new Float(0.0), new Float(0.0)},
                {"Внедрение силами ИТ", new Float(28000.0), new Float(28000.0)},
                {"Внешние подрядчики", new Float(0.0), new Float(0.0)},
                {"Оборудование", new Float(0.0), new Float(0.0)},
                {"Обучение", null, new Float(0.0)}
            },
            new String [] {
                "Показатель затрат", "Итого", "Present value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Float.class, java.lang.Float.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        costsTable.setEnabled(false);
        costsPane.setViewportView(costsTable);

        jTextPane1.setBorder(null);
        jTextPane1.setText(resourceMap.getString("jTextPane1.text")); // NOI18N
        jTextPane1.setOpaque(false);
        jTextPane1.setPreferredSize(new java.awt.Dimension(959, 28));
        jScrollPane1.setViewportView(jTextPane1);

        javax.swing.GroupLayout costsPanelLayout = new javax.swing.GroupLayout(costsPanel);
        costsPanel.setLayout(costsPanelLayout);
        costsPanelLayout.setHorizontalGroup(
            costsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(costsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(costsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(costsYearScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 544, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 544, Short.MAX_VALUE)
                    .addComponent(costsLabel)
                    .addComponent(costsPane, javax.swing.GroupLayout.DEFAULT_SIZE, 544, Short.MAX_VALUE))
                .addContainerGap())
        );
        costsPanelLayout.setVerticalGroup(
            costsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(costsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(costsYearScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(costsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(costsPane, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(113, Short.MAX_VALUE))
        );

        tabs.addTab(resourceMap.getString("costsPanel.TabConstraints.tabTitle"), costsPanel); // NOI18N

        valuePanel.setLayout(new java.awt.GridBagLayout());

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jToolBar3.setRollover(true);

        jButton11.setIcon(resourceMap.getIcon("jButton11.icon")); // NOI18N
        jButton11.setFocusable(false);
        jButton11.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton11.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar3.add(jButton11);

        jButton12.setIcon(resourceMap.getIcon("jButton12.icon")); // NOI18N
        jButton12.setFocusable(false);
        jButton12.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton12.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar3.add(jButton12);

        jButton13.setIcon(resourceMap.getIcon("jButton13.icon")); // NOI18N
        jButton13.setFocusable(false);
        jButton13.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton13.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar3.add(jButton13);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        jPanel1.add(jToolBar3, gridBagConstraints);

        jPanel2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        coastUserHourLabe1.setText(resourceMap.getString("coastUserHourLabe1.text")); // NOI18N
        coastUserHourLabe1.setToolTipText(resourceMap.getString("coastUserHourLabe1.toolTipText")); // NOI18N

        coastUserHourLabe2.setText(resourceMap.getString("coastUserHourLabe2.text")); // NOI18N
        coastUserHourLabe2.setToolTipText(resourceMap.getString("coastUserHourLabe2.toolTipText")); // NOI18N

        coastUserHourFormattedTextField1.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.00"))));
        coastUserHourFormattedTextField1.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        coastUserHourFormattedTextField1.setText(resourceMap.getString("coastUserHourFormattedTextField1.text")); // NOI18N
        coastUserHourFormattedTextField1.setToolTipText(resourceMap.getString("coastUserHourFormattedTextField1.toolTipText")); // NOI18N

        periodFinancialLabel5.setText(resourceMap.getString("periodFinancialLabel5.text")); // NOI18N
        periodFinancialLabel5.setToolTipText(resourceMap.getString("periodFinancialLabel5.toolTipText")); // NOI18N

        periodFinancialLabel2.setText(resourceMap.getString("periodFinancialLabel2.text")); // NOI18N
        periodFinancialLabel2.setToolTipText(resourceMap.getString("periodFinancialLabel2.toolTipText")); // NOI18N

        coastUserHourFormattedTextField2.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.00"))));
        coastUserHourFormattedTextField2.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        coastUserHourFormattedTextField2.setText(resourceMap.getString("coastUserHourFormattedTextField2.text")); // NOI18N
        coastUserHourFormattedTextField2.setToolTipText(resourceMap.getString("coastUserHourFormattedTextField2.toolTipText")); // NOI18N

        periodFinancialLabel3.setText(resourceMap.getString("periodFinancialLabel3.text")); // NOI18N
        periodFinancialLabel3.setToolTipText(resourceMap.getString("periodFinancialLabel3.toolTipText")); // NOI18N

        periodFinancialLabelP1.setText(resourceMap.getString("periodFinancialLabelP1.text")); // NOI18N
        periodFinancialLabelP1.setToolTipText(resourceMap.getString("periodFinancialLabelP1.toolTipText")); // NOI18N

        jButton5.setText(resourceMap.getString("jButton5.text")); // NOI18N

        periodFinancialFormattedTextField1.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(java.text.NumberFormat.getIntegerInstance())));
        periodFinancialFormattedTextField1.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        periodFinancialFormattedTextField1.setText(resourceMap.getString("periodFinancialFormattedTextField1.text")); // NOI18N
        periodFinancialFormattedTextField1.setToolTipText(resourceMap.getString("periodFinancialFormattedTextField1.toolTipText")); // NOI18N

        periodFinancialLabel4.setText(resourceMap.getString("periodFinancialLabel4.text")); // NOI18N
        periodFinancialLabel4.setToolTipText(resourceMap.getString("periodFinancialLabel4.toolTipText")); // NOI18N

        jButton4.setText(resourceMap.getString("jButton4.text")); // NOI18N

        jTextField1.setText(resourceMap.getString("jTextField1.text")); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jButton5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton4))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(periodFinancialLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField1, javax.swing.GroupLayout.DEFAULT_SIZE, 371, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(periodFinancialLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(periodFinancialFormattedTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(periodFinancialLabelP1))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addComponent(periodFinancialLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(coastUserHourFormattedTextField2))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addComponent(periodFinancialLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(coastUserHourFormattedTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(coastUserHourLabe2)
                            .addComponent(coastUserHourLabe1))))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(periodFinancialLabel3)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(periodFinancialLabel2)
                    .addComponent(periodFinancialFormattedTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(periodFinancialLabelP1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(periodFinancialLabel4)
                    .addComponent(coastUserHourFormattedTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(coastUserHourLabe1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(periodFinancialLabel5)
                    .addComponent(coastUserHourFormattedTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(coastUserHourLabe2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton5)
                    .addComponent(jButton4))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        jPanel1.add(jPanel2, gridBagConstraints);

        costsYearTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Выгода от отказа приобретения библиотеки компонентов  FIBPlus для работы с БД FireBird", new Float(74025.0)},
                {"Выгода от отказа приобретения дополнительного ПО для работы с редактором печатных форм (в Delphi - FastReport) ", new Float(95760.0)}
            },
            new String [] {
                "Наименование", "Полная выгода"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Float.class
            };
            boolean[] canEdit = new boolean [] {
                true, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        costsYearTable1.setMinimumSize(new java.awt.Dimension(30, 132));
        costsYearScrollPane1.setViewportView(costsYearTable1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 100.0;
        gridBagConstraints.weighty = 100.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        jPanel1.add(costsYearScrollPane1, gridBagConstraints);

        jTabbedPane1.addTab(resourceMap.getString("jPanel1.TabConstraints.tabTitle"), jPanel1); // NOI18N

        jPanel3.setLayout(new java.awt.GridBagLayout());

        costsYearTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Выгода от использования свойства редактора \"подсветка слова под курсором\"", new Float(9855.0)}
            },
            new String [] {
                "Наименование", "Полная выгода"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Float.class
            };
            boolean[] canEdit = new boolean [] {
                true, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        costsYearScrollPane2.setViewportView(costsYearTable2);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 100.0;
        gridBagConstraints.weighty = 100.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        jPanel3.add(costsYearScrollPane2, gridBagConstraints);

        jToolBar2.setRollover(true);

        jButton6.setIcon(resourceMap.getIcon("jButton6.icon")); // NOI18N
        jButton6.setText(resourceMap.getString("jButton6.text")); // NOI18N
        jButton6.setFocusable(false);
        jButton6.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton6.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar2.add(jButton6);

        jButton7.setIcon(resourceMap.getIcon("jButton7.icon")); // NOI18N
        jButton7.setText(resourceMap.getString("jButton7.text")); // NOI18N
        jButton7.setFocusable(false);
        jButton7.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton7.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar2.add(jButton7);

        jButton8.setIcon(resourceMap.getIcon("jButton8.icon")); // NOI18N
        jButton8.setText(resourceMap.getString("jButton8.text")); // NOI18N
        jButton8.setFocusable(false);
        jButton8.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton8.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar2.add(jButton8);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        jPanel3.add(jToolBar2, gridBagConstraints);

        jPanel4.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        coastUserHourLabe3.setText(resourceMap.getString("coastUserHourLabe3.text")); // NOI18N
        coastUserHourLabe3.setToolTipText(resourceMap.getString("coastUserHourLabe3.toolTipText")); // NOI18N

        coastUserHourFormattedTextField3.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.00"))));
        coastUserHourFormattedTextField3.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        coastUserHourFormattedTextField3.setText(resourceMap.getString("coastUserHourFormattedTextField3.text")); // NOI18N
        coastUserHourFormattedTextField3.setToolTipText(resourceMap.getString("coastUserHourFormattedTextField3.toolTipText")); // NOI18N

        periodFinancialLabel8.setText(resourceMap.getString("periodFinancialLabel8.text")); // NOI18N
        periodFinancialLabel8.setToolTipText(resourceMap.getString("periodFinancialLabel8.toolTipText")); // NOI18N

        periodFinancialLabel9.setText(resourceMap.getString("periodFinancialLabel9.text")); // NOI18N
        periodFinancialLabel9.setToolTipText(resourceMap.getString("periodFinancialLabel9.toolTipText")); // NOI18N

        periodFinancialLabelP2.setText(resourceMap.getString("periodFinancialLabelP2.text")); // NOI18N
        periodFinancialLabelP2.setToolTipText(resourceMap.getString("periodFinancialLabelP2.toolTipText")); // NOI18N

        jButton9.setText(resourceMap.getString("jButton9.text")); // NOI18N

        periodFinancialFormattedTextField2.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(java.text.NumberFormat.getIntegerInstance())));
        periodFinancialFormattedTextField2.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        periodFinancialFormattedTextField2.setText(resourceMap.getString("periodFinancialFormattedTextField2.text")); // NOI18N
        periodFinancialFormattedTextField2.setToolTipText(resourceMap.getString("periodFinancialFormattedTextField2.toolTipText")); // NOI18N

        periodFinancialLabel10.setText(resourceMap.getString("periodFinancialLabel10.text")); // NOI18N
        periodFinancialLabel10.setToolTipText(resourceMap.getString("periodFinancialLabel10.toolTipText")); // NOI18N

        jButton10.setText(resourceMap.getString("jButton10.text")); // NOI18N

        jTextField2.setText(resourceMap.getString("jTextField2.text")); // NOI18N

        periodFinancialLabel11.setText(resourceMap.getString("periodFinancialLabel11.text")); // NOI18N
        periodFinancialLabel11.setToolTipText(resourceMap.getString("periodFinancialLabel11.toolTipText")); // NOI18N

        coastUserHourFormattedTextField4.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.00"))));
        coastUserHourFormattedTextField4.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        coastUserHourFormattedTextField4.setText(resourceMap.getString("coastUserHourFormattedTextField4.text")); // NOI18N
        coastUserHourFormattedTextField4.setToolTipText(resourceMap.getString("coastUserHourFormattedTextField4.toolTipText")); // NOI18N

        coastUserHourLabe4.setText(resourceMap.getString("coastUserHourLabe4.text")); // NOI18N
        coastUserHourLabe4.setToolTipText(resourceMap.getString("coastUserHourLabe4.toolTipText")); // NOI18N

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(periodFinancialLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(coastUserHourFormattedTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(coastUserHourLabe4))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(periodFinancialLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField2, javax.swing.GroupLayout.DEFAULT_SIZE, 371, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(periodFinancialLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(periodFinancialFormattedTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(periodFinancialLabelP2))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(periodFinancialLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(coastUserHourFormattedTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(coastUserHourLabe3))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jButton9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton10)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(periodFinancialLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(periodFinancialLabel8)
                    .addComponent(periodFinancialFormattedTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(periodFinancialLabelP2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(periodFinancialLabel10)
                    .addComponent(coastUserHourFormattedTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(coastUserHourLabe3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(periodFinancialLabel11)
                    .addComponent(coastUserHourFormattedTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(coastUserHourLabe4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton9)
                    .addComponent(jButton10))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        jPanel3.add(jPanel4, gridBagConstraints);

        jTabbedPane1.addTab(resourceMap.getString("jPanel3.TabConstraints.tabTitle"), jPanel3); // NOI18N

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.weighty = 60.0;
        gridBagConstraints.insets = new java.awt.Insets(11, 10, 10, 10);
        valuePanel.add(jTabbedPane1, gridBagConstraints);

        costsYearScrollPane3.setMinimumSize(new java.awt.Dimension(23, 123));

        costsYearTable3.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Выгода от отказа приобретения библиотеки компонентов  FIBPlus для работы с БД FireBird", new Float(74025.0), new Float(0.0), new Float(0.0), new Float(74025.0), new Float(64369.57)},
                {"Выгода от отказа приобретения дополнительного ПО для работы с редактором печатных форм (в Delphi - FastReport) ", new Float(95760.0), new Float(0.0), new Float(0.0), new Float(95760.0), new Float(83269.57)},
                {"Выгода от использования свойства редактора \"подсветка слова под курсором\"", new Float(4927.5), new Float(9855.0), new Float(9855.0), new Float(24637.5), new Float(18216.4)}
            },
            new String [] {
                "Выгода", "Год 1", "Год 2", "Год 3", "Итого", "Present Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        costsYearScrollPane3.setViewportView(costsYearTable3);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weighty = 90.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        valuePanel.add(costsYearScrollPane3, gridBagConstraints);

        costsLabel1.setFont(resourceMap.getFont("costsLabel1.font")); // NOI18N
        costsLabel1.setText(resourceMap.getString("costsLabel1.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 10.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 10, 0, 0);
        valuePanel.add(costsLabel1, gridBagConstraints);

        tabs.addTab(resourceMap.getString("valuePanel.TabConstraints.tabTitle"), valuePanel); // NOI18N

        costsLabel2.setFont(resourceMap.getFont("costsLabel2.font")); // NOI18N
        costsLabel2.setText(resourceMap.getString("costsLabel2.text")); // NOI18N

        costsYearTable4.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Выгода от отказа приобретения библиотеки компонентов  FIBPlus для работы с БД FireBird", new Float(80.0), new Float(100.0), new Float(100.0), new Float(93.0)},
                {"Выгода от отказа приобретения дополнительного ПО для работы с редактором печатных форм (в Delphi - FastReport) ", new Float(80.0), new Float(100.0), new Float(100.0), new Float(93.0)},
                {"Выгода от использования свойства редактора \"подсветка слова под курсором\"", new Float(70.0), new Float(100.0), new Float(100.0), new Float(90.0)}
            },
            new String [] {
                "Наименование", "Минимальный уровень", "Наиболее вероятный уровень", "Максимальный уровень", "Размер поправки"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        costsYearScrollPane4.setViewportView(costsYearTable4);

        costsLabel3.setFont(resourceMap.getFont("costsLabel3.font")); // NOI18N
        costsLabel3.setText(resourceMap.getString("costsLabel3.text")); // NOI18N

        costsYearTable5.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Выгода от отказа приобретения библиотеки компонентов  FIBPlus для работы с БД FireBird", new Float(69090.0), new Float(0.0), new Float(0.0), new Float(69090.0), new Float(60078.26)},
                {"Выгода от отказа приобретения дополнительного ПО для работы с редактором печатных форм (в Delphi - FastReport) ", new Float(89376.0), new Float(0.0), new Float(0.0), new Float(89376.0), new Float(77718.26)},
                {"Выгода от использования свойства редактора \"подсветка слова под курсором\"", new Float(4434.75), new Float(8869.5), new Float(8869.5), new Float(22173.75), new Float(16394.76)}
            },
            new String [] {
                "Выгода", "Год 1", "Год 2", "Год 3", "Итого", "Present Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        costsYearScrollPane5.setViewportView(costsYearTable5);

        jTextPane2.setBorder(null);
        jTextPane2.setText(resourceMap.getString("jTextPane2.text")); // NOI18N
        jTextPane2.setOpaque(false);
        jTextPane2.setPreferredSize(new java.awt.Dimension(959, 28));
        jScrollPane2.setViewportView(jTextPane2);

        javax.swing.GroupLayout risksPanelLayout = new javax.swing.GroupLayout(risksPanel);
        risksPanel.setLayout(risksPanelLayout);
        risksPanelLayout.setHorizontalGroup(
            risksPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, risksPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(risksPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(costsYearScrollPane5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 544, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 544, Short.MAX_VALUE)
                    .addComponent(costsLabel2, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(costsYearScrollPane4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 544, Short.MAX_VALUE)
                    .addComponent(costsLabel3, javax.swing.GroupLayout.Alignment.LEADING))
                .addContainerGap())
        );
        risksPanelLayout.setVerticalGroup(
            risksPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(risksPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(costsLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(costsYearScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(costsLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(costsYearScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(118, Short.MAX_VALUE))
        );

        tabs.addTab(resourceMap.getString("risksPanel.TabConstraints.tabTitle"), risksPanel); // NOI18N

        costsYearTable6.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Риск недостаточного качества продукта", new Integer(10)},
                {"Риск реализации. ", new Integer(90)},
                {"Риск неверной постановки технического задания", new Integer(20)},
                {"Риск сложности.", new Integer(70)},
                {"Риск несоответствия. ", new Integer(80)},
                {"Риск ошибок планирования сроков выполнения", new Integer(40)},
                {"Риск нестабильной работы собственного оборудования и каналов связи", new Integer(30)},
                {"Риск неоплаты выполненной работы", new Integer(20)},
                {"Риск неверного определения квалификации персонала.", new Integer(60)},
                {"Неопределенность законодательного изменения", new Integer(10)},
                {"Риск оценки затрат на продвижение", new Integer(10)}
            },
            new String [] {
                "Наименование", "Вероятность"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Integer.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        costsYearScrollPane6.setViewportView(costsYearTable6);

        jToolBar4.setRollover(true);

        addRiskButton.setIcon(resourceMap.getIcon("addRiskButton.icon")); // NOI18N
        addRiskButton.setFocusable(false);
        addRiskButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        addRiskButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        addRiskButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addRiskButtonActionPerformed(evt);
            }
        });
        jToolBar4.add(addRiskButton);

        delRiskButton.setIcon(resourceMap.getIcon("delRiskButton.icon")); // NOI18N
        delRiskButton.setFocusable(false);
        delRiskButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        delRiskButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        delRiskButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                delRiskButtonActionPerformed(evt);
            }
        });
        jToolBar4.add(delRiskButton);

        javax.swing.GroupLayout identifPanelLayout = new javax.swing.GroupLayout(identifPanel);
        identifPanel.setLayout(identifPanelLayout);
        identifPanelLayout.setHorizontalGroup(
            identifPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, identifPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(identifPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(costsYearScrollPane6, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 519, Short.MAX_VALUE)
                    .addComponent(jToolBar4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 519, Short.MAX_VALUE))
                .addContainerGap())
        );
        identifPanelLayout.setVerticalGroup(
            identifPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(identifPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jToolBar4, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(costsYearScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 439, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane2.addTab(resourceMap.getString("identifPanel.TabConstraints.tabTitle"), identifPanel); // NOI18N

        analitPanel.setLayout(new java.awt.BorderLayout());

        rangTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Риск несоответствия.", null, null, new Integer(3)},
                {"Риск реализации.", null, null, new Integer(6)},
                {"Риск сложности.", null, null, new Integer(6)},
                {"Риск неверного определения квалификации персонала.", null, null, new Integer(4)}
            },
            new String [] {
                "Наименование", "Вероятность", "Воздействие", "Ранг"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        rangScrollPane.setViewportView(rangTable);

        javax.swing.GroupLayout tblRangePanelLayout = new javax.swing.GroupLayout(tblRangePanel);
        tblRangePanel.setLayout(tblRangePanelLayout);
        tblRangePanelLayout.setHorizontalGroup(
            tblRangePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 539, Short.MAX_VALUE)
            .addGroup(tblRangePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(tblRangePanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(rangScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 519, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        tblRangePanelLayout.setVerticalGroup(
            tblRangePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 154, Short.MAX_VALUE)
            .addGroup(tblRangePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(tblRangePanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(rangScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        analitPanel.add(tblRangePanel, java.awt.BorderLayout.PAGE_START);

        nameFormLabel.setFont(resourceMap.getFont("nameFormLabel.font")); // NOI18N
        nameFormLabel.setText(resourceMap.getString("nameFormLabel.text")); // NOI18N

        statusRiskLabel.setText(resourceMap.getString("statusRiskLabel.text")); // NOI18N

        statusRiskTextField.setText(resourceMap.getString("statusRiskTextField.text")); // NOI18N

        metodRiskLabel.setText(resourceMap.getString("metodRiskLabel.text")); // NOI18N

        metodRiskComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Принятие риска ", "Уклонение от риска ", "Передача риска ", "Снижение риска" }));

        catRiskLabel.setText(resourceMap.getString("catRiskLabel.text")); // NOI18N

        catRiskTextField.setText(resourceMap.getString("catRiskTextField.text")); // NOI18N

        PLabel.setText(resourceMap.getString("PLabel.text")); // NOI18N

        PTextPane.setText(resourceMap.getString("PTextPane.text")); // NOI18N
        jScrollPane3.setViewportView(PTextPane);

        SimLabel.setText(resourceMap.getString("SimLabel.text")); // NOI18N

        simTextPane.setText(resourceMap.getString("simTextPane.text")); // NOI18N
        jScrollPane5.setViewportView(simTextPane);

        vozLabel.setText(resourceMap.getString("vozLabel.text")); // NOI18N

        vozTextPane.setText(resourceMap.getString("vozTextPane.text")); // NOI18N
        jScrollPane6.setViewportView(vozTextPane);

        triggerRiskLabel.setText(resourceMap.getString("triggerRiskLabel.text")); // NOI18N

        triggerRiskTextField.setText(resourceMap.getString("triggerRiskTextField.text")); // NOI18N

        printReportRisksButton.setText(resourceMap.getString("printReportRisksButton.text")); // NOI18N
        printReportRisksButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printReportRisksButtonActionPerformed(evt);
            }
        });

        clearCartButton.setText(resourceMap.getString("clearCartButton.text")); // NOI18N
        clearCartButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearCartButtonActionPerformed(evt);
            }
        });

        saveCartButton.setText(resourceMap.getString("saveCartButton.text")); // NOI18N
        saveCartButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveCartButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout matixPanelLayout = new javax.swing.GroupLayout(matixPanel);
        matixPanel.setLayout(matixPanelLayout);
        matixPanelLayout.setHorizontalGroup(
            matixPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(matixPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(matixPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(nameFormLabel)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, matixPanelLayout.createSequentialGroup()
                        .addComponent(printReportRisksButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearCartButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(saveCartButton))
                    .addGroup(matixPanelLayout.createSequentialGroup()
                        .addComponent(statusRiskLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(statusRiskTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 425, Short.MAX_VALUE))
                    .addGroup(matixPanelLayout.createSequentialGroup()
                        .addComponent(metodRiskLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(metodRiskComboBox, 0, 363, Short.MAX_VALUE))
                    .addGroup(matixPanelLayout.createSequentialGroup()
                        .addComponent(triggerRiskLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(triggerRiskTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 381, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, matixPanelLayout.createSequentialGroup()
                        .addGroup(matixPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(SimLabel)
                            .addComponent(catRiskLabel)
                            .addComponent(PLabel)
                            .addComponent(vozLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 68, Short.MAX_VALUE)
                        .addGroup(matixPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jScrollPane6, 0, 0, Short.MAX_VALUE)
                            .addComponent(jScrollPane5, javax.swing.GroupLayout.Alignment.TRAILING, 0, 0, Short.MAX_VALUE)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING, 0, 0, Short.MAX_VALUE)
                            .addComponent(catRiskTextField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 382, Short.MAX_VALUE))))
                .addContainerGap())
        );
        matixPanelLayout.setVerticalGroup(
            matixPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(matixPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(nameFormLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(matixPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusRiskLabel)
                    .addComponent(statusRiskTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(matixPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(metodRiskLabel)
                    .addComponent(metodRiskComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(matixPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(catRiskLabel)
                    .addComponent(catRiskTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(matixPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(PLabel)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(matixPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(SimLabel)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(matixPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(vozLabel)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(matixPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(triggerRiskLabel)
                    .addComponent(triggerRiskTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(matixPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(saveCartButton)
                    .addComponent(clearCartButton)
                    .addComponent(printReportRisksButton))
                .addGap(144, 144, 144))
        );

        analitPanel.add(matixPanel, java.awt.BorderLayout.CENTER);

        jTabbedPane2.addTab(resourceMap.getString("analitPanel.TabConstraints.tabTitle"), analitPanel); // NOI18N

        javax.swing.GroupLayout msfRisksPanelLayout = new javax.swing.GroupLayout(msfRisksPanel);
        msfRisksPanel.setLayout(msfRisksPanelLayout);
        msfRisksPanelLayout.setHorizontalGroup(
            msfRisksPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(msfRisksPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 544, Short.MAX_VALUE)
                .addContainerGap())
        );
        msfRisksPanelLayout.setVerticalGroup(
            msfRisksPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(msfRisksPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 520, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabs.addTab(resourceMap.getString("msfRisksPanel.TabConstraints.tabTitle"), msfRisksPanel); // NOI18N

        conclPanel.setPreferredSize(new java.awt.Dimension(452, 608));
        conclPanel.setLayout(new java.awt.GridLayout(2, 3, 1, 1));

        chartPanel.setBackground(resourceMap.getColor("chartPanel.background")); // NOI18N
        chartPanel.setMinimumSize(new java.awt.Dimension(200, 300));
        chartPanel.setRequestFocusEnabled(false);
        chartPanel.setVerifyInputWhenFocusTarget(false);
        chartPanel.setLayout(new java.awt.BorderLayout());

        calcResultDiagramButton.setText(resourceMap.getString("calcResultDiagramButton.text")); // NOI18N
        calcResultDiagramButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                calcResultDiagramButtonActionPerformed(evt);
            }
        });
        chartPanel.add(calcResultDiagramButton, java.awt.BorderLayout.PAGE_START);

        chartScrollPane.setPreferredSize(new java.awt.Dimension(200, 200));
        chartScrollPane.setWheelScrollingEnabled(false);
        chartPanel.add(chartScrollPane, java.awt.BorderLayout.CENTER);

        conclPanel.add(chartPanel);

        jPanel8.setLayout(new java.awt.BorderLayout());

        costsYearScrollPane7.setWheelScrollingEnabled(false);

        costsYearTable7.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Общие затраты (приведенная стоимость, PV)", new Float(28000.0), new Float(28000.0)},
                {"Общая выгода (приведенная стоимость, PV)", new Float(165855.53), new Float(154191.28)},
                {"NPV", new Float(137855.53), new Float(126191.28)},
                {"NPV на сотрудника", new Float(27571.11), new Float(25238.26)},
                {"ROI (за весь период)", new Float(252.0), new Float(551.0)},
                {"Выгода в месяц, PV", new Float(4607.1), new Float(4283.09)},
                {"Период окупаемости, месяцев", new Float(6.08), new Float(6.54)}
            },
            new String [] {
                "Показатель", "Значение", "Значение (с учетом риска)"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Float.class, java.lang.Float.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        costsYearScrollPane7.setViewportView(costsYearTable7);

        jPanel8.add(costsYearScrollPane7, java.awt.BorderLayout.CENTER);

        conclPanel.add(jPanel8);

        tabs.addTab(resourceMap.getString("conclPanel.TabConstraints.tabTitle"), conclPanel); // NOI18N

        javax.swing.GroupLayout dataPanelLayout = new javax.swing.GroupLayout(dataPanel);
        dataPanel.setLayout(dataPanelLayout);
        dataPanelLayout.setHorizontalGroup(
            dataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, dataPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(tabs, javax.swing.GroupLayout.PREFERRED_SIZE, 690, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        dataPanelLayout.setVerticalGroup(
            dataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, dataPanelLayout.createSequentialGroup()
                .addContainerGap(22, Short.MAX_VALUE)
                .addComponent(tabs, javax.swing.GroupLayout.PREFERRED_SIZE, 547, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        tabs.getAccessibleContext().setAccessibleParent(dataPanel);

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addComponent(dataPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addComponent(dataPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(analytics_it.Analytics_ITApp.class).getContext().getActionMap(Analytics_ITView.class, this);
        newMenuItem.setAction(actionMap.get("showNewProjJDialog")); // NOI18N
        newMenuItem.setText(resourceMap.getString("newMenuItem.text")); // NOI18N
        fileMenu.add(newMenuItem);

        openMenuItem.setAction(actionMap.get("showOpenProjJDialog")); // NOI18N
        openMenuItem.setText(resourceMap.getString("openMenuItem.text")); // NOI18N
        fileMenu.add(openMenuItem);

        saveMenuItem.setAction(actionMap.get("quit")); // NOI18N
        saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveMenuItem.setText(resourceMap.getString("saveMenuItem.text")); // NOI18N
        fileMenu.add(saveMenuItem);
        fileMenu.add(jSeparator1);

        optionMenuItem.setAction(actionMap.get("showPropJDialog")); // NOI18N
        optionMenuItem.setText(resourceMap.getString("optionMenuItem.text")); // NOI18N
        fileMenu.add(optionMenuItem);

        printMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        printMenuItem.setText(resourceMap.getString("printMenuItem.text")); // NOI18N
        printMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(printMenuItem);
        fileMenu.add(jSeparator2);

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setText(resourceMap.getString("exitMenuItem.text")); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setText(resourceMap.getString("aboutMenuItem.text")); // NOI18N
        aboutMenuItem.setToolTipText(resourceMap.getString("aboutMenuItem.toolTipText")); // NOI18N
        helpMenu.add(aboutMenuItem);

        aboutMenuItem1.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem1.setText(resourceMap.getString("aboutMenuItem1.text")); // NOI18N
        aboutMenuItem1.setToolTipText(resourceMap.getString("aboutMenuItem1.toolTipText")); // NOI18N
        helpMenu.add(aboutMenuItem1);

        menuBar.add(helpMenu);

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 720, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 700, Short.MAX_VALUE)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel))
                .addGap(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    
    private void mainPanelFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_mainPanelFocusGained
        // TODO add your handling code here:
    }//GEN-LAST:event_mainPanelFocusGained

    private void calcResultDiagramButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_calcResultDiagramButtonActionPerformed

addChart();
    }//GEN-LAST:event_calcResultDiagramButtonActionPerformed

    private void addRiskButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addRiskButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_addRiskButtonActionPerformed

    private void delRiskButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_delRiskButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_delRiskButtonActionPerformed

    private void printReportRisksButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printReportRisksButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_printReportRisksButtonActionPerformed

    private void clearCartButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearCartButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_clearCartButtonActionPerformed

    private void saveCartButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveCartButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_saveCartButtonActionPerformed

    private void printMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printMenuItemActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_printMenuItemActionPerformed

    /*
     * Добавление комбобокса в ячейку таблицы
     */
    public void addComboboxToTable(TableColumn col, Object[] value) {
        DefaultComboBoxModel comboModel = new DefaultComboBoxModel( value );
JComboBox combo = new JComboBox();
combo.setModel( comboModel );

col.setCellEditor( new DefaultCellEditor( combo ) );
    }
    public void getSourceData(){
        ResultSet rsAProjects=null;
        try{
              if (idProj > 0) {
                  rsAProjects = stmt.executeQuery( "SELECT * FROM APP.AProjects where idProj = "+String.valueOf(idProj));
                  rsAProjects.last();
                  if (rsAProjects!=null) {
                     this.getFrame().setTitle(rsAProjects.getString("nameProj"));
                      ResultSet rs=null;
                      rs = stmt.executeQuery( "SELECT * FROM APP.sourceData where idProj = "+String.valueOf(idProj));
                      rs.last();
                      idSourceData = rs.getInt("idSourceData");
                      userCountTextField.setText(String.valueOf(rs.getInt("userCont")));
                      discountRateFormattedTextField.setText(String.valueOf( rs.getInt("discountRate")));
                      workDaysCountFormattedTextField.setText(String.valueOf(rs.getInt("workDaysCount")));
                      coastUserHourFormattedTextField.setText(String.valueOf(rs.getFloat("coastUserHour")));
                      coastSoftwareFormattedTextField.setText(String.valueOf(rs.getFloat("coastSoftware")));
                      coastWorkContractorFormattedTextField.setText(String.valueOf(rs.getFloat("coastWorkContractor")));
                      coastHarwareFormattedTextField.setText(String.valueOf(rs.getFloat("coastHarware")));
                      coastEducationFormattedTextField.setText(String.valueOf(rs.getFloat("coastEducation")));
                      periodFinancialFormattedTextField.setText(String.valueOf(rs.getInt("periodFinancial")));
                      rs.close();
                  }
                  else {
                      ((FrameView) this).getFrame().setTitle("Проект не создан");
                      inputPanel.setEnabled(false);
                  }
                  rsAProjects.close();
            }
        }
        catch(SQLException e){
            e.printStackTrace();
        }

    }

    public void updateDataForm (int idProj) {
        this.idProj = idProj;
        getSourceData();
    }

     private static CategoryDataset createDatasetChart() {
      final String v2 = "с учетом рисков";
      final String v1 = "без учета рисков";

      final String sv1 = "Выгода от отказа приобретения библиотеки компонентов FIBPlus для работы с БД FireBird, руб.";
      final String sv2 = "Выгода от отказа приобретения дополнительного ПО для работы с редактором печатных форм FastReport, руб.";
      final String sv3 = "Выгода от использования свойства редактора Подсветка слова под курсором, руб.";

      final DefaultCategoryDataset dataset =
      new DefaultCategoryDataset( );

      dataset.addValue( 74025 , v1 , sv1 );
      dataset.addValue( 95760 , v1 , sv2 );
      dataset.addValue( 24637.50  , v1 , sv3 );

      dataset.addValue( 69090 , v2 , sv1 );
      dataset.addValue( 89376.00 , v2 , sv2 );
      dataset.addValue( 4434.75 , v2 , sv3 );


      return dataset;
    }
    private static JFreeChart createChart(TimeTableXYDataset dataset) {

        // OX - ось абсцисс
        // задаем название оси
        DateAxis domainAxis = new DateAxis("Year");
        // Показываем стрелочку вправо
        domainAxis.setPositiveArrowVisible(true);
        // Задаем отступ от графика
        domainAxis.setUpperMargin(0.2);

        // OY - ось ординат
        // Задаём название оси
        NumberAxis rangeAxis = new NumberAxis("Color");
        // Задаём величину деления
        rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
        rangeAxis.setTickUnit(new NumberTickUnit(200));
        // Показываем стрелочку вверх
        rangeAxis.setPositiveArrowVisible(true);


        // Render
        // Создаем стопковый (не знаю как лучше перевести) график
        // 0.02 - расстояние между столбиками
        StackedXYBarRenderer renderer = new StackedXYBarRenderer(0.02);
        // без рамки
        renderer.setDrawBarOutline(false);
        // цвета для каждого элемента стопки
        renderer.setSeriesPaint(0, Color.blue);
        renderer.setSeriesPaint(1, Color.red);
        // Задаём формат и текст подсказки
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator("{0} : {1} = {2} tonnes", new SimpleDateFormat("yyyy"), new DecimalFormat("#,##0")));
        renderer.setSeriesItemLabelGenerator(0, new StandardXYItemLabelGenerator());
        renderer.setSeriesItemLabelGenerator(1, new StandardXYItemLabelGenerator());
        // Делаем её видимой
        renderer.setSeriesItemLabelsVisible(0, true);
        renderer.setSeriesItemLabelsVisible(1, true);
        // И описываем её шрифт
        renderer.setSeriesItemLabelFont(0, new Font("Serif", Font.BOLD, 10));
        renderer.setSeriesItemLabelFont(1, new Font("Serif", Font.BOLD, 10));

        // Plot
        // Создаем область рисования
        XYPlot plot = new XYPlot(dataset, domainAxis, rangeAxis, renderer);
        // Закрашиваем
        plot.setBackgroundPaint(Color.white);
        // Закрашиваем сетку
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        // Отступ от осей

        plot.setOutlinePaint(null);

        // Chart
        // Создаем новый график
        JFreeChart chart = new JFreeChart(plot);
        // Закрашиваем
        chart.setBackgroundPaint(Color.white);
        // Перемещаем легенду в верхний правый угол
       // chart.getLegend().setPosition(RectangleEdge.RIGHT);
       // chart.getLegend().setVerticalAlignment(VerticalAlignment.TOP);

        return chart;
    }
        
    public void addChart(){
      
       JFreeChart barChart = ChartFactory.createBarChart(
         "Наименование выгоды",
         "",
         "Итого, руб.",
         createDatasetChart(),
         PlotOrientation.VERTICAL,
         true, true, false);

      ChartPanel chartPanel2 = new ChartPanel( barChart );
      chartPanel2.setSize(chartScrollPane.getWidth(),chartScrollPane.getHeight());
      chartPanel2.setVisible(true);
      chartScrollPane.add(chartPanel2);
  chartScrollPane.validate();

      chartPanel2.repaint();
//chartPanel.validate();



    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel PLabel;
    private javax.swing.JTextPane PTextPane;
    private javax.swing.JLabel SimLabel;
    private javax.swing.JButton addRiskButton;
    private javax.swing.JFormattedTextField aggCoastContractorFormattedTextField;
    private javax.swing.JLabel aggCoastContractorLabel;
    private javax.swing.JLabel aggCoastContractorLabelP;
    private javax.swing.JLabel aggCoastSoftwareLabel;
    private javax.swing.JFormattedTextField aggCoastSoftwareLabelFormattedTextField;
    private javax.swing.JLabel aggCoastSoftwareLabelLabelP;
    private javax.swing.JPanel analitPanel;
    private javax.swing.JFormattedTextField annualСostFormattedTextField;
    private javax.swing.JLabel annualСostLabel;
    private javax.swing.JLabel annualСostLabelP;
    private javax.swing.JButton calcResultDiagramButton;
    private javax.swing.JLabel catRiskLabel;
    private javax.swing.JTextField catRiskTextField;
    private javax.swing.JPanel chartPanel;
    private javax.swing.JScrollPane chartScrollPane;
    private javax.swing.JButton clearCartButton;
    private javax.swing.JFormattedTextField coastEducationFormattedTextField;
    private javax.swing.JLabel coastEducationLabel;
    private javax.swing.JLabel coastEducationLabelP;
    private javax.swing.JFormattedTextField coastHarwareFormattedTextField;
    private javax.swing.JLabel coastHarwareLabel;
    private javax.swing.JLabel coastHarwareLabelP;
    private javax.swing.JFormattedTextField coastSoftwareFormattedTextField;
    private javax.swing.JLabel coastSoftwareLabel;
    private javax.swing.JLabel coastSoftwareLabelP;
    private javax.swing.JFormattedTextField coastUserHourFormattedTextField;
    private javax.swing.JFormattedTextField coastUserHourFormattedTextField1;
    private javax.swing.JFormattedTextField coastUserHourFormattedTextField2;
    private javax.swing.JFormattedTextField coastUserHourFormattedTextField3;
    private javax.swing.JFormattedTextField coastUserHourFormattedTextField4;
    private javax.swing.JLabel coastUserHourLabe;
    private javax.swing.JLabel coastUserHourLabe1;
    private javax.swing.JLabel coastUserHourLabe2;
    private javax.swing.JLabel coastUserHourLabe3;
    private javax.swing.JLabel coastUserHourLabe4;
    private javax.swing.JLabel coastUserHourLabel;
    private javax.swing.JFormattedTextField coastWorkContractorFormattedTextField;
    private javax.swing.JLabel coastWorkContractorLabel;
    private javax.swing.JLabel coastWorkContractorLabelP;
    private javax.swing.JPanel conclPanel;
    private javax.swing.JLabel costsLabel;
    private javax.swing.JLabel costsLabel1;
    private javax.swing.JLabel costsLabel2;
    private javax.swing.JLabel costsLabel3;
    private javax.swing.JScrollPane costsPane;
    private javax.swing.JPanel costsPanel;
    private javax.swing.JTable costsTable;
    private javax.swing.JScrollPane costsYearScrollPane;
    private javax.swing.JScrollPane costsYearScrollPane1;
    private javax.swing.JScrollPane costsYearScrollPane2;
    private javax.swing.JScrollPane costsYearScrollPane3;
    private javax.swing.JScrollPane costsYearScrollPane4;
    private javax.swing.JScrollPane costsYearScrollPane5;
    private javax.swing.JScrollPane costsYearScrollPane6;
    private javax.swing.JScrollPane costsYearScrollPane7;
    private javax.swing.JTable costsYearTable;
    private javax.swing.JTable costsYearTable1;
    private javax.swing.JTable costsYearTable2;
    private javax.swing.JTable costsYearTable3;
    private javax.swing.JTable costsYearTable4;
    private javax.swing.JTable costsYearTable5;
    private javax.swing.JTable costsYearTable6;
    private javax.swing.JTable costsYearTable7;
    private javax.swing.JPanel dataPanel;
    private javax.swing.JButton delRiskButton;
    private javax.swing.JFormattedTextField discountRateFormattedTextField;
    private javax.swing.JLabel discountRateLabel;
    private javax.swing.JLabel discountRateLabelP;
    private javax.swing.JPanel identifPanel;
    private javax.swing.JPanel inputPanel;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextPane jTextPane1;
    private javax.swing.JTextPane jTextPane2;
    private javax.swing.JToolBar jToolBar2;
    private javax.swing.JToolBar jToolBar3;
    private javax.swing.JToolBar jToolBar4;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JPanel matixPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JComboBox metodRiskComboBox;
    private javax.swing.JLabel metodRiskLabel;
    private javax.swing.JPanel msfRisksPanel;
    private javax.swing.JLabel nameFormLabel;
    private javax.swing.JFormattedTextField periodFinancialFormattedTextField;
    private javax.swing.JFormattedTextField periodFinancialFormattedTextField1;
    private javax.swing.JFormattedTextField periodFinancialFormattedTextField2;
    private javax.swing.JLabel periodFinancialLabel;
    private javax.swing.JLabel periodFinancialLabel1;
    private javax.swing.JLabel periodFinancialLabel10;
    private javax.swing.JLabel periodFinancialLabel11;
    private javax.swing.JLabel periodFinancialLabel12;
    private javax.swing.JLabel periodFinancialLabel13;
    private javax.swing.JLabel periodFinancialLabel2;
    private javax.swing.JLabel periodFinancialLabel3;
    private javax.swing.JLabel periodFinancialLabel4;
    private javax.swing.JLabel periodFinancialLabel5;
    private javax.swing.JLabel periodFinancialLabel6;
    private javax.swing.JLabel periodFinancialLabel8;
    private javax.swing.JLabel periodFinancialLabel9;
    private javax.swing.JLabel periodFinancialLabelP;
    private javax.swing.JLabel periodFinancialLabelP1;
    private javax.swing.JLabel periodFinancialLabelP2;
    private javax.swing.JButton printReportRisksButton;
    private javax.swing.JScrollPane rangScrollPane;
    private javax.swing.JTable rangTable;
    private javax.swing.JPanel risksPanel;
    private javax.swing.JButton saveCartButton;
    private javax.swing.JTextPane simTextPane;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JLabel statusRiskLabel;
    private javax.swing.JTextField statusRiskTextField;
    private javax.swing.JTabbedPane tabs;
    private javax.swing.JPanel tblRangePanel;
    private javax.swing.JLabel triggerRiskLabel;
    private javax.swing.JTextField triggerRiskTextField;
    private javax.swing.JLabel userContLabel;
    private javax.swing.JLabel userContLabelP;
    private javax.swing.JFormattedTextField userCountTextField;
    private javax.swing.JPanel valuePanel;
    private javax.swing.JLabel vozLabel;
    private javax.swing.JTextPane vozTextPane;
    private javax.swing.JButton workDaysCountButton;
    private javax.swing.JFormattedTextField workDaysCountFormattedTextField;
    private javax.swing.JLabel workDaysCountLabel;
    private javax.swing.JLabel workDaysCountLabelP;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;
    private JDialog newProjBox;
    private JDialog propBox;

    private DBConnection mdbc;
    private java.sql.Statement stmt;
    private int idProj=-1;
    private int idSourceData = -1;
}
