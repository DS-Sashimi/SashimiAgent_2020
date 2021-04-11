package jp.ac.tsukuba.s.s2020602;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;

import java.util.Objects;

public class SashimiMedium extends SashimiBasePlayer_15 {
    
    @Override
    public void dayStart() {
        super.dayStart();
        
        // 霊媒結果を待ち行列に入れる
        Judge ident = currentGameInfo.getMediumResult();
        if (!Objects.isNull(ident)) {
            Agent identified = ident.getTarget();
            myResultList.add(ident);
            // myTableを更新
            myTable.テーブルを更新(identified, Role.WEREWOLF, (ident.getResult() == Species.WEREWOLF), 500);
        }
    }
    
    @Override
    public String getName() {
        return "SashimiMedium";
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
    
    @Override
    public Agent guard() {
        throw new UnsupportedOperationException();
    }
    
}
