package jp.ac.tsukuba.s.s2020602;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;

import java.math.BigDecimal;
import java.util.*;

/** テーブル（15人村で，信念の代わりに用いる。） */
public class Table15 {
    
    /** 視点エージェント */
    private final Agent me;
    
    /** me の役職 */
    private final Role myRole;
    
    /** 他のエージェントたち */
    private final List<Agent> allOtherAgents;
    
    /** 各役職のカウンタ */
    private final Map<Role, Map<Agent, Integer>> カウンタ群;
    
    /** コンストラクタ */
    public Table15(Agent me, Role myRole, List<Agent> allOtherAgents) {
        this.me = me;
        this.myRole = myRole;
        this.allOtherAgents = allOtherAgents;
        
        カウンタ群 = new HashMap<>();
        
        // メモ：「初期化」されたカウンタは，各人に対して0を割り当てています。
        
        Map<Agent, Integer> 人狼カウンタ = new HashMap<Agent, Integer>();
        Utility.カウンタを初期化(人狼カウンタ, allOtherAgents);
        カウンタ群.put(Role.WEREWOLF, 人狼カウンタ);
        
        Map<Agent, Integer> 占い師カウンタ = new HashMap<Agent, Integer>();
        Utility.カウンタを初期化(占い師カウンタ, allOtherAgents);
        カウンタ群.put(Role.SEER, 占い師カウンタ);
        
        Map<Agent, Integer> 狩人カウンタ = new HashMap<Agent, Integer>();
        Utility.カウンタを初期化(狩人カウンタ, allOtherAgents);
        カウンタ群.put(Role.BODYGUARD, 狩人カウンタ);
        
        Map<Agent, Integer> 霊媒師カウンタ = new HashMap<Agent, Integer>();
        Utility.カウンタを初期化(霊媒師カウンタ, allOtherAgents);
        カウンタ群.put(Role.MEDIUM, 霊媒師カウンタ);
        
        Map<Agent, Integer> 狂人カウンタ = new HashMap<Agent, Integer>();
        Utility.カウンタを初期化(狂人カウンタ, allOtherAgents);
        カウンタ群.put(Role.POSSESSED, 狂人カウンタ);
        
    }
    
    /** agent さんの role 度を更新 */
    void テーブルを更新(Agent agt, Role role, boolean isAdd) {
        テーブルを更新(agt, role, isAdd, 1);
    }
    
    /**
     * agent さんの role 度を更新。numを500にすることで，事実上の（？）固定も可能です。
     *
     * @param isAdd 度合いを加算するかどうか。agtさんの役職がroleっぽいのであればtrue（このとき，agtさんのrole度がnumポイント加算されます。）
     */
    void テーブルを更新(Agent agt, Role role, boolean isAdd, int num) {
        // agt が me と一致する場合は，何もしない。
        if (agt == me) {
            return;
        }
        
        int 増分 = 0;
        if (isAdd) {
            増分 = num;
        } else {
            増分 = -1 * num;
        }
        switch (role) {
            case WEREWOLF:
            case POSSESSED:
            case SEER:
            case BODYGUARD:
            case MEDIUM:
                int 新しいカウント = カウンタ群.get(role).get(agt) + 増分;
                カウンタ群.get(role).put(agt, 新しいカウント);
                break;
            case VILLAGER:
                // 「村人カウンタ」はないので，人狼カウンタを用いる。
                // （村人っぽければ人狼度を下げる，村人っぽくなければ人狼度を上げる。）
                int 新しいカウント2 = カウンタ群.get(Role.WEREWOLF).get(agt) - 増分;
                カウンタ群.get(Role.WEREWOLF).put(agt, 新しいカウント2);
                break;
            default:
                break;
        }
    }
    
    void 確定白の人(Agent agt) {
        カウンタ群.get(Role.WEREWOLF).put(agt, -500);
    }
    
    // TODO：同率の場合にどうするかを決める必要がある。
    
    /** 与えられたエージェントたちのなかで，一番 role である可能性が高いエージェントを返す */
    Agent 役職度最大(ArrayDeque<Agent> agents, Role role) {
        Agent ret = agents.poll();
        while (!agents.isEmpty()) {
            Agent agt = agents.poll();
            if (カウンタ群.get(role).get(agt) > カウンタ群.get(role).get(ret)) {
                // もし，retよりagtのほうが roleっぽかったら
                ret = agt;
            }
        }
        return ret;
    }
    
