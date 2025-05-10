package Servidor;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Servidor {
    public static final int PORTA = 4000;
    private static final Map<String, ClientHandler> clientesConectados = new ConcurrentHashMap<>();
    private static final Logger logger = Logger.getLogger(Servidor.class.getName());
    private static FileHandler fileHandlerLog;

    public static void main(String[] args) {
        try {
            fileHandlerLog = new FileHandler("conexões_servidor.log", true);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandlerLog.setFormatter(formatter);
            logger.addHandler(fileHandlerLog);
            logger.setLevel(Level.INFO);
        } catch (IOException e) {
            System.err.println("Erro ao configurar o logger: " + e.getMessage());
            return;
        }

        System.out.println("Servidor de chat iniciado na porta " + PORTA);
        logger.info("Servidor iniciado na porta " + PORTA + " em " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new ClientHandler(clientSocket)).start();
                } catch (IOException e) {
                    System.err.println("Erro ao aceitar conexão de cliente: " + e.getMessage());
                    logger.log(Level.SEVERE, "Erro ao aceitar nova conexão de cliente", e);
                }
            }
        } catch (IOException e) {
            System.err.println("Erro fatal ao iniciar o servidor na porta " + PORTA + ": " + e.getMessage());
            logger.log(Level.SEVERE, "Erro fatal no servidor na porta " + PORTA, e);
        } finally {
            if (fileHandlerLog != null) {
                fileHandlerLog.close();
            }
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private String nomeUsuario;
        private ObjectInputStream ois;
        private ObjectOutputStream oos;
        private final Map<String, BiConsumer<String, String[]>> commandHandlers = new ConcurrentHashMap<>();


        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            initializeCommandHandlers();
        }

        private void initializeCommandHandlers() {
            commandHandlers.put("/send", this::handleSendCommand);
            commandHandlers.put("/users", (cmd, args) -> listarUsuarios());
            commandHandlers.put("/sair", (cmd, args) -> {});
        }

        public ObjectOutputStream getObjectOutputStream() {
            return oos;
        }

        public String getNomeUsuario() {
            return nomeUsuario;
        }

        @Override
        public void run() {
            try {
                oos = new ObjectOutputStream(clientSocket.getOutputStream());
                ois = new ObjectInputStream(clientSocket.getInputStream());

                oos.writeObject("Por favor, digite seu nome de usuário:");
                String nomeTentativo = (String) ois.readObject();

                synchronized (clientesConectados) {
                    if (nomeTentativo == null || nomeTentativo.trim().isEmpty() || clientesConectados.containsKey(nomeTentativo.trim())) {
                        oos.writeObject("ERRO: Nome de usuário inválido ou já em uso. Desconectando.");
                        oos.flush();
                        return;
                    }
                    this.nomeUsuario = nomeTentativo.trim();
                    clientesConectados.put(this.nomeUsuario, this);
                }

                String clientIpAddress = clientSocket.getInetAddress().getHostAddress();
                System.out.println("Nova conexão de: " + clientIpAddress + " como " + nomeUsuario);
                String logMessage = String.format("Cliente conectado: %s | IP: %s | Data/Hora: %s",
                        nomeUsuario, clientIpAddress, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                logger.info(logMessage);

                oos.writeObject("Conectado com sucesso como " + nomeUsuario);
                enviarMensagemParaTodos("Info: " + nomeUsuario + " entrou no chat.", true, null);

                Object input;
                while ((input = ois.readObject()) != null) {
                    if (input instanceof String) {
                        String linhaComando = (String) input;
                        System.out.println("[" + nomeUsuario + "]: " + linhaComando);

                        String[] parts = linhaComando.trim().split("\\s+", 2);
                        String command = parts[0].toLowerCase();
                        String[] args = (parts.length > 1 && parts[1] != null) ? parts[1].split("\\s+") : new String[0];

                        if (command.equals("/sair")) {
                            break;
                        }

                        if (command.equals("/send")) {
                            if (args.length > 0) {
                                String subCommand = args[0].toLowerCase();
                                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                                if (subCommand.equals("message")) {
                                    processarMensagemTexto(subArgs);
                                } else if (subCommand.equals("file")) {
                                    processarComandoEnvioArquivo(subArgs);
                                } else {
                                    oos.writeObject("ERRO: Subcomando desconhecido para /send. Use /send message ou /send file.");
                                }
                            } else {
                                oos.writeObject("ERRO: Comando /send incompleto. Especifique 'message' ou 'file'.");
                            }
                        } else {
                            BiConsumer<String, String[]> handler = commandHandlers.get(command);
                            if (handler != null) {
                                handler.accept(command, args);
                            } else {
                                oos.writeObject("ERRO: Comando desconhecido: " + command);
                            }
                        }
                    }
                }

            } catch (EOFException | SocketException e) {
                String msgDesconexao = (nomeUsuario != null ? nomeUsuario : "Cliente (" + clientSocket.getInetAddress().getHostAddress() + ")") + " desconectou-se.";
                System.out.println(msgDesconexao);
                if (nomeUsuario != null) {
                    logger.info(msgDesconexao + " (" + e.getClass().getSimpleName() + ")");
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Erro no handler para " + (nomeUsuario != null ? nomeUsuario : "cliente desconhecido") + ": " + e.getMessage());
                logger.log(Level.WARNING, "Erro no handler para " + (nomeUsuario != null ? nomeUsuario : "cliente desconhecido"), e);
            } finally {
                removerCliente();
            }
        }

        private void handleSendCommand(String command, String[] args) {
            try {
                if (args.length > 0) {
                    String subCommand = args[0].toLowerCase();
                    String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                    if (subCommand.equals("message")) {
                        processarMensagemTexto(subArgs);
                    } else if (subCommand.equals("file")) {
                        processarComandoEnvioArquivo(subArgs);
                    } else {
                        oos.writeObject("ERRO: Subcomando desconhecido para /send. Use /send message ou /send file.");
                    }
                } else {
                    oos.writeObject("ERRO: Comando /send incompleto. Especifique 'message' ou 'file'.");
                }
            } catch (IOException e) {
                System.err.println("Erro ao processar comando /send no handler: " + e.getMessage());
                logger.log(Level.WARNING, "Erro no handleSendCommand para " + nomeUsuario, e);
            }
        }


        private void processarMensagemTexto(String[] args) throws IOException {
            if (args.length < 2) {
                oos.writeObject("ERRO: Formato inválido. Use: /send message <destinatario> <mensagem>");
                return;
            }
            String destinatarioNome = args[0];
            StringBuilder mensagemBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                mensagemBuilder.append(args[i]).append(i == args.length - 1 ? "" : " ");
            }
            String mensagem = mensagemBuilder.toString();

            ClientHandler destinatarioHandler = clientesConectados.get(destinatarioNome);
            if (destinatarioHandler != null) {
                if (destinatarioHandler == this) {
                    oos.writeObject("Info: Você não pode enviar uma mensagem para si mesmo desta forma.");
                    return;
                }
                destinatarioHandler.getObjectOutputStream().writeObject(nomeUsuario + ": " + mensagem);
                oos.writeObject("Mensagem enviada para " + destinatarioNome);
                logger.info("Mensagem de " + nomeUsuario + " para " + destinatarioNome);
            } else {
                oos.writeObject("ERRO: Usuário '" + destinatarioNome + "' não encontrado ou offline.");
            }
        }

        private void processarComandoEnvioArquivo(String[] args) throws IOException {
            if (args.length < 2) {
                oos.writeObject("ERRO: Formato inválido. Use: /send file <destinatario> <nome_do_arquivo_no_remetente>");
                return;
            }
            String destinatarioNome = args[0];
            String nomeArquivoOriginal = args[1];

            ClientHandler destinatarioHandler = clientesConectados.get(destinatarioNome);
            if (destinatarioHandler == null) {
                oos.writeObject("ERRO: Usuário '" + destinatarioNome + "' não encontrado ou offline para envio de arquivo.");
                try {
                    ois.readLong();
                    ois.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    logger.warning("Falha ao consumir dados de arquivo não entregue de " + nomeUsuario + " para " + destinatarioNome + ": " + e.getMessage());
                }
                return;
            }
            if (destinatarioHandler == this) {
                oos.writeObject("Info: Você não pode enviar um arquivo para si mesmo desta forma.");
                try {
                    ois.readLong();
                    ois.readObject();
                } catch (IOException | ClassNotFoundException e) { }
                return;
            }

            try {
                long tamanhoArquivo = ois.readLong();
                byte[] conteudoArquivo = (byte[]) ois.readObject();

                destinatarioHandler.getObjectOutputStream().writeObject("INCOMING_FILE:" + nomeUsuario + ":" + nomeArquivoOriginal + ":" + tamanhoArquivo);
                destinatarioHandler.getObjectOutputStream().writeObject(conteudoArquivo);
                destinatarioHandler.getObjectOutputStream().flush();

                oos.writeObject("Arquivo '" + nomeArquivoOriginal + "' encaminhado para " + destinatarioNome);
                logger.info("Arquivo '" + nomeArquivoOriginal + "' (" + tamanhoArquivo + " bytes) de " + nomeUsuario + " encaminhado para " + destinatarioNome);

            } catch (IOException | ClassNotFoundException e) {
                String erroMsg = "ERRO: Falha ao processar/encaminhar o arquivo '" + nomeArquivoOriginal + "' para " + destinatarioNome;
                oos.writeObject(erroMsg);
                logger.log(Level.WARNING, "Erro ao processar envio de arquivo de " + nomeUsuario + " para " + destinatarioNome, e);
                if (destinatarioHandler.getObjectOutputStream() != null) {
                    try {
                        destinatarioHandler.getObjectOutputStream().writeObject("ERRO_ARQUIVO: Falha no recebimento do arquivo '" + nomeArquivoOriginal + "' de " + nomeUsuario + " devido a um erro no servidor.");
                    } catch (IOException ex) {
                        logger.log(Level.WARNING, "Falha ao notificar destinatário " + destinatarioNome + " sobre erro de arquivo.", ex);
                    }
                }
            }
        }


        private void listarUsuarios() {
            try {
                if (clientesConectados.isEmpty()) {
                    oos.writeObject("Nenhum usuário conectado no momento.");
                    return;
                }
                StringBuilder lista = new StringBuilder("Usuários conectados (" + clientesConectados.size() + "):\n");
                for (String nome : clientesConectados.keySet()) {
                    lista.append("- ").append(nome).append(nome.equals(this.nomeUsuario) ? " (Você)" : "").append("\n");
                }
                oos.writeObject(lista.toString().trim());
            } catch (IOException e) {
                System.err.println("Erro ao enviar lista de usuários para " + nomeUsuario + ": " + e.getMessage());
                logger.log(Level.WARNING, "Erro ao enviar lista de usuários para " + nomeUsuario, e);
            }
        }

        private void removerCliente() {
            if (this.nomeUsuario != null) {
                ClientHandler removedHandler;
                synchronized (clientesConectados) {
                    removedHandler = clientesConectados.remove(this.nomeUsuario);
                }

                if (removedHandler != null) {
                    String clientIpAddress = clientSocket.getInetAddress().getHostAddress();
                    System.out.println(nomeUsuario + " (" + clientIpAddress + ") desconectou-se.");
                    String logMessage = String.format("Cliente desconectado: %s | IP: %s | Data/Hora: %s",
                            nomeUsuario, clientIpAddress, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                    logger.info(logMessage);
                    enviarMensagemParaTodos("Info: " + nomeUsuario + " saiu do chat.", false, this);
                }
            }
            try {
                if (ois != null) ois.close();
            } catch (IOException e) {
            }
            try {
                if (oos != null) oos.close();
            } catch (IOException e) {
            }
            try {
                if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar socket para " + (nomeUsuario != null ? nomeUsuario : "cliente desconhecido") + ": " + e.getMessage());
            }
        }

        private void enviarMensagemParaTodos(String mensagem, boolean skipCurrentHandlerItself, ClientHandler handlerToSkip) {
            ClientHandler[] handlersArray;
            synchronized(clientesConectados) {
                handlersArray = clientesConectados.values().toArray(new ClientHandler[0]);
            }

            for (ClientHandler handler : handlersArray) {
                if (skipCurrentHandlerItself && handler == this) {
                    continue;
                }
                if (handlerToSkip != null && handler == handlerToSkip) {
                    continue;
                }
                try {
                    if (handler.getNomeUsuario() != null && handler.getObjectOutputStream() != null) {
                        handler.getObjectOutputStream().writeObject(mensagem);
                        handler.getObjectOutputStream().flush();
                    }
                } catch (IOException e) {
                    System.err.println("Erro ao enviar mensagem para " + handler.getNomeUsuario() + ". Pode ter se desconectado: " + e.getMessage());
                }
            }
        }
    }
}