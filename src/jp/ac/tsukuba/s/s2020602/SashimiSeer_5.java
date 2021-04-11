package jp.ac.tsukuba.s.s2020602;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SashimiSeer_5 extends SashimiBasePlayer_5 {
    
    /** 自分の占い結果の時系列 */
    private final List<Judge> myDivinationList = new ArrayList<>();
    
    /** 自分の占い済みエージェントと判定のマップ */
    private final Map<Agent, Judge> myDivinationMap = new HashMap<>();
    
    /** 占って人狼だったエージェント */
    private final List<Agent> blackList = new ArrayList<>();
    
    /** 占って人狼じゃなかったエージェント */
    private final List<Agent> myWhiteList = new ArrayList<>();
    
    
    @Override
    public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
        super.initialize(gameInfo, gameSetting);
        
        myDivinationList.clear();
        myDivinationMap.clear();
        blackList.clear();
        myWhiteList.clear();
    }
    
    @Override
    public void dayStart() {
        super.dayStart();
        
        // 占い結果を登録し，白黒に振り分ける
        Judge divination = currentGameInfo.getDivineResult();
        if (divination != null) {
            Agent divined = divination.getTarget();
            myDivinationList.add(divination);
            if (divination.getResult() == Species.HUMAN) {
                myWhiteList.add(divined);
                myBelief.役職でない固定(divined, Role.WEREWOLF);
            } else {
                blackList.add(divined);
                myBelief.役職固定(divined, Role.WEREWOLF);
            }
            myDivinationMap.put(divined, divination);
        }
    }
    
    @Override
    public Agent divine() {
        Map<Agent, BigDecimal> 生存者の人狼度 = myBelief.役職度(aliveOthers, Role.WEREWOLF);
        return 投票先を混合戦略から決定(生存者の人狼度);
    }
    
    @Override
    public String getName() {
        return "SashimiSeer_5";
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
    public Agent guard() {
        throw new UnsupportedOperationException();
    }
    
    
    
}
