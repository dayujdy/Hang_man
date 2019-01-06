import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CMessage implements Serializable {
	public static final long serialVersionUID = 1;
	public String username; // can be empty
	public String password;
	public String timestamp;
	public String gameName;
	public MessageType type;
	public int maxUserCount;
	public String letter;
	public String word;
	
	public CMessage(MessageType m ,String username, String password, String gameName, int maxUserCount, String letter, String word) {
		this.type = m;
		this.username = username;
		this.password = password;
		this.gameName = gameName;
		this.timestamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
		this.maxUserCount = maxUserCount;
		this.letter = letter;
		this.word = word;
		
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