package logic;

import java.io.IOException;

/**
 * This class provides :
 *  Trinucleotide representation over interger :
 *  It stores the 3 letters, the class and the permutation :
 *  2 bits per letter + 2 bit for the permutation + 5 bits for the class :
 *  13 bits.
 *  
 *  The table of trinucleotides, 20 classes of 3 permutations.
 */

public class Trinucleotide
{

    /* Masks definition : */
    public static final int MASK_WORD        = 0X3F;   // 00 00 00 00 11 11 11 = 63
    public static final int MASK_LETTER1     = 0X30;   // 00 00 00 00 11 00 00 = 48
    public static final int MASK_LETTER2     = 0XC;    // 00 00 00 00 00 11 00 = 12
    public static final int MASK_LETTER3     = 0X3;    // 00 00 00 00 00 00 11 = 3
    public static final int MASK_DILETTER12  = 0X3C;   // 00 00 00 00 11 11 00 = 60 // Préfixe
    public static final int MASK_DILETTER23  = 0XF;    // 00 00 00 00 00 11 11 = 15 // Suffixe
    public static final int MASK_PERMUTATION = 0XC0;   // 00 00 00 11 00 00 00 = 192
    public static final int MASK_CLASS       = 0X1F00; // 01 11 11 00 00 00 00 = 7936

    /* Letters definition : A, C, G and T */
    public static final int LETTRE_A          = 0X0;    // 00 = 0
    public static final int LETTRE_C          = 0X1;    // 01 = 1
    public static final int LETTRE_G          = 0X2;    // 10 = 2
    public static final int LETTRE_T          = 0X3;    // 11 = 3

    /** The NULL trinucleotide, with class and permutation higher
     * than other trinucleotide.
     */
    public static final int NULL              = 0XFFFFFFFF;

    /**
     * Constructor.
     * @param s Trinucleotide's 3 letters among 'A', 'C', 'G' and 'T'
     * @param class_ Trinucleotide's class number
     * @param permutation Trinucleotide's permutation number
     * @return The integer representing the trinucleotide.
     */
    static public int creer(String s,  int class_, int permutation)
    {
        int trinucleotide = 0; 
        s.toUpperCase();

        for (int i = 0; i < 3; ++i)
        {
            trinucleotide = trinucleotide << 2;
            switch (s.charAt(i))
            {
                case 'A': trinucleotide = trinucleotide | LETTRE_A; break;
                case 'C': trinucleotide = trinucleotide | LETTRE_C; break;
                case 'G': trinucleotide = trinucleotide | LETTRE_G; break;
                case 'T': trinucleotide = trinucleotide | LETTRE_T; break;
            }
        }

        trinucleotide = setClass(trinucleotide, class_);
        trinucleotide = setPermutation(trinucleotide, permutation);
        return trinucleotide;
    }

    /**
     * Constructor that produce a trinucleotide with 0 as permutation number 
     * @param s Trinucleotide's letter.
     * @param class_ Trinucleotide's class.
     * @return The integer representing the trinucleotide.
     * @see getPermut1
     * @see getPermut2
     */
    static public int creerPermut0(String s, int class_)
    { return creer(s, class_, 0); }

    /*
     * Statics accesses to letters :
     *************************************************************************/

    public static int getLettre1(int mot)
    { return (mot & MASK_LETTER1) >> 4; }

    public static int setLettre3(int mot, int val)
    { return (mot & ~MASK_LETTER3) | val; }

    /*
     * Statics accesses to diletters = prefix and suffix :
     *************************************************************************/

    public static int getPrefixe(int word)
    { return (word & MASK_DILETTER12) >> 2; }

    public static int getSuffixe(int word)
    { return (word & MASK_DILETTER23); }

    public static int setPrefixe(int word, int prefix)
    { return (word & ~MASK_DILETTER12) | prefix << 2; }

    /*
     * Statics accesses to word, class and permutation :
     *************************************************************************/

    public static int getClass(int word)
    { return (word & MASK_CLASS) >> 8; }

    public static int getPermutation(int word)
    { return (word & MASK_PERMUTATION) >> 6; }

    public static int getWord(int word)
    { return (word & MASK_WORD); }

    public static int setClass(int mot, int class_)
    { return (mot & ~MASK_CLASS) | class_ << 8; }

    public static int setPermutation(int mot, int permutation)
    { return (mot & ~MASK_PERMUTATION) | permutation << 6; }

