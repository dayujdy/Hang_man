import java.net.Socket;
import java.io.*;
import java.util.*;

public class ServerThread extends Thread {
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	private HangManServer hm;
	private String gameName = null;
	private String userName = null;
	public ServerThread(Socket s, HangManServer hm) {
		try {
			this.hm = hm;
			this.oos = new ObjectOutputStream(s.getOutputStream());
			this.ois = new ObjectInputStream(s.getInputStream());
			this.start();
		} catch (IOException ioe) {
			System.out.println("ioe in ServerThread constructor: " + ioe.getMessage());
		}
	}
	public String getGameName(){
		return this.gameName;
	}

	public void sendMessage(SMessage sm) {
		try {
			this.oos.writeObject(sm);
			this.oos.flush();
		} catch (IOException ioe) {
			System.out.println("ioe: " + ioe.getMessage());
		}
	}
	private String padString(int leng) {
		String str = "";
		for (int i = 0; i < leng; i++) {
			str += "_";
		}
		return str;
	}
	@Override
	public void run() {
		while (true) {
			try {
				CMessage cm = (CMessage) ois.readObject();
			    SMessage sm = null;
			    
				if (cm == null) {
					sm = null;
					continue;
				} 
				if (cm.type == MessageType.LOGIN){
					System.out.println(cm.timestamp + " " + cm.username + " - trying to log in with password " + cm.password + ".");
					int checker = HangManServer.checkLogin(cm.username, cm.password);
					if (checker == 0) { // User doesn't exist
						System.out.println(cm.timestamp + " " + cm.username + " - does not have an account so not successfully logged in.");
						sm = new SMessage(MessageType.LOGINNOTFOUND, cm.username, cm.password, 0, 0, null, -1, null, -1, null, null);
					}
					else if (checker == 1) { // User exists but wrong password
						System.out.println( cm.timestamp + " " + cm.username + " - has an account but not successfully logged in.");
						sm = new SMessage(MessageType.INCORRECTLOGIN, null, null, 0 , 0, null, -1,null, -1, null, null);
					}
					else if (checker == 2) { //Both right
						System.out.println( cm.timestamp + " " + cm.username + " - successfully logged in.");
						sm = new SMessage(MessageType.LOGINSUCCESS,cm.username, null, 0, 0, null, -1,null, -1, null, null);
						this.userName = cm.username;
						this.hm.userToThread.put(this.userName, this);
						this.sendMessage(sm);
						int win = HangManServer.getWins(cm.username);
						int loss = HangManServer.getLosses(cm.username);
						sm = new SMessage(MessageType.SHOWUSERSTATS,cm.username, null,  win, loss, null, -1,null, -1, null, null);
						System.out.println( cm.timestamp + " " + cm.username + " - has record " + win + " wins and " + loss + " losses.");
						this.sendMessage(sm);
						sm = new SMessage(MessageType.CHOOSEGAMETYPE, cm.username, null, 0,0, null, -1,null, -1, null, null);
					} else {
						break;
					}
				} else if (cm.type == MessageType.CREATEACCT) {
					HangManServer.createUser(cm.username, cm.password);
					System.out.println( cm.timestamp + " " + cm.username + " - successfully created an account with password " + cm.password);
					sm = new SMessage(MessageType.LOGINSUCCESS,cm.username, null, 0, 0, null, -1,null, -1, null, null);
					this.userName = cm.username;
					this.hm.userToThread.put(this.userName, this);
					this.sendMessage(sm);
					sm = new SMessage(MessageType.SHOWUSERSTATS,cm.username, null, 0, 0, null, -1,null, -1, null, null);
					this.sendMessage(sm);
					System.out.println( cm.timestamp + " " + cm.username + " - has record 0 wins and 0 losses.");
					sm = new SMessage(MessageType.CHOOSEGAMETYPE, cm.username, null, 0,0, null, -1,null, -1, null, null);
					
				} else if (cm.type == MessageType.STARTGAME) {
					System.out.println( cm.timestamp + " " + cm.username + " - wants to start a game called " + cm.gameName);
					// check if game name exists 
					boolean exists = HangManServer.checkGameName(cm.gameName);
					if (exists) {
						System.out.println( cm.timestamp + " " + cm.gameName + " already exists, so unable to start " + cm.gameName);
						sm = new SMessage(MessageType.DUPLICATENAME, cm.username, null, 0, 0, cm.gameName, -1,null, -1, null, null);
					} else {
						sm = new SMessage(MessageType.REQUESTUSERCOUNT, cm.username, null, 0, 0, cm.gameName, -1,null, -1, null, null);
					}

				} else if (cm.type == MessageType.RESPONSEUSERCOUNT) {
					HangManServer.createGame(cm.gameName, cm.maxUserCount);
					LinkedList<String> users = new LinkedList<String>();
					users.add(this.userName);
					this.hm.trackingUsers.put(cm.gameName, users);
					System.out.println(cm.timestamp + " " + cm.username + " - successfully started game " + cm.gameName + ".");
					this.gameName = cm.gameName;
					// TODO: WAIT UNTIL THE MAXUSERCT IS FILLED
					if (cm.maxUserCount == 1) {
						String secretWrd = this.hm.chooseSecretWord(cm.gameName);
						System.out.println("Secret Word is " + secretWrd);
						HangManServer.updateSecretWord(cm.gameName, secretWrd);
					
						sm = new SMessage(MessageType.GAMEBEGIN, cm.username, null, 0, 0, cm.gameName, -1,null, -1, null, null);
						String str = this.padString(secretWrd.length());
						HangManServer.updateCurrWord(cm.gameName, str);
						sm = new SMessage(MessageType.CURRSECRETWORD, cm.username, null, 0,0, cm.gameName, -1, str, 7, null, null);
						this.sendMessage(sm);
						sm = new SMessage(MessageType.PLAY, cm.username, null, 0, 0, cm.gameName, -1, null, 7, null, null);
					} else {
						int leftover = cm.maxUserCount - 1; 
						System.out.println(cm.timestamp + " " + cm.username + " - " + cm.gameName +" needs "+ leftover + " to start game.");
						sm = new SMessage(MessageType.WAITUNTILMAX, cm.username, null, 0, 0, cm.gameName, leftover,null, -1, null, null);
					}
				}
				else if (cm.type == MessageType.JOINGAME) {
					System.out.println( cm.timestamp + " " + cm.username + " - wants to join a game called " + cm.gameName);
					// check if game name exists
					boolean exists = HangManServer.checkGameName(cm.gameName);
					if (!exists) {
						System.out.println( cm.timestamp + " " +  cm.username + " - " + cm.gameName + " doesn't exist, so unable to join " + cm.gameName);
						sm = new SMessage(MessageType.INCORRECTNAME, cm.username, null, 0, 0, cm.gameName, -1,null, -1, null, null);
					} else {
						boolean full = HangManServer.checkGameUserCount(cm.gameName);
						if (full) {
							System.out.println( cm.timestamp + " " + cm.username + " - " + cm.gameName + " exists, but " + cm.username+ 
									" unable to join because maximum number of players have already joined "+ cm.gameName);
							sm = new SMessage(MessageType.ROOMFULL, cm.username, null, 0,0,cm.gameName, -1,null, -1, null, null);
						} else {
							System.out.println(cm.timestamp + " " + cm.username + " - " + "successfully joined game " +  cm.gameName);
							
							LinkedList<String> oldUsers = this.hm.trackingUsers.get(cm.gameName);
							int win, loss;
							this.gameName = cm.gameName;
							for (int j = 0; j < oldUsers.size(); j++) {
								String person2 = oldUsers.get(j);
								win = HangManServer.getWins(person2);
								loss = HangManServer.getLosses(person2);
								sm = new SMessage(MessageType.SHOWUSERSTATS,person2, null, win, loss, null, -1,null, -1, null, null);
								this.sendMessage(sm);
							}
							oldUsers.add(this.userName);
							this.hm.trackingUsers.put(cm.gameName, oldUsers);
							
							HangManServer.updateCurrUserCount(this.gameName); // curr increment by 1
							// TODO: BROADCAST TO THE PEOPLE IN THE GAME ROOM the user that just got entered (statistics of the user entered)
							
							
							int ct = HangManServer.getLeftOverCount(this.gameName); // max - curr
							win = HangManServer.getWins(cm.username);
							loss = HangManServer.getLosses(cm.username);
							sm = new SMessage(MessageType.USERENTERED, cm.username, null, 0, 0, this.gameName, -1,null, -1, null, null);
							this.hm.broadcast(sm, this,this.gameName);
							sm = new SMessage(MessageType.SHOWUSERSTATS,cm.username, null, win, loss, null, -1,null, -1, null, null);
							this.hm.broadcast(sm, this, this.gameName);
							sm = new SMessage(MessageType.USERCTCHANGED, cm.username, null, 0, 0, this.gameName, ct,null, -1, null, null);
							this.hm.broadcast(sm, this, this.gameName);
							if (ct == 0) {
								int getMax = HangManServer.getMaxUserCount(cm.gameName); //get Max
								System.out.println(cm.timestamp + " " + cm.username + " - " + cm.gameName + " has " + getMax + " so starting game.");
								sm = new SMessage(MessageType.FINDSECRETWRD, null, null, 0, 0, null, -1,null, -1, null, null);
								// broadcast to ALL 
								this.hm.broadcastAll(sm, this.gameName);
								String secretWrd = this.hm.chooseSecretWord(cm.gameName);
								// TODO: Broadcast to the people that all have joined. 
								System.out.println("Secret Word is " + secretWrd);
								HangManServer.updateSecretWord(cm.gameName, secretWrd);
								
								sm = new SMessage(MessageType.ROOMFILLED, null, null, 0, 0, cm.gameName, -1,null, -1, null, null);
								this.hm.broadcastAll(sm, this.gameName);
								String str = this.padString(secretWrd.length());
								HangManServer.updateCurrWord(cm.gameName, str);
								sm = new SMessage(MessageType.GAMEBEGIN, cm.username, null, 0, 0, cm.gameName, -1, null, -1, null, null);
								this.hm.broadcastAll(sm, this.gameName);
								sm = new SMessage(MessageType.CURRSECRETWORD, cm.username, null, 0,0, cm.gameName, -1, str, 7, null, null);
								this.hm.broadcastAll(sm, this.gameName);

								String host = this.hm.trackingUsers.get(this.gameName).get(0);
								sm = new SMessage(MessageType.WAITFORPLAY, host, null, 0, 0, cm.gameName, -1, null, 7, null, null);
								this.hm.broadcast(sm, this.hm.userToThread.get(host),this.gameName);
								sm = new SMessage(MessageType.PLAY, host, null, 0, 0, cm.gameName, -1, null, 7, null, null);
								this.hm.sendIndex(sm, cm.gameName, 0);
								continue;
							} else {
								System.out.println(cm.timestamp + " " + cm.username + " - " + cm.gameName +" needs "+ ct + " to start game.");
								sm = new SMessage(MessageType.WAITUNTILMAX, cm.username, null, 0, 0, cm.gameName, ct, null, -1, null, null);
								
							}
						}
						
					}
					
				} else if (cm.type == MessageType.GUESSEDLETTER){
					System.out.println(cm.timestamp + " " + cm.gameName + " " +  cm.username + " - guessed letter " + cm.letter); 
					String secretWord = this.hm.getSecretWord(cm.gameName);
					String currWord = this.hm.getCurrWord(cm.gameName);
					boolean found = false;
					String positions = "";
					for (int i = 0; i < secretWord.length(); i++) {
						if (secretWord.charAt(i) == cm.letter.charAt(0)) {
							currWord = currWord.substring(0, i) + cm.letter + currWord.substring(i+1);
							found = true;
							positions = positions + " " + i;
						}
					}
					int chances = HangManServer.getChances(cm.gameName);
					if (found == true) {
						System.out.println(cm.timestamp + " " + cm.gameName + " " + cm.username + " - " + cm.letter + " is in " + secretWord
								+ " in position(s)" + positions + ". Secret word now shows " + currWord);
						HangManServer.updateCurrWord(cm.gameName, currWord);
						sm = new SMessage(MessageType.NOTIFYCORRECTNESSLETTER, cm.username, null, 0, 0, cm.gameName, -1, currWord, chances, cm.letter, null);
						this.hm.broadcast(sm, this, cm.gameName);
						sm = new SMessage(MessageType.GUESSEDCORRECTLYLETTER, cm.username, null, 0, 0, cm.gameName, -1, currWord, chances, cm.letter, null);
					} else {
						System.out.println(cm.timestamp + " " + cm.gameName + " " + cm.username + " - " + cm.letter + " is not in " + secretWord);
						chances -= 1;
						System.out.println(cm.timestamp + " " + cm.gameName + " now " + chances + " guesses remaining.");
						HangManServer.updateChances(cm.gameName);
						sm = new SMessage(MessageType.NOTIFYINCORRECTNESSLETTER, cm.username, null, 0, 0, cm.gameName, -1, currWord, chances, cm.letter, null);
						this.hm.broadcast(sm, this, cm.gameName);
						sm = new SMessage(MessageType.GUESSEDINCORRECTLYLETTER, cm.username, null, 0, 0, cm.gameName, -1, currWord, chances, cm.letter, null);
					}
				} else if (cm.type == MessageType.GUESSEDWORD){
					System.out.println(cm.timestamp + " " + cm.gameName + " " +  cm.username + " - guessed word " + cm.word); 
					String secretWord = this.hm.getSecretWord(cm.gameName);
					if (secretWord.equalsIgnoreCase(cm.word)) {
						System.out.println(cm.timestamp + " " + cm.gameName + " " +  cm.username + " - " + cm.word + " is correct."); 
						
						sm = new SMessage(MessageType.NOTIFYCORRECTNESSWORD, cm.username, null, 0, 0, cm.gameName, -1, null, -1, null, cm.word);
						this.hm.broadcast(sm, this, cm.gameName);
						sm = new SMessage(MessageType.GUESSEDCORRECTLYWORD, cm.username, null, 0, 0, cm.gameName, -1, null, -1, null, cm.word);
					} else {
						System.out.println(cm.timestamp + " " + cm.gameName + " " +  cm.username + " - " + cm.word + " is incorrect."); 
						sm = new SMessage(MessageType.NOTIFYINCORRECTNESSWORD, cm.username, null, 0, 0, cm.gameName, -1, null, -1, null, cm.word);
						this.hm.broadcast(sm, this, cm.gameName);
						sm = new SMessage(MessageType.GUESSEDINCORRECTLYWORD, cm.username, null, 0, 0, cm.gameName, -1, secretWord, -1, null, cm.word);
					}
				} else if (cm.type == MessageType.NEXTPERSON) {
					LinkedList<String> usernames  = this.hm.trackingUsers.get(cm.gameName);
					int index = (usernames.indexOf(cm.username) + 1) % usernames.size(); // next person
					String nextPerson = usernames.get(index);
					int chances = HangManServer.getChances(cm.gameName);
					String currWord = this.hm.getCurrWord(cm.gameName);
					sm = new SMessage(MessageType.CURRSECRETWORD, cm.username, null, 0,0, cm.gameName, -1, currWord, chances, null, null);
					this.hm.broadcastAll(sm, this.gameName);
					sm = new SMessage(MessageType.WAITFORPLAY, nextPerson, null, 0, 0, cm.gameName, -1, null, chances, null, null);
					this.hm.broadcast(sm, this.hm.userToThread.get(nextPerson),this.gameName);
					sm = new SMessage(MessageType.PLAY, nextPerson, null, 0, 0, cm.gameName, -1, null, chances, null, null);
					this.hm.sendIndex(sm, cm.gameName, index);
					continue;
				} else if (cm.type == MessageType.ILOST) {
					System.out.println(cm.timestamp + " " +  cm.username + " has lost the game."); 
					LinkedList<String> usernames  = this.hm.trackingUsers.get(cm.gameName);
					if (usernames.size() == 1) {
						System.out.println(cm.timestamp + " " + cm.gameName + " - finished as there are no more players in the game. ");
						this.hm.trackingUsers.remove(this.gameName);
						this.hm.updateResult(cm.username, false);
						int win = HangManServer.getWins(cm.username);
						int loss = HangManServer.getLosses(cm.username);
						sm = new SMessage(MessageType.SHOWUSERSTATS,cm.username, null, win, loss, null, -1,null, -1, null, null);	
						this.sendMessage(sm);
						sm = new SMessage(MessageType.THANKYOU, cm.username, null, win, loss, null, -1, null, -1, null, null);
						this.hm.removeGame(cm.gameName);
					} else {
						int index = usernames.indexOf(cm.username);	
						int nextIndex = (index + 1) % usernames.size();
						String nextPerson = usernames.get(nextIndex);
						int chances = HangManServer.getChances(cm.gameName);
						usernames.remove(index);
						this.hm.trackingUsers.put(this.gameName, usernames);
						System.out.println(cm.timestamp + " " + cm.gameName + " " +  cm.username + " has lost and is no longer in the game."); 
						sm = new SMessage(MessageType.HEDEAD, cm.username, null, 0, 0, cm.gameName, -1, null, -1, null, null);
						this.hm.broadcastAll(sm, cm.gameName);
						String currWord = this.hm.getCurrWord(cm.gameName);
						sm = new SMessage(MessageType.CURRSECRETWORD, cm.username, null, 0,0, cm.gameName, -1, currWord, chances, null, null);
						this.hm.broadcastAll(sm, this.gameName);
						sm = new SMessage(MessageType.WAITFORPLAY, nextPerson, null, 0, 0, cm.gameName, -1, null, chances, null, null);
						this.hm.broadcast(sm, this.hm.userToThread.get(nextPerson),this.gameName);
						sm = new SMessage(MessageType.PLAY, nextPerson, null, 0, 0, cm.gameName, -1, null, chances, null, null);
						usernames  = this.hm.trackingUsers.get(this.gameName);
						index = usernames.indexOf(nextPerson);	
						this.hm.sendIndex(sm, cm.gameName, index);
						// update results
						this.hm.updateResult(cm.username, false);
						int win = HangManServer.getWins(cm.username);
						int loss = HangManServer.getLosses(cm.username);
						sm = new SMessage(MessageType.SHOWUSERSTATS,cm.username, null, win, loss, null, -1,null, -1, null, null);	
						this.sendMessage(sm);
						sm = new SMessage(MessageType.THANKYOU, cm.username, null, win, loss, null, -1, null, -1, null, null);
						
					}
					

				} else if (cm.type == MessageType.IWIN) {
					LinkedList<String> usernames  = this.hm.trackingUsers.get(this.gameName);
					System.out.println(cm.timestamp + " " +  cm.username + " wins game."); 
					for (int i = 0; i < usernames.size(); i++) {
						
						String person = usernames.get(i);
						if (person.equals(cm.username)) {
							this.hm.updateResult(person, false);
						} else {
							this.hm.updateResult(person, true);
						}
						
					}
					int win,loss;
					for (int i = 0; i < usernames.size(); i++) {
						
						String person = usernames.get(i);
						win = HangManServer.getWins(person);
						loss = HangManServer.getLosses(person);
						sm = new SMessage(MessageType.SHOWUSERSTATS,person, null, win, loss, null, -1,null, -1, null, null);
						this.hm.sendIndex(sm, cm.gameName, i);
						for (int j = 0; j < usernames.size(); j++) {
							String person2 = usernames.get(j);
							if (j != i) {
								win = HangManServer.getWins(person2);
								loss = HangManServer.getLosses(person2);
								sm = new SMessage(MessageType.SHOWUSERSTATS,person2, null, win, loss, null, -1,null, -1, null, null);
								this.hm.sendIndex(sm, cm.gameName, i);
							}
						}
						
					}
					sm = new SMessage(MessageType.THANKYOU, null, null, 0,0, null, -1, null ,-1, null, null);
					this.hm.broadcastAll(sm, cm.gameName);
					this.hm.removeGame(cm.gameName);
					System.out.println(cm.timestamp + " " + cm.gameName + " - has finished. ");
					continue;
				} else if (cm.type == MessageType.ALLLOSE) {
					System.out.println(cm.timestamp + " " + cm.gameName +" - All users lost with zero guesses remaining.");
					LinkedList<String> usernames  = this.hm.trackingUsers.get(cm.gameName);
					for (int i = 0; i < usernames.size(); i++) {
						String person = usernames.get(i);
						this.hm.updateResult(person, false);
						
					}
					int win,loss;
					for (int i = 0; i < usernames.size(); i++) {
						
						String person = usernames.get(i);
						win = HangManServer.getWins(person);
						loss = HangManServer.getLosses(person);
						sm = new SMessage(MessageType.SHOWUSERSTATS,person, null, win, loss, null, -1,null, -1, null, null);
						this.hm.sendIndex(sm, cm.gameName, i);
						for (int j = 0; j < usernames.size(); j++) {
							String person2 = usernames.get(j);
							if (j != i) {
								win = HangManServer.getWins(person2);
								loss = HangManServer.getLosses(person2);
								sm = new SMessage(MessageType.SHOWUSERSTATS,person2, null, win, loss, null, -1,null, -1, null, null);
								this.hm.sendIndex(sm, cm.gameName, i);
							}
						}
						
					}
					sm = new SMessage(MessageType.THANKYOU, null, null, 0,0, null, -1, null ,-1, null, null);
					this.hm.broadcastAll(sm, cm.gameName);
					this.hm.removeGame(cm.gameName);
					System.out.println(cm.timestamp + " " + cm.gameName + " has finished. ");
					continue;
					
				}
				this.sendMessage(sm);
				
			} catch(EOFException ex) {
				continue;
			} catch (Exception e) {
				
				e.printStackTrace();
				break;
			}
		}
	}
}