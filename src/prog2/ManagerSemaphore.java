/* 
 * Author: Pratistha Shrestha
 * Assignment: Project 2 for 4337 01
 * Due Date: 2/22/2019
 * Purpose: Using Semaphores for Synchronization
 */

package prog2;

import java.util.Date;
import java.util.Random;
import java.util.ArrayList;

import java.util.concurrent.*;

public class ManagerSemaphore {

	// Maximum time in between fan arrivals
	private static final int MAX_TIME_IN_BETWEEN_ARRIVALS = 3000;

	// Maximum amount of break time in between celebrity photos
	private static final int MAX_BREAK_TIME = 10000;

	// Maximum amount of time a fan spends in the exhibit
	private static final int MAX_EXHIBIT_TIME = 10000;

	// Minimum number of fans for a photo
	private static final int MIN_FANS = 3;

	// Maximum number of fans allowed in queue
	private static final int MAX_ALLOWED_IN_QUEUE = 10;

	// Holds the queue of fans
	private static ArrayList<Fan> line = new ArrayList<Fan>();

	// The current number of fans in line
	private static int numFansInLine = 0;

	// For generating random times
	private Random rndGen = new Random(new Date().getTime());
	
    /* creating semaphores */
	/*
	 * There are no fans in the beginning and celebrity is
	 * sleeping. So setting the current permit for fans Ready to 
	 * click pictures to zero initially
	 */
	public static Semaphore wakeUpCelebrity = new Semaphore(0);
	/*
	 * This semaphore is to get access to
	 * either adding fans to line 
	 * or removing fans from line.
	 * As we need to add fans to the line in the beginning
	 * the semaphore is created with one permit
	 */
	public static Semaphore changeLine = new Semaphore(1);

	public static void main(String[] args) {
		new ManagerSemaphore().go();
	}

	private void go() {

		// Create the celebrity thread
		Celebrity c = new Celebrity();
		/* Assigning a name to the celebrity thread
		 * So that the celebrity thread can be interrupted from sleep
		 * if there are required number of fans for taking a picture
		 */
		Thread celeb = new Thread(c, "Celebrity");
		celeb.start();

		// Continually generate new fans
		int i = 0;
		while (true) {
			new Thread(new Fan(), "Fan " + i++).start();

			try {
				Thread.sleep(rndGen.nextInt(MAX_TIME_IN_BETWEEN_ARRIVALS));
				// Interrupting Celebrity Thread to wake up
				celeb.interrupt();

			} catch (InterruptedException e) {

				System.err.println(e.toString());
				System.exit(1);
			}
		}
	}

	class Celebrity implements Runnable {
		@Override
		public void run() {
			while (true) {
				try {
					/*
					 * acquireUninterruptibly used to acquire permit
					 * to wake up celebrity as soon as there are enough 
					 * fans in the line
					 */
					wakeUpCelebrity.acquireUninterruptibly();
					
					/*
					 * Acquiring the permit to access the
					 * numFansInLine variable to see if the celebrity
					 * was woken up for the current number of fans
					 * acquireUninterruptibly used so that the celebrity can remove fans
					 * from the line before Fan thread adds more fans to the line
					 */
					changeLine.acquireUninterruptibly();
					
					// Check to see if celebrity flips out
					checkCelebrityOK();
					
				} catch (Exception e) {
					// TODO: handle exception
				}
				// Take picture with fans
				System.out.println("Celebrity takes a picture with fans");

				try {
					// Remove the fans from the line
					for (int i = 0; i < MIN_FANS; i++) {
						System.out.println(line.remove(0).getName() + ": OMG! Thank you!");
					}
					// Adjust the numFans variable
					numFansInLine -= MIN_FANS;
					/*
					 *  Releasing permit once the celebrity has
					 *  taken pictures with the fan 
					 */
					changeLine.release();
				} catch (Exception e) {
					// TODO: handle exception
				}

				// Take a break
				try {
					System.out.println("Celebrity is going to sleep");
					Thread.sleep(rndGen.nextInt(MAX_BREAK_TIME));
				} catch (InterruptedException e) {
					//Waking up celebrity Thread 
					System.out.println("My celebrity has woken up.  ");
				}
			}
		}
	}

	public void checkCelebrityOK() {
		if (numFansInLine > MAX_ALLOWED_IN_QUEUE) {
			System.err.println("Celebrity becomes claustrophobic and flips out");
			System.out.println("Number of Fans in line currently " + numFansInLine);
			System.exit(1);
		}

		if (numFansInLine < MIN_FANS) {
			System.err.println("Celebrity becomes enraged that he was woken from nap for too few fans");
			System.out.println("Number of Fans in line currently " + numFansInLine);
			System.exit(1);
		}
	}

	class Fan implements Runnable {
		String name;

		public String getName() {
			return name;
		}

		@Override
		public void run() {
			// Set the thread name
			name = Thread.currentThread().toString();

			System.out.println(Thread.currentThread() + ": arrives");

			// Look in the exhibit for a little while
			try {
				Thread.sleep(rndGen.nextInt(MAX_EXHIBIT_TIME));
			} catch (InterruptedException e) {
				System.err.println(e.toString());
				System.exit(1);
			}

			try {
				/*
				 * Acquiring the permit to add fans to the line
				 */
				changeLine.acquire();

				line.add(0, this);
				// Get in line
				System.out.println(Thread.currentThread() + ": gets in line");
				numFansInLine++;

				/*
				 * Condition check to see if there are enough fans 
				 * in the line to wake up celebrity
				 */
				if (line.size() > MIN_FANS && line.size() <= MAX_ALLOWED_IN_QUEUE) {

					// Releasing the permit to wake up celebrity
					wakeUpCelebrity.release();
					
					/*
					 * Releasing the permit to change line 
					 * so celebrity can remove fans from line 
					 * as soon is celebrity is awake
					 */
					changeLine.release();
				} else {
					// Releasing permit once fans have been added to line
					changeLine.release();
				}

			} catch (InterruptedException e) {
				// TODO: handle exception
			}

		}

	}
}
