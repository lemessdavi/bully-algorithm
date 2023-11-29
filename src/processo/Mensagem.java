package processo;

public class Mensagem {
    private String Host = null ;
    private int Porta = 0 ;
    private long timeStamp = 0 ;
    private ContentType Content = null ;
    private String body = " ";
    public enum ContentType{
        NOVO,VIVO,ELEICAO,OK,LIST,ADD_NO,VITORIA
    }
    Mensagem(String message){
        String tmp[] = message.split(",");
        timeStamp=Long.parseLong( tmp[0]);
        Host = tmp[1];
        Porta = Integer.parseInt(tmp[2]);
        Content = ContentType.valueOf(tmp[3]);
        if(tmp.length >4)
            body = tmp[4];
        else
            body =" ";
    }
    Mensagem(long timeStamp, String host , int porta , ContentType contentType ){
        this(timeStamp ,host , porta ,contentType," ");
    }
    Mensagem(long timeStamp, String host , int porta , ContentType contentType, String body ){
        this.timeStamp = timeStamp;
        this.Host = host;
        this.Porta = porta;
        this.Content = contentType;
        this.body = body;
    }
    public String toString(){
        return timeStamp+","+Host +"," +Porta+","+Content+","+body;
    }

    public String getHost() {
        return Host;
    }

    public int getPorta() {
        return Porta;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public ContentType getContent() {
        return Content;
    }

    public void setPorta(int port) {
        Porta = port;
    }

    public void setHost(String host) {
        Host = host;
    }

    public void setContent(ContentType content) {
        Content = content;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
