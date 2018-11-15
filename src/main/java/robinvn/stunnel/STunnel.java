package robinvn.stunnel;


import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class STunnel {

    private static Session session;
    private static Channel channel;
    private static String host;
    private static String user;
    private static String password;
    private static int port;

    private static String bindingAddress;
    private static String forwards;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
            System.out.println("Shutdown");
        }));
        host = System.getProperty("sshHost");
        port = Integer.valueOf(System.getProperty("sshPort"));
        user = System.getProperty("sshUser");
        password = System.getProperty("sshPassword");

        bindingAddress = System.getProperty("bindingAddress", "");
        bindingAddress = "".equals(bindingAddress)? host : bindingAddress;
        forwards = System.getProperty("forwards");

        STunnel t = new STunnel();
        try {
            t.go();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void go() throws Exception {
        JSch jsch = new JSch();
        System.out.println("attempt to connect to " + host + ":" + port + " with user " + user);
        session = jsch.getSession(user, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        String[] forwardArr = forwards.split(",");
        for (String item: forwardArr) {
            try {
                System.out.println("attempt to forward " + item);
                String[] tmp = item.split(":");
                int localPort = Integer.valueOf(tmp[0]);
                String remoteHost = tmp[1];
                int remotePort = Integer.valueOf(tmp[2]);
                session.setPortForwardingL(bindingAddress, localPort, remoteHost, remotePort);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        session.connect(30000);

        channel = session.openChannel("shell");
        channel.connect();

        while (true) {
            session.sendKeepAliveMsg();
            Thread.sleep(5000l);
        }
    }
}
