package fr.shoqapik.btemobs.rumors;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class Rumor {

    private String title;
    private UnlockLevel unlockLevel;
    private String description;
    private int orden;
    private transient List<ResourceLocation> dialogSounds;

    public Rumor() {}

    public Rumor(String title, UnlockLevel unlockLevel, String dialogs,int orden) {
        this.title = title;
        this.unlockLevel= unlockLevel;
        this.description = dialogs;
        this.orden = orden;
    }

    public String getTitle() {
        return title;
    }

    public UnlockLevel getUnlockLevel() {
        return this.unlockLevel;
    }

    public String getDescription() {
        return description;
    }

    public int getOrden() {
        return orden;
    }

    public List<ResourceLocation> getDialogSounds() {
        if(dialogSounds == null) {
            dialogSounds = new ArrayList<>();
            
        }

        return dialogSounds;
    }
    

    public static void encode(Rumor rumor, FriendlyByteBuf packetBuffer) {
        packetBuffer.writeUtf(rumor.title);
        packetBuffer.writeUtf(rumor.getUnlockLevel().name());

        packetBuffer.writeUtf(rumor.description);
        packetBuffer.writeInt(rumor.orden);
    }

    public static Rumor decode(FriendlyByteBuf packetBuffer) {
        String entityId = packetBuffer.readUtf();
        UnlockLevel unlockLevel = UnlockLevel.valueOf(packetBuffer.readUtf());
        String description = packetBuffer.readUtf();
        int orden = packetBuffer.readInt();
        return new Rumor(entityId, unlockLevel, description,orden);
    }

    public enum UnlockLevel {
        OVERWORLD,
        NETHER,
        END;

        public boolean isUnlocked(UnlockLevel level){
            if(this==OVERWORLD){
                return true;
            }else if(this==NETHER){
                return level==NETHER || level==END;
            }else {
                return level==END;
            }
        }
    }
}