package logic;

import java.io.IOException;
import java.util.Arrays;

import logic.Trinucleotide;

public class Code
{
    /*
     * Class's data :
     * ***********************************************************************/

    /** Stack of trinucleotides : size = length+1 (+1 is for maximal test). */
    private int[]     stack;
    /** Array for constant time prefixes tests : size = 4*4 = 16. */
    private int[]     prefixes;
    /** Array for constant time trinucleotides tests : size = 4*4*4 = 64. */
    private int[]     items;
    /** Matrix for testing codes : size=[length+1]^2(+1 is for max test). */
    private int[][]   matrix;
    /** Current index in the stack ; empty stack -> index = -1. */
    private int       index;
    /** Length of the code. */
    private final int length;

    /*
     * Méthode de construction :
     * ***********************************************************************/

    /** Constructor : take as parameter the length of the code. */
    public Code(int length)
    {
        this.length = length;
        // Set up the stack :
        this.stack = new int[length + 1]; // +1 pour code maximaux.
        index = -1;
        // Allocate memory (set up to 0 by default)
        prefixes = new int[16];
        items = new int[64];
        matrix = new int[length + 1][length + 1];
    }

    /**
     * Set the code with appropriate trinucleotides, base on an index. It also
     * complete the matrix used for circularity tests.
     */
    public void makeAt(long index_number)
    {
        // Valeurs temporaires :
        long casPossibles = 0;
        int etape;
        int classe = 0;
        int rang = 0;
        long coef = (long) Math.pow(3, length);

        // Réinitialisation :
        clear();

        // Pile à construire à partir du premier étage :
        // Autant de fois que la longueur du code.
        for(etape = 1 ; etape <= length ; ++etape)
        {
            // 1) Identifier la classe du prochain trinucléotide :
            // Les codes sont construits dans un ordre dicté par la table
            // présente dans la classe Trinucléotide ;
            // Pour chaque classe (ligne) supposée comme étage courant
            // de la pile, il reste (longueurCode-etape) parmis (20-1-classe)
            // cas possibles :
            // -> -1 : ne pas prendre en compte la classe courante
            // -> -classe : ne pas prendre en compte les classes passées
            // Si l'index est plus grand que le nombre de cas possibles,
            // c'est que la classe courante n'est pas la bonne,
            casPossibles = combinaison(length - etape, 19 - classe);
            casPossibles *= coef;

            // < index_number+1 -> casPossibles = quantité [1..], index [0..]
            while(casPossibles <= index_number)
            {
                // Changement de classe + élimination des cas de la classe
                // courante
                ++classe;
                index_number -= casPossibles;
                // Réévalutation :
                casPossibles = combinaison(length - etape, 19 - classe);
                casPossibles *= coef;
            }

            // 2) On a la bonne classe, il faut trouver le rang :
            // L'index est « ajusté » (les éléments précédents sont coupés)
            // et on a dans « casPossibles » les 3 rangs de la classe courante
            // ayant chacun autant de possibilités :
            // Obtenir le nombre de cas possibles par rang :
            casPossibles /= 3;
            // Trouver dans quel rang se situe l'index :
            // Note : casPossibles [1..N] et index[0..N-1] -> rang [0,1,2]
            rang = (int) (index_number / casPossibles);

            // 3) Création du code :
            push(Trinucleotide.get(classe, rang));
            buildMatrix();

            // 4) Ajustements pour la prochaine étape :
            // Enlever les rangs dépassés.
            index_number -= rang * casPossibles;
            // Prochaine classe est forcément après :
            ++classe;
            // Ajustement du coefficient : une classe de possibilité en moins.
            coef /= 3;
        }
    }

    /*
     * Méthode d'accès :
     * ***********************************************************************/

    public int top() { return stack[index]; }

    public int item(int idx) { return stack[idx]; }

    public int length() { return index+1; }

    public int maxLength() { return length; }

    /*
     * Méthode de manipulation du code :
     * ***********************************************************************/

    /**
     * Push a new trinucleotide on the stack and modifies testing array.
     */
    public void push(int elem)
    {
        ++index;
        stack[index] = elem;
        ++items[Trinucleotide.getWord(elem)];
        ++prefixes[Trinucleotide.getPrefixe(elem)];
    }

    /**
     * Pop the trinucleotide of the top of the stack and modifiers testing
     * array.
     * 
     * @return The trinucleotide of the top of the stack.
     */
    public int pop()
    {
        int elem = stack[index];
        --index;
        --items[Trinucleotide.getWord(elem)];
        --prefixes[Trinucleotide.getPrefixe(elem)];
        return elem;
    }