    /*
     * Statics accesses to permutation of a trinucleotide :
     *************************************************************************/

    /**
     * Get the first permutation of the trinucleotide "word".
     * It is supposed that "word" has permutation 0.
     * The class of the new permutation is unchanged,
     * its permutation is set to 1.
     */
    public static int getPermut1(int word)
    {   // L1L2L3  --> L2L3  L1             Décalage L2L3                      Décalage L1
        word = (word & ~MASK_WORD) | ((word & MASK_DILETTER23) << 2) | ((word & MASK_LETTER1) >> 4);
        word = setPermutation(word, 1);
        return word;
    }

    /**
     * Get the second permutation of the trinucleotide "word".
     * It is supposed that "word" has permutation 0.
     * The class of the new permutation is unchanged,
     * its permutation is set to 2.
     */
    public static int getPermut2(int word)
    {   // L1L2L3  --> L3  L1L2             Décalage L1L2                      Décalage L3
        word = (word & ~MASK_WORD) | ((word & MASK_DILETTER12) >> 2) | ((word & MASK_LETTER3) << 4);
        word = setPermutation(word, 2);
        return word;
    }

    /**
     * The trinucleotides table.
     */
    private static int[][] table = new int[20][3];
    static
    {
        // Trinucléotides de rang 0 :
        table[0][0] = creerPermut0("AAC", 0);
        table[1][0] = creerPermut0("AAG", 1);
        table[2][0] = creerPermut0("AAT", 2);

        table[3][0] = creerPermut0("ACC", 3);
        table[4][0] = creerPermut0("ACG", 4);
        table[5][0] = creerPermut0("ACT", 5);

        table[6][0] = creerPermut0("AGC", 6);
        table[7][0] = creerPermut0("AGG", 7);
        table[8][0] = creerPermut0("AGT", 8);

        table[9][0] = creerPermut0("ATC", 9);
        table[10][0] = creerPermut0("ATG", 10);
        table[11][0] = creerPermut0("ATT", 11);

        table[12][0] = creerPermut0("CCG", 12);
        table[13][0] = creerPermut0("CCT", 13);

        table[14][0] = creerPermut0("CGG", 14);
        table[15][0] = creerPermut0("CGT", 15);

        table[16][0] = creerPermut0("CTG", 16);
        table[17][0] = creerPermut0("CTT", 17);

        table[18][0] = creerPermut0("GGT", 18);
        table[19][0] = creerPermut0("GTT", 19);

        // Permutations P¹ et P² :
        for (int i = 0; i < 20; ++i)
        {
            table[i][1] = getPermut1(table[i][0]);
            table[i][2] = getPermut2(table[i][0]);
        }
    }

    /**
     * Get a trinucleotide from the table with its class and permutation
     */
    public static int get(int class_, int permutation)
    { return table[class_][permutation]; }

    /**
     * Get the trinucleotide from the table that follow "word".
     * If "word" is the last, the NULL trinucleotide is returned. 
     */
    public static int next(int word)
    {
        if (getPermutation(word) == 2) // Passer à la classe suivante
        { return nextClasse(word);}
        else // Passer à la permutation suivante
        { return table[getClass(word)][getPermutation(word) + 1]; }
    }

    /**
     * Get the first trinucleotide of the class after the word's one.
     * If word has the higher class, the NULL trinucleotide is returned.
     */
    public static int nextClasse(int elem)
    {
        int classe = getClass(elem) + 1;
        if (classe < 20) { return table[classe][0]; }
        else { return NULL; }
    }

    /**
     * Comparison methode :
     */
    public static boolean lower (int t1, int t2)
    {
        return (
                getClass(t1) < getClass(t2)
                ||
                (getClass(t1) == getClass(t2)
                 && getPermutation(t1) < getPermutation(t2)
                )
               );
    }

    /**
     * Printing method.
     */
    public static void print(int word, Appendable out) throws IOException
    {
        for (int i = 0; i < 3; ++i)
        {
            switch ( (word & MASK_LETTER1) >> 4 )
            {
                case LETTRE_A: out.append('A'); break;
                case LETTRE_C: out.append('C'); break;
                case LETTRE_G: out.append('G'); break;
                case LETTRE_T: out.append('T'); break;
            }
            word = word << 2;
        }
    }

    /**
     * Quick print method on System.out.
     */
    public static void print(int word)
    {
        try { print(word, System.out); }
        catch(IOException e) { e.printStackTrace(); }
    }
}
