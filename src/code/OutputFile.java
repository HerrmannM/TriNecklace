package code;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import logic.Code;

/**
 * Permet d'écrire dans un fichier
 */
public class OutputFile
{

    /** À utiliser si lon ne veut aucune sortie dans un fichier */
    static public final OutputFile devNull = new OutputFile()
    {

        @Override
        public void writeCode(Code code)
        {
            // do nothing
        }

        @Override
        public void close()
        {
            // do nothing
        }
    };
    private final BufferedWriter output;
    private final File file;

    public OutputFile(File file)
    {
        try
        {
            this.file = file;
            output = new BufferedWriter(new FileWriter(file));
        } catch (IOException ex)
        {
            Logger.getLogger(OutputFile.class.getName()).
                    log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    private OutputFile()
    {
        output = null;
        file = null;
    }

    public synchronized void writeCode(Code code)
    {
        try
        {
            code.print(output);
            output.append('\n');
        } catch (IOException ex)
        {
            Logger.getLogger(OutputFile.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
    }

    public void close()
    {
        try
        {
            output.close();
            ExternalSort.sortFile(file);
        } catch (IOException ex)
        {
            Logger.getLogger(OutputFile.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
    }
}
