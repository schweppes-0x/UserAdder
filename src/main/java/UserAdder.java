import gearth.extensions.Extension;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.HEntity;
import gearth.extensions.parsers.HEntityType;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

import java.lang.*;
import java.util.*;



@ExtensionInfo(
        Title = "[Private] User Adder",
        Description = "Adds all users in room",
        Version = "1.0",
        Author = "schweppes0x"
)

public class UserAdder extends Extension {
    private boolean isOn;
    private boolean isSent;
    private boolean showInfo = true;
    private int addedInCurrRoom = 0;
    private int addedInTotal = 0;

    private Bot bot;

    private Set<HEntity> loadedUsers = new HashSet<>();

    public UserAdder(String[] args){
        super(args);
    }

    public static void main(String[] args) {
        new UserAdder(args).run();
    }



    @Override
    protected void initExtension() {
        super.initExtension();

        String infoMessage = "Hello, Use commands: /on , /off and /info. Info will be shown about friends, turn this on or off with /info";
        bot = new Bot(this, infoMessage, "schweppes0x", null);


        bot.onInput(s -> {
            try{
                switch (s){
                    case "/on":
                        isOn = true;
                        bot.writeOutput("You enabled me. You can start visiting rooms, i will add everybody. Turn me off with /off",false);
                        break;
                    case "/off":
                        isOn = false;
                        bot.writeOutput("You disabled me. You can turn me on again with /on",false);
                        break;
                    case "/info":
                        if(showInfo)
                            showInfo = !showInfo;
                        bot.writeOutput("Info is enabled = "+showInfo+". Enabled = info about how many i added. If you want to toggle me on/off, type /info.",false);

                        break;
                    default:
                        bot.writeOutput("Wrong command. try /on or /off", false);
                }
            }catch (Exception e){
                bot.writeOutput("Error", false);
            }
        });

        intercept(HMessage.Direction.TOCLIENT, "RoomReady", hMessage -> {
            if(!isOn)
                return;
            loadedUsers.clear();
            addedInCurrRoom = 0;
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
            for (HEntity entity: loadedUsers) {
                sendToServer(new HPacket("{out:RequestFriend}{s:\""+entity.getName()+"\"}"));
                addedInCurrRoom++;
                addedInTotal++;
            }
            isSent = true;
            if(bot!=null && showInfo){
                bot.writeOutput("I added "+ addedInCurrRoom+ " in this room and total since connected: " + addedInTotal, false);
            }
        }
    }


}
