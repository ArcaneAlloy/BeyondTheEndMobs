package fr.shoqapik.btemobs;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import fr.shoqapik.btemobs.capability.QuestStateData;
import fr.shoqapik.btemobs.capability.RecipeCapability;
import fr.shoqapik.btemobs.capability.UnlockState;
import fr.shoqapik.btemobs.entity.BteNpcType;
import fr.shoqapik.btemobs.quest.ConditionUnlockData;
import fr.shoqapik.btemobs.quest.Quest;
import fr.shoqapik.btemobs.quest.QuestManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comandos de depuración de quests (requieren permisos de OP, nivel 2):
 *   /btequests unlockall            -> cumple todos los requisitos (condiciones) de todas las quests
 *   /btequests unlock <quest>       -> cumple los requisitos de una sola quest
 *   /btequests relockall            -> deshace lo anterior: vuelve a bloquear los requisitos que no se cumplen de verdad
 *
 * Solo toca los requisitos para que la quest aparezca disponible; no completa tareas ni da recompensas.
 * El estado de las quests es compartido en cooperativo, así que afecta a todos los jugadores.
 */
@Mod.EventBusSubscriber(modid = BteMobsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class QuestDebugCommand {

    private static final SimpleCommandExceptionType NO_PLAYER =
            new SimpleCommandExceptionType(Component.literal("No hay ningún jugador conectado."));
    private static final SimpleCommandExceptionType NO_DATA =
            new SimpleCommandExceptionType(Component.literal("No se han podido cargar las quests del jugador."));

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("btequests")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("unlockall")
                        .executes(ctx -> unlock(ctx, null)))
                .then(Commands.literal("unlock")
                        .then(Commands.argument("quest", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                        QuestManager.getQuests().stream().map(q -> q.id), builder))
                                .executes(ctx -> unlock(ctx, ResourceLocationArgument.getId(ctx, "quest")))))
                .then(Commands.literal("relockall")
                        .executes(QuestDebugCommand::relockAll))
                .then(Commands.literal("twilightportal")
                        .executes(ctx -> {
                            boolean ok = fr.shoqapik.btemobs.quest.TwilightPortalUnlock.build(ctx.getSource().getServer());
                            if (ok) ctx.getSource().sendSuccess(Component.literal("Portal al Twilight Forest construido en la fuente del Lobby"), true);
                            else ctx.getSource().sendFailure(Component.literal("No existe la dimensión del Lobby"));
                            return ok ? 1 : 0;
                        })));
    }

    /** Capability del jugador que ejecuta el comando (o de cualquier jugador conectado si se lanza desde la consola). */
    @SuppressWarnings("rawtypes")
    private static RecipeCapability capability(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getEntity() instanceof ServerPlayer sp ? sp
                : source.getServer().getPlayerList().getPlayers().stream().findFirst().orElse(null);
        if (player == null) throw NO_PLAYER.create();
        RecipeCapability cap = RecipeCapability.get(player);
        if (cap == null) throw NO_DATA.create();
        return cap;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int unlock(CommandContext<CommandSourceStack> ctx, ResourceLocation questId) throws CommandSyntaxException {
        RecipeCapability cap = capability(ctx.getSource());
        Map<BteNpcType, Map<Quest, QuestStateData>> all = cap.quests;

        int quests = 0;
        int conditions = 0;
        boolean found = false;
        for (Map<Quest, QuestStateData> questMap : all.values()) {
            for (Map.Entry<Quest, QuestStateData> entry : questMap.entrySet()) {
                if (entry.getKey() == null) continue;
                if (questId != null && !entry.getKey().id.equals(questId)) continue;
                found = true;
                int before = conditions;
                for (UnlockState state : entry.getValue().unlockStates) {
                    if (!state.unlock) {
                        state.unlock = true;
                        conditions++;
                    }
                }
                if (conditions > before) quests++;
            }
        }

        if (questId != null && !found) {
            ctx.getSource().sendFailure(Component.literal("No existe la quest " + questId));
            return 0;
        }

        cap.dirty = true;
        cap.checkChangedInventory();
        int total = conditions;
        int affected = quests;
        ctx.getSource().sendSuccess(Component.literal(questId == null
                ? "Desbloqueados " + total + " requisitos en " + affected + " quests."
                : "Desbloqueados " + total + " requisitos de " + questId + "."), true);
        return Math.max(1, total);
    }

    /**
     * Vuelve a bloquear los requisitos: un PARENT_QUEST queda cumplido solo si esa quest está reclamada;
     * los de portal y logros se recalculan solos en unos segundos (QuestTriggers comprueba cada jugador).
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int relockAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        RecipeCapability cap = capability(ctx.getSource());
        Map<BteNpcType, Map<Quest, QuestStateData>> all = cap.quests;

        Map<String, QuestStateData> byId = new HashMap<>();
        for (Map<Quest, QuestStateData> questMap : all.values())
            for (Map.Entry<Quest, QuestStateData> e : questMap.entrySet())
                if (e.getKey() != null) byId.put(e.getKey().id.toString(), e.getValue());

        int relocked = 0;
        for (Map<Quest, QuestStateData> questMap : all.values()) {
            for (QuestStateData data : questMap.values()) {
                if (data.isReclaim) continue;
                relocked += relock(data.unlockStates, byId);
            }
        }

        cap.dirty = true;
        cap.checkChangedInventory();
        ctx.getSource().sendSuccess(Component.literal("Bloqueados de nuevo " + relocked
                + " requisitos (los de portal y logros se vuelven a comprobar solos)."), true);
        return Math.max(1, relocked);
    }

    private static int relock(List<UnlockState> states, Map<String, QuestStateData> byId) {
        int count = 0;
        for (UnlockState state : states) {
            if (!state.unlock) continue;
            boolean really = false;
            if (state.type == ConditionUnlockData.Type.PARENT_QUEST) {
                QuestStateData parent = byId.get(state.id);
                really = parent != null && parent.isReclaim;
            }
            if (!really) {
                state.unlock = false;
                count++;
            }
        }
        return count;
    }
}
