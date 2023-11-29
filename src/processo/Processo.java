package processo;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Processo {
    No meuNo =null;
    List<No> nos = new ArrayList<>();
    static final int LIDER_DEFAULT = 9090;
    final int VivoTimeOut = 200;
    final int VITORIA_TIMEOUT = 100;
    private boolean AMA_LIDER = false ;
    Processo(){
        meuNo = new No(LIDER_DEFAULT);
        meuNo.setProcesso(this);
    }
    void run(){
        if(this.mandaOiProLider()!=null){
            ///we have coordinator
            meuNo.Listen();
        }
        else{
            /// we dont have coordinator create one
            System.out.println("no consigo me conectar ao lider ;(");
            setAMA_LIDER(true);
            meuNo.Listen();
        }
    }
    Mensagem encodeResponse(String response){
        /// received this msg and encode a proper response and handle actions
        Mensagem message= new Mensagem(response);
        String messageBody=  message.getBody();
        Mensagem.ContentType contentType = message.getContent();
        switch (contentType){
            case NOVO: /// if we receive NEW
                /// if we received new no we respond with list of other nos
                /// adding coordinator port and other ports including last which is the port the receiver will be listening to
                addNovoNo(response);
                notificadoComNovoNo();
                return new Mensagem(getTimeStampAtual(),meuNo.getHost(),meuNo.getPorta(), Mensagem.ContentType.LIST,encodeNos(meuNo));
            case VITORIA:
                /// we need to remove the no we got that won the election and become COORDINATOR
                String[] ipComompleto = messageBody.split(":");
                removeNo(Integer.parseInt(ipComompleto[1]));
                meuNo.setHost(ipComompleto[0]);
                meuNo.setAtivo(true);
                return encodeMensagem(Mensagem.ContentType.OK);
//                return new Message(getTimeStampAtual(),meuNo.getHost(),meuNo.getPorta(), Message.ContentType.OK);
            case ADD_NO:
                /// we received new Peer and need to add it to nos list
                notificadoComNovoNo(message);
                return encodeMensagem(Mensagem.ContentType.OK);
            default:
                return encodeMensagem(Mensagem.ContentType.OK);
        }
    }
    void decodeResponse(String response ){
        /// sent msg and got this as response
        Mensagem message= new Mensagem(response);
        String msg=  message.getBody();
        int sender = message.getPorta();
        Mensagem.ContentType contentType = message.getContent();
        switch (contentType){
            case LIST:
                System.out.println("lista de nos recebida");
                listaDeNosNotificados(msg);
                break;
            case OK:
                /// received ok
                System.out.println("OK recebido de " + sender);
                break;
            default:
                System.out.println("nao consegui resolver essa resposta");
                break;
        }
    }

    /**
     * method overloading
     * @param contentType
     * @return
     */
    Mensagem encodeMensagem(Mensagem.ContentType contentType){
        return encodeMensagem(contentType," ");
    } 
    /// utility function
    Mensagem encodeMensagem(Mensagem.ContentType contentType, String body){
        switch (contentType){
            case ADD_NO:
                return new Mensagem(getTimeStampAtual(),meuNo.getHost(),meuNo.getPorta(), contentType,body);
            case OK:
                return new Mensagem(getTimeStampAtual() , meuNo.getHost(),meuNo.getPorta(), Mensagem.ContentType.OK);
            case NOVO:
                return new Mensagem(getTimeStampAtual() , meuNo.getHost(),meuNo.getPorta(), Mensagem.ContentType.NOVO, body);
            default:
                return null;
        }
    }
    void listaDeNosNotificados( String body){
        List<No> l  =  decodeNos(body); /// remove first char
        No ultimo = getNoPorIndex(l,l.size()-1);
        System.out.println(ultimo.getPorta());
        meuNo.setPorta( ultimo.getPorta()); //setting my port as last in list
        l.remove(l.size()- 1);///remove myself -last-
        setNos(l);
    }


    /**
     * we notify election by sending election message to all other processes
     * then listen to all processes
     * if i got an election from a process with higher priority which here is lower port
     * then i'm not coordinator
     * if i received victory then i'm not coordinator
     * else i'm coordinator
     *
     */
    void notificaEleicao() {
        Mensagem message = new Mensagem(getTimeStampAtual(),meuNo.getHost(),meuNo.getPorta(), Mensagem.ContentType.ELEICAO);
        message.setBody(meuNo.getPorta() + "");
        broadcast(message,100);
        boolean isLider = true;
        for (int i = 0 ; i <nos.size()-1 ; ++i){
            Mensagem message1 = meuNo.enviarEReceberResposta(200);
            if(message1.getContent() == Mensagem.ContentType.VITORIA){
                isLider = false   ;
            }
            else if(message1.getContent() == Mensagem.ContentType.ELEICAO){
                System.out.println("eleicao de "+ message1.getBody());
                if(Integer.parseInt( message1.getBody()) > meuNo.getPorta()){
                    isLider=  false;
                }
            }
        }
        if(isLider)
            notificaVitoria();
        meuNo.Listen();
    }

    /**
     * when i won the election i set my port to the defualt
     * i send my old port to others to remove from their lists
     */
    void notificaVitoria(){

        System.out.println("Vitoria de "+ meuNo.getPorta());
        int porta_anterior = meuNo.getPorta();
        try {
            meuNo.getServerSocket().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        meuNo.setAtivo(true);
        setAMA_LIDER(true);
        meuNo.setPorta(LIDER_DEFAULT);
        removeNo(LIDER_DEFAULT);/// remove coordinator
        broadcast(new Mensagem(getTimeStampAtual(),meuNo.getHost()
                ,meuNo.getPorta(), Mensagem.ContentType.VITORIA,porta_anterior + ""), VITORIA_TIMEOUT);
        meuNo.Listen();
    }
    void notificadoComNovoNo(){
        /// used in coordinator
        /// don't notify last one he already got response
        /// last in list is the new no
        System.out.println("notifying other with the new ");
        for (int i = 0; i < nos.size()-1 ; i++) {
            meuNo.enviarEReceberResposta(nos.get(i) ,encodeMensagem(
                    Mensagem.ContentType.ADD_NO,""+nos.get(nos.size() -1).getPorta()),1000);
        }

    }
    void notificadoComNovoNo(Mensagem message){
        int newPeerPort = Integer.parseInt(message.getBody());
        nos.add(new No(newPeerPort));
    }
    String mandaOiProLider(){
        return meuNo.enviarEReceberResposta(meuNo,encodeMensagem(Mensagem.ContentType.NOVO),4000); /// wait for 4 seconds
    }
    /*
        send message to all other processes
     */
    void broadcast(Mensagem message , int timeOut ){
        for (No no:nos ) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    meuNo.enviarEReceberResposta(no , message ,timeOut);
                }
            }) ;
            t.start();
        }
    }
    /// alive timeout 100ms
    void enviaVivo(){
        broadcast(new Mensagem(getTimeStampAtual(),meuNo.getHost(),meuNo.getPorta(), Mensagem.ContentType.VIVO),VivoTimeOut);
    }
    void addNovoNo(String ip){
        ///coordinator only
        if(this.nos.size() ==0 ){
            this.nos.add(new No(LIDER_DEFAULT +1)) ;
        }
        else {
            int sz = this.nos.size();
            int last = this.nos.get(sz -1).getPorta();

            this.nos.add(new No(last+ 1));
        }

    }

    ///utility functions
    long getTimeStampAtual(){
        return new Timestamp(System.currentTimeMillis()).getTime();
    }
    /*
        i receive body of message as list of ports so i decode the body by splitting
        and encode when i want to send
     */
    String encodeNos(No no){
        String ret= no.getPorta() + "";
        for(No p : nos){
            ret += (" " + p.getPorta() );
        }
        return ret;
    }
    List<No> decodeNos(String body){
        List<No> ret = new ArrayList<>();
        for(String ip : body.split(" ")){
            ret.add(new No(Integer.parseInt(ip)));
        }
        return ret;
    }
    void removeNo(int port){
        for (No p: nos) {
            if(p.getPorta() == port){
                nos.remove(p);
                return;
            }
        }
    }
    No getNoPorIndex(List <No> ret, int indx){
        for (int i = 0 ; i <ret.size() ;++i){
            if(i == indx){
                return ret.get(i);
            }
        }
        return null;
    }
    public boolean isAMA_LIDER() {
        return AMA_LIDER;
    }
    public void setAMA_LIDER(boolean AMA_LIDER) {
        this.AMA_LIDER = AMA_LIDER;
    }
    public void setNos(List<No> nos) {
        this.nos = nos;
    }
}
