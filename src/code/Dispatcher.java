package code;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gui.Main;
import logic.Code;

public class Dispatcher
{
    /*
     * Données :
     * ***********************************************************************
     */

    // Paramètre de l'exécution :
    private int                   longueur;
    private int                   nbPartitions;
    private boolean               isCountingMax;
    private Progress              progress;
    private final File            outputFile;
    private final ArrayList<File> outputFiles;

    // Contrôle du dispatch :
    private long                  currentCount;
    private long                  stopAt;
    private long                  nbTotal;
    private long                  perPartition;
    private long                  missingFromPartitions;
    private boolean               isKilled;

    // Tâches threadées :
    private List<Worker>          workers;

    // Délai de surveillance :
    private final int             DELAY = 1000;

    /*
     * Constructeur
     * ***********************************************************************
     */
    public Dispatcher(int longueur_, int nbPartitions_, boolean isCountingMax_,
            Progress progress_, File outputFile_)
    {   // Récupérations des données :
        longueur = longueur_;
        nbPartitions = nbPartitions_;
        isCountingMax = isCountingMax_;
        progress = progress_;
        outputFile = outputFile_;

        // Initialisation :
        outputFiles = new ArrayList<File>();
        workers = new ArrayList<Worker>();
        isKilled = false;

        // Initialisation des informations de dispatch :
        nbTotal = Code.combinaison(longueur, 20) * (long) Math.pow(3, longueur);
        currentCount = 0;
        perPartition = nbTotal / nbPartitions;
        missingFromPartitions = nbTotal % nbPartitions;
        stopAt = nbTotal - missingFromPartitions - perPartition;
    }

    /*
     * Méthodes
     * ***********************************************************************
     */

    /**
     * Ajout d'un worker dans le dispatcher :
     */
    public void add(Worker worker) { workers.add(worker); }

    /**
     * Arrêter le dispatcher :
     */
    public void kill() { stop(); isKilled = true; }

    /**
     * Lancer le dispatcher :
     */
    public void launch()
    {
        // Lancer les workers :
        start();

        // Surveiller l'exécution toutes les DELAY ms.
        while(!isKilled && getCount() != nbTotal)
        {
            progress.setValue("Computing...", getCount() / (float) nbTotal);
            
            try { Thread.sleep(DELAY); }
            catch(InterruptedException ex)
            { Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex); }
        }

        mergeFiles(progress, getCircularCount());

        // Mise à jour de la progression :
        if(!isKilled)
        { progress.setValue("Done", 1/*getCount() / (float) nbTotal*/); }

