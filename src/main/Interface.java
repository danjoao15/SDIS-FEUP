package main;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

import database.Backup;
import database.DatabaseManager;

public class Interface{

	private PeerMain peer;
	
	public Interface(PeerMain peer){
		this.peer = peer;
	}
	
	public void run() {
		while(true) {
			System.out.println("input number correspondant to desired action");
			System.out.println("1 - backup file");
			System.out.println("2 - restore file");
			System.out.println("0 - end program");
			Scanner scan = new Scanner(System.in);
			Integer option = null;
			try {
				option = scan.nextInt();
			}catch(InputMismatchException e) {
				System.out.println("invalid option");
				scan.nextLine();
				continue;
			}
			switch (option) {
			case 1:{
				Interface.backup(scan, peer);
				break;
			}
			case 2:{
				Interface.restore(scan, peer);
				break;
			}
			case 0:{
				scan.close();
				Thread.currentThread().interrupt();
				return;
			}
			default: {
				System.out.println("please choose a valid option");
				continue;
			}
			}
		}
		
	}

	private static void backup(Scanner scan, PeerMain peer) {
		System.out.println("input file name");
		String name;
		name = scan.next();
		if(!Files.exists(Paths.get(name))) {
			System.out.println("file not found");
			return;
		}
		Integer deg = 0;
		do {
			System.out.println("input desired replication degree");
			try {
				deg = scan.nextInt();
			}catch(InputMismatchException e) {
				System.out.println("invalid input");
				scan.nextLine();
			}
		} while((deg < 1));
			
		peer.backup(name, deg,null);
		System.out.println("backup request sent");
	}
	
	private static void restore(Scanner scan, PeerMain peer) {
		ArrayList<Backup> requests = DatabaseManager.getRequestedBackups(peer.getConnection());
		if (requests.size() > 0) {
			int option = -1;
			do {
				System.out.println("input number correspondant to desired file to restore");
				for (int i = 0; i < requests.size(); i++) {
					System.out.println(i + " - " + requests.get(i).getname() + " (" + requests.get(i).getid()+")");
				}
				try {
					option = scan.nextInt();
				}catch(InputMismatchException e) {
					System.out.println("invalid input");
					scan.nextLine();
				}
			} while(option < 0 && option >= requests.size());
			peer.restore(requests.get(option));
		}
	}
	

}