    /**
     * Pop all item from the top while current item is at maximal class and
     * rank. The top item is always pop.
     *
     * @return last item item pop.
     */
    public int fullPop()
    {
        int last, endClasses = 20;
        do
        {
            last = pop();
            --endClasses;
        }while( Trinucleotide.getClass(last) == endClasses 
                && Trinucleotide.getPermutation(last) == 2 && (index != -1) );
        return last;
    }

    /**
     * Clear the code and testing arrays.
     */
    public void clear()
    {
        index = -1;
        int i;
        for(i = 0 ; i < 64 ; ++i) { items[i] = 0; }
        for(i = 0 ; i < 16 ; ++i) { prefixes[i] = 0; }
    }

    /*
     * Méthode de tests basiques :
     * **********************************************************************
     */

    public boolean empty() { return index == -1; }

    public boolean has(int elem) { return items[elem] != 0; }

    public boolean hasPrefix(int elem) { return prefixes[elem] != 0; }

    public boolean hasClass(int class_)
    {
        for(int i = 0 ; i < length ; ++i)
        { if(class_ == Trinucleotide.getClass(stack[i])) return true; }
        return false;
    }

    public boolean lowerEqual(Code ref)
    {
        // Sélection de la plus petite longueur :
        int top = length < ref.length ? length : ref.length;

        // Comparaison des éléments :
        for(int i=0; i<top; ++i)
        {
            if( Trinucleotide.lower(stack[i], ref.stack[i]) ){return true;}
            if( Trinucleotide.lower(ref.stack[i], stack[i]) ){return false;}
        }
        // Retour :
        return true;
    }

    /*
     * Méthode de constructions avancées et test de circularité :
     * ***********************************************************************
     */

    /**
     * Uses the internal matrix for testing circularity of the code. Note :
     * values on diagonal should be ignored, but as the matrix is initialized
     * with 0, its like AAA and AAA is never in the code, so it is ok without
     * adding more test.
     */
    public boolean testCircularity()
    {
        int index1, index2, index3, index4;

        // Construire la matrice :
        buildMatrix();

        // Amorce : inutile de regarder le dernier, si il y'a un collier avec,
        // on le trouve avant --> comparaison " < "
        for(index1 = 0 ; index1 < index ; ++index1)
        { // Regarder si le code contient un préfixe P1, le suffixe de l'élément
            // courant.
            if(hasPrefix(Trinucleotide.getSuffixe(item(index1))))
            { // Oui : potentiellement un collier !
                // Boucle 3-collier
                for(index2 = index1 + 1 ; index2 <= index ; ++index2)
                { // Vérifier si le code contient un élément de la matrice ayant
                    // le préfixe P1
                    // Ainsi qu'un préfixe P2, suffixe du 2eme élément courant.
                    if(has(matrix[index1][index2]) && hasPrefix(Trinucleotide.getSuffixe(item(index2))))
                    { // Oui : Test 3 collier :
                        if(has(matrix[index2][index1])) return false;
                        // Si pas de 3 collier, tester 4 collier :
                        // Boucle 4-collier
                        for(index3 = index1 + 1 ; index3 <= index ; ++index3)
                        { // Vérifier si le code contient un élément de la
                            // matrice ayant le préfixe P2
                            // Ainsi qu'un préfixe P3, suffixe du 3eme élément
                            // courant
                            if(has(matrix[index2][index3]) && hasPrefix(Trinucleotide.getSuffixe(item(index3))))
                            { // Oui ; Test 4 collier :
                                if(has(matrix[index3][index1])) return false;
                                // Si pas de 4 collier, tester 5 collier :
                                // Boucle 5 collier
                                for(index4 = index1 + 1 ; index4 <= index ; ++index4)
                                { // Test 5 collier
                                    if(has(matrix[index3][index4]) && has(matrix[index4][index1])) return false;
                                }// Fin for index 4
                            }// Fin if index 3
                        }// Fin for index 3
                    }// Fin if index2
                }// Fin for index 2
            }// Fin if index1
        }// Fin for index 1
        return true;
    }

    /**
     * Test if a trinucleotide code is maximal.
     * Use the internal matrix.
     */
    public boolean testMax()
    {
        // Ce test n'a de sens que si «code» a déjà été testé et est circulaire.
        // Déclarations :
        int classe;
        int rang;

        // Parcourir les classes :
        for (classe = 0; classe < 20; ++classe)
        {
            // Tester les classes non présentes dans le code :
            if( !hasClass(classe) )
            {
                // Tester les rangs de la classe :
                for (rang = 0; rang < 3; ++rang)
                {
                    // Ajouter le code :
                    push(Trinucleotide.get(classe, rang));

                    // Tester :
                    if( TestAndBuildMatrix() && testCircularity() )
                    {
                        // Circulaire -> pas maximal
                        // Restaurer le code (dépiler) et renvoyer faux.
                        pop();
                        return false;
                    } else
                    {
                        // Pas circulaire -> dépiler et tester la suite :
                        pop();
                    }
                }// Fin for rang
            }// Fin if classe
        }// Fin for classe

        return true;
    }

