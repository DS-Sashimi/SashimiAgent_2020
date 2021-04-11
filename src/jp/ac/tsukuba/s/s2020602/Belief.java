package jp.ac.tsukuba.s.s2020602;

import org.aiwolf.client.lib.Content;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** プレイヤーの信念（5人村専用） */
public class Belief {
    
    // NOTE：絶対，視点のエージェント（誰から見た信念なのか）をフィールドに持つべき。
    //  そうでないと，たいへんデバッグしづらい。。
    //  -> フィールド me を持つようになりました！
    
    /** デバッグ中かどうか */
    boolean debug = false;
    
    /** 視点となるエージェント（主人公的な） */
    Agent me;
    
    /** 視点となるエージェントの役職 */
    Role myRole;
    
    /** 可能世界から確率へのマップ */
    // Map<Integer, BigDecimal> probMap = new HashMap<>();
    Map<PossibleWorld, BigDecimal> probMap;
    
    /** 可能世界の集合 */
    PossibleWorld[] 可能世界の集合;
    
    /** scale */
    // int scale = 5;
    MathContext mc = new MathContext(5, RoundingMode.HALF_UP);
    
    /**
     * コンストラクタ： 入力の可能世界配列の各要素に等しい確率を割り当てる信念を作成します。
     */
    public Belief(PossibleWorld[] possibleWorlds, Agent me, Role myRole) {
        this.可能世界の集合 = possibleWorlds;
        
        this.me = me;
        this.myRole = myRole;
        
        probMap = new HashMap<PossibleWorld, BigDecimal>();
        // unit：各世界の確率（すべての可能世界についての等分，すなわち 100/(世界の個数) とする。）
        BigDecimal unit = (BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(可能世界の集合.length), mc);
        for (PossibleWorld pw : 可能世界の集合) {
            probMap.put(pw, unit);
        }
        
        // 自身の役職を固定
        役職固定(me, myRole);
    }
    
    /** 最初から1つの世界が確定している信念 */
    public Belief(PossibleWorld[] possibleWorlds, Agent me, Role myRole, PossibleWorld possibleWorld) {
        this.可能世界の集合 = possibleWorlds;
        this.me = me;
        this.myRole = myRole;
        
        probMap = new HashMap<>();
        probMap.put(possibleWorld, BigDecimal.valueOf(100));
    }
    
