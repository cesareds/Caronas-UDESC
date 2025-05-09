import Servidor.Servidor;
import Cliente.Cliente;
import java.util.InputMismatchException;
import java.util.Scanner;

// Para compilar (assumindo que Servidor.java está em uma pasta Servidor/ e Cliente.java em uma pasta Cliente/,
// e ambas as pastas estão no mesmo nível que Main.java):
// 1. Navegue até o diretório que contém Main.java, Servidor/ e Cliente/
// 2. javac Main.java Servidor/Servidor.java Cliente/Cliente.java
//
// Para executar:
// java Main

public class Main {
    private static final int PORTA_SERVIDOR_PADRAO = Servidor.PORTA;
    private static final String ENDERECO_SERVIDOR_PADRAO = "localhost";

    public static void main(String[] args) {
        System.out.println("====================================");
        System.out.println("  SISTEMA DE CHAT COM SOCKETS");
        System.out.println("  Conectando Cliente a: " + ENDERECO_SERVIDOR_PADRAO + ":" + PORTA_SERVIDOR_PADRAO);
        System.out.println("====================================");
        System.out.println("1 - Iniciar Servidor");
        System.out.println("2 - Iniciar Cliente (em " + ENDERECO_SERVIDOR_PADRAO + ")");
        System.out.println("3 - Sair");
        System.out.print("Escolha uma opção (1-3): ");

        Scanner scanner = new Scanner(System.in);
        int opcao = -1;

        try {
            if (scanner.hasNextInt()) {
                opcao = scanner.nextInt();
            } else {
                System.out.println("Entrada inválida. Por favor, digite um número.");
                scanner.close();
                return;
            }
        } catch (InputMismatchException e) {
            System.out.println("Opção inválida. Por favor, insira um número (1-3).");
            scanner.close();
            return;
        } finally {
            scanner.nextLine();
        }


        switch (opcao) {
            case 1:
                iniciarServidor();
                break;
            case 2:
                iniciarCliente(ENDERECO_SERVIDOR_PADRAO);
                break;
            case 3:
                System.out.println("Saindo do aplicativo...");
                break;
            default:
                System.out.println("Opção inválida! Por favor, escolha entre 1 e 3.");
        }
        scanner.close();
    }

    private static void iniciarServidor() {
        System.out.println("\nIniciando servidor na porta " + PORTA_SERVIDOR_PADRAO + "...");
        System.out.println("O servidor permanecerá ativo. Pressione CTRL+C para encerrá-lo manualmente quando desejado.\n");
        try {
            Servidor.main(new String[]{});
        } catch (Exception e) {
            System.err.println("Ocorreu um erro crítico ao tentar iniciar o servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void iniciarCliente(String enderecoServidor) {
        System.out.println("\nTentando conectar o cliente ao servidor em " + enderecoServidor + ":" + PORTA_SERVIDOR_PADRAO + "\n");
        try {
            Cliente.main(new String[]{enderecoServidor});
        } catch (Exception e) {
            System.err.println("Ocorreu um erro crítico ao tentar iniciar o cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }
}