    /**
     * Build the matrix used for circularity tests and testes 3-LDCCN. MUST be
     * call after a push, as it is an incremental construction.
     */
    public boolean TestAndBuildMatrix()
    {
        // 1) Construire la matrice :
        buildMatrix();

        // 2) test 3 collier :
        int index1, index2;
        for(index1 = 0 ; index1 < index ; ++index1)
        {
            if(hasPrefix(Trinucleotide.getSuffixe(item(index1))))
            {
                for(index2 = index1 + 1 ; index2 <= index ; ++index2)
                {
                    if(has(matrix[index1][index2]) && has(matrix[index2][index1]))
                        return false;
                }
            }
        }

        return true;
    }

    /**
     * Build the matrix used for circularity tests. MUST be call after each
     * push, as it is an incremental construction.
     */
    public void buildMatrix()
    {
        // Élément de collier pour la nouvelle (dernière) colonne :
        int precedentDernier = Trinucleotide.setLettre3(0, Trinucleotide.getLettre1(top()));

        // Élément de collier pour la nouvelle (dernière) ligne :
        int dernierPrecedent = Trinucleotide.setPrefixe(0, Trinucleotide.getSuffixe(top()));

        for(int idx = 0 ; idx < index ; ++idx)
        {
            matrix[idx][index] = Trinucleotide.setPrefixe(precedentDernier, Trinucleotide.getSuffixe(item(idx)));
            matrix[index][idx] = Trinucleotide.setLettre3(dernierPrecedent, Trinucleotide.getLettre1(item(idx)));
        }
    }

    /*
     * Affichage :
     * ***********************************************************************
     */

    public void print()
    {
        for(int i = 0 ; i <= index ; ++i)
        {
            try
            {
                Trinucleotide.print(stack[i], System.out);
                System.out.print(" ");
            } catch(IOException e) { }
        }
        System.out.println();
    }

    public void print(Appendable out) throws IOException
    {
        int[] stackClone = Arrays.copyOf(stack, index + 1);

        for(int i = 0 ; i <= index ; ++i)
        {
            stackClone[i] &= Trinucleotide.MASK_WORD;
        }

        Arrays.sort(stackClone);

        for (int i = 0; i <= index; ++i)
        {
            if(i > 0) {
                out.append(' ');
            }
            Trinucleotide.print(stackClone[i], out);
        }

    }

    /*
     * Méthode de classe :
     * ***********************************************************************
     */

    /**
     * Calcul des combinaisons k parmis n
     */
    public static long combinaison(int k, int n)
    {
        long resultat = 1;
        for(int i = 0 ; i < k ; i++)
        { resultat = resultat * (n - i) / (i + 1); }
        return resultat;
    }

    /*
     * Autres tests utilisés pour la méthode TestAndBuildMatrix :
     * le 3-LDCCN est, au final, le plus performant et celui en place dane le code.
     * ***********************************************************************
     */

    public boolean Test_4LDCCN_AndBuildMatrix()
    {
        // 1) Construire la matrice :
        buildMatrix();

        // 2) test 4 collier :
        int index1, index2, index3;
        for(index1=0; index1<index; ++index1)
        {
            if( hasPrefix(Trinucleotide.getSuffixe(item(index1))) )
            {
                for( index2=index1+1 ; index2<=index ; ++index2 )
                {
                    if( has(matrix[index1][index2]) && hasPrefix(Trinucleotide.getSuffixe(item(index2))) )
                    {
                        if( has(matrix[index2][index1])) return false;
                        for(index3=index1+1; index3<=index; ++index3)
                        { if( has(matrix[index2][index3]) && has(matrix[index3][index1]) )  return false; }
                    }
                }
            }
        }
        return true;
    }

    public boolean Test_5LDCCN_AndBuildMatrix()
    {
        // 1) Construire la matrice :
        buildMatrix();

        // 2) test 5 collier :
        int index1, index2, index3, index4;
        for(index1=0; index1<index; ++index1)
        {
            if( hasPrefix(Trinucleotide.getSuffixe(item(index1))) )
            {
                for( index2=index1+1 ; index2<=index ; ++index2 )
                {
                    if( has(matrix[index1][index2]) && hasPrefix(Trinucleotide.getSuffixe(item(index2))) )
                    {
                        if( has(matrix[index2][index1])) return false;
                        for(index3=index1+1; index3<=index; ++index3)
                        {
                            if( has(matrix[index2][index3]) && hasPrefix( Trinucleotide.getSuffixe(item(index3))))
                            {
                                if( has(matrix[index3][index1]) )  return false;

                                for(index4=index1+1; index4<=index; ++index4)
                                { if( has(matrix[index3][index4]) && has(matrix[index4][index1])) return false; }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

}
