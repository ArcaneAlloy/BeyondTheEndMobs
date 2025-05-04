package fr.shoqapik.btemobs.quests;

import fr.shoqapik.btemobs.BteMobsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class Quest {

    private String entityId;
    private Quest.Type type;
    private List<String> dialogs;
    private List<QuestAnswer> answers;

    private transient List<ResourceLocation> dialogSounds;
    private transient ResourceLocation entityIdLocation;

    public Quest() {}

    public Quest(String entityId, Type type, List<String> dialogs, List<QuestAnswer> answers) {
        this.entityId = entityId;
        this.type = type;
        this.dialogs = dialogs;
        this.answers = answers;
    }

    public ResourceLocation getEntityId() {
        if(entityIdLocation == null) {
            entityIdLocation = ResourceLocation.tryParse(entityId);

        }
        return entityIdLocation;
    }

    public Quest.Type getType() {
        return type;
    }

    public List<String> getDialogs() {
        return dialogs;
    }

    public List<ResourceLocation> getDialogSounds() {
        if(dialogSounds == null) {
            dialogSounds = new ArrayList<>();
            for(int i = 0; i < dialogs.size(); i++) {
                ResourceLocation location = new ResourceLocation(BteMobsMod.MODID, getEntityId().getPath() + "_" + type.name().toLowerCase() + "_" + i);
                dialogSounds.add(location);
            }
        }

        return dialogSounds;
    }

    public List<QuestAnswer> getAnswers() {
        return answers;
    }

    public static void encode(Quest quest, FriendlyByteBuf packetBuffer) {
        packetBuffer.writeUtf(quest.entityId);
        packetBuffer.writeUtf(quest.type.name());

        packetBuffer.writeInt(quest.dialogs.size());
        for (String dialog : quest.dialogs) {
            packetBuffer.writeUtf(dialog);
        }

        packetBuffer.writeInt(quest.answers.size());
        for (QuestAnswer answer : quest.answers) {
            packetBuffer.writeUtf(answer.getFormattedAwnser());
            packetBuffer.writeUtf(answer.getAction());
        }
    }

    public static Quest decode(FriendlyByteBuf packetBuffer) {
        String entityId = packetBuffer.readUtf();
        Type type = Type.valueOf(packetBuffer.readUtf());

        int dialogNumber = packetBuffer.readInt();
        List<String> dialogs = new ArrayList<>();
        for(int i = 0; i < dialogNumber; i++) {
            dialogs.add(packetBuffer.readUtf());
        }

        int answersNumber = packetBuffer.readInt();
        List<QuestAnswer> answers = new ArrayList<>();
        for(int i = 0; i < answersNumber; i++) {
            answers.add(new QuestAnswer(packetBuffer.readUtf(), packetBuffer.readUtf()));
        }

        return new Quest(entityId, type, dialogs, answers);
    }

    public enum Type {
        PRESENTATION,
        TASKING
    }
}