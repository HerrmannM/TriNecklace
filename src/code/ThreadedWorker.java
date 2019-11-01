package code;

import code.Dispatcher.Worker;
import logic.Counter;

public class ThreadedWorker extends Thread implements Worker
{
	/*
	 * Données :
	 *************************************************************************/
	
	// Objets de travail :
	private Counter    counter;
	private Dispatcher dispatcher;
	
	// Index de partition :
	private long       startIndex;
	private long       endIndex;
	
	// Données à transmettre :
	private long       circularCount;
	private long       maximalCount;
	private long       generated;
	private long       count;
	private OutputFile outputFile;

	/*
	 * Constructeur :
	 *************************************************************************/
	public ThreadedWorker(Dispatcher dispatcher_)
	{
		// Initialisations :
		dispatcher    = dispatcher_;
		circularCount = 0;
		maximalCount  = 0;
		generated     = 0;
		count         = 0;

		counter = new Counter(dispatcher.getLongueur());
	}
	
	/*
	 * Méthode run
	 *************************************************************************/
	@Override
	public void run()
	{
		// Demander une partition de travail :
		dispatcher.nextJob(this);

		while(startIndex<=endIndex)
		{
			counter.count(startIndex, endIndex, outputFile, dispatcher.isCountingMax());
			
			circularCount += counter.getCircularCount();
			maximalCount  += counter.getMaximalCount();
			generated     += counter.getGenerated();
			count         += endIndex - startIndex + 1;
			
			// Demander la prochaine partition de travail :
			dispatcher.nextJob(this);
		}
	}
	
	/*
	 * Méthode interface Dispatcher.worker : 
	 *************************************************************************/
	@Override
	public void set(long startIndex_, long endIndex_, OutputFile outputFile_)
	{
		startIndex = startIndex_;
		endIndex   = endIndex_;
		outputFile = outputFile_;
	}

	@Override
	public long getCircularCount() { return circularCount; }
	
	@Override
	public long getMaximalCount() { return maximalCount; }

	@Override
	public long getGenerated() { return generated; }

	/**
	 * Retourne le nombre de codes testées jusqu'a présent.
	 * count + les éléments générés actuellement.
	 */
	@Override
	public long getCount()
	{ return count + (startIndex<=endIndex ? counter.getGenerated() : 0)  ;}

	/*
	public void stop()
	{ }

	public void join() throws InterruptedException
	{ }*/

}