package processo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;


/// NO EH RESPONSAVEL APENAS POR MANDAR E RECEBER
public class No {
    final int PERIODO_SEM_RESPOSTA = 5000 ; ///RECEB QQR MSG EM 3 SEC, VIVO OU ELEITO
    private int porta = 8090;
    private String host = "127.0.0.1"; /// default, all on local host
    private ServerSocket serverSocket = null;
    private boolean ativo = true ;
    Processo processo = null;
    No(int porta){
        this.porta= porta;
    }
    String enviarEReceberResposta(No no , Mensagem message , int timeOut){
        try{
            Socket s=new Socket(no.getHost(),no.getPorta());
            System.out.println("enviando  "+message.toString()+" para "+ no.getPorta());
            s.setSoTimeout(timeOut);
            DataOutputStream dout=new DataOutputStream(s.getOutputStream());
            DataInputStream din = new DataInputStream(s.getInputStream());
            dout.writeUTF(message.toString());
            String response = din.readUTF(); /// we need to decode response
            processo.decodeResponse(response);
            dout.flush();
            dout.close();
            s.close();
            return response;
        }catch(Exception e){System.out.println(e +" " + no.getPorta());}
        return null;
    }
    Mensagem enviarEReceberResposta(int timeOut){
        try{
//            ServerSocket ss=new ServerSocket(this.getPorta());
            if(timeOut> 0)
                serverSocket.setSoTimeout(timeOut);
            Socket s=serverSocket.accept();//establishes connection
            DataInputStream din=new DataInputStream(s.getInputStream());
            DataOutputStream dout=new DataOutputStream(s.getOutputStream());
            String str=din.readUTF();
            System.out.println("recebendo mesagem :  "+str);
            Mensagem message = processo.encodeResponse(str);
            String response = message.toString();
            System.out.println("respondido com " +response);
            dout.writeUTF(response);
            dout.flush();
            dout.close();
            din.close();
            return new Mensagem(str);
        }catch(Exception e){
            /// if we timed out and !AMACoordinator, we send election
            if(!processo.isAMA_LIDER())
                ativo = false;

        }
        return null;
    }
    void Listen(){
        System.out.println("Estou escutando a " + this.getPorta());
        while(ativo){
            if(processo.isAMA_LIDER()){
                /// when timed out the socket is not closed so we don't need to open it again
                if(serverSocket == null||serverSocket.isClosed())
                    bindServerSocket();
                enviarEReceberResposta(1000); ///listen for 2 second and send alive wait indefinitely
                processo.enviaVivo();
            }else {
                /// when timed out the socket is not closed so we don't need to open it again
                if(serverSocket == null||serverSocket.isClosed())
                    bindServerSocket();

                enviarEReceberResposta(PERIODO_SEM_RESPOSTA ); /// wait 3 seconds
            }
        }
        System.out.println(this.getPorta()+" sem lider");
        if(!processo.isAMA_LIDER())
            processo.notificaEleicao();

    }
    

    boolean bindServerSocket(){
        System.out.println( "binding "+ this.getPorta());
        try {
            serverSocket = new ServerSocket(this.getPorta());
            return true;
        } catch (Exception e) {
            System.out.println("nao consegue bindar em "+this.getPorta());
//            e.printStackTrace();
        }
        return false;
    }

    public String getHost() {
        return host;
    }

    public int getPorta() {
        return porta;
    }

    public void setPorta(int porta) {
        this.porta = porta;
    }
    
    public void setProcesso(Processo process) {
        this.processo = process;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void setAtivo(boolean active) {
        this.ativo = active;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
