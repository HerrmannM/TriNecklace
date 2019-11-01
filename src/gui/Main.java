package gui;

import java.awt.Toolkit;
import java.io.File;
import code.Dispatcher;
import code.Dispatcher.Finish;
import code.Dispatcher.Progress;
import code.ThreadedWorker;

public class Main extends javax.swing.JFrame
{
    static private final long serialVersionUID = 0;

    /* ********************************************************************* */
    private File file;
    private Dispatcher dispatcher;
    private long startTime;
    private long stopTime;

    /* ********************************************************************* */

    /**
     * @param args the command line arguments
     */
    public static void main(String args[])
    {
        File file = null;
        // En ligne de commande
        if(args.length > 0)
        {
            // Déclarations :
            int len, thr, parts;
            boolean max=false, silent=false;
            Progress progress;

            // Récupération des données :
            len = Integer.parseInt(args[0]);
            thr = Integer.parseInt(args[1]);
            parts = Integer.parseInt(args[2]);

            for(int i=3; i<args.length; ++i)
            {
                if( args[i].equals("--max") )
                { max=true; }
                else if( args[i].equals("--silent") )
                { silent=true; }
                else if( args[i].equals("--output") )
                {
                    ++i;
                    if(i<args.length)
                    { file = new File(args[i]); }
                    else
                    { System.err.println(" Output : no file !");}
                }
                else
                {
                    System.err.println(args[i]+" bad argument");
                    System.err.println(" --max    : count maximal");
                    System.err.println(" --silent : silent mode");
                }
            }

            // Création objet :
            final Main main = new Main(false);

            if(silent)
            {
                // Création d'un retour de progression :
                progress = new Progress()
                { public void setValue(String subject, float value){} };
            }
            else
            {
                System.out.println("----- length = " + len + " -----");

                // Création d'un retour de progression :
                progress = new Progress()
                {
                    public void setValue(String subject, float value)
                    { System.out.println(subject + " " + Math.round(value * 100) + "%"); }
                };
            }

            // Création rappel de fin :
            Finish finish = new Finish()
            {
                public void call(Dispatcher dispatcher)
                {
                    main.stopTime = System.currentTimeMillis();
                    System.out.println("Generated: "+ dispatcher.getGenerated());
                    System.out.println("Circular codes: "+ dispatcher.getCircularCount());
                    if(dispatcher.isCountingMax())
                    { System.out.println("Maximals: "+dispatcher.getMaximalCount()); }
                    System.out.println(formatDuration(main.stopTime - main.startTime));
                }
            };

            // Lancement du calcul :
            main.file = file;
            main.start(progress, finish, max, len, thr, parts);
        }
        // Par interface graphique
        else
        {
            java.awt.EventQueue.invokeLater(new Runnable()
                    { public void run() { new Main(true).setVisible(true); } });
        }
    }


    /* ********************************************************************* */
    /* Gestion interface graphique */
    /* ********************************************************************* */

    public Main(boolean gui)
    {
        if(gui)
        {initComponents();}
    }

    // Bouton COUNT/CANCEL
    private void startActionPerformed(java.awt.event.ActionEvent evt)
    {
        if (dispatcher == null)
        {
            int len = (Integer) longueur.getValue();
            int thr = (Integer) nbThreads.getValue();
            int parts = (Integer) nbPartitions.getValue();
            final  StringBuilder text = new StringBuilder();

            // Barre de progression
            Progress progress = new Progress()
            {
                public void setValue(String subject, float value)
                {
                    int percent = Math.round(value * 100);
                    progressBar.setString(subject + " " + percent + "%");
                    progressBar.setValue(percent);
                }
            };

            // Appel de fin de calcul threadé :
            Finish finish = new Finish()
            {
                public void call(Dispatcher result)
                {
                    // Gestion de l'arrêt :
                    if (!result.isKilled())
                    {
                        // Arrêt normal :
                        stopTime = System.currentTimeMillis();
                        text.append("Circular codes\t: ").append(result.getCircularCount()).append("\n");
                        if(result.isCountingMax())
                        { text.append("Maximal\t: ").append(result.getMaximalCount()).append("\n"); }
                        runningTime.setText(formatDuration(stopTime - startTime));
                    } else
                    {
                        // Arrêt provoqué :
                        text.append("Cancelled");
                        progressBar.setValue(0);
                        progressBar.setString("");
                    }
                    resultsField.setText(text.toString());

                    // Permettre un nouveau calcul :
                    start.setText("Count");
                    dispatcher = null;
                }
            };

            text.append("Length\t: ").append(len).append("\n");
            resultsField.setText(text.toString());
            start.setText("Cancel");

            start(progress, finish, maximaux.isSelected(), len, thr, parts);
        }
        else
        { dispatcher.kill(); }
    }

    // Coche OUTPUT FILE
    private void writeFileActionPerformed(java.awt.event.ActionEvent evt)
    {
        if(file == null)
        {
            fileChooser.showSaveDialog(this);
            file = fileChooser.getSelectedFile();
            if(file != null)
            { outputFileLabel.setText(file.getPath()); }
        }
        else
        {
            file = null;
            outputFileLabel.setText("");
        }
        writeFile.setSelected(file != null);
    }

