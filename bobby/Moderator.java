package bobby;

import java.net.*;
import java.io.*;
import java.util.*;

import java.util.concurrent.Semaphore;

public class Moderator implements Runnable{
	private Board board;
	
	public Moderator(Board board){
		this.board = board;
	}

	public void run(){
		while (true){
			try{
				/*acquire permits: 
				
				1) the moderator itself needs a permit to run, see Board
				2) one needs a permit to modify thread info

				*/

				board.moderatorEnabler.acquire();
				board.threadInfoProtector.acquire();
                         
                            


				/* 
				look at the thread info, and decide how many threads can be 
				permitted to play next round

				playingThreads: how many began last round
				quitThreads: how many quit in the last round
				totalThreads: how many are ready to play next round

				RECALL the invariant mentioned in Board.java

				T = P - Q + N

				P - Q is guaranteed to be non-negative.
				*/

				//base case
				
				if (this.board.embryo){
					board.registration.release();
					board.playingThreads = 1;
					board.threadInfoProtector.release();                                                
					continue;
				}
				
				//find out how many newbies
				int newbies = board.totalThreads-board.playingThreads+board.quitThreads;

				// System.out.println("+++++++++++++");
				// System.out.println("Total threads : " + board.totalThreads);
				// System.out.println("Playing threads : " + board.playingThreads);
				// System.out.println("Quit threads : " + board.quitThreads);
				// System.out.println("Newbie threads : " + newbies);
				// System.out.println("+++++++++++++");

				// -1 3 
				


				/*
				If there are no threads at all, it means Game Over, and there are no 
				more new threads to "reap". dead has been set to true, then 
				the server won't spawn any more threads when it gets the lock.

				Thus, the moderator's job will be done, and this thread can terminate.
				As good practice, we will release the "lock" we held. 
				*/
				if(board.totalThreads==0)
				{
					board.threadInfoProtector.release();
					return;
				}                                         
           
				/* 
				If we have come so far, the game is afoot.

				totalThreads is accurate. 
				Correct playingThreads
				reset quitThreads


				Release permits for threads to play, and the permit to modify thread info
				*/
				board.playingThreads = board.totalThreads;
				board.quitThreads = 0;
				board.registration.release(newbies);
				board.reentry.release(board.playingThreads);
				board.threadInfoProtector.release();
				
				                                                    
                               
				
                                             
                                                          
                                             
			}
			catch (InterruptedException ex){
				System.err.println("An InterruptedException was caught: " + ex.getMessage());
				ex.printStackTrace();
				return;
			}
		}
	}
}