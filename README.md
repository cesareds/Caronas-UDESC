# ChatApp - Sistema de Mensagens Instantâneas via Sockets  

![Java](https://img.shields.io/badge/Java-17%2B-blue)  
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

Bem-vindo ao **ChatApp**, um sistema de comunicação em tempo real desenvolvido como projeto para a disciplina de **Programação para Dispositivos Móveis** do curso de **Tecnólogo em Análise e Desenvolvimento de Sistemas** na **UDESC**.  

Este projeto consiste em um **servidor** que gerencia conexões de múltiplos **clientes**, permitindo o envio de mensagens de texto e arquivos entre usuários conectados.  

## 📌 Funcionalidades  

✔ **Comunicação cliente-servidor-cliente**  
✔ **Listagem de usuários online** via comando `/users`  
✔ **Envio de mensagens privadas** com `/send message <destino> <mensagem>`  
✔ **Transferência de arquivos** usando `/send file <destino> <caminho_do_arquivo>`  
✔ **Log de conexões** no servidor (IP, data e hora)  
✔ **Encerramento de sessão** com `/sair`  

## ⚙️ Arquitetura  

- **Servidor**:  
  - Gerencia conexões de clientes.  
  - Roteia mensagens e arquivos.  
  - Armazena logs em `server_log.txt`.  

- **Cliente**:  
  - Conecta-se ao servidor.  
  - Envia/recebe mensagens e arquivos.  
  - Exibe mensagens recebidas no console.  

## 🚀 Como Executar  

### Pré-requisitos  
- Java 17+  
- IDE ou terminal  

### Passos:  

1. **Inicie o Servidor**:  
   ```bash  
   java Server <porta>  
   ```  
   Exemplo:  
   ```bash  
   java Server 12345  
   ```  

2. **Conecte Clientes**:  
   ```bash  
   java Client <ip_servidor> <porta> <nome_usuario>  
   ```  
   Exemplo:  
   ```bash  
   java Client localhost 12345 Alice  
   ```  

## 📋 Comandos Disponíveis  

| Comando | Descrição |  
|---------|-----------|  
| `/users` | Lista usuários conectados |  
| `/send message <destino> <msg>` | Envia mensagem para um usuário |  
| `/send file <destino> <caminho>` | Envia um arquivo |  
| `/sair` | Desconecta do servidor |  

## 📂 Estrutura do Projeto  

```  
ChatApp/  
├── src/  
│   ├── Server.java         # Lógica do servidor  
│   ├── Client.java         # Lógica do cliente  
│   └── ...                 # Classes auxiliares  
├── server_log.txt          # Log de conexões  
└── README.md               # Este arquivo  
```  

## 📄 Licença  

Este projeto está sob a licença **GNU General Public License v3.0**. Consulte o arquivo [LICENSE](LICENSE) para mais detalhes.  

---  

Desenvolvido por:  
🔹 [César Eduardo de Souza](https://github.com/cesareds)  
🔹 [Débora Lawall](https://github.com/user2)  
🔹 [Lucas Thomas](https://github.com/user3)  
🔹 [Tamy Gabrielle](https://github.com/TamyGabrielle)  

📅 **Prazo de entrega**: 09/05/2024  
🎓 **UDESC - Programação para Dispositivos Móveis**