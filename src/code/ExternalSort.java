package code;

import java.util.*;
import java.io.*;

/**
 * This file is based on the ExternalSort by (in alphabetical order)
 * Philippe Beaudoin, Jon Elsas, Christan Grant, Daniel Haran, Daniel Lemire,
 * April 2010
 * Originally posted at
 *  http://www.daniel-lemire.com/blog/archives/2010/04/01/external-memory-sorting-in-java/
 *  This software is in the public domain.
 *
 * Goal: offer a generic external-memory sorting program in Java.
 * It must be :
 *  - hackable (easy to adapt)
 *  - scalable to large files
 *  - sensibly efficient.
 */
public class ExternalSort {

    // we divide the file into small blocks. If the blocks
    // are too small, we shall create too many temporary files.
    // If they are too big, we shall be using too much memory.
    public static long estimateBestSizeOfBlocks(File filetobesorted) {
        long sizeoffile = filetobesorted.length();
        // we don't want to open up much more than 1024 temporary files, better run
        // out of memory first. (Even 1024 is stretching it.)
        final int MAXTEMPFILES = 1024;
        long blocksize = sizeoffile / MAXTEMPFILES;
        // on the other hand, we don't want to create many temporary files
        // for naught. If blocksize is smaller than half the free memory, grow it.
        long freemem = Runtime.getRuntime().freeMemory();
        if (blocksize < freemem / 2) {
            blocksize = freemem / 2;
        } else {
            if (blocksize >= freemem) {
                System.err.println("We expect to run out of memory. ");
            }
        }
        return blocksize;
    }

    /**
     * This will simply load the file by blocks of x rows, then
     * sort them in-memory, and write the result to a bunch of
     * temporary files that have to be merged later.
     *
     * @param file some flat  file
     * @return a list of temporary flat files
     */
    public static List<File> sortInBatch(File file, Comparator<String> cmp) throws IOException {
        List<File> files = new ArrayList<File>();
        BufferedReader fbr = new BufferedReader(new FileReader(file));
        long blocksize = estimateBestSizeOfBlocks(file);// in bytes
        try {
            List<String> tmplist = new ArrayList<String>();
            String line = "";
            try {
                while (line != null) {
                    long currentblocksize = 0;// in bytes
                    while ((currentblocksize < blocksize)
                            && ((line = fbr.readLine()) != null)) { // as long as you have 2MB
                        tmplist.add(line);
                        currentblocksize += line.length() * 2; // java uses 16 bits per character?
                    }
                    files.add(sortAndSave(tmplist, cmp));
                    tmplist.clear();
                }
            } catch (EOFException oef) {
                if (tmplist.size() > 0) {
                    files.add(sortAndSave(tmplist, cmp));
                    tmplist.clear();
                }
            }
        } finally {
            fbr.close();
        }
        return files;
    }

    public static File sortAndSave(List<String> tmplist, Comparator<String> cmp) throws IOException {
        Collections.sort(tmplist, cmp);  //
        File newtmpfile = File.createTempFile("sortInBatch", "flatfile");
        newtmpfile.deleteOnExit();
        BufferedWriter fbw = new BufferedWriter(new FileWriter(newtmpfile));
        try {
            for (String r : tmplist) {
                fbw.write(r);
                fbw.newLine();
            }
        } finally {
            fbw.close();
        }
        return newtmpfile;
    }

    /**
     * This merges a bunch of temporary flat files
     * @param files
     * @param output file
     * @param progress callback to print progress
     * @param max number of lines to sort
     * @return The number of lines sorted. (P. Beaudoin)
     */
    public static void mergeSortedFiles(List<File> files, File outputfile,
            final Comparator<String> cmp, Dispatcher.Progress progress, long max) throws IOException {
        PriorityQueue<BinaryFileBuffer> pq = new PriorityQueue<BinaryFileBuffer>(11,
                new Comparator<BinaryFileBuffer>() {

                    public int compare(BinaryFileBuffer i, BinaryFileBuffer j) {
                        return cmp.compare(i.peek(), j.peek());
                    }
                });
        for (File f : files) {
            BinaryFileBuffer bfb = new BinaryFileBuffer(f);
            if(bfb.empty()) {
                bfb.close();
            }
            else {
                pq.add(bfb);
            }
        }
        BufferedWriter fbw = new BufferedWriter(new FileWriter(outputfile));
        float count = 0;
        try {
            while (pq.size() > 0) {
                BinaryFileBuffer bfb = pq.poll();
                String r = bfb.pop();
                fbw.write(r);
                fbw.newLine();

                count++;
                progress.setValue("Sorting files...", count / max);

                if (bfb.empty()) {
                    bfb.close();
                } else {
                    pq.add(bfb); // add it back
                }
            }
        } finally {
            fbw.close();
            count = 0;
            for (BinaryFileBuffer bfb : pq) {
                bfb.close();
                count++;
                progress.setValue("Closing files...", count / pq.size());
            }
        }
    }

    /**
     * Sort a file
     * @param input
     * @throws IOException
     */
    public static void sortFile(File input) throws IOException {
        Comparator<String> comparator = new Comparator<String>() {

            public int compare(String r1, String r2) {
                return r1.compareTo(r2);
            }
        };
        List<File> l = sortInBatch(input, comparator);
        mergeSortedFiles(l, input, comparator, Dispatcher.Progress.devNull, 0);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("please provide input and output file names");
            return;
        }
        String inputfile = args[0];
        String outputfile = args[1];
        Comparator<String> comparator = new Comparator<String>() {

            public int compare(String r1, String r2) {
                return r1.compareTo(r2);
            }
        };
        List<File> l = sortInBatch(new File(inputfile), comparator);
        mergeSortedFiles(l, new File(outputfile), comparator, Dispatcher.Progress.devNull, 0);
    }
}

class BinaryFileBuffer {

    public static int BUFFERSIZE = 2048;
    public BufferedReader fbr;
    public File originalfile;
    private String cache;
    private boolean empty;

    public BinaryFileBuffer(File f) throws IOException {
        originalfile = f;
        fbr = new BufferedReader(new FileReader(f), BUFFERSIZE);
        reload();
    }

    public boolean empty() {
        return empty;
    }

    private void reload() throws IOException {
        try {
            if ((this.cache = fbr.readLine()) == null || cache.isEmpty()) {
                empty = true;
                cache = null;
            } else {
                empty = false;
            }
        } catch (EOFException oef) {
            empty = true;
            cache = null;
        }
    }

    public String peek() {
        if (empty()) {
            return null;
        }
        return cache.toString();
    }

    public String pop() throws IOException {
        String answer = peek();
        reload();
        return answer;
    }

    public void close() throws IOException {
        fbr.close();
        originalfile.delete();
    }
}
