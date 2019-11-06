/* 
 * Author: Pratistha Shrestha
 * Assignment: Project 2 for 4337 01
 * Due Date: 2/22/2019
 * Purpose: Using Java Monitor for Synchronization
 */
 package prog2;

import java.util.Date;
import java.util.Random;
import java.util.ArrayList;

public class ManagerMonitor {

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

	// Monitor object named monitor
	private final Object monitor = new Object();

	public static void main(String[] args) {
		new ManagerMonitor().go();

	}

	private void go() {
		// Create the celebrity thread
		Celebrity c = new Celebrity();
		new Thread(c, "Celebrity").start();

		// Continually generate new fans
		int i = 0;
		while (true) {

			new Thread(new Fan(), "Fan " + i++).start();

			try {
				Thread.sleep(rndGen.nextInt(MAX_TIME_IN_BETWEEN_ARRIVALS));
			} catch (InterruptedException e) {
				System.err.println(e.toString());
				System.exit(1);
			}
		}

	}

	class Celebrity implements Runnable {
		@Override
		public void run() {
			/*
			 * Celebrity goes to sleep
			 * As there are no fans initially
			 */
			try {
				Thread.sleep(8000);
			} catch (InterruptedException e) {
				System.err.println(e.toString());
				System.exit(1);
			}

			while (true) {
				// Stall Celebrity if there are not enough fans in line so he wont flip out.
				synchronized (monitor) {
					// Maintain the min no of fans in line so that Celebrity wont flip out
					if (numFansInLine < MIN_FANS) {
						try {
							//Stalling Celebrity so he wont flip out due to less number of fans in line
							//waiting on the monitor object
							monitor.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch monitor
							e.printStackTrace();
						}

					}
				}
				// Check to see if celebrity flips out
				checkCelebrityOK();

				// Take picture with fans

				System.out.println("Celebrity takes a picture with fans");

				synchronized (monitor) { // line is a shared resource
					// Remove the fans from the line
					for (int i = 0; i < MIN_FANS; i++) {

						System.out.println(line.remove(0).getName() + ": OMG! Thank you!");

					}
				}
				// Making sure numFansInLine Write is accessed by only one thread
				synchronized (monitor) {
		
					// Adjust the numFans variable
					numFansInLine -= MIN_FANS;

					// Allowing fans to enter the line
					if (numFansInLine < MAX_ALLOWED_IN_QUEUE) {
						monitor.notifyAll();
					}
				}
				// Take a break
				try {
					Thread.sleep(rndGen.nextInt(MAX_BREAK_TIME));
				} catch (InterruptedException e) {
					System.err.println(e.toString());
					System.exit(1);
				}
			}

		}

	}

	public void checkCelebrityOK() {
		// Making sure numFansInLine is accessed by only one thread
		synchronized (monitor) {
			if (numFansInLine > MAX_ALLOWED_IN_QUEUE) {
				System.err.println("Celebrity becomes claustrophobic and flips out");
				System.exit(1);
			}

			if (numFansInLine < MIN_FANS) {
				System.err.println("Celebrity becomes enraged that he was woken from nap for too few fans");
				System.exit(1);
			}
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
			// Does not let any fan get in line once max is reached. Enjoy the Exhibit for a
			// while :)
			synchronized (monitor) {
				// Maintain the max no of fans in line so that Celebrity wont flip out
				//While loop makes sure Not all waiting fans gets in line
				while (numFansInLine == MAX_ALLOWED_IN_QUEUE) {
					try {
						monitor.wait();
						if (numFansInLine < MAX_ALLOWED_IN_QUEUE) {
							break;
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch monitor
						e.printStackTrace();
					}
				}

				// Get in line
				System.out.println(Thread.currentThread() + ": gets in line");

				// Making sure that line and numFansInLine is accessed by only one thread at a
				// time
				line.add(0, this);
				numFansInLine++;

				// Stop Stalling Celebrity if there are more than 3 fans in line
				if (numFansInLine > MIN_FANS) {
					monitor.notify();
				}
			}

		}

	}
}
