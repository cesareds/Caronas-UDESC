package Servidor;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Servidor {
    private static final int PORTA = 4000;
    private static final Map<String, PrintWriter> clientes = new ConcurrentHashMap<>();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static FileWriter logWriter;

    public static void main(String[] args) {
        try {
            logWriter = new FileWriter("server_log.txt", true);
            ServerSocket serverSocket = new ServerSocket(PORTA);
            System.out.println("Servidor iniciado na porta " + PORTA);

            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket).start();
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String nome;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Registrar conexão no log
                String endereco = socket.getInetAddress().getHostAddress();
                String dataHora = sdf.format(new Date());
                logWriter.write("Conexão de " + endereco + " em " + dataHora + " - Nome: " + nome + "\n");
                logWriter.flush();

                out.println("Digite seu nome:");
                nome = in.readLine();
                clientes.put(nome, out);
                broadcast("Servidor", nome + " entrou no chat");

                String mensagem;
                while ((mensagem = in.readLine()) != null) {
                    if (mensagem.equalsIgnoreCase("/sair")) {
                        break;
                    } else if (mensagem.equalsIgnoreCase("/users")) {
                        out.println("Usuários conectados: " + String.join(", ", clientes.keySet()));
                    } else if (mensagem.startsWith("/send message ")) {
                        String[] partes = mensagem.split(" ", 4);
                        if (partes.length == 4) {
                            enviarMensagemPrivada(nome, partes[2], partes[3]);
                        }
                    } else if (mensagem.startsWith("/send file ")) {
                        String[] partes = mensagem.split(" ", 4);
                        if (partes.length == 4) {
                            encaminharArquivo(nome, partes[2], partes[3]);
                        }
                    } else if (mensagem.equals("/cancel")) {
                        // Ignora mensagens de cancelamento
                    }
                }
            } catch (IOException e) {
                System.err.println("Erro no handler: " + e.getMessage());
            } finally {
                if (nome != null) {
                    clientes.remove(nome);
                    broadcast("Servidor", nome + " saiu do chat");
                }
                try { socket.close(); } catch (IOException e) {}
            }
        }

        private void enviarMensagemPrivada(String remetente, String destinatario, String mensagem) {
            PrintWriter destino = clientes.get(destinatario);
            if (destino != null) {
                destino.println(remetente + ": " + mensagem);
            } else {
                out.println("Usuário " + destinatario + " não encontrado.");
            }
        }

        private void broadcast(String remetente, String mensagem) {
            for (PrintWriter writer : clientes.values()) {
                writer.println(remetente + ": " + mensagem);
            }
        }

        private void encaminharArquivo(String remetente, String destinatario, String nomeArquivo) {
            PrintWriter destinoWriter = clientes.get(destinatario);
            if (destinoWriter != null) {
                try {
                    // Envia comando de início para o destinatário
                    destinoWriter.println("/file_start " + remetente + " " + nomeArquivo); //está parando aqui

                    // Transfere os dados do arquivo
                    InputStream is = socket.getInputStream();
                    OutputStream os = socket.getOutputStream();

                    System.out.println("chegou ate aq");

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                        // Verifica por comando especial de cancelamento
                        if (new String(buffer, 0, bytesRead).contains("/cancel")) {
                            break;
                        }
                    }

                    // Envia comando de fim
                    destinoWriter.println("/file_end " + nomeArquivo);

                } catch (Exception e) {
                    System.err.println("Erro ao transferir arquivo: " + e.getMessage());
                }
            } else {
                out.println("Usuário " + destinatario + " não encontrado.");
            }
        }
    }
}