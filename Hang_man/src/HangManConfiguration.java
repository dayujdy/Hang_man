import java.io.*;
import java.util.Properties;
import java.util.Scanner;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
public class HangManConfiguration {
    public String hostname;
    public int port;
    public String dbConnection;
    public String dbUsername;
    public String dbPassword;
    public String secretWordFile;

    public HangManConfiguration() {
        Map<String, String> map = new HashMap<>();
        map.put("ServerHostname", "Server Hostname");
        map.put("ServerPort", "Server Port");
        map.put("DBConnection", "Database Connection String");
        map.put("DBUsername", "Database Username");
        map.put("DBPassword", "Database Password");
        map.put("SecretWordFile", "Secret Word File");


        Scanner scan = new Scanner(System.in);
        String filename = "";
        File file;
        Properties prop = new Properties();
        InputStream input = null;
        boolean valid = false;
        while (!valid){
            try {
                System.out.print("What is the name of the configuration file? ");
                
                filename = scan.nextLine();
                file = new File(filename);
                input = new FileInputStream(file);
                prop.load(input);
                List<String> propNames = Arrays.asList("ServerHostname", "ServerPort",
                        "DBConnection", "DBUsername", "DBPassword", "SecretWordFile");
                System.out.println("Reading config file...");
                this.hostname = prop.getProperty(propNames.get(0));
                if (this.hostname == null){
                    System.out.println(propNames.get(0) + " is a required parameter in the configuration file. ");
                    continue;
                }
                try {
                    String temp = prop.getProperty((propNames.get(1)));
                    if (temp == null){
                        System.out.println(propNames.get(1) + " is a required parameter in the configuration file. ");
                        continue;
                    }
                    this.port = Integer.parseInt(temp);

                } catch (NumberFormatException ex) {
                    System.out.println(propNames.get(1) + " is a required parameter in the configuration file. ");
                    continue;
                }

                this.dbConnection = prop.getProperty(propNames.get(2));
                if (this.dbConnection == null){
                    System.out.println(propNames.get(2) + " is a required parameter in the configuration file. ");
                    continue;
                }
                this.dbUsername = prop.getProperty(propNames.get(3));
                if (this.dbUsername == null){
                    System.out.println(propNames.get(3) + " is a required parameter in the configuration file. ");
                    continue;
                }
                this.dbPassword = prop.getProperty(propNames.get(4));
                if (this.dbPassword == null){
                    System.out.println(propNames.get(4) + " is a required parameter in the configuration file. ");
                    continue;
                }
                this.secretWordFile = prop.getProperty(propNames.get(5));
                if (this.secretWordFile == null){
                    System.out.println(propNames.get(5) + " is a required parameter in the configuration file. ");
                    continue;
                }


                System.out.println(map.get(propNames.get(0)) + " - " + this.hostname);
                System.out.println(map.get(propNames.get(1)) + " - " + this.port);
                System.out.println(map.get(propNames.get(2)) + " - " + this.dbConnection);
                System.out.println(map.get(propNames.get(3)) + " - " + this.dbUsername);
                System.out.println(map.get(propNames.get(4)) + " - " + this.dbPassword);
                System.out.println(map.get(propNames.get(5)) + " - " + this.secretWordFile);

                valid = true;
            } catch (IOException ex) {
                System.out.println("Configuration file "+ filename + " could not be found.");
            } finally {
            	if (input != null) {
            		try {
            			input.close();
            		} catch (IOException e) {
            			e.printStackTrace();
            		}
            	}
            }
        }




    }

}
