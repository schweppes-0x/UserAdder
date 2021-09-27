import gearth.extensions.IExtension;
import gearth.extensions.extra.tools.ChatInputListener;
import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HPacket;
import gearth.protocol.HMessage.Direction;

public class Bot {
    private volatile int chatid;
    private volatile String name;
    private String figureString;
    private volatile String infoMessage;
    private volatile boolean firstTime;
    private volatile Observable<ChatInputListener> chatInputObservable;
    private final IExtension extension;


    public Bot(IExtension extension, String infoMessage, String name, String figure_string) {
        this.name = name;
        if(figure_string == null){
            figure_string = "hr-802-31.hd-209-8.ch-210-91.lg-3391-64-1408.sh-3524-1408-1408.ha-3620.ca-4130-72-1408.cc-3389-64-1408";
        }
        this.figureString = figure_string;
        this.firstTime = true;
        this.chatInputObservable = new Observable();
        this.extension = extension;
        this.chatid = this.name.hashCode() % 300000000 + 300000000;
        this.infoMessage = infoMessage;
        boolean[] doOncePerConnection = new boolean[]{false};
        extension.onConnect((host, port, hotelversion, clientIdentifier, clientType) -> {
            doOncePerConnection[0] = true;
        });
        extension.intercept(Direction.TOSERVER, (hMessage) -> {
            if (this.firstTime) {
                this.firstTime = false;
                if (hMessage.getPacket().headerId() != 4000) {
                    doOncePerConnection[0] = false;
                    this.createChat();
                }
            }

        });
        extension.intercept(Direction.TOCLIENT, "FriendListFragment", (hMessage) -> {
            if (doOncePerConnection[0]) {
                doOncePerConnection[0] = false;
                (new Thread(() -> {
                    try {
                        Thread.sleep(1000L);
                        this.createChat();
                    } catch (InterruptedException var2) {
                        var2.printStackTrace();
                    }

                })).start();
            }

        });
        extension.intercept(Direction.TOSERVER, "SendMsg", (hMessage) -> {
            HPacket packet = hMessage.getPacket();
            if (packet.readInteger() == this.chatid) {
                hMessage.setBlocked(true);
                String str = packet.readString();
                if (str.equals(":info") && infoMessage != null) {
                    this.writeOutput(infoMessage, false);
                } else {
                    this.chatInputObservable.fireEvent((l) -> {
                        l.inputEntered(str);
                    });
                }
            }

        });
    }


    public void UpdateBot(String name, String figure_string){
        this.name = name;
        figureString = figure_string;

    }

    private void createChat() {
        HPacket packet = new HPacket("FriendListUpdate", Direction.TOCLIENT, new Object[]{0, 1, 0, this.chatid, this.name, 1, true, false, figureString, 0, "", 0, true, false, true, ""});
        this.extension.sendToClient(packet);
        if (this.infoMessage != null) {
            this.writeOutput(this.infoMessage, false);
        }

    }

    public void writeOutput(String string, boolean asInvite) {
        if (asInvite) {
            this.extension.sendToClient(new HPacket("RoomInvite", Direction.TOCLIENT, new Object[]{this.chatid, string}));
        } else {
            this.extension.sendToClient(new HPacket("NewConsole", Direction.TOCLIENT, new Object[]{this.chatid, string, 0, ""}));
        }

    }

    public void onInput(ChatInputListener listener) {
        this.chatInputObservable.addListener(listener);
    }
}
