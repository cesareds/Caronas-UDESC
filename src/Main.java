//Trabalho 1 de Desenvolvimento Mobile - TADS 2025/1
//César; Débora; Lucas; Tamy
//ChatApp

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) throws IOException {
//        cria e conecta o socket
        System.out.println("Bem vindo ao ChatApp da Equipe Computação Invaders");
        ServerSocket serverSocket = new ServerSocket(4000);
        Socket socket = serverSocket.accept();
        System.out.println("----------CONECTADO!----------");

//        estrutura para ler mensagem do socket
        InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String leituraDoBuffer;

        while((leituraDoBuffer = bufferedReader.readLine()) != null){
            System.out.println("Servidor:\t" + leituraDoBuffer);
        }

    }
}
