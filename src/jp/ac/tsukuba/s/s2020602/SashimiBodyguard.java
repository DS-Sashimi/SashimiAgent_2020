package jp.ac.tsukuba.s.s2020602;

import org.aiwolf.common.data.Agent;

import java.util.ArrayDeque;

public class SashimiBodyguard extends SashimiBasePlayer_15 {
    
    /* メモ：たぶん，連続護衛は禁止されていないようです。 */
    @Override
    public Agent guard() {
        // 村人陣営貢献度がいちばん高い人を守る。
        return myTable.村人陣営貢献度最大(new ArrayDeque<>(aliveOthers));
    }
    
    @Override
    public String getName() {
        return "SashimiBodyguard";
    }
    
    @Override
    public String whisper() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public Agent attack() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public Agent divine() {
        throw new UnsupportedOperationException();
    }
    
    
}
