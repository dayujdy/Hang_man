import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class HangManClient extends Thread {
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	private HangManConfiguration hmc = null;
	//private SMessage sm = null;
	//private CMessage cm = null;
	public HangManClient() {
		Socket s = null;
		this.hmc = new HangManConfiguration();
		try {
			System.out.print("Trying to connect to server...");
			s = new Socket(this.hmc.hostname, this.hmc.port);
			System.out.println("Connected!");
			
			this.ois = new ObjectInputStream(s.getInputStream());
			this.oos = new ObjectOutputStream(s.getOutputStream());
			this.start();
			this.sendMessage(this.askLogin());
			
		} catch (IOException ioe) {
			System.out.println("ioe in ChatClient constructor: " + ioe.getMessage());
			try {
				if (s != null) {
					s.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}
	private CMessage askLogin() {
		Scanner scan = new Scanner(System.in);
		System.out.print("Username: ");
		String username = scan.nextLine();
		System.out.print("Password: ");
		String password = scan.nextLine();
		return new CMessage(MessageType.LOGIN, username, password, null, -1, null, null);
	}
	public void sendMessage(CMessage cm) {
		try {
			this.oos.writeObject(cm);
			this.oos.flush();
		} catch (IOException ioe) {
			System.out.println("ioe: " + ioe.getMessage());
		}
		
	}
	@Override
	public void run() {
		Scanner scan = new Scanner(System.in);
		
		while (true) {
			CMessage cm = null;
			SMessage sm = null;
			try {
				sm = (SMessage) ois.readObject();
				if (sm == null) {
					continue;
//					System.out.print("Username: ");
//					String username = scan.nextLine();
//					System.out.print("Password: ");
//					String password = scan.nextLine();
//					
//					cm  = new CMessage(MessageType.LOGIN, username, password, null, -1);
				}
				if (sm.type == MessageType.LOGINNOTFOUND) { // smsg should have username, password
					System.out.println("No account exists with those credentials.");
					System.out.print("Would you like to create a new account? (y/n) ");
					String resp = scan.nextLine();
					//System.out.println(resp);
					if (resp.equals("Yes") || resp.equals("Y") || resp.equals("y") || resp.equals("yes")) {
						System.out.print("Would you like to use the username and password above? (y/n) " );
						resp = scan.nextLine();
						if (resp.equals("Yes") || resp.equals("Y") || resp.equals("y") || resp.equals("yes")) {
							cm = new CMessage(MessageType.CREATEACCT, sm.username, sm.password, null, -1, null, null);
						} else {
							System.out.println("Please try again.");
							cm = this.askLogin();
						}
					} else {
						System.out.println("Alright please enter an existing account then.");
						cm = this.askLogin();
					}
				}
				if (sm.type == MessageType.INCORRECTLOGIN) {
					System.out.println("Account found but incorrect credentials. Try Login Again. ");
					cm = this.askLogin();
				}
				if (sm.type == MessageType.LOGINSUCCESS) {
					System.out.println("");
					System.out.println("Great! You are now logged in as " + sm.username + "!");
					System.out.println("");
					continue;
				}
				
				if (sm.type == MessageType.SHOWUSERSTATS) {
					
					System.out.println(sm.username + "'s record:");
					System.out.println("-----------------------------");
					System.out.println("Wins - " + sm.wins);
					System.out.println("Losses - " + sm.losses);
					continue;
					
				}
				if (sm.type == MessageType.CHOOSEGAMETYPE) {
					System.out.println("1) Start a game");
					System.out.println("2) Join a game"); 
					System.out.print("Would you like to start a game or join a game? ");
					String resp = scan.nextLine();
					System.out.print("What is the name of the game? ");
					String gameName = scan.nextLine();
					if (resp.equals("1")) {
						cm = new CMessage(MessageType.STARTGAME, sm.username, null, gameName, -1, null, null);
					} else if (resp.equals("2")){
						cm = new CMessage(MessageType.JOINGAME, sm.username, null, gameName, -1, null, null);
					} else {
						break;
					}
					
				} 
				if (sm.type == MessageType.INCORRECTNAME) { //join
					System.out.println("There is no game with name " + sm.gameName);
					System.out.println("1) Start a game");
					System.out.println("2) Join a game"); 
					System.out.print("Would you like to start a game or join a game? ");
					String resp = scan.nextLine();
					System.out.print("What is the name of the game? ");
					String gameName = scan.nextLine();
					if (resp.equals("1")) {
						cm = new CMessage(MessageType.STARTGAME, sm.username, null, gameName, -1, null, null);
					} else if (resp.equals("2")){
						cm = new CMessage(MessageType.JOINGAME, sm.username, null, gameName, -1, null, null);
					} else {
						break;
					}
				}
				if (sm.type == MessageType.DUPLICATENAME) {// start
					System.out.println(sm.gameName + " already exists.");
					System.out.println("1) Start a game");
					System.out.println("2) Join a game"); 
					System.out.print("Would you like to start a game or join a game? ");
					String resp = scan.nextLine();
					System.out.print("What is the name of the game? ");
					String gameName = scan.nextLine();
					if (resp.equals("1")) {
						cm = new CMessage(MessageType.STARTGAME, sm.username, null, gameName, -1, null, null);
					} else if (resp.equals("2")){
						cm = new CMessage(MessageType.JOINGAME, sm.username, null, gameName, -1, null, null);
					} else {
						break;
					}
				}
				if (sm.type == MessageType.REQUESTUSERCOUNT) {
					
					while (true) {
						System.out.print("How many users will be playing (1-4)? ");
						String resp = scan.nextLine();
						int num = 0;
						try {  
							num = Integer.parseInt(resp);
						} catch(NumberFormatException nfe) {  
							System.out.println("A game can only have between 1-4 players. ");
						}
					
						if (num>4 || num <1) {
							System.out.println("A game can only have between 1-4 players. ");
						}
					
						else {
							cm = new CMessage(MessageType.RESPONSEUSERCOUNT, sm.username, null, sm.gameName, num, null, null);
							break;
						}
					}
				}
				if (sm.type == MessageType.ROOMFULL) {
					System.out.println("the game " + sm.gameName + " does not have space for another user to join.");
					System.out.println("1) Start a game");
					System.out.println("2) Join a game"); 
					System.out.print("Would you like to start a game or join a game? ");
					String resp = scan.nextLine();
					System.out.print("What is the name of the game? ");
					String gameName = scan.nextLine();
					if (resp.equals("1")) {
						cm = new CMessage(MessageType.STARTGAME, sm.username, null, gameName, -1, null, null);
					} else if (resp.equals("2")){
						cm = new CMessage(MessageType.JOINGAME, sm.username, null, gameName, -1, null, null);
					} else {
						break;
					}
				}
				if (sm.type == MessageType.WAITUNTILMAX) {
					System.out.println("Waiting for "+ sm.leftOver + " others to join...");
					continue;
				}
				if (sm.type == MessageType.USERENTERED) {
					System.out.println(sm.username + " is in the game.");
					continue;
				}
				if (sm.type == MessageType.USERCTCHANGED) {
					System.out.println("Waiting for "+ sm.leftOver + " others to join...");
					continue;
				}
				if (sm.type == MessageType.ROOMFILLED) {
					System.out.println("All users have joined. ");
					continue;
				}
				if (sm.type == MessageType.GAMEBEGIN) {
					System.out.println("Determining secret word...");
					continue;
				}
				if (sm.type == MessageType.CURRSECRETWORD) {
					System.out.println("Secret Word " + sm.currWord);
					System.out.println("You have " + sm.guessRemaining+ " incorrect guesses remaining.");
					continue;
				}
				if (sm.type == MessageType.PLAY){
					while (true){
						System.out.println("1) Guess a letter.");
						System.out.println("2) Guess the word.");
						System.out.print("What would you like to do? ");
						String resp = scan.nextLine();
						if (resp.equals("1")){
							System.out.print("Letter to guess - ");
							String letter = scan.nextLine();
							cm = new CMessage(MessageType.GUESSEDLETTER, sm.username, null, sm.gameName, -1, letter, null);
							break;
						} else if (resp.equals("2")){
							System.out.print("What is the secret word? ");
							String word = scan.nextLine();
							cm = new CMessage(MessageType.GUESSEDWORD, sm.username, null, sm.gameName, -1, null, word);
							break;
						} else {
							System.out.println("That is not a valid option. ");
						}
					}
				} 
				if (sm.type == MessageType.WAITFORPLAY) {
					System.out.println("Waiting for " + sm.username + " to do something...");
					continue;
				}
				if (sm.type == MessageType.GUESSEDCORRECTLYLETTER){
					System.out.println("The letter '" + sm.letter + "' is in the secret word.");
					cm = new CMessage(MessageType.NEXTPERSON, sm.username, null, sm.gameName, -1, null, null);
				}  
				if (sm.type == MessageType.NOTIFYCORRECTNESSLETTER){
					System.out.println(sm.username + " has guessed letter '" + sm.letter + "'");
					System.out.println("The letter '" + sm.letter + "' is in the secret word.");
					continue;
				}
				if (sm.type == MessageType.GUESSEDINCORRECTLYLETTER) {
					System.out.println("The letter '" + sm.letter + "' is not in the secret word.");
					if (sm.guessRemaining <= 0) {
						cm = new CMessage(MessageType.ALLLOSE, sm.username, null, sm.gameName, -1, null, null);
					} else {
						cm = new CMessage(MessageType.NEXTPERSON, sm.username, null, sm.gameName, -1, null, null);
					}
				} 
				if (sm.type == MessageType.NOTIFYINCORRECTNESSLETTER) {
					System.out.println(sm.username + " has guessed letter '" + sm.letter + "'");
					System.out.println("The letter '" + sm.letter + "' is not in the secret word.");
					continue;
				} 
				if (sm.type == MessageType.GUESSEDCORRECTLYWORD){
					System.out.println("That is correct! You win!");
					cm = new CMessage(MessageType.IWIN, sm.username, null, sm.gameName, -1, null, null);
				}  
				if (sm.type == MessageType.NOTIFYCORRECTNESSWORD){
					System.out.println(sm.username + " has guessed the word '" + sm.guessedWord + "'");
					System.out.println(sm.username + " guessed the word correctly. You lose!");
					continue;
				}
				if (sm.type == MessageType.GUESSEDINCORRECTLYWORD) {
					System.out.println("The word '" + sm.guessedWord + "' is not the secret word.");
					System.out.println("You lost. Correct Answer was: " + sm.currWord);
					cm = new CMessage(MessageType.ILOST, sm.username, null, sm.gameName, -1, null, null);
				} 
				if (sm.type == MessageType.NOTIFYINCORRECTNESSWORD) {
					System.out.println(sm.username + " has guessed the word '" + sm.guessedWord + "'");
					System.out.println("The word '" + sm.guessedWord + "' is not the secret word.");
					continue;
				} 
				if (sm.type == MessageType.HEDEAD) {
					System.out.println(sm.username + " is no longer part of the game. ");
					continue;
				}
				if (sm.type == MessageType.THANKYOU) {
					System.out.println("Thank you for playing Hangman!");
					break;
				} if (sm.type == MessageType.ALLLOSE) {
					
				}
				
				
				
				
				this.sendMessage(cm);
				
			} catch (Exception e) {
				//TODO: handle exception
				e.printStackTrace();
				break;
			}
		}

	}
	public static void main(String [] args) {
		HangManClient cc = new HangManClient();
	}
}