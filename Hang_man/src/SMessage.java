import java.io.Serializable;


public class SMessage implements Serializable {
	public static final long serialVersionUID = 1;
	public String username; // can be empty
	public String password;
	public MessageType type;
	public int wins;
	public int losses;
	public String gameName;
	public int leftOver;
	public String currWord;
	public int guessRemaining;
	public String letter;
	public String guessedWord;
	
	public SMessage(MessageType m ,String username, String password, int wins, int losses, 
			String gameName, int leftOver, String currWord, int guessRemaining, String letter, String guessedWord) {
		this.type = m;
		this.username = username;
		this.password = password;
		this.wins = wins;
		this.losses = losses;
		this.gameName = gameName;
		this.leftOver = leftOver;
		this.currWord = currWord;
		this.guessRemaining = guessRemaining;
		this.letter = letter;
		this.guessedWord = guessedWord;
		
		
	}
	
	
	
//	public String getUsername() {
//		return username;
//	}
//	public String getMessage() {
//		return message;
//	}
//	public String getTimeStamp() {
//		return timestamp;
//	}
	
//	@Override
//	public String toString() {
//		if (type == MessageType.LOGIN) {
//			
//		}
//		return getTimeStamp() + " " + getUsername() + " - " + getMessage(); 
//	}
}