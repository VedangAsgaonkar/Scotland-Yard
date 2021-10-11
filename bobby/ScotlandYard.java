package bobby;

import java.net.*;
import java.io.*;
import java.util.*;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ArrayBlockingQueue;

// import jdk.internal.module.IllegalAccessLogger.Mode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScotlandYard implements Runnable {

	/*
	 * this is a wrapper class for the game. It just loops, and runs game after game
	 */

	public int port;
	public int gamenumber;

	public ScotlandYard(int port) {
		this.port = port;
		this.gamenumber = 0;
	}

	public void run() {
		while (true) {
			Thread tau = new Thread(new ScotlandYardGame(this.port, this.gamenumber));
			tau.start();
			
			try {
				tau.join();
			} catch (InterruptedException e) {
				return;
			}
			this.gamenumber++;
		}
	}

	public class ScotlandYardGame implements Runnable {
		private Board board;
		private ServerSocket server;
		public int port;
		public int gamenumber;
		// private ExecutorService threadPool;
		private ThreadPoolExecutor threadPool;

		public ScotlandYardGame(int port, int gamenumber) {
			this.port = port;
			this.board = new Board();
			this.gamenumber = gamenumber;
			try {				
				this.server = new ServerSocket(port);
				System.out.println(String.format("Game %d:%d on", port, gamenumber));
				server.setSoTimeout(1000);
			} catch (IOException i) {
				
				return;
			}
			this.threadPool = (ThreadPoolExecutor)Executors.newFixedThreadPool(10);
			// this.threadPool = new ThreadPoolExecutor(10, 10, 1, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(15));
			

		}

		public void run() {

			try {

				// INITIALISATION: get the game going

				Socket socket = null;
				boolean fugitiveIn = false;
				board.dead = false;
				// Thread detectives[];
				// detectives = new Thread[5];
				// int index = 0;

				/*
				 * listen for a client to play fugitive, and spawn the moderator.
				 * 
				 * here, it is actually ok to edit this.board.dead, because the game hasn't
				 * begun
				 */
				do {
					try {
						socket = server.accept();
						fugitiveIn = true;
					} catch (SocketTimeoutException ex){
						continue;
					}
					// dekhte

				} while (!fugitiveIn);

				System.out.println(this.gamenumber);

				// Spawn a thread to run the Fugitive
				board.threadInfoProtector.acquire();
				Thread fugitiveThread = new Thread(new ServerThread(board, -1, socket, port, gamenumber));
				board.totalThreads ++ ;
				
				threadPool.execute(fugitiveThread);
				
				board.threadInfoProtector.release();

				// Spawn the moderator
				Thread moderator = new Thread(new Moderator(board));
				threadPool.execute(moderator);
				// moderator.start();


				while (true) {
					/*
					 * listen on the server, accept connections if there is a timeout, check that
					 * the game is still going on, and then listen again!
					 */

					try {
						socket = server.accept();
					} catch (SocketTimeoutException t) {
						if (board.dead) {
							break;
						}
						continue;
					}

					/*
					 * acquire thread info lock, and decide whether you can serve the connection at
					 * this moment,
					 * 
					 * if you can't, drop connection (game full, game dead), continue, or break.
					 * 
					 * if you can, spawn a thread, assign an ID, increment the totalThreads
					 * 
					 * don't forget to release lock when done!
					 */

					board.threadInfoProtector.acquire();
					if (board.dead) {
						break;
					}
					if (board.totalThreads == 6) {
						continue;
					}
					threadPool.execute(new Thread(new ServerThread(board, board.getAvailableID(), socket, port, gamenumber)));
					// detectives[index] = (new Thread(new ServerThread(board, board.getAvailableID(), socket, port, gamenumber)));
					// index ++;
					// board.installPlayer(board.getAvailableID());
					board.totalThreads++;
					board.threadInfoProtector.release();

				}

				/*
				 * reap the moderator thread, close the server,
				 * 
				 * kill threadPool (Careless Whispers BGM stops)
				 */
				// fugitiveThread.join();
				// for(int i=0 ; i<index; i++){
				// 	detectives[i].join();
				// }
				moderator.join();
				server.close();
				threadPool.shutdownNow();
				// threadPool.awaitTermination(10, TimeUnit.SECONDS);
				

				System.out.println(String.format("Game %d:%d Over", this.port, this.gamenumber));
				return;
			} catch (InterruptedException ex) {
				System.err.println("An InterruptedException was caught: " + ex.getMessage());
				ex.printStackTrace();
				return;
			} catch (IOException i) {
				return;
			}

		}

	}

	public static void main(String[] args) {
		for (int i = 0; i < args.length; i++) {
			int port = Integer.parseInt(args[i]);
			Thread tau = new Thread(new ScotlandYard(port));
			tau.start();
		}
	}
}