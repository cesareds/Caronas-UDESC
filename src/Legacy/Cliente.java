package Legacy;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) throws IOException{
        Socket socket = new Socket("localhost", 4000);
        Scanner scanner = new Scanner(System.in);

        ClienteThread clienteThread = new ClienteThread(socket);
        clienteThread.start();

        PrintStream printStream = new PrintStream(socket.getOutputStream());
        String entradaDoTeclado;
        while((entradaDoTeclado = scanner.nextLine())!= null){
            printStream.println(entradaDoTeclado);
        }
    }


}
