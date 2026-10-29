package fr.shoqapik.btemobs.rumors;

/**
 * Los rumores de Antonio se han quitado (interfaz en desuso). Solo queda el nivel de desbloqueo
 * (Overworld / Nether / End), que se sigue usando en BteMobsMod, CommonEvents y SyncUnlockLevelPacket.
 */
public final class Rumor {

    private Rumor() {}

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
