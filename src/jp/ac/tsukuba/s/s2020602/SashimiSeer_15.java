package jp.ac.tsukuba.s.s2020602;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SashimiSeer_15 extends SashimiBasePlayer_15 {
    
    /** グレー（白とも黒とも判明していない生存者）のリスト */
    List<Agent> grayList = new ArrayList<>();
    
    @Override
    public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
        super.initialize(gameInfo, gameSetting);
        grayList = new ArrayList<>(aliveOthers);
    }
    
    @Override
    public void dayStart() {
        super.dayStart();
        
        // 占い師が占いの結果を受け取る。
        // 占い結果を登録し，白黒に振り分ける
        Judge divination = currentGameInfo.getDivineResult();
        if (!Objects.isNull(divination)) {
            Agent divined = divination.getTarget();
            myResultList.add(divination);
            // グレーのリストから削除
            grayList.remove(divined);
            // myTableを更新
            myTable.テーブルを更新(divined, Role.WEREWOLF, (divination.getResult() == Species.WEREWOLF), 500);
        }
        
    }
    
    @Override
    void addExecutedAgent(Agent executedAgent) {
        if (executedAgent != null) {
            aliveOthers.remove(executedAgent);
            if (!executedAgents.contains(executedAgent)) {
                executedAgents.add(executedAgent);
                grayList.remove(executedAgent); // grayListには生存者のみ含めることにする
            }
        }
    }
    
    @Override
    void addKilledAgent(Agent killedAgent) {
        if (killedAgent != null) {
            aliveOthers.remove(killedAgent);
            if (!killedAgents.contains(killedAgent)) {
                killedAgents.add(killedAgent);
                全員から確定白の人(killedAgent);
                grayList.remove(killedAgent); // grayListには生存者のみ含めることにする
            }
        }
    }
    
    @Override
    public Agent divine() {
        // 白とも黒とも判明していない生存者のうち，いちばん人狼陣営貢献度が高い人（いちばん人狼かもしれない人）を占う
        return myTable.人狼陣営度最大(new ArrayDeque<>(grayList));
    }
    
    @Override
    public String getName() {
        return "SashimiSeer_15";
    }
    
}
