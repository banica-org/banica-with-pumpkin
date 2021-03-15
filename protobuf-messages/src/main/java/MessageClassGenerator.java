/**
 * We need this class to generate proto files in the target folder and then use them in other modules.
 * Without the main function we can't build this module and accordingly we can't generate our proto files
 * and use them later in other modules.
 */
public class MessageClassGenerator {
    public static void main(String[] args) {
        System.out.println("Compiling the code and generating proto files in target folder.");
    }
}
