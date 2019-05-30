package util;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

import communication.PeerMain;
import database.Backup;
import database.DBUtils;

public class IOManager{

	private PeerMain peer;
	
	public IOManager(PeerMain peer){
		this.peer = peer;
	}
	
	public void run() {
		while(true) {
			System.out.println("Select a Protocol: ");
			System.out.println("\t0. Terminate Program");
			System.out.println("\t1. Backup File");
			System.out.println("\t2. Restore File");
			System.out.println("\t3. Delete File");
			Scanner scanner = new Scanner(System.in);
			Integer op = null;
			try {
				op = scanner.nextInt();
			}catch(InputMismatchException e) {
				System.out.println("Invalid Input");
				scanner.nextLine();
				continue;
			}
			switch (op) {
			case 0:{
				scanner.close();
				Thread.currentThread().interrupt();
				return;
			}
			case 1:{
				IOManager.backupOption(scanner, peer);
				break;
			}
			case 2:{
				IOManager.restoreOption(scanner, peer);
				break;
			}
			case 3:{
				IOManager.deleteOption(scanner, peer);
				break;
			}
			default: {
				System.out.println("Error: Invalid input!");
				continue;
			}
			}
		}
		
	}
	
	private static void restoreOption(Scanner scanner, PeerMain peer) {
		ArrayList<Backup> allRequests = DBUtils.getBackupsRequested(peer.getConnection());
		if (allRequests.size() > 0) {
			int option = -1;
			do {
				System.out.println("Choose a file to restore:");
				for (int i = 0; i < allRequests.size(); i++) {
					System.out.println(i + ". " + allRequests.get(i).getname() + " -> " + allRequests.get(i).getid());
				}
				try {
					option = scanner.nextInt();
				}catch(InputMismatchException e) {
					System.out.println("Invalid Input");
					scanner.nextLine();
				}
			} while(option < 0 && option >= allRequests.size());
			peer.restore(allRequests.get(option));
		} else {
			System.out.println("You must backup files before restoring");
		}
	}
	
	private static void deleteOption(Scanner scanner, PeerMain peer) {
		ArrayList<Backup> allRequests = DBUtils.getBackupsRequested(peer.getConnection());
		if (allRequests.size() > 0) {
			int option = -1;
			do {
				System.out.println("Choose a file to delete:");
				for (int i = 0; i < allRequests.size(); i++) {
					System.out.println(i + ". " + allRequests.get(i).getname() + " -> " + allRequests.get(i).getid());
				}
				try {
					option = scanner.nextInt();
				}catch(InputMismatchException e) {
					System.out.println("Invalid Input");
					scanner.nextLine();
				}
			} while(option < 0 || option >= allRequests.size());
			peer.delete(allRequests.get(option).getid());
			
		} else {
			System.out.println("You must backup files before deleting");
		}
		

	}

	private static void backupOption(Scanner in, PeerMain peer) {
		System.out.println("FileName:");
		String filename;
		filename = in.next();
		if(!Files.exists(Paths.get(filename))) {
			System.out.println("Error: That file does not exist!");
			return;
		}
		Integer degree = 0;
		do {
			System.out.println("Replication Degree (From 1-8):");
			try {
				degree = in.nextInt();
			}catch(InputMismatchException e) {
				System.out.println("Invalid Input");
				in.nextLine();
			}
		} while((degree < 1) || (degree > 9));
			
		peer.backup(filename, degree,null);
		System.out.println("Called Backup!");
	}
	

}
