package jp.ac.tsukuba.s.s2020602;

import org.aiwolf.sample.lib.AbstractRoleAssignPlayer;

public class SashimiRoleAssignPlayer extends AbstractRoleAssignPlayer {
    
    @Override
    public String getName() {
        return "SashimiRoleAssignPlayer";
    }
    
    public SashimiRoleAssignPlayer() {
        setVillagerPlayer(new SashimiPlayer());
        setSeerPlayer(new SashimiPlayer());
        setBodyguardPlayer(new SashimiPlayer());
        setMediumPlayer(new SashimiPlayer());
        setWerewolfPlayer(new SashimiPlayer());
        setPossessedPlayer(new SashimiPlayer());
    }
    
}
