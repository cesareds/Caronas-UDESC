package LegacyNaoUsar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClienteThread extends Thread{
    private Socket socket;

    public ClienteThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {

//        estrutura para ler mensagem do servidor pelo socket
            InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String leituraDoBuffer;
            while((leituraDoBuffer = bufferedReader.readLine()) != null){
                System.out.println("Thread Legacy.Cliente recebendo do servidor:\t" + leituraDoBuffer);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
