package util;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

import database.Backup;
import database.DatabaseManager;
import main.PeerMain;

public class Interface{

	private PeerMain peer;
	
	public Interface(PeerMain peer){
		this.peer = peer;
	}
	
	public void run() {
		while(true) {
			System.out.println("input number correspondant to desired action");
			System.out.println("0 - backup file");
			System.out.println("1 - restore file");
			System.out.println("2 - delete file");
			System.out.println("3 - end program");
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
			case 0:{
				Interface.backup(scan, peer);
				break;
			}
			case 1:{
				Interface.restore(scan, peer);
				break;
			}
			case 2:{
				Interface.delete(scan, peer);
				break;
			}
			case 3:{
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
	
	private static void delete(Scanner scan, PeerMain peer) {
		ArrayList<Backup> requests = DatabaseManager.getRequestedBackups(peer.getConnection());
		if (requests.size() > 0) {
			int option = -1;
			do {
				System.out.println("input number correspondant to desired file to restore");
				for (int i = 0; i < requests.size(); i++) {
					System.out.println(i + ". " + requests.get(i).getname() + " - " + requests.get(i).getid());
				}
				try {
					option = scan.nextInt();
				}catch(InputMismatchException e) {
					System.out.println("invalid input");
					scan.nextLine();
				}
			} while(option < 0 || option >=requests.size());
			peer.delete(requests.get(option).getid());
			
		}
	}
	

}
