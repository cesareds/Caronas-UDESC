package LegacyNaoUsar.ReferenciaChat.ChatApp.Cliente;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 4000);
        PrintStream saida = new PrintStream(socket.getOutputStream());
        BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        Scanner scanner = new Scanner(System.in);

        new Thread(() -> {
            try {
                String linha;
                while ((linha = entrada.readLine()) != null) {
                    if (linha.startsWith("/file ")) {
                        String[] partes = linha.split(" ", 3);
                        receberArquivo(partes[2], socket);
                    } else {
                        System.out.println(linha);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        while (scanner.hasNextLine()) {
            String comando = scanner.nextLine();
            saida.println(comando);
            if (comando.startsWith("/send file ")) {
                String[] partes = comando.split(" ", 4);
                enviarArquivo(partes[3], socket);
            }
        }
    }

    private static void enviarArquivo(String caminho, Socket socket) throws IOException {
        FileInputStream fis = new FileInputStream(caminho);
        OutputStream os = socket.getOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        os.flush();
        fis.close();
    }

    private static void receberArquivo(String nomeArquivo, Socket socket) throws IOException {
        FileOutputStream fos = new FileOutputStream(nomeArquivo);
        InputStream is = socket.getInputStream();
        byte[] buffer = new byte[4096];
        int bytesRead = is.read(buffer);
        fos.write(buffer, 0, bytesRead);
        fos.close();
        System.out.println("Arquivo " + nomeArquivo + " recebido.");
    }
}