    // Bouton ABOUT
    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt)
    { new About().setVisible(true); }

    // Bouton CLOSE
    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt)
    {
        if(dispatcher != null)
        {dispatcher.kill();}
        dispose();
    }


    /* ********************************************************************* */
    /* Gestion du calcul */
    /* ********************************************************************* */

    /**
     * Lancement du calcul
     */
    private void start(Progress progress, Finish finish, boolean max, int len, int thr, int parts)
    {
        startTime = System.currentTimeMillis();

        // Création et lancement des workers et du dispatcher :
        dispatcher = new Dispatcher(len, parts, max, progress, file);
        for (int i = 0; i < thr; ++i)
        { dispatcher.add(new ThreadedWorker(dispatcher)); }
        dispatcher.threadedLaunch(finish);
    }

    /* ********************************************************************* */

    public static String formatDuration(long time) 
    {
        //long ms = time % 1000;
        time /= 1000;
        long s = time % 60;
        time /= 60;
        long m = time % 60;
        time /= 60;
        long h = time % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSpinner longueur;
    private javax.swing.JCheckBox maximaux;
    private javax.swing.JSpinner nbPartitions;
    private javax.swing.JSpinner nbThreads;
    private javax.swing.JLabel outputFileLabel;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JTextArea resultsField;
    private javax.swing.JLabel runningTime;
    private javax.swing.JLabel runningTimeLabel;
    private javax.swing.JButton start;
    private javax.swing.JCheckBox writeFile;
    // End of variables declaration//GEN-END:variables

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents()
    {
        fileChooser = new javax.swing.JFileChooser();
        jButton1 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        longueur = new javax.swing.JSpinner();
        maximaux = new javax.swing.JCheckBox();
        start = new javax.swing.JButton();
        progressBar = new javax.swing.JProgressBar();
        jScrollPane1 = new javax.swing.JScrollPane();
        resultsField = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        nbPartitions = new javax.swing.JSpinner();
        writeFile = new javax.swing.JCheckBox();
        nbThreads = new javax.swing.JSpinner();
        jScrollPane2 = new javax.swing.JScrollPane();
        outputFileLabel = new javax.swing.JLabel();
        runningTimeLabel = new javax.swing.JLabel();
        runningTime = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();

        jButton1.setText("jButton1");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Circular codes");
        setIconImage(Toolkit.getDefaultToolkit().getImage("icon.gif"));
        setResizable(false);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Count"));

        jLabel1.setText("Length");

        jLabel4.setText("Maximal");

        longueur.setModel(new javax.swing.SpinnerNumberModel(2, 1, 20, 1));

        start.setText("Count");
        start.addActionListener(new java.awt.event.ActionListener()
                {
                    public void actionPerformed(java.awt.event.ActionEvent evt)
        { startActionPerformed(evt);}
                });

        progressBar.setString("");
        progressBar.setStringPainted(true);

        resultsField.setColumns(10);
        resultsField.setEditable(false);
        resultsField.setRows(5);
        jScrollPane1.setViewportView(resultsField);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel4)
                                .addComponent(jLabel1))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(maximaux)
                                .addComponent(longueur, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addComponent(progressBar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 202, Short.MAX_VALUE)
                        .addComponent(start, javax.swing.GroupLayout.DEFAULT_SIZE, 202, Short.MAX_VALUE)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 202, Short.MAX_VALUE))
                    .addContainerGap())
                );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(longueur, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel1))
                    .addGap(6, 6, 6)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jLabel4)
                        .addComponent(maximaux))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(start)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Options"));

        jLabel2.setText("Threads");

        jLabel3.setText("Partition number");

        jLabel8.setText("Output file");

        nbPartitions.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(512), Integer.valueOf(1), null, Integer.valueOf(1)));

        writeFile.addActionListener(new java.awt.event.ActionListener()
                {
                    public void actionPerformed(java.awt.event.ActionEvent evt)
        { writeFileActionPerformed(evt); }
                });

        nbThreads.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(4), Integer.valueOf(1), null, Integer.valueOf(1)));

        outputFileLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        jScrollPane2.setViewportView(outputFileLabel);

        runningTimeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        runningTimeLabel.setText("Last execution running time:");

        runningTime.setText("<>");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel3)
                                .addComponent(jLabel2)
                                .addComponent(jLabel8))
                            .addGap(12, 12, 12)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(nbPartitions, javax.swing.GroupLayout.DEFAULT_SIZE, 85, Short.MAX_VALUE)
                                .addComponent(nbThreads, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 85, Short.MAX_VALUE)
                                .addComponent(writeFile)))
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 202, Short.MAX_VALUE)
                        .addComponent(runningTimeLabel)
                        .addComponent(runningTime, javax.swing.GroupLayout.Alignment.TRAILING))
                    .addContainerGap())
                );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel2)
                        .addComponent(nbThreads, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel3)
                        .addComponent(nbPartitions, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jLabel8)
                        .addComponent(writeFile))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(runningTimeLabel)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(runningTime)
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );

        jButton2.setText("About");
        jButton2.addActionListener(new java.awt.event.ActionListener()
                {
                    public void actionPerformed(java.awt.event.ActionEvent evt)
        { jButton2ActionPerformed(evt); }
                });

        jButton3.setText("Close");
        jButton3.addActionListener(new java.awt.event.ActionListener() 
                {
                    public void actionPerformed(java.awt.event.ActionEvent evt)
        { jButton3ActionPerformed(evt); }
                });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                            .addComponent(jButton2)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(jButton3)))
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jButton3)
                                .addComponent(jButton2))))
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );

        pack();
    }// </editor-fold>//GEN-END:initComponents
}
