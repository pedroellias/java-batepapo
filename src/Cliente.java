import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Cliente {
    private static final String ENDEREÇO = "127.0.0.1";
    private static final int PORTA = 6767;

    public static void main(String[] args) throws UnknownHostException, IOException {
        Socket cliente;
        if (args.length == 2) {
            cliente = new Socket(args[0], Integer.parseInt(args[1]));
        } else {
            cliente = new Socket(ENDEREÇO, PORTA);
        }
        System.out.println("CLIENTE: Conexão estabelecida com servidor em " + ENDEREÇO + ":" + PORTA + "!");

        PrintStream saida = new PrintStream(cliente.getOutputStream());
        Scanner teclado = new Scanner(System.in);

        System.out.print("Digite o seu nome: ");
        if (teclado.hasNextLine()) saida.print(teclado.nextLine() + "\n");

        Scanner entrada = new Scanner(cliente.getInputStream());
        new Recebedor(entrada);

        System.out.println("CLIENTE: conectado com sucesso. Digite sua mensagem para iniciar a conversa!");
        System.out.print("> ");
        while (teclado.hasNextLine()) {
            System.out.print("> ");
            String s = teclado.nextLine();

            if (s.equals("exit")) break;

            saida.println(s);
        }

        saida.close();
        teclado.close();
        cliente.close();
    }
}

class Recebedor implements Runnable {
    Scanner entrada;

    Recebedor(Scanner entrada) {
        this.entrada = entrada;

        new Thread(this, "Recebedor").start();
    }
    
    public void run() {
        while (entrada.hasNextLine()) {
            String s = entrada.nextLine();

            System.out.println(s);
        }
    }
}
