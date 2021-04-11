package jp.ac.tsukuba.s.s2020602;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;

import java.math.BigDecimal;
import java.util.Map;

public class SashimiWerewolf_5 extends SashimiBasePlayer_5 {
    
    @Override
    public Agent attack() {
        Map<Agent, BigDecimal> 生存者の人狼度 = myBelief.役職度(aliveOthers, Role.SEER);
        return 投票先を混合戦略から決定(生存者の人狼度);
    }
    
    @Override
    public String getName() {
        return "SashimiWerewolf_5";
    }
    
}
