import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Cliente {
    private String endereço;
    private int porta;
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter saida;
    private boolean conectado;

    public Cliente(String endereço, int porta) throws IOException {
        this.endereço = endereço;
        this.porta = porta;
        this.conectado = true;
        this.socket = new Socket(this.endereço, this.porta);
        this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.saida = new PrintWriter(socket.getOutputStream(), true);
    }
    
    public void enviar() {
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        try {
            while (conectado) {
                String s = teclado.readLine();
                if (s == null) break;
                if (s.equals("exit")) {
                    this.parar();
                    break;
                }

                saida.println(s);
            }
        } catch (IOException e) {
            
        }
    }

    public void receber() {
        try {
            while (conectado) {
                String s = entrada.readLine();
                if (s == null) break;

                System.out.println(s);
            }
        } catch (IOException e) {
            
        }
    }

    public void parar() throws IOException {
        this.conectado = false;
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

        var cliente = new Cliente(endereco, porta);
        new Thread(cliente::enviar).start();
        new Thread(cliente::receber).start();

    }
}