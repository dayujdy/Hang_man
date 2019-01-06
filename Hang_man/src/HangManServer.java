import java.io.*;
import java.util.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class HangManServer {

	private Vector<ServerThread> serverThreads;
	public Map<String, LinkedList<String>> trackingUsers = new HashMap<String, LinkedList<String>>();
	public Map<String, ServerThread> userToThread = new HashMap<String, ServerThread>();
	private static PreparedStatement ps = null;
	private static ResultSet rs = null;
	private static Connection conn = null;
	private HangManConfiguration hmc = null;
	public HangManServer() {
		this.hmc = new HangManConfiguration();
		ServerSocket ss = null;
		try {
			ss = new ServerSocket(this.hmc.port); // BOUND TO THE PORT
			
			//connect to database
			dbConnect();
			
			//accept threads
			serverThreads = new Vector<ServerThread>();
			while(true) {
				Socket s = ss.accept(); // blocking
				System.out.println("Connection from: " + s.getInetAddress());
				ServerThread st = new ServerThread(s, this);
				serverThreads.add(st);
			}
		} catch (IOException ioe) {
			System.out.println("ioe in HangManServer constructor: " + ioe.getMessage());
		}  finally {
			if (ss != null) {
        		try {
        			ss.close();
        		} catch (IOException e) {
        			e.printStackTrace();
        		}
			}
		}
	}
	
	private void dbConnect() {
		try {
			System.out.print("Trying to connect to database...");
			Class.forName("com.mysql.cj.jdbc.Driver");
			String cnStr = this.hmc.dbConnection + "?user=" + this.hmc.dbUsername + 
					"&password=" + this.hmc.dbPassword + "&useSSL=false&serverTimezone=UTC";
			conn = DriverManager.getConnection(cnStr);
			System.out.println("Connected!");
		} catch (Exception e) {
			System.out.println("Unable to connect to database " + this.hmc.dbConnection + " with username " +
					this.hmc.dbUsername + " and password " + this.hmc.dbPassword);
			e.printStackTrace();
		}
	}

	public void broadcastAll(SMessage sm, String gameName) {
		if (sm != null){
			LinkedList<String> users = this.trackingUsers.get(gameName);
			for (String user : users) {
				ServerThread thisThread = this.userToThread.get(user);
				thisThread.sendMessage(sm);
			}


		}
	}
	public void broadcast(SMessage sm, ServerThread st, String gameName) {
		if (sm != null) {
			LinkedList<String> users = this.trackingUsers.get(gameName);
			for (String user : users) {
				ServerThread thisThread = this.userToThread.get(user);
				if (thisThread != st) {
					thisThread.sendMessage(sm);
				}
				
			}
		}
	}
	public void sendIndex(SMessage sm, String gameName, int index){
		if (sm != null) {
			LinkedList<String> users = this.trackingUsers.get(gameName);
			ServerThread thisThread = this.userToThread.get(users.get(index));
			thisThread.sendMessage(sm);
		}
	}

	
	public static int checkLogin(String username, String password){
		
		try {
			ps = conn.prepareStatement("SELECT COUNT(*) FROM Users  WHERE userName = ?" );
			ps.setString(1, username);
			//System.out.println("2");
			rs = ps.executeQuery();
			int count = 0;
			if (rs.next()) {
		        count = rs.getInt(1);
		    }
			if (count == 0) {
				return 0;
			} 
			ps = conn.prepareStatement("SELECT userName, password_ FROM Users WHERE userName=?");
			ps.setString(1, username);
			rs = ps.executeQuery();
			if (rs.next()) {
				if (rs.getString("password_").equals(password)) {
					return 2;
				}
				
			}
			return 1;
		} catch(SQLException sqle){
			System.out.println("SQLException in function \"checkLogin\"");
			sqle.printStackTrace();
			return -1;
		}
	}
	
	public static int getWins(String username){
		try {
			ps = conn.prepareStatement("SELECT SUM(u.wins) AS sumofwins FROM Users u  "
					+ "WHERE username=?");
			ps.setString(1, username);
			//System.out.println("2");
			rs = ps.executeQuery();
			if (rs.next()) {
				int wins = rs.getInt("sumofwins");
				return wins;
			}
			return -1;
			
		} catch(SQLException sqle){
			System.out.println("SQLException in function \"getWins\"");
			sqle.printStackTrace();
			return -1;
		}
	}
	
	public static int getLosses(String username){
		try {
			ps = conn.prepareStatement("SELECT SUM(u.losses) AS sumoflosses FROM Users u  "
					+ "WHERE username=?");
			ps.setString(1, username);
			rs = ps.executeQuery();
			if (rs.next()) {
				int losses = rs.getInt("sumoflosses");
				return losses;
			}
			return -1;
			
		} catch(SQLException sqle){
			System.out.println("SQLException in function \"getLosses\"");
			sqle.printStackTrace();
			return -1;
		}
	}
	
	public static void createUser(String username, String password) {
		try {
			
			ps = conn.prepareStatement("SELECT userName, password_ FROM Users WHERE userName =?");
			ps.setString(1, username);
			rs = ps.executeQuery();
			
			if(!rs.next()){
				ps = conn.prepareStatement("INSERT INTO Users (userName, password_, wins, losses) value (?,?,?,?);");
				ps.setString(1, username);
				ps.setString(2, password);
				ps.setInt(3, 0);
				ps.setInt(4, 0);
				ps.executeUpdate();
			}else {
				//System.out.println(rs.getString("userEmail"));
			}
			rs.close();
		}catch(SQLException sqle){
			System.out.println("SQLException in function \"createUser\"");
			sqle.printStackTrace();
		}
	}
	public static boolean checkGameName(String gamename){
		
		try {
			ps = conn.prepareStatement("SELECT COUNT(*) FROM Game  WHERE gameName = ?" );
			ps.setString(1, gamename);
			
			//System.out.println("2");
			rs = ps.executeQuery();
			int count = 0;
			if (rs.next()) {
		        count = rs.getInt(1);
		    }
			if (count == 0) {
				return false;
			} 
			else {
				return true;
			}
			
		} catch(SQLException sqle){
			System.out.println("SQLException in function \"checkGamename\"");
			sqle.printStackTrace();
			return false;
		}
	}
	public static void createGame(String gamename, int maxUserCount) {
		try {
			
			ps = conn.prepareStatement("SELECT * FROM Game  WHERE gameName = ?" );
			ps.setString(1, gamename);
			rs = ps.executeQuery();
			
			if(!rs.next()){
				ps = conn.prepareStatement("INSERT INTO Game (gameName, currUserCount, maxUserCount, secretWord, currWord, numofchance) value (?, ?, ?, ?, ?, ?);");
				ps.setString(1, gamename);
				ps.setInt(2,  1);
				ps.setInt(3, maxUserCount);
				ps.setString(4, "**");
				ps.setString(5, "**");
				ps.setInt(6, 7);
				ps.executeUpdate();
			}
			rs.close();
		}catch(SQLException sqle){
			System.out.println("SQLException in function \"createGame\"");
			sqle.printStackTrace();
		}
	}
	public void removeGame(String gamename) {
		try {
			ps = conn.prepareStatement("DELETE FROM Game WHERE gameName = ?");
			ps.setString(1, gamename);
			ps.executeUpdate();
		} catch (SQLException sqle){
			System.out.println("SQLException in function \"removeGame\"");
			sqle.printStackTrace();
		}
	}
	public static boolean checkGameUserCount(String gamename){
		
		try {
			ps = conn.prepareStatement("SELECT currUserCount, maxUserCount FROM Game  WHERE gameName =?" );
			ps.setString(1, gamename);
			
			//System.out.println("2");
			rs = ps.executeQuery();
			if (rs.next()) {
				if (rs.getInt("currUserCount") == rs.getInt("maxUserCount")) {
					return true;
				}
				
			}
			return false;
		} catch(SQLException sqle){
			System.out.println("SQLException in function \"checkGameUsrCount\"");
			sqle.printStackTrace();
			return false;
		}
	}
	public static void updateCurrUserCount(String gamename) {
		try {
			
			ps = conn.prepareStatement("SELECT * FROM Game  WHERE gameName = ?" );
			ps.setString(1, gamename);
			rs = ps.executeQuery();
			
			if(rs.next()){
				ps = conn.prepareStatement("UPDATE GAME SET currUserCount = ? WHERE gameName =?");
				int temp = rs.getInt("currUserCount") + 1;
				ps.setInt(1, temp);
				ps.setString(2, gamename);
				ps.executeUpdate();
						
			}else {
				//System.out.println(rs.getString("userEmail"));
			}
			rs.close();
		}catch(SQLException sqle){
			System.out.println("SQLException in function \"updateCurrUserCount\"");
			sqle.printStackTrace();
		}
	}
	public void updateResult(String username, boolean won) {
		try {
			
			ps = conn.prepareStatement("SELECT * FROM Users WHERE username = ?" );
			ps.setString(1, username);
			rs = ps.executeQuery();
			
			if(rs.next()){
				ps = conn.prepareStatement("UPDATE Users SET wins =?, losses =? WHERE username =?");
				int win = rs.getInt("wins");
				int loss = rs.getInt("losses");
				if (won) {
					win += 1;
				} else {
					loss += 1;
				}
				ps.setInt(1, win);
				ps.setInt(2, loss);
				ps.setString(3,username);
				ps.executeUpdate();
						
			}
			rs.close();
		}catch(SQLException sqle){
			System.out.println("SQLException in function \"updateResult\"");
			sqle.printStackTrace();
		}
	}
	public static int getLeftOverCount(String gamename){
		
		try {
			ps = conn.prepareStatement("SELECT currUserCount, maxUserCount FROM Game  WHERE gameName =?" );
			ps.setString(1, gamename);
			
			//System.out.println("2");
			rs = ps.executeQuery();
			if (rs.next()) {
				int temp1 = rs.getInt("currUserCount") ;
				int temp2 =rs.getInt("maxUserCount") ;
				int remain = temp2 -temp1;
				return remain;
				
			}
			return -1;
		} catch(SQLException sqle){
			System.out.println("SQLException in function \"getLeftOverCount\"");
			sqle.printStackTrace();
			return -1;
		}
	}
	public static int getMaxUserCount(String gamename){
		
		try {
			ps = conn.prepareStatement("SELECT currUserCount, maxUserCount FROM Game  WHERE gameName =?" );
			ps.setString(1, gamename);
			
			//System.out.println("2");
			rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getInt("maxUserCount");
				
			}
			return -1;
		} catch(SQLException sqle){
			System.out.println("SQLException in function \"getMaxUserCount\"");
			sqle.printStackTrace();
			return -1;
		}
	}
		public  String chooseSecretWord(String gamename){
			Scanner scan = new Scanner(System.in);
			String fileName = hmc.secretWordFile;
			try {
				Scanner fileScan = new Scanner(new File(fileName));
				List<String> words = new ArrayList<>();
				String word;
				while (fileScan.hasNext()) {
		               word = fileScan.next();
		               words.add(word);
		        }
				int numWords = words.size();
	            Random generator = new Random();
	            int wordNum = generator.nextInt(numWords);
	            String selectedWord = (String)words.get(wordNum);
	            return selectedWord;
			
			} catch (IOException ex) {
	        System.out.println("Word file "+ fileName + " could not be found.");
	        return null;
			}
			
		}
		public String getSecretWord(String gamename) {
			try {
				ps = conn.prepareStatement("SELECT secretWord FROM Game WHERE gameName = ?");
				ps.setString(1, gamename);
				rs = ps.executeQuery();
				if (rs.next()) {
					return rs.getString("secretWord");
				}
				return null;
			} catch(SQLException sqle) {
				System.out.println("SQLException in function \"getSecretWord\"");
				return null;
			}
		}
		public String getCurrWord(String gamename) {
			try {
				ps = conn.prepareStatement("SELECT currWord FROM Game WHERE gameName = ?");
				ps.setString(1, gamename);
				rs = ps.executeQuery();
				if (rs.next()) {
					return rs.getString("currWord");
				}
				return null;
			} catch(SQLException sqle) {
				System.out.println("SQLException in function \"getCurrWord\"");
				return null;
			}
			
		}
		public static void updateSecretWord(String gamename, String secret){
			try {
				
				ps = conn.prepareStatement("SELECT * FROM Game  WHERE gameName = ?" );
				ps.setString(1, gamename);
				rs = ps.executeQuery();
				
				if(rs.next()){
					ps = conn.prepareStatement("UPDATE GAME SET secretWord =?, currWord =? WHERE gameName =?");
					int l = secret.length();
					String curr =null ;
					for (int i = 0; i <secret.length(); i++) {
						curr += "_ ";
					}
					curr = curr.substring(0, l-1);
					
					ps.setString(1, secret);
					ps.setString(2, curr);
					ps.setString(3,gamename);
					ps.executeUpdate();
							
				}else {
					//System.out.println(rs.getString("userEmail"));
				}
				rs.close();
			}catch(SQLException sqle){
				System.out.println("SQLException in function \"updateSecretWord\"");
				sqle.printStackTrace();
			}
			
		}
		public static void updateCurrWord(String gamename, String curr){
			try {
				
				ps = conn.prepareStatement("SELECT * FROM Game  WHERE gameName = ?" );
				ps.setString(1, gamename);
				rs = ps.executeQuery();
				
				if(rs.next()){
					ps = conn.prepareStatement("UPDATE GAME SET currWord =? WHERE gameName =?");
					
					ps.setString(1, curr);
					ps.setString(2,gamename);
					ps.executeUpdate();
							
				}else {
					//System.out.println(rs.getString("userEmail"));
				}
				rs.close();
			}catch(SQLException sqle){
				System.out.println("SQLException in function \"updateSecretWord\"");
				sqle.printStackTrace();
			}
			
		}
		public static int getChances(String gamename) {
			try {
				ps = conn.prepareStatement("SELECT numofchance FROM Game WHERE gameName = ?");
				ps.setString(1, gamename);
				rs = ps.executeQuery();
				if (rs.next()) {
					return rs.getInt("numofchance");
				}
				return -1;
			} catch(SQLException sqle) {
				System.out.println("SQLException in function \"getChances\"");
				return -1;
			}
		}
		public static void updateChances(String gamename){
			try {
				
				ps = conn.prepareStatement("SELECT * FROM Game  WHERE gameName = ?" );
				ps.setString(1, gamename);
				rs = ps.executeQuery();
				
				if(rs.next()){
					ps = conn.prepareStatement("UPDATE GAME SET numofchance =? WHERE gameName =?");
					int temp = rs.getInt("numofchance") - 1;
					ps.setInt(1, temp);
					ps.setString(2,gamename);
					ps.executeUpdate();
							
				}else {
					//System.out.println(rs.getString("userEmail"));
				}
				rs.close();
			}catch(SQLException sqle){
				System.out.println("SQLException in function \"updateSecretWord\"");
				sqle.printStackTrace();
			}
			
		}
		
	
	
	public static void main(String [] args) {
		
		HangManServer hm = new HangManServer();
	}
}