    /** 与えられたエージェントたちのなかで，一番 人狼陣営の可能性が高いエージェントを返す */
    Agent 人狼陣営度最大(ArrayDeque<Agent> agents) {
        Map<Agent, Integer> 人狼カウンタ = カウンタ群.get(Role.WEREWOLF);
        Map<Agent, Integer> 狂人カウンタ = カウンタ群.get(Role.POSSESSED);
        
        Agent ret = agents.poll();
        while (!agents.isEmpty()) {
            Agent agt = agents.poll();
            if (人狼カウンタ.get(agt) + 狂人カウンタ.get(agt) > 人狼カウンタ.get(ret) + 狂人カウンタ.get(ret)) {
                // もし，retよりagtのほうが 人狼陣営っぽかったら
                ret = agt;
            }
        }
        return ret;
    }
    
    /**
     * 与えられたエージェントたちのなかで，一番 村人陣営貢献度が高いエージェントを返す。 メモ：SashimiWerewolf_15 の
     * whisper() において，「村人陣営貢献度の最も高いエージェントが変わったら」という条件での分岐がある（ので，ArrayDequeの最初の要素から走査することにする）。
     */
    Agent 村人陣営貢献度最大(ArrayDeque<Agent> agents) {
        Agent ret = agents.poll();
        BigDecimal 最大貢献度 = 村人陣営貢献度(ret);
        
        while (!agents.isEmpty()) {
            Agent agt = agents.poll();
            BigDecimal agt貢献度 = 村人陣営貢献度(agt);
            if (agt貢献度.compareTo(最大貢献度) > 0) {
                ret = agt;
                最大貢献度 = agt貢献度;
            }
        }
        return ret;
    }
    
    /** 村人陣営の勝利への貢献度（？）を，人狼知能の本の表5.2に基づいて算出します。 */
    BigDecimal 村人陣営貢献度(Agent agt) {
        // TODO：カウンタの値が500とか-500とかになってたら困りますが，
        //  これを使うであろう人狼・狂人から見て，誰かの村人度・占い師度などが固定されることはないだろうから，
        //  とりあえず大丈夫かなと…思います…。
        
        BigDecimal ret = BigDecimal.ZERO;
        
        ret = ret.add(BigDecimal.valueOf(
                カウンタ群.get(Role.WEREWOLF).getOrDefault(agt, 0) * (-0.096)));
        ret = ret.add(BigDecimal.valueOf(カウンタ群.get(Role.SEER).getOrDefault(agt, 0) * 1.219));
        ret = ret.add(BigDecimal.valueOf(カウンタ群.get(Role.MEDIUM).getOrDefault(agt, 0) * 0.423));
        ret = ret.add(BigDecimal.valueOf(カウンタ群.get(Role.BODYGUARD).getOrDefault(agt, 0) * 0.743));
        
        return ret;
    }
    
    
    
    
    
    List<Agent> getAllOtherAgents() {
        return allOtherAgents;
    }
    
    void グローバルカウンタから転記する(Map<Role, Map<Agent, Integer>> グローバルカウンタ群) {
        for (Map.Entry<Role, Map<Agent, Integer>> entry : グローバルカウンタ群.entrySet()) {
            if (entry.getKey() == myRole) {
                // 自分の役職については，不要なので，break。
                // 自分が人狼のときについても，もともと3人分かってる（反映してある）はずなので。
                break;
            }
            Map<Agent, Integer> グローバルカウンタ = entry.getValue();
            Map<Agent, Integer> マイカウンタ = カウンタ群.get(entry.getKey());
            for (Agent agt : allOtherAgents) {
                マイカウンタ.put(agt, グローバルカウンタ.get(agt));
            }
        }
    }
    
    Set<Agent> 投票先の純粋戦略(List<Agent> aliveAgentList) {
        List<Agent> aliveOthers = new ArrayList<>(aliveAgentList);
        aliveOthers.remove(me);
        
        HashSet<Agent> 投票先 = new HashSet<>();
        
        switch (myRole.getTeam()) {
            case VILLAGER:
                // 人狼確定の人（人狼度が300を超える人）がいたら，その人を選ぶ。
                // いなければ，人狼陣営貢献度が最も高いエージェントを選ぶ。
                for (Agent agt : aliveOthers) {
                    if (カウンタ群.get(Role.WEREWOLF).get(agt) >= 300) {
                        投票先.add(agt);
                    }
                }
                if (投票先.isEmpty()) {
                    // TODO：同率トップがいても，1人だけしかセットされません。
                    投票先.add(人狼陣営度最大(new ArrayDeque<>(aliveOthers)));
                }
                break;
            case WEREWOLF:
                // TODO：同率トップがいても，1人だけしかセットされません。
                投票先.add(村人陣営貢献度最大(new ArrayDeque<>(aliveOthers)));
                break;
            case OTHERS:
            case ANY:
                // ここには来ないはず
                break;
        }
        return 投票先;
    }
    
    /** このテーブルの視点のエージェントを返す。 */
    Agent getAgent() {
        return me;
    }
    
    /** このテーブルの視点のエージェントの役職を返す。 */
    Role getMyRole() {
        return myRole;
    }
    
    
}
