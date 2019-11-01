# TriNecklace
A necklace algorithm to determine the growth function of trinucleotide circular codes

# About

Supporting software for the paper `A necklace algorithm to determine the growth function of trinucleotide circular codes`, openly published and available here: http://www.scienpress.com/journal_focus.asp?Main_Id=57&Sub_id=IV&Issue=859.
When refering to this software, please cite

```bibtex
@Article{HMZ13,
 author = {Herrmann, M. and Michel, C. and Zugmeyer, B.},
 title = {A necklace algorithm to determine the growth function of trinucleotide circular codes},
 journal = {Journal of Applied Mathematics and Bioinformatics},
 volume = {3},
 pages = {1-40},
 year = {2013},
}
```

# How to use

This is an eclipse project. Import the project and launch `gui.Main`.
You will be presented with a GUI with several parameters:
*  Lenght: count the number of trinucleotide circular codes for that length
*  Maximal: only count maximal code
*  Threads: number of threads to launch
*  Partition number: number of jobs shared among all threads (they pick one job at a time)
*  Output file: save the results in a file
