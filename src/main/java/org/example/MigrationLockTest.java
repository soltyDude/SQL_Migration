package org.example;

public class MigrationLockTest {
    public static void main(String[] args) throws InterruptedException {
        // Ensure the lock table exists
        MigrationLock.ensureLockTableExists();

        // Create two threads simulating two users
        Thread user1 = new Thread(() -> runMigration("User1"));
        Thread user2 = new Thread(() -> runMigration("User2"));


        // Start the first thread immediately
        user1.start();

        // Delay the start of user2 by 10 seconds
        Thread.sleep(70000); // Wait for 10 seconds before starting user2
        user2.start();




        // Wait for threads to finish
        try {
            user1.join();
            user2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Test completed.");
    }

    private static void runMigration(String username) {
        System.out.println(username + " attempting to acquire lock...");
        if (MigrationLock.acquireLock(username)) {
            try {
                System.out.println(username + " acquired the lock. Running migration...");
                // Simulate migration running
                Thread.sleep(80000); // Simulate some work
                System.out.println(username + " finished migration.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                MigrationLock.releaseLock();
                System.out.println(username + " released the lock.");
            }
        } else {
            System.out.println(username + " could not acquire lock. Migration already in progress.");
        }
    }
}