        // Attendre les travailleurs :
        try { join(); }
        catch(InterruptedException ex)
        { Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex); }
    }

    /**
     * Lancer le dispatcher dans un nouveau thread :
     */
    public void threadedLaunch(final Finish finish)
    {
        final Dispatcher me = this;
        new Thread
        (
                new Runnable()
                { public void run() { launch(); finish.call(me);} }
        ).start();
    }

    /**
     * Merge and sort the temporary files into the output file.
     * @param progress callback to print progress
     * @param max number of circular codes
     */
    private void mergeFiles(Progress progress, long max)
    {
        if(outputFile == null) return;
        try {
            Comparator<String> cmp = new Comparator<String>() {

                @Override
                public int compare(String o1, String o2) {
                    return o1.compareTo(o2);
                }
            };
            ExternalSort.mergeSortedFiles(outputFiles, outputFile, cmp, progress, max);
        } catch (IOException ex) {
            Logger.getLogger(Dispatcher.class.getName()).log(Level.SEVERE, null, ex);
        }

        /*FileChannel outChannel = null;
        try
        {
            outChannel = new FileOutputStream(outputFile).getChannel();
            int count = 0;
            for(File file : outputFiles)
            {
                if(!isKilled)
                {
                    FileChannel inChannel = new FileInputStream(file).getChannel();
                    inChannel.transferTo(0, inChannel.size(), outChannel);
                    inChannel.close();
                }
                file.delete();
                count++;
                progress.setValue("Merging files...", count / (float) outputFiles.size());
            }
        } catch(IOException ex)
        { Logger.getLogger(Dispatcher.class.getName()).log(Level.SEVERE, null, ex); }

        try
        {
            if(outChannel != null) { outChannel.close(); }
        } catch(IOException ex)
        { Logger.getLogger(Dispatcher.class.getName()).log(Level.SEVERE, null, ex); }
         * */
    }

    /**
     * Envoie une plage de calcul au worker en paramètre.
     */
    public synchronized void nextJob(Worker worker)
    {
        if(currentCount <= stopAt)
        {
            long endCount = currentCount + perPartition;

            // On ajoute les codes manquants à la dernière partition
            if(currentCount == stopAt)
            { endCount += missingFromPartitions; }

            OutputFile output = OutputFile.devNull;
            if(outputFile != null)
            {
                try
                {
                    File file = File.createTempFile(outputFile.getName() + ".", "", outputFile.getParentFile());
                    outputFiles.add(file);
                    output = new OutputFile(file);
                } catch(IOException ex)
                { Logger.getLogger(Dispatcher.class.getName()).log(Level.SEVERE, null, ex); }
            }

            worker.set(currentCount, endCount - 1, output);
            currentCount = endCount;
        }
        else // plus de boulot -> End < Start
        { worker.set(1, 0, null); }
    }

    /*
     * Accès aux données :
     * ***********************************************************************
     */

    public long getTotal() { return nbTotal; }

    public int getLongueur() { return longueur; }

    public boolean isCountingMax() { return isCountingMax; }

    public boolean isKilled() { return isKilled; }

    /*
     * Accès aux workers :
     * ***********************************************************************
     */

    public long getCircularCount()
    {
        long result = 0;
        for(Worker w : workers) { result += w.getCircularCount(); }
        return result;
    }

    public long getMaximalCount()
    {
        long result = 0;
        for(Worker w : workers) { result += w.getMaximalCount(); }
        return result;
    }

    public long getGenerated()
    {
        long result = 0;
        for(Worker w : workers) { result += w.getGenerated(); }
        return result;
    }

    public long getCount()
    {
        long result = 0;
        for(Worker w : workers) { result += w.getCount(); }
        return result;
    }

    public void start()
    { for(Worker w : workers) { w.start(); } }

    public void stop()
    { for(Worker w : workers) { w.stop(); } }

    public void join() throws InterruptedException
    { for(Worker w : workers) { w.join(); } }

    public File getOutputFile() { return outputFile; }

    /*
     * Interface de rappel pour le dispatcher : progession
     * ************************************************************************
     */
    public interface Progress
    {
        public static Progress devNull = new Progress() {

            @Override
            public void setValue(String subject, float value) {
            }
        };
        /** Envoie le taux d'avancement [0..1]. */
        public void setValue(String subject, float value);
    }

    /*
     * Interface de rappel pour le dispatcher : fin du calcul
     * ************************************************************************
     */
    public interface Finish
    {
        /** Appel de fin de calcul. */
        public void call(Dispatcher dispatcher);
    }

    /*
     * Interface définissant les objets acceptés par le dispatcher :
     * ************************************************************************
     */
    public interface Worker
    {
        /**
         * Mise en place des bornes pour la prochaine partition ainsi
         * que le fichier de sortie.
         * Si endIndex < startIndex, alors stop.
         */
        public void set(long startIndex, long endIndex, OutputFile file);

        /** @return Nombre de codes circulaires trouvés par ce travailleur. */
        public long getCircularCount();

        /**
         * @return Nombre de codes circulaires maximaux trouvés par ce
         *         travailleur.
         */
        public long getMaximalCount();

        /** @return Nombre de code générés par ce travailleur. */
        public long getGenerated();

        /** @return Nombre de codes «traités» par ce travailleur. */
        public long getCount();

        /** Commence le calcul. */
        public void start();

        /** Arrête le calcul. */
        public void stop();

        /** Attends la fin du calcul et quitte le worker. */
        public void join() throws InterruptedException;
    }
}