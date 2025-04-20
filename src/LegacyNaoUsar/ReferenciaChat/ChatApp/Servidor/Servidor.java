package LegacyNaoUsar.ReferenciaChat.ChatApp.Servidor;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Servidor {
    private static final int PORTA = 4000;
    private static final Map<String, PrintStream> clientesConectados = new ConcurrentHashMap<>();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws IOException {
        ServerSocket servidorSocket = new ServerSocket(PORTA);
        System.out.println("Servidor iniciado na porta " + PORTA);
        FileWriter logWriter = new FileWriter("log.txt", true);

        while (true) {
            Socket socket = servidorSocket.accept();
            String endereco = socket.getInetAddress().getHostAddress();
            String dataHora = sdf.format(new Date());
            logWriter.write("Conexão de " + endereco + " em " + dataHora + "\n");
            logWriter.flush();

            new Thread(new ClienteHandler(socket)).start();
        }
    }

    static class ClienteHandler implements Runnable {
        private Socket socket;
        private String nome;
        private BufferedReader entrada;
        private PrintStream saida;

        public ClienteHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.saida = new PrintStream(socket.getOutputStream());
        }

        public void run() {
            try {
                saida.println("Digite seu nome:");
                nome = entrada.readLine();
                clientesConectados.put(nome, saida);
                broadcast("Servidor", nome + " entrou no chat");

                String linha;
                while ((linha = entrada.readLine()) != null) {
                    if (linha.equals("/sair")) break;
                    if (linha.equals("/users")) {
                        saida.println("Usuários conectados: " + clientesConectados.keySet());
                    } else if (linha.startsWith("/send message ")) {
                        String[] partes = linha.split(" ", 4);
                        if (partes.length < 4) continue;
                        String destinatario = partes[2];
                        String mensagem = partes[3];
                        enviarMensagem(nome, destinatario, mensagem);
                    } else if (linha.startsWith("/send file ")) {
                        String[] partes = linha.split(" ", 4);
                        if (partes.length < 4) continue;
                        String destinatario = partes[2];
                        String nomeArquivo = partes[3];
                        receberEEnviarArquivo(destinatario, nomeArquivo);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientesConectados.remove(nome);
                    socket.close();
                    broadcast("Servidor", nome + " saiu do chat");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void enviarMensagem(String remetente, String destinatario, String mensagem) {
            PrintStream destino = clientesConectados.get(destinatario);
            if (destino != null) {
                destino.println(remetente + ": " + mensagem);
            } else {
                saida.println("Usuário " + destinatario + " não encontrado.");
            }
        }

        private void broadcast(String remetente, String mensagem) {
            for (PrintStream ps : clientesConectados.values()) {
                ps.println(remetente + ": " + mensagem);
            }
        }

        private void receberEEnviarArquivo(String destinatario, String nomeArquivo) throws IOException {
            PrintStream destino = clientesConectados.get(destinatario);
            if (destino != null) {
                saida.println("Enviando arquivo...");
                destino.println("/file " + nome + " " + nomeArquivo);
                InputStream is = socket.getInputStream();
                OutputStream os = ((Socket) destino).getOutputStream(); // Não funciona diretamente, adaptação será feita
            } else {
                saida.println("Usuário " + destinatario + " não encontrado.");
            }
        }
    }
}
