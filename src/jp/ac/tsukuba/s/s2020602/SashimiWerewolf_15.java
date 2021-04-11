package jp.ac.tsukuba.s.s2020602;


import org.aiwolf.client.lib.Content;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import java.util.*;


public class SashimiWerewolf_15 extends SashimiBasePlayer_15 {
    
    /** 襲撃投票先候補 */
    private Agent attackVoteCandidate;
    
    /** ささやきにて宣言済みの 襲撃投票先候補 */
    private Agent declaredAttackVoteCandidate;
    
    /** ささやきリスト読み込みのヘッド */
    private int whisperListHead;
    
    /** ささやき用待ち行列 */
    private final Deque<Content> whisperQueue = new LinkedList<>();
    
    
    
    @Override
    public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
        super.initialize(gameInfo, gameSetting);
        
        whisperListHead = 0;
        whisperQueue.clear();
        
        attackVoteCandidate = null;
        declaredAttackVoteCandidate = null;
        
        // 仲間の人狼の情報をテーブルに反映
        for (Map.Entry<Agent, Role> entry : currentGameInfo.getRoleMap().entrySet()) {
            Agent wolfAgent = entry.getKey();
            if (wolfAgent != me) {
                // 自分以外の人狼について，人狼度を500にする
                myTable.テーブルを更新(wolfAgent, Role.WEREWOLF, true, 500);
            }
        }
    }
    
    @Override
    public void update(GameInfo gameInfo) {
        super.update(gameInfo);
        
        // ささやきを読み取る。
        // Estimate発言があれば，テーブルを更新する。
        // 役職COについては，重複があれば やり直す。
    }
    
    @Override
    public void dayStart() {
        super.dayStart();
        
        attackVoteCandidate = null;
        declaredAttackVoteCandidate = null;
        whisperListHead = 0;
        
    }
    
    @Override
    public Agent attack() {
        // 生存している人の中で一番「村人陣営貢献度」が高い人を選んである（はず）。
        return attackVoteCandidate;
    }
    
    @Override
    /** 投票先候補を1人選び，voteCandidate にセットする */
    void chooseVoteCandidate() {
        // 生存している人の中で一番「村人陣営貢献度」が高い人を選ぶ。
        voteCandidate = myTable.村人陣営貢献度最大(new ArrayDeque<>(aliveOthers));
    }
    
    @Override
    public String whisper() {
        if (day == 0) {
            // 0日目は，役職COに関してのささやき。
            
        } else {
            // 1日目以降は，襲撃先についてのささやき。
            chooseAttackVoteTargetCandidate();
            if (Objects.isNull(declaredAttackVoteCandidate)) {
                // まだ襲撃投票の予定を宣言していなければ，宣言する。
                enqueueWhisper(attackContent(me, attackVoteCandidate));
                declaredAttackVoteCandidate = attackVoteCandidate;
            } else if (attackVoteCandidate != declaredAttackVoteCandidate) {
                // その日の最新の宣言から，村人陣営貢献度の最も高いエージェントが変わっていたら，宣言し直す。
                enqueueWhisper(attackContent(me, attackVoteCandidate));
                declaredAttackVoteCandidate = attackVoteCandidate;
            }
        }
        return dequeueWhisper();
    }
    
    /** 襲撃先候補を1人選び，attackVoteCandidate にセットする。 */
    void chooseAttackVoteTargetCandidate() {
        // 生存している人の中で一番「村人陣営貢献度」が高い人を選ぶ。
        attackVoteCandidate = myTable.村人陣営貢献度最大(new ArrayDeque<>(aliveOthers));
    }
    
    void enqueueWhisper(Content content) {
        if (content.getSubject() == Content.UNSPEC) {
            whisperQueue.offer(replaceSubject(content, me));
        } else {
            whisperQueue.offer(content);
        }
    }
    
    String dequeueWhisper() {
        if (whisperQueue.isEmpty()) {
            return Talk.SKIP;
        }
        Content content = whisperQueue.poll();
        if (content.getSubject() == me) {
            return Content.stripSubject(content.getText());
        }
        return content.getText();
    }
    
    
    @Override
    public String getName() {
        return "SashimiWerewolf_15";
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
