package logic;


import code.OutputFile;

public class Counter
{
    /*
     * Data :
     *************************************************************************/
    // Test's results :
    private long      circularCount;
    private long      maximalCount;
    private long      generated;

    // Trinucleotide codes boudaries
    private Code      code;
    private Code      upperBound;

    // Code's information
    private final int length;
    private final int baseClassCompare; // Voir count() comment

    /*
     * Constructor :
     *************************************************************************/
    public Counter(int length_)
    {
        // Récupération :
        length = length_;

        // Initialisations :
        generated = 0;
        circularCount = 0;
        maximalCount = 0;

        // Initialisation des codes :
        upperBound = new Code(length);
        code = new Code(length);

        // Base pour comparaisons utilisées dans construction des codes :
        baseClassCompare = 21 - length;
    }

    /*
     * Récupération des données :
     *************************************************************************/

    public long getCircularCount() { return circularCount; }

    public long getMaximalCount() { return maximalCount; }

    public long getGenerated() { return generated; }

    /*
     * Fonctions de test selon les cas.
     *************************************************************************/

    /**
     * Count the trinucleotide circular codes from startIndex to
     * endIndex included. Write the result in output and also count the number
     * of maximal code if set.
     *
     * baseClassCompare = 21 - length : Used to know if a trinucleotide allows
     * to build a code or if its class is to high to finish the code.
     * Ex : length = 5 : maximal class for the FIRST trinucleotide is 20-5 = 15.
     * For all trinucleotides, the classe must be <= 20-5+code.length()
     * that is < 21-length-code.length()
     */
    public void count(long startIndex, long endIndex, OutputFile output, boolean countMax)
    {
        // Génération des bornes :
        upperBound.makeAt(endIndex);
        code.makeAt(startIndex);

        // Réinitialisation des compteurs :
        generated = 0;
        circularCount = 0;
        maximalCount = 0;

        // Trinucleotide temporaire :
        int last = 0;

        do
        {
            // Tester code courant :
            if(code.length() == length)
            {
                ++generated;
                if(code.testCircularity())
                {
                    ++circularCount;
                    if(!countMax)
                    { output.writeCode(code); }
                    if(countMax && code.testMax())
                    {
                        output.writeCode(code);
                        ++maximalCount;
                    }
                }
            }

            // Faire nouveau code :
            last = code.fullPop();
            last = Trinucleotide.next(last);
            if(Trinucleotide.getClass(last) < baseClassCompare + code.length())
            {
                do
                {
                    code.push(last);
                   // code.buildMatrix();
                    last = Trinucleotide.nextClasse(last);
                } while(code.length() < length && code.TestAndBuildMatrix());
            }
           

        } while(code.lowerEqual(upperBound) && !code.empty());
        output.close();
    }

}
