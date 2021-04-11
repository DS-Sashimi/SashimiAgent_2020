package jp.ac.tsukuba.s.s2020602;

import java.util.ArrayDeque;

/** 15人村用 狂人エージェント */
public class SashimiPossessed_15 extends SashimiBasePlayer_15 {
    
    @Override
    public String getName() {
        return "SashimiPossessed_15";
    }
    
    @Override
    /** 投票先候補を1人選び，voteCandidate にセットする */
    void chooseVoteCandidate() {
        voteCandidate = myTable.村人陣営貢献度最大(new ArrayDeque<>(aliveOthers));
    }
    
}
