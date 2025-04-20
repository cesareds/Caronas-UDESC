package Principal;

import Cliente.Cliente;
import Servidor.Servidor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {

        criarClientes();
        mostrarClientes();
        servidor.start();
    }
    public static Servidor servidor = new Servidor();
    public static Scanner scannerInt = new Scanner(System.in);
    public static Scanner scannerString = new Scanner(System.in);
    public static ArrayList<Cliente> clientes = new ArrayList<>();




    public static void criarClientes(){
        System.out.println("Quer criar quantos clientes?");
        int quantidadeDeClientes = scannerInt.nextInt();
        for (int i = 0; i < quantidadeDeClientes; i++) {
            System.out.println("Qual o nome do cliente " + (i+1) + "?");
            String nome = scannerString.nextLine();
            Cliente clienteAux = new Cliente(nome);
            clientes.add(clienteAux);
        }
    }
    public static void mostrarClientes(){
        int i = 1;
        for (Cliente cliente: clientes){
            System.out.println("Cliente " + i + ":\t" + cliente.getNome());
            i++;
        }
    }
}
