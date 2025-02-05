package ApJavaProject;

import java.sql.SQLOutput;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

/*
* The basic functionality of user input and outputting via the command line is controlled with this interface.
* Any user input is defined by the functions will be prefixed by `input`
* Any output is defined by the functions  will be prefixed by `output`
*/

public interface Cli {
    /*
    * Gets port from commandline args
    * Returns -1 when no port is given or is malformed
    */
    static int getArgPort(String[] args){
        if (args.length < 1){
            return -1;
        }
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e){
            return -1;
        }
        return port;
    }

    /*
    * Gets IP from commandline args
    * Returns null if no IP is given or is malformed
    */
    static String getArgIP(String[] args){
        if (args.length < 2){
            return null;
        }

        String ip = args[1];
        return ip.matches("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$")? ip: null;
    }

    /*
    * Gets ID from commandline args
    * Returns null if no ID is given
    */
    static String getArgID(String[] args){
        if (args.length < 3){
            return null;
        }

        return args[2];
    }

    static void outputInvalidPort(){
        System.out.println("Port argument is invalid");
    }

    static void outputInvalidIP(){
        System.out.println("IP argument is invalid");
    }

    static void outputInvalidID(){
        System.out.println("ID argument is invalid");
    }

    /*
    * Outputs message to the screen
    */
    static void outputMessage(Message msg){
        // Blank line
        System.out.println();

        // Time
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm.ss");
        System.out.print(sdf.format(new Date(msg.timeStamp())));

        // Public or private
        System.out.print(msg.isPrivate()? "#" : "+");

        // ID if not server
        if (!msg.senderId().isEmpty()){
            System.out.print("["+msg.senderId()+"]:");
        }

        // Message
        System.out.println(" " + msg.content());
    }

    /*
    * Tells user input is invalid
     */
    static void outputInvalidInput(String input){
        System.out.println("Invalid input: " + input);
    }

    /*
    * outputs input prompt
     */
    static void outputOptionPrompt(){
        System.out.println("1: Send Message\n2: Send Private Message\n3: Request Data\n4: Exit\n");
    }

    static void outputServerError(){
        System.out.println("Cannot connect to server");
    }


    /*
     * Asks for input option
     */
    static String inputOption(){
        return input(">>:");
    }

    /*
     * Asks for message string
     */
    static String inputMessage(){
        return input("Message: ");
    }

    /*
     * Asks for sender ID
     */
    static String inputID(){
        return input("ID: ");
    }

    /*
    * Prompts user for an input
    */
    static String input(String prompt){
        // Output prompt
        System.out.print(prompt);

        // Take user input
        return new Scanner(System.in).nextLine();
    }
}
