package Cliente;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Cliente {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String nome;
    private final BlockingQueue<String> userInputQueue = new ArrayBlockingQueue<>(10);
    private volatile boolean running = true;
    private final Scanner mainScanner = new Scanner(System.in); // Scanner único para todo o programa

    public static void main(String[] args) {
        System.out.print("Digite seu nome: ");
        Scanner scanner = new Scanner(System.in);
        String nome = scanner.nextLine();

        Cliente cliente = new Cliente();
        cliente.iniciar(nome);
    }

    public void iniciar(String nome) {
        this.nome = nome;

        try {
            socket = new Socket("localhost", 4000);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Thread para receber mensagens
            new Thread(this::receberMensagens).start();

            // Enviar nome para o servidor
            out.println(nome);

            // Loop principal para comandos do usuário
            while (running) {
                String comando = mainScanner.nextLine();

                if (comando.equalsIgnoreCase("/sair")) {
                    out.println("/sair");
                    running = false;
                    break;
                } else if (comando.equalsIgnoreCase("/users")) {
                    out.println("/users");
                } else if (comando.startsWith("/send message ")) {
                    out.println(comando);
                } else if (comando.startsWith("/send file ")) {
                    out.println(comando);
                    enviarArquivo(comando.split(" ")[3]);
                }
            }
        } catch (IOException e) {
            System.err.println("Erro no cliente: " + e.getMessage());
        } finally {
            running = false;
            mainScanner.close();
            try { socket.close(); } catch (IOException e) {}
        }
    }

    private void receberMensagens() {
        try {
            String mensagem;
            while ((mensagem = in.readLine()) != null && running) {
                if (mensagem.startsWith("/file_request ")) {
                    processarSolicitacaoArquivo(mensagem);
                } else if (mensagem.startsWith("/file_complete ")) {
                    System.out.println("Transferência concluída: " + mensagem.split(" ")[1]);
                } else {
                    System.out.println(mensagem);
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Erro ao receber mensagens: " + e.getMessage());
            }
        }
    }

    private void processarSolicitacaoArquivo(String mensagem) {
        String[] partes = mensagem.split(" ");
        String remetente = partes[1];
        String nomeArquivo = partes[2];

        System.out.print("Deseja receber o arquivo '" + nomeArquivo + "' de " + remetente + "? (s/n) ");

        // Usar o Scanner principal de forma sincronizada
        String resposta;
        synchronized (mainScanner) {
            resposta = mainScanner.nextLine();
        }

        if (resposta.equalsIgnoreCase("s")) {
            out.println("/accept_file");
            receberArquivo(nomeArquivo);
        } else {
            out.println("/reject_file");
            System.out.println("Transferência recusada");
        }
    }
    private void receberArquivo(String mensagem) {
        String[] partes = mensagem.split(" ");
        String comando = partes[0];
        String remetente = partes[1];
        String nomeArquivo = partes[2];

        try {
            if (comando.equals("/file_start")) {
                System.out.println("Preparando para receber arquivo " + nomeArquivo + " de " + remetente);

                FileOutputStream fos = new FileOutputStream(nomeArquivo);
                InputStream is = socket.getInputStream();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while (true) {
                    bytesRead = is.read(buffer);
                    if (bytesRead == -1) break;

                    // Verifica se é o comando de fim
                    String conteudo = new String(buffer, 0, bytesRead);
                    if (conteudo.contains("/file_end")) {
                        break;
                    }

                    fos.write(buffer, 0, bytesRead);
                }
                fos.close();
                System.out.println("Arquivo recebido com sucesso: " + nomeArquivo);
            }
        } catch (IOException e) {
            System.err.println("Erro ao receber arquivo: " + e.getMessage());
        }
    }

    private void enviarArquivo(String caminhoArquivo) {
        try (FileInputStream fis = new FileInputStream(caminhoArquivo)) {
            OutputStream os = socket.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1 && running) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
            System.out.println("Arquivo enviado: " + caminhoArquivo);
        } catch (IOException e) {
            System.err.println("Erro ao enviar arquivo: " + e.getMessage());
        }
    }
}