    /** コンストラクタ： 与えられた信念の平均として，信念を作成します。 */
    public Belief(PossibleWorld[] possibleWorlds, Agent me, Role myRole, Set<Belief> beliefs) {
        this.可能世界の集合 = possibleWorlds;
        
        this.me = me;
        this.myRole = myRole;
        
        probMap = new HashMap<PossibleWorld, BigDecimal>();
        int m = beliefs.size();
        for (PossibleWorld pw : 可能世界の集合) {
            BigDecimal prob = BigDecimal.ZERO;
            for (Belief belief : beliefs) {
                prob = prob.add(belief.probMap.get(pw));
            }
            prob = prob.divide(BigDecimal.valueOf(m), mc);
            probMap.put(pw, prob);
        }
    }
    
    
    boolean 発言をすべて聞く(Set<Content> contents) {
        
        long start_time = System.currentTimeMillis();
        if (debug) {
            System.out.println("発言をすべて聞く() スタート：" + (System.currentTimeMillis() - start_time));
        }
        
        // 複数の発言を同時に聞くことができるようにする
        // TODO：今のところ，同一人物による発言どうしは矛盾しないものとして扱っています。
        // 現在の実装では，矛盾した場合は，なかったこと（その人からは何も聞かないこと）になります。
        
        if (debug) {
            System.out.println("【Beliefクラス】" + me + "さんが発言を聞きます。");
        }
        if (contents.isEmpty()) {
            System.out.println("と思ったら，Emptyなんですけど…。");
        }
        
        for (Content content : contents) {
            if (debug) {
                System.out.println(content + " ■ 発言者：" + content.getSubject());
            }
            switch (content.getTopic()) {
                case COMINGOUT:
                    // 「content.getRole() != myRole」という条件が必要のような気がする（5人村のとき限定ですが）
                    // TODO：よくわからん。
                    //  https://www.notion.so/2c12b9a809fd4e7a8985f491f18ff6ea の「5人村の問題」を参照のこと。
                    役職固定(content.getSubject(), content.getRole());
                    break;
                case DIVINED:
                    役職固定(content.getSubject(), Role.SEER);
                    占い結果反映(content.getTarget(), content.getResult());
                    break;
                case ESTIMATE:
                    役職固定(content.getTarget(), content.getRole());
                    break;
                case VOTE:
                    // ちょっと雑な実装ですが
                    if (debug && false) {
                        System.out.println("この信念の主人公は " + me + "，役職は " + myRole);
                    }
                    switch (myRole.getTeam()) {
                        case VILLAGER:
                            役職固定(content.getTarget(), Role.WEREWOLF);
                            break;
                        case WEREWOLF:
                            役職固定(content.getTarget(), Role.SEER);
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
        // if (debug) { System.out.println(); }
        // もし，すべての発言を聞いた結果，あり得る世界がなくなってしまったら，
        // 聞くべきでない発言が混ざっていたということで，なにもなかったこと（false）にする。
        if (確率の和が0である()) {
            if (debug) {
                System.out.println("世界が0になり，聞けませんでした：" + contents);
                System.out.println("return：" + (System.currentTimeMillis() - start_time));
            }
            return false; // 聞くべきでない発言が混ざっていた
        }
        if (debug) {
            System.out.println("return：" + (System.currentTimeMillis() - start_time));
        }
        return true; // 正常に聞くことができる発言たちだった
    }
    
    boolean 確率の和が0である() {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal val : probMap.values()) {
            sum = sum.add(val);
        }
        return sum.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /** agent さんの役職を role に固定します。 */
    void 役職固定(Agent agent, Role role) {
        // 削除する世界について，確率を0にします。
        // Set<PossibleWorld> 削除対象の世界 = new HashSet<PossibleWorld>(); // いや，削除する必要はない。確率を0にするだけでよい。
        BigDecimal 削除による確率減少分 = BigDecimal.ZERO;
        int 残る世界の個数 = 0;
        for (PossibleWorld world : probMap.keySet()) {
            if (world.getRole(agent) != role) { // 削除対象の世界
                削除による確率減少分 = 削除による確率減少分.add(probMap.get(world));
                // 削除対象の世界.add(world);
                probMap.replace(world, BigDecimal.ZERO);
            } else if (probMap.get(world).compareTo(BigDecimal.ZERO) != 0) {
                // 残る世界（削除対象とならず，かつ，もともと 0 でもなかった世界）
                残る世界の個数++;
            }
        }
        
        if (残る世界の個数 == 0) {
            return; // でいいのかな？
        }
        
        // 帳尻を合わせる（確率の和が100になるようにする）
        // up =（残る世界ひとつあたりに足す確率）
        BigDecimal up = 削除による確率減少分.divide(BigDecimal.valueOf(残る世界の個数), mc);
        for (PossibleWorld world : probMap.keySet()) {
            // 確率が 0 でなければ，upを足す。
            BigDecimal tmp = probMap.get(world);
            if (tmp.compareTo(BigDecimal.ZERO) != 0) {
                probMap.replace(world, tmp.add(up));
            }
        }
        if (debug) {
            BigDecimal sum = BigDecimal.ZERO;
            for (PossibleWorld world : probMap.keySet()) {
                // sum += probMap.get(world) 的な
                sum = sum.add(probMap.get(world));
            }
            if (sum.compareTo(BigDecimal.valueOf(100.5)) >= 0 || sum.compareTo(BigDecimal.valueOf(99.5)) <= 0) {
                System.out.println("信念の和が100でない：" + sum + "（Belief.javaの役職固定メソッド）");
                System.out.println("調整する必要があります。");
                (new Throwable()).printStackTrace();
                System.exit(0);
            }
        }
    }
    
    /** agent さんの役職は role ではない，とします。 */
    void 役職でない固定(Agent agent, Role role) {
        // 削除する世界について，確率を0にします。
        BigDecimal 削除による確率減少分 = BigDecimal.ZERO;
        int 残る世界の個数 = 0;
        for (PossibleWorld world : probMap.keySet()) {
            if (world.getRole(agent) == role) { // 削除対象の世界
                削除による確率減少分 = 削除による確率減少分.add(probMap.get(world));
                probMap.replace(world, BigDecimal.ZERO);
            } else if (probMap.get(world).compareTo(BigDecimal.ZERO) != 0) {
                // 残る世界（削除対象とならず，かつ，もともと 0 でもなかった世界）
                残る世界の個数++;
            }
        }
        
        if (残る世界の個数 == 0) {
            return; // でいいのかな？
        }
        
        // 帳尻を合わせる（確率の和が100になるようにする）
        // up =（残る世界ひとつあたりに足す確率）
        BigDecimal up = 削除による確率減少分.divide(BigDecimal.valueOf(残る世界の個数), mc);
        for (PossibleWorld world : probMap.keySet()) {
            // 確率が 0 でなければ，upを足す。
            BigDecimal tmp = probMap.get(world);
            if (tmp.compareTo(BigDecimal.ZERO) != 0) {
                probMap.replace(world, tmp.add(up));
            }
        }
        if (debug) {
            BigDecimal sum = BigDecimal.ZERO;
            for (PossibleWorld world : probMap.keySet()) {
                // sum += probMap.get(world) 的な
                sum = sum.add(probMap.get(world));
            }
            if (sum.compareTo(BigDecimal.valueOf(100.5)) >= 0 || sum.compareTo(BigDecimal.valueOf(99.5)) <= 0) {
                System.out.println("信念の和が100でない：" + sum + "（Belief.javaの役職固定メソッド）");
                System.out.println("調整する必要があります。");
                (new Throwable()).printStackTrace();
                System.exit(0);
            }
        }
    }
    
    void 占い結果反映(Agent target, Species result) {
        switch (result) {
            case HUMAN:
                役職でない固定(target, Role.WEREWOLF);
                break;
            case WEREWOLF:
                役職固定(target, Role.WEREWOLF);
                break;
            case ANY:
                break;
        }
    }
    
    Map<Agent, BigDecimal> 役職度(List<Agent> aliveOthers, Role role) {
        Map<Agent, BigDecimal> 度合いmap = new HashMap<Agent, BigDecimal>();
        for (Agent agent : aliveOthers) {
            度合いmap.put(agent, BigDecimal.ZERO);
        }
        // 対象：aliveOthers
        for (Map.Entry<PossibleWorld, BigDecimal> entry : probMap.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                for (Agent agent : aliveOthers) {
                    if (entry.getKey().getRole(agent) == role) {
                        BigDecimal tmp = 度合いmap.get(agent);
                        度合いmap.put(agent, tmp.add(entry.getValue()));
                    }
                }
            }
        }
        return 度合いmap;
    }
    
    @Override
    public String toString() {
        String text = "{";
        for (Map.Entry<PossibleWorld, BigDecimal> entry : probMap.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) != 0) {
                // text = text.concat("世界(" + entry.getKey().getWorldIndex() + ")=" + entry.getValue() + ", ");
                text = text.concat(entry.getKey() + "=" + entry.getValue() + ", ");
                
            }
        }
        return text.concat("}");
        //return probMap.toString();
    }
    
    
    
}
