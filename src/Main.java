import Servidor.Servidor;
import Cliente.Cliente;
import java.util.Scanner;

//para compilar: abra o terminal:
//1 - javac Main.java Servidor/Servidor.java Cliente/Cliente.java
//2- java Main
public class Main {
    public static void main(String[] args) {
        System.out.println("====================================");
        System.out.println("  SISTEMA DE CHAT COM SOCKETS");
        System.out.println("====================================");
        System.out.println("1 - Iniciar Servidor");
        System.out.println("2 - Iniciar Cliente");
        System.out.println("3 - Sair");
        System.out.print("Escolha uma opção: ");

        Scanner scanner = new Scanner(System.in);
        int opcao = scanner.nextInt();
        scanner.nextLine(); // Limpar buffer

        switch (opcao) {
            case 1:
                iniciarServidor();
                break;
            case 2:
                iniciarCliente(scanner);
                break;
            case 3:
                System.out.println("Saindo...");
                break;
            default:
                System.out.println("Opção inválida!");
        }
    }

    private static void iniciarServidor() {
        System.out.println("\nIniciando servidor na porta 4000...");
        System.out.println("Pressione CTRL+C para encerrar o servidor\n");
        Servidor.main(new String[]{});
    }

    private static void iniciarCliente(Scanner scanner) {
        System.out.print("\nDigite seu nome: ");
        String nome = scanner.nextLine();
        System.out.println("Iniciando cliente para " + nome + "...");
        System.out.println("Conectando ao servidor localhost:4000\n");
        Cliente.main(new String[]{nome});
    }
}