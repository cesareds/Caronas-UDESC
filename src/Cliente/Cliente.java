package Cliente;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Cliente {
    private static String enderecoServidor = "localhost";
    private static final int PORTA_SERVIDOR = 4000;
    private static ObjectOutputStream oos;
    private static ObjectInputStream ois;
    private static Socket socket;
    private static String meuNomeUsuario;

    private static final AtomicBoolean conectado = new AtomicBoolean(false);
    private static Thread threadReceptora;

    public static void main(String[] args) {
        if (args.length > 0) {
            enderecoServidor = args[0];
        }

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(enderecoServidor, PORTA_SERVIDOR), 10000);
            System.out.println("Conectado ao servidor: " + enderecoServidor + ":" + PORTA_SERVIDOR);
            conectado.set(true);

            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());

            threadReceptora = new Thread(() -> {
                try {
                    while (conectado.get()) {
                        Object mensagemDoServidor = ois.readObject();
                        if (mensagemDoServidor instanceof String) {
                            processarMensagemServidor((String) mensagemDoServidor);
                        }
                    }
                } catch (EOFException e) {
                    if (conectado.get()) {
                        System.out.println("Desconexão inesperada do servidor (EOF).");
                    }
                } catch (SocketException e) {
                    if (conectado.get()) {
                        System.out.println("Conexão com o servidor perdida: " + e.getMessage());
                    }
                } catch (IOException | ClassNotFoundException e) {
                    if (conectado.get()) {
                        System.err.println("Erro ao receber mensagem do servidor: " + e.getMessage());
                    }
                } finally {
                    System.out.println("Thread receptora finalizada.");
                    if (conectado.getAndSet(false)) {
                        fecharConexao();
                    }
                }
            });
            threadReceptora.start();

            Scanner scanner = new Scanner(System.in);
            String linhaDoUsuario;

            System.out.println("Aguardando prompt do servidor para nome de usuário...");

            while (conectado.get() && scanner.hasNextLine()) {
                linhaDoUsuario = scanner.nextLine().trim();

                if (meuNomeUsuario == null && !linhaDoUsuario.isEmpty()) {
                    oos.writeObject(linhaDoUsuario);
                    oos.flush();
                    System.out.println("Nome de usuário '" + linhaDoUsuario + "' enviado. Aguardando confirmação...");
                    continue;
                }

                if (meuNomeUsuario != null) {
                    if (linhaDoUsuario.toLowerCase().startsWith("/send file ")) {
                        processarEnvioArquivo(linhaDoUsuario);
                    } else if (linhaDoUsuario.equalsIgnoreCase("/sair")) {
                        System.out.println("Comando /sair detectado. Desconectando...");
                        oos.writeObject(linhaDoUsuario);
                        oos.flush();
                        conectado.set(false);
                        break;
                    } else if (!linhaDoUsuario.isEmpty()) {
                        oos.writeObject(linhaDoUsuario);
                        oos.flush();
                    }
                } else if (conectado.get()) {
                    if (linhaDoUsuario.isEmpty() && meuNomeUsuario == null) {
                        System.out.println("Por favor, digite seu nome de usuário como solicitado pelo servidor.");
                    }
                }
            }
            scanner.close();

        } catch (UnknownHostException e) {
            System.err.println("Host desconhecido: " + enderecoServidor);
        } catch (ConnectException e) {
            System.err.println("Não foi possível conectar ao servidor (Connection timed out ou refused): " + e.getMessage() +
                    ". Verifique se o servidor está rodando e acessível em " + enderecoServidor + ":" + PORTA_SERVIDOR);
        } catch (IOException e) {
            System.err.println("Erro de comunicação com o servidor: " + e.getMessage());
        } finally {
            if (conectado.getAndSet(false)) {
                fecharConexao();
            }
            if (threadReceptora != null && threadReceptora.isAlive()) {
                try {
                    System.out.println("Aguardando finalização da thread receptora...");
                    threadReceptora.join(2000);
                    if (threadReceptora.isAlive()) {
                        System.out.println("Thread receptora não finalizou, interrompendo...");
                        threadReceptora.interrupt();
                    }
                } catch (InterruptedException e) {
                    System.err.println("Interrompido enquanto aguardava a thread receptora.");
                    Thread.currentThread().interrupt();
                }
            }
            System.out.println("Cliente encerrado.");
        }
    }

    private static void processarMensagemServidor(String msgStr) {
        if (msgStr.startsWith("Por favor, digite seu nome de usuário:")) {
            System.out.println(msgStr);
            System.out.print("> ");
        } else if (msgStr.startsWith("ERRO: Nome de usuário inválido ou já em uso")) {
            System.err.println(msgStr);
            System.out.println("Desconectando devido a erro de nome de usuário.");
            conectado.set(false);
        } else if (msgStr.startsWith("Conectado com sucesso como ")) {
            meuNomeUsuario = msgStr.substring("Conectado com sucesso como ".length());
            System.out.println(msgStr + " (Você)");
            System.out.println("Digite seus comandos (ex: /send message Fulano Olá, /send file Fulano caminho/arquivo.txt, /users, /sair):");
            System.out.print(meuNomeUsuario + "> ");
        } else if (msgStr.startsWith("INCOMING_FILE:")) {
            String[] partes = msgStr.split(":", 4);
            if (partes.length == 4) {
                String remetente = partes[1];
                String nomeArquivo = partes[2];
                try {
                    long tamanhoArquivo = Long.parseLong(partes[3]);
                    System.out.println("Recebendo arquivo '" + nomeArquivo + "' (" + tamanhoArquivo + " bytes) de " + remetente + "...");
                    Object obj = ois.readObject();
                    if (obj instanceof byte[]) {
                        byte[] conteudoArquivo = (byte[]) obj;
                        if (conteudoArquivo.length == tamanhoArquivo) {
                            receberArquivo(nomeArquivo, conteudoArquivo, remetente);
                        } else {
                            System.err.println("ERRO: Tamanho do arquivo recebido (" + conteudoArquivo.length +
                                    ") não corresponde ao esperado (" + tamanhoArquivo + ") para " + nomeArquivo);
                        }
                    } else {
                        System.err.println("ERRO: Esperava bytes de arquivo após INCOMING_FILE, mas recebi: " + obj.getClass().getName());
                    }
                } catch (NumberFormatException e) {
                    System.err.println("ERRO: Formato inválido para tamanho do arquivo em INCOMING_FILE: " + partes[3]);
                } catch (IOException | ClassNotFoundException e) {
                    System.err.println("ERRO ao processar dados do arquivo recebido: " + e.getMessage());
                }
            } else {
                System.err.println("ERRO: Formato inválido da mensagem INCOMING_FILE: " + msgStr);
            }
            System.out.print((meuNomeUsuario != null ? meuNomeUsuario : "") + "> ");
        } else if (msgStr.startsWith("ERRO_ARQUIVO:")) {
            System.err.println("Erro relacionado a arquivo vindo do servidor: " + msgStr.substring("ERRO_ARQUIVO:".length()).trim());
            System.out.print((meuNomeUsuario != null ? meuNomeUsuario : "") + "> ");
        } else if (msgStr.startsWith("ERRO:")) {
            System.err.println(msgStr);
            System.out.print((meuNomeUsuario != null ? meuNomeUsuario : "") + "> ");
        }
        else {
            System.out.println(msgStr);
            System.out.print((meuNomeUsuario != null ? meuNomeUsuario : "") + "> ");
        }
    }

    private static void processarEnvioArquivo(String linhaComando) throws IOException {
        String[] partes = linhaComando.split("\\s+", 4);
        if (partes.length == 4) {
            String destinatario = partes[2];
            String caminhoArquivoLocal = partes[3];
            Path path = Paths.get(caminhoArquivoLocal);

            if (Files.exists(path) && Files.isRegularFile(path)) {
                try {
                    byte[] bytesArquivo = Files.readAllBytes(path);
                    long tamanhoArquivo = bytesArquivo.length;
                    String nomeArquivoOriginal = path.getFileName().toString();

                    String comandoServidor = "/send file " + destinatario + " " + nomeArquivoOriginal;
                    oos.writeObject(comandoServidor);
                    oos.writeLong(tamanhoArquivo);
                    oos.writeObject(bytesArquivo);
                    oos.flush();
                    System.out.println("Enviando arquivo '" + nomeArquivoOriginal + "' ("+ tamanhoArquivo +" bytes) para " + destinatario + "...");
                } catch (IOException e) {
                    System.err.println("ERRO ao ler o arquivo local '" + caminhoArquivoLocal + "': " + e.getMessage());
                }
            } else {
                System.err.println("ERRO: Arquivo não encontrado ou não é um arquivo regular: " + caminhoArquivoLocal);
            }
        } else {
            System.err.println("Formato inválido. Use: /send file <destinatario> <caminho_completo_do_arquivo_local>");
        }
        System.out.print((meuNomeUsuario != null ? meuNomeUsuario : "") + "> ");
    }

    private static void receberArquivo(String nomeArquivo, byte[] conteudoArquivo, String remetente) {
        try {
            Path diretorioAtual = Paths.get("").toAbsolutePath();
            Path arquivoRecebido = diretorioAtual.resolve(nomeArquivo);

            int contador = 1;
            String nomeBase = nomeArquivo.contains(".") ? nomeArquivo.substring(0, nomeArquivo.lastIndexOf('.')) : nomeArquivo;
            String extensao = nomeArquivo.contains(".") ? nomeArquivo.substring(nomeArquivo.lastIndexOf('.')) : "";
            while (Files.exists(arquivoRecebido)) {
                arquivoRecebido = diretorioAtual.resolve(nomeBase + "_" + contador + extensao);
                contador++;
            }

            Files.write(arquivoRecebido, conteudoArquivo);
            System.out.println("\nArquivo '" + nomeArquivo + "' de '" + remetente + "' recebido e salvo como: " + arquivoRecebido.toAbsolutePath());
        } catch (InvalidPathException e) {
            System.err.println("Erro: Nome de arquivo inválido recebido de " + remetente + ": " + nomeArquivo + " (" + e.getMessage() + ")");
        } catch (IOException e) {
            System.err.println("Erro ao salvar o arquivo recebido '" + nomeArquivo + "' de " + remetente + ": " + e.getMessage());
        }
    }

    private static void fecharConexao() {
        if (!conectado.getAndSet(false)) {
            return;
        }
        System.out.println("Fechando conexão com o servidor...");
        try {
            if (oos != null) {
                oos.close();
            }
        } catch (IOException e) {
            // Silenciado intencionalmente
        }
        try {
            if (ois != null) ois.close();
        } catch (IOException e) {
            // Silenciado intencionalmente
        }
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Erro ao fechar socket: " + e.getMessage());
        }
        System.out.println("Conexão fechada.");
    }
}