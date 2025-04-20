package Servidor;

import Cliente.Cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Servidor extends Thread{
    ArrayList<Cliente> clientes = new ArrayList<>();
    public Servidor(ArrayList<Cliente> clientes) {
        this.clientes = clientes;
    }
    public static void executarServidor() throws IOException {
        System.out.println("Bem vindo ao ChatApp da Equipe Computação Invaders");
        ServerSocket serverSocket = new ServerSocket(4000);
        Socket socket = serverSocket.accept();
        System.out.println("----------CONECTADO!----------");

//        estrutura para ler mensagem do socket
        InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String leituraDoBuffer;

//        estrutura para responder mensagens
        PrintStream printStream = new PrintStream(socket.getOutputStream());
        while((leituraDoBuffer = bufferedReader.readLine()) != null){
            printStream.println("Servidor:\t" + leituraDoBuffer);
            System.out.println("RESPONDIDO:\t" + leituraDoBuffer);
        }
    }

    @Override
    public void run() {
        try {
            executarServidor();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
