import gearth.extensions.Extension;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.HEntity;
import gearth.extensions.parsers.HEntityType;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import java.lang.*;
import java.util.*;


@ExtensionInfo(
        Title = "User Adder",
        Description = "Adds all users in a room",
        Version = "1.1",
        Author = "schweppes0x"
)

public class UserAdder extends Extension {
    private boolean isOn;
    private boolean isSent;
    private boolean showInfo = true;

    private Bot bot;

    private Set<HEntity> loadedUsers = new HashSet<>();
    private Set<Integer> rooms = new HashSet<>();
    private boolean visitRooms = false;

    public UserAdder(String[] args){
        super(args);
    }

    public static void main(String[] args) {
        new UserAdder(args).run();
    }


    @Override
    protected void initExtension() {
        super.initExtension();

        String infoMessage = "Welcome, commands: /on , /off and /info";
        bot = new Bot(this, infoMessage, "schweppes0x", null);

        bot.onInput(s -> {
            try{
                if(!s.startsWith("/"))
                    return;
                s = s.replace("/", "");
                System.out.println(s);
                switch (s.toLowerCase()){
                    case "on":
                        isOn = true;
                        bot.writeOutput("[!] - You enabled me.",false);
                        break;
                    case "off":
                        isOn = false;
                        bot.writeOutput("[!] - You disabled me.",false);
                        break;
                    case "info":
                        showInfo = !showInfo;
                        System.out.println();
                        bot.writeOutput("[!] - Info shown: " + showInfo, false);
                        break;
                    default:
                        bot.writeOutput("[!] - Wrong command. try /on or /off", false);
                }
            }catch (Exception e){
                bot.writeOutput("[x] - Error", false);
            }
        });

        intercept(HMessage.Direction.TOCLIENT, "RoomReady", hMessage -> {
            if(!isOn)
                return;
            loadedUsers.clear();
            isSent = false;

        }); // Loaded room

        intercept(HMessage.Direction.TOCLIENT, "Users", hMessage -> {
            if(!isOn)
                return;

            Arrays.stream(HEntity.parse(hMessage.getPacket()))
                    .filter(entity -> entity.getEntityType().equals(HEntityType.HABBO))
                    .forEach(entity -> {
                        loadedUsers.add(entity);
                    });
            if(loadedUsers.size()>0)
                addUsers();
        }); // Load users


    }

    private void addUsers() {
        if(isOn && !isSent){
            int total = 0;
            for (HEntity entity: loadedUsers) {
                if(sendToServer(new HPacket("{out:RequestFriend}{s:\""+entity.getName()+"\"}"))){
                    total++;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
            }
            isSent = true;
            if(showInfo){
                sendToServer(new HPacket("{out:Whisper}{s:\"schweppes0x I have added "+ total +" users in this room. \"}{i:0}"));
            }
        }
    }


}
