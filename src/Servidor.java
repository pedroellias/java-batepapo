import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Scanner;

public class Servidor {
    private final String endereço;
    private final int porta;
    private boolean conectado;
    private Map<String, GerenciadorCliente> clientes;

    private ServerSocket socket;
    
    public Servidor(String endereço, int porta) throws IOException {
        this.endereço = endereço;
        this.porta = porta;
        this.conectado = true;
        this.clientes = new Hashtable<>();
        this.socket = new ServerSocket(porta, 10, InetAddress.getByName(endereço));
    }

    public void executar() {
        this.log("Escutando clientes em " + endereço + ":" + porta);
        while (conectado) {
            try {
                Socket novoCliente = socket.accept();
                new GerenciadorCliente(this, novoCliente);
            } catch (IOException e) {
                if (conectado) e.printStackTrace();
            }
        }
    }

    public boolean conectar(String nome, GerenciadorCliente gc) {
        if (this.clientes.containsKey(nome))
            return false;

        serverBroadcast(nome + " entrou no servidor!");
        addCliente(nome, gc);
        return true;
    }

    public void desconectar(GerenciadorCliente gc) {
        try {
            gc.getCliente().close();
            removerCliente(gc.getNome());
            serverBroadcast(gc.getNome() + " saiu do servidor!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addCliente(String nome, GerenciadorCliente gc) {
        this.clientes.put(nome, gc);
    }

    private void removerCliente(String nome) {
        this.clientes.remove(nome);
    }

    public boolean isConectado() {
        return conectado;
    }

    public String getEndereço() {
        return endereço;
    }

    public int getPorta() {
        return porta;
    }

    public void log(String s) {
        System.out.println("[server] " + s);
    }

    private void serverBroadcast(String msg) {
        for (GerenciadorCliente gc : clientes.values()) {
            gc.enviar("server", msg);
        }
    }

    public void broadcast(GerenciadorCliente remetente, String msg) {
        for (GerenciadorCliente gc : clientes.values()) {
            if (gc.equals(remetente)) continue;

            gc.enviar(remetente.getNome(), msg);
        }
    }

    public void parar() throws IOException {
        this.conectado = false;
        log("Servidor fechando...");
        serverBroadcast("Servidor fechando...");
        for (GerenciadorCliente gc : new ArrayList<>(clientes.values())) {
            desconectar(gc);
        }
        this.socket.close();
    }

    public static void main(String[] args) throws IOException {
        String endereco;
        int porta;
        if (args.length != 2) {
            endereco = "localhost";
            porta = 6767;
        } else {
            endereco = args[0];
            porta = Integer.parseInt(args[1]);
        }

        var server = new Servidor(endereco, porta);
        new Thread(server::executar).start();

        Scanner teclado = new Scanner(System.in);
        while (teclado.hasNextLine()) {
            String s = teclado.nextLine();

            if (s.equals("exit")) {
                server.parar();
                break;
            }
        }
        teclado.close();
    }
}

class GerenciadorCliente implements Runnable {
    private Servidor servidor;
    private Socket cliente;
    private String nome;
    BufferedReader entrada;
    PrintWriter saida;

    public GerenciadorCliente(Servidor servidor, Socket cliente) throws IOException {
        this.servidor = servidor;
        this.cliente = cliente;
        this.nome = "_";
        this.entrada = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
        this.saida = new PrintWriter(cliente.getOutputStream(), true);

        new Thread(this).start();
    }

    public boolean autenticar() throws IOException {
        enviar("server", "Digite seu nome: ");
        
        String s = entrada.readLine().strip();
        
        if (s.isBlank())  {
            servidor.log("Falha ao se autenticar com " + cliente.getInetAddress().getHostAddress());
            enviar("server", "O nome digitado já está sendo usado!");
            return false;
        }

        this.nome = s;
        servidor.log("Cliente em " + cliente.getInetAddress().getHostAddress() + " se autenticou como " + nome);
        return servidor.conectar(nome, this);
    }

    public void enviar(String remetente, String msg) {
        this.saida.println("[" + remetente + "] " + msg);
    }

    public void run() {
        servidor.log("Conexão estabelecida com " + cliente.getInetAddress().getHostAddress());
        try {
            if (!autenticar())
                return;

            while (servidor.isConectado()) {
                String s = entrada.readLine();
                if (s == null) break;

                servidor.broadcast(this, s);
            }
        } catch (IOException e) {
            if (servidor.isConectado()) e.printStackTrace();
        }

        servidor.desconectar(this);
        servidor.log("Conexão com [" + nome + "] encerrada.");
    }

    public Socket getCliente() {
        return cliente;
    }

    public String getNome() {
        return nome;
    }
}