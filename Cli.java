package ApJavaProject;

import java.util.Scanner;

/*
* The basic functionality of user input and outputting via the command line is controlled with this interface.
* Any user input is defined by the functions will be prefixed by `input`
* Any output is defined by the functions  will be prefixed by `output`
*/

public interface Cli {
    default void outputHello(String name){
        System.out.println("Hello " + name + "!");
    }

    default String input(String prompt){
        // Output prompt
        System.out.print(prompt);

        // Take user input
        return new Scanner(System.in).nextLine();
    }
}
