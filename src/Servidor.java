import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Servidor {
    private static final String ENDEREÇO = "127.0.0.1";
    private static final int PORTA = 6767;
    private static final int MAX_CONEXOES = 4;
    private List<Conexao> conexoes;
    private boolean rodando;
    ServerSocket servidor;
    Scanner teclado;
    
    public Servidor() {
        try {
            this.servidor = new ServerSocket(PORTA, MAX_CONEXOES, InetAddress.getByName(ENDEREÇO));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.teclado = new Scanner(System.in);
        this.conexoes = new ArrayList<>(MAX_CONEXOES);
    }

    public synchronized void addConexao(Conexao c) {
        System.out.println("SERVIDOR: Cliente \"" + c.nome + "\" conectado com sucesso!");
        broadcastMensagemServidor(c.nome + " entrou no chat. Dê boas vindas!");
        conexoes.add(c);
    }
    
    public synchronized void removerConexao(Conexao c) {
        System.out.println("SERVIDOR: Cliente \"" + c.nome + "\" saiu do servidor.");
        conexoes.remove(c);
        c.clienteIn.close();
        c.clienteOut.close();
        try {
            c.clienteSocket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public synchronized void encerrar() {
        broadcastMensagemServidor("O servidor foi encerrado.");
        System.out.println("SERVIDOR: Fechando conexões...");
        for (Conexao c : conexoes) {
            removerConexao(c);
        }
        System.out.println("saiu do for ------------------------");
        teclado.close();
        try {
            servidor.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("SERVIDOR: Servidor encerrado com sucesso");
    }

    public synchronized void broadcastMensagem(Conexao remetente, String msg) {
        String msgCompleta = "" + remetente.nome + ": " + msg;
        // System.out.println(msgCompleta);
        for (Conexao c : conexoes) {
            if (c.equals(remetente)) continue;
            c.clienteOut.println(msgCompleta);
        }
    }

    private synchronized void broadcastMensagemServidor(String msg) {
        for (Conexao c : conexoes) {
            c.clienteOut.println("SERVIDOR: " + msg);
        }
    }

    public void info() {
        System.out.println(String.format("Clientes conectados: (%d/%d)\n%s\n", conexoes.size(), MAX_CONEXOES, conexoes));
    }

    public void help() {
        System.out.println("Comandos disponíveis:\n\t'help' -- Mostra essa mensagem.\n\t'info' -- Mostra informações sobre as conexões atuais.\n\t'exit' -- Encerra o servidor.");
    }
    
    public void iniciar() {
        this.rodando = true;
        new Gerenciador(teclado, this);

        System.out.println("SERVIDOR: Socket em " + ENDEREÇO + ":" + PORTA + " aberto!");
        System.out.println("Digite 'help' para ver os comandos disponíveis.");

        Socket cliente;
        while (rodando) {
            System.out.println("SERVIDOR: Aguardando conexão com cliente...");
            try {
                cliente = servidor.accept();
            } catch (IOException e) {
                // TODO: lidar com esse erro no caso do servidor encerrar enquanto espera uma conexão
                System.err.println("SERVIDOR: erro fatal no socket servidor. Encerrando...");
                //encerrar();
                break;
            }
            System.out.println("SERVIDOR: Conexão estabelecida com cliente em " + cliente.getInetAddress());
            
            Scanner leitor;
            PrintStream saida;
            try {
                leitor = new Scanner(cliente.getInputStream());
                saida = new PrintStream(cliente.getOutputStream());
            } catch (IOException e) {
                System.err.println("SERVIDOR: erro fatal ao obter canal de entrada do cliente. Encerrando...");
                encerrar();
                break;
            }
            
            new Conexao(this, cliente, leitor, saida);
        }
        // TODO: lidar com max de conexões
        // System.out.println("SERVIDOR: limite de conexões atingido (" + conexoes.size() + "/" + MAX_CONEXOES + "). Parando de aceitar novas conexões");
    }
    
    public static void main(String[] args) throws IOException {
        
        new Servidor().iniciar();
    }
}

class Gerenciador implements Runnable {
    Scanner teclado;
    Servidor servidor;

    Gerenciador(Scanner teclado, Servidor servidor) {
        this.teclado = teclado;
        this.servidor = servidor;

        new Thread(this, "gerenciador").start();
    }

    public void run() {
        while (teclado.hasNextLine()) {
            String s = teclado.nextLine();

            if (s.equals("exit")) {
                break;
            }
            if (s.equals("info")) {
                servidor.info();
            }
            if (s.equals("help")) {
                servidor.help();
            }
        }
        servidor.encerrar();
    }
}

class Conexao implements Runnable {
    private static final String NOME_PADRAO = "Usuário";

    Socket clienteSocket;
    Scanner clienteIn;
    PrintStream clienteOut;
    Servidor servidor;
    String nome;
    
    public Conexao(Servidor servidor, Socket clienteSocket, Scanner clienteIn, PrintStream clienteOut) {
        this.servidor = servidor;
        this.clienteIn = clienteIn;
        this.clienteOut = clienteOut;
        this.clienteSocket = clienteSocket;

        iniciar();
    }
    
    public void iniciar() {
        System.out.println("SERVIDOR: Esperando cliente digitar seu nome...");
        nome = "";
        if (clienteIn.hasNextLine()) nome = clienteIn.nextLine();
        nome = nome.trim();
        if (nome.isEmpty()) nome = NOME_PADRAO;
        
        servidor.addConexao(this);
        new Thread(this, "conexão_" + nome).start();;
    }
    
    public void run() {
        while (clienteIn.hasNextLine()) {
            String entrada = clienteIn.nextLine();
            
            if (entrada.equals("exit")) {
                break;
            }
            
            // System.out.println(nome + ": " + entrada);
            servidor.broadcastMensagem(this, entrada);
        }

        servidor.removerConexao(this);
    }
}