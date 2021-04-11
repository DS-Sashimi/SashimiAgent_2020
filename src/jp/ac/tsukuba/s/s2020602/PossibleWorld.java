package jp.ac.tsukuba.s.s2020602;


import org.aiwolf.client.lib.Content;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;


public class PossibleWorld {
    
    /** デバッグ中かどうか */
    boolean debug = false;
    
    /** 主人公（これを考えてる Player） */
    private final SashimiBasePlayer_5 myPlayer;
    
    /** 主人公のエージェント */
    //private Agent me;
    
    /** 主人公の役職 */
    //private Role myRole;
    
    /** BigDecimal の除算をする際の MathContext */
    MathContext mc = new MathContext(5, RoundingMode.HALF_UP);
    
    /** 【可能世界の本体】役職のマップ */
    private final Map<Agent, Role> roleMap;
    
    /** プレイヤーの人数 */
    private final int N;
    
    /** 可能世界の番号 */
    private final int worldIndex;
    
    /** 各人の信念 */
    private final Map<Agent, Belief> beliefMap; // = new HashMap<>();
    
    /** 各エージェントの発言を格納するマップ（主人公Playerから取得） */
    private final Map<Agent, Set<Content>> talksMap;
    
    /** 現時点で確定している利得（この世界における役職で計算したもの） */
    private BigDecimal currentPayoff;
    
    /** 可能世界の集合 */
    private PossibleWorld[] 可能世界の集合;
    
    // ========================================================================
    // ========================================================================
    
    /** コンストラクタ */
    public PossibleWorld(SashimiBasePlayer_5 myPlayer, int n, List<Agent> agentList, Role[] roleProfile,
                         int worldIndex) {
        this.myPlayer = myPlayer;
        //this.me = myPlayer.me;
        //this.myRole = myRole;
        N = n;
        
        可能世界の集合 = null;
        
        // roleMap = new HashMap<Agent, Role>(argRoleMap); の代わり：
        roleMap = new HashMap<Agent, Role>();
        for (int i = 0; i < n; i++) {
            roleMap.put(agentList.get(i), roleProfile[i]);
        }
        
        this.worldIndex = worldIndex;
        
        // 各フィールドの初期化
        beliefMap = new HashMap<>();
        talksMap = new HashMap<>();
        
        // 各人の役職に基づいて，各人の信念を作成
        // これは，可能世界が全部できてないとだめなので，可能世界の生成と同時にはできない．
    }
    
    
    // ========================================================================
    // ========================================================================
    
    void set可能世界の集合(PossibleWorld[] 可能世界の集合) {
        // いや，可能世界は直接には持たないことにしようか．
        // なんか計算が大変そう？？？
        // いや，参照の値（？）だけの保持ならそうでもないかも？
        // そもそも必要かどうかわからないけど・・・
        this.可能世界の集合 = 可能世界の集合;
    }
    
    void 各人の役職により各人の信念を初期化() {
        for (Map.Entry<Agent, Role> entry : roleMap.entrySet()) {
            if (entry.getKey() == myPlayer.me) {
                // 主人公は，確定した世界のみを100%の可能世界とする
                Belief tmpBelief = new Belief(可能世界の集合, myPlayer.me, myPlayer.myRole, this);
                beliefMap.put(entry.getKey(), tmpBelief);
            } else {
                // 主人公以外は，普通の信念
                Belief tmpBelief = new Belief(可能世界の集合, entry.getKey(), entry.getValue());
                beliefMap.put(entry.getKey(), tmpBelief);
            }
        }
    }
    
    void 白確定者を追加(Agent agt) {
        for (Agent other : myPlayer.aliveOthers) {
            // 各人 other の信念を更新する
            beliefMap.get(other).役職でない固定(agt, Role.WEREWOLF);
        }
    }
    
    // 信念は，発言が増えたときに もとの信念を ちょこちょこっと変えればよい，というわけではないので，
    // 期待利得メソッドの引数には要らないと思います。
    // 一方で，なんらかの形で，最新の「誰から見ても白確定の人」リストは取得する必要があります。
    
    /** 発言の集合を受け取り，それをもとに投票を行ったときの主人公の期待利得を求めます */ // （全員の期待利得を求める必要はないですよね！？）
    BigDecimal 期待利得(Map<Agent, Set<Content>> 発言プロファイル) {
        // とりあえず，「誰から見ても白確定の人」リストも一緒に受け取る方のメソッドを使いましょう。<- え，そんなメソッドなくない？？？ どゆことや（たぶん，新しくメソッドを作ろうとして結局作らなかったのだろう。）。
        // いや，白確定の人かどうかは，世界を見て，「もしこの世界だったら，もうOOさんが追放されてるから，ゲームは終わってるはず。よってこの世界は起こっていない。」というようなかんじで判断されるのだろう，，，たぶん，，。
        
        // PossibleWorld[] 可能世界の集合 = new PossibleWorld[myPlayer.possibleWorlds.length];
        
        // 各人の信念を計算します：
        // 各人の信念を，各人の役職で削る
        // 白確定リストを反映する
        // 発言を聞く
        // TODO：事実に反するのは無視するので，発言を聞く前にtalksMapのなかに聞かない発言が混ざってないか確かめる必要があるかもですね
        // -> 聞きながら，矛盾が発生したら（≒ 可能世界が0個になったら），発言を聞かなかったことにする，というようなかんじで実現できてると思います。
        
        if (debug) {
            System.out.println("発言プロファイル：" + 発言プロファイル);
            if (発言プロファイル.size() != 5) {
                System.out.println("発言プロファイルのAgentが5人以外（未満または以上）になっています！");
                (new Throwable()).printStackTrace();
            }
        }
        
        // 白確定リストを反映する
        // ：いや，これは，白確定者が増えたときに行うことにしましょう．
        
        // 各人の心の中（各人の信念を更新します）
        for (Agent 聞く人 : 発言プロファイル.keySet()) { // forall agents
            
            // TODO：ここで，「聞く人」としては，生存エージェントだけでいいよね？
            
            if (debug) {
                System.out.println(聞く人 + "さんがみんなの発言を聞きます。");
            }
            // 発言を聞きます。
            // 世界観の数：矛盾し合う発言の組の数。（聞くべき発言をしたエージェントの人数）
            // int 世界観の数 = 0; // tmpBeliefSet の size() として取得できるので，不要っぽい
            Set<Belief> tmpBeliefSet = new HashSet<Belief>();
            for (Agent 発言者 : 発言プロファイル.keySet()) {
                // もし発言者が何も言っていないならば，continue
                // また，発言者 が自分である場合も，continue
                if (debug && false) {
                    System.out.print(聞く人 + "さんが" + 発言者 + "さんの発言を聞きます。");
                    // System.out.println(聞く人 + "さんが" + 発言者 + "さんの発言を聞きます：" + 発言プロファイル.get(発言者));
                }
                if (発言プロファイル.get(発言者).isEmpty()) {
                    if (debug && false) {
                        System.out.print("Emptyだったので，" + 聞く人 + "さんが continue; しました。");
                    }
                    continue;
                }
                if (聞く人 == 発言者) {
                    if (debug && false) {
                        System.out.print("自分の発言だったので，" + 聞く人 + "さんが continue; しました。");
                    }
                    continue;
                }
                
                // TODO：矛盾する発言は無視するんですけど…どうすればいいでしょう
                // 発言プロファイルから削除するわけにはいかないし…
                // 無視する発言とは：自分（聞く人）の役職や既得占い結果に反することを言ってるもの
                // TODO：↓ ほんと！？ ↓ 要確認。
                // そういった発言があるときは，可能世界が0個になります（と思います）ので，
                // そのようなときは，世界観の数を増やさずにcontinueするということでどうでしょうか。
                // おそらくそのように実装できております。
                
                Belief tmpBelief = new Belief(可能世界の集合, 聞く人, roleMap.get(聞く人));
                if (tmpBelief.発言をすべて聞く(発言プロファイル.get(発言者))) {
                    tmpBeliefSet.add(tmpBelief);
                    // 世界観の数++;
                }
            }
            
            Belief 最終的なagentさんの信念 = null;
            
            if (tmpBeliefSet.isEmpty()) {
                if (debug && false) {
                    System.out.print(聞く人 + "さんは，何も聞きませんでした。");
                }
                最終的なagentさんの信念 = beliefMap.get(聞く人);
            } else {
                // 最終的なagentさんの信念 = new Belief(可能世界の集合, 聞く人, tmpBeliefSet);
                最終的なagentさんの信念 = new Belief(可能世界の集合, 聞く人, roleMap.get(聞く人), tmpBeliefSet);
            }
            
            if (debug && false) {
                System.out.println("最終的な" + 聞く人 + "さんの信念：" + 最終的なagentさんの信念);
            }
            
            beliefMap.put(聞く人, 最終的なagentさんの信念);
        }
        
        // 全員の信念が更新できたら，次は投票先の混合戦略を求めます：
        // 各人について，狙う役職を定義します。
        // 各人は，狙う役職と，信念に応じて，投票先の混合戦略を求めます。
        
        // 投票先の混合戦略
        Map<Agent, Map<Agent, BigDecimal>> 投票先の混合戦略 = new HashMap<Agent, Map<Agent, BigDecimal>>();
        
        // まず，投票者を格納 ->  いや，ここでは格納する必要はない
		/*
		for (Agent agent : 発言プロファイル.keySet()) { // forall agents
		    // 投票先の混合戦略.put(agent, new HashMap<Agent, BigDecimal>());
		} */
        
        // 各人の狙う役職を設定　＆　投票先の混合戦略を求める
        // for (Agent 視点agent : 発言プロファイル.keySet()) { // forall agents
        for (Agent 視点agent : myPlayer.currentGameInfo.getAliveAgentList()) { // forall agents
            
            // TODO：ここで，視点agent は，生存しているエージェントに限定するべきでは？？
            //  myPlayer.currentGameInfo.getAliveAgentList() でいいよね？？
            //  視点を考えるのは，投票先を考えたいからで，
            //  投票するのは，生存しているエージェントだけだから。
            
            // 各人の狙う役職を設定
            Set<Role> 狙う役職の集合 = new HashSet<Role>();
            // 所属陣営ごとに，狙う役職を決定
            switch (roleMap.get(視点agent).getTeam()) {
                case VILLAGER:
                    狙う役職の集合.add(Role.WEREWOLF);
                    狙う役職の集合.add(Role.POSSESSED);
                    break;
                case WEREWOLF:
                    狙う役職の集合.add(Role.SEER);
                    break;
                case OTHERS:
                case ANY:
                    break;
            }
            
            if (debug && false) {
                System.out.println(視点agent + "さん（役職は" + roleMap.get(視点agent) + "）の狙う役職：" + 狙う役職の集合);
            } // OK
            
            // 視点agent さんが OOさんに投票する確率は，
            // OOさんが 狙う役職であるような世界の確率の和であります。
            
            // 各人の狙う役職っぽい度合いを求める
            // 求：Agent -> BigDecimal（Agentが狙う役職のいずれかである確率）
            Map<Agent, BigDecimal> 度合い配列 = new HashMap<Agent, BigDecimal>();
            // この配列（のちに投票先の混合戦略にもなるかも）には，他の人だけを格納します。
            // for (Agent 他の人 : 発言プロファイル.keySet()) { // ではなく
            // 発言プロファイル.keySet() の代わりに getAliveAgentList にする：
            for (Agent 他の人 : myPlayer.currentGameInfo.getAliveAgentList()) {
				/* if (視点agent == 他の人) {
				    continue;
				} else {
				    度合い配列.put(他の人, BigDecimal.ZERO);
				} */ // の代わりに：
                // if (視点agent != 他の人 && myPlayer.currentGameInfo.getAliveAgentList().contains(他の人)) {
                // myPlayer.currentGameInfo.getAliveAgentList().contains(他の人) の代わりに，myPlayer.isAlive(他の人) を使う。
                if (視点agent != 他の人) { // 生存エージェントに関するfor文なのでこの部分は不要： && myPlayer.isAlive(他の人)) {
                    // 「他の人」が，（視点agent以外の人）であり，かつ生存していれば，「他の人」を度合い配列に格納
                    度合い配列.put(他の人, BigDecimal.ZERO);
                }
            }
            
            for (Role role : 狙う役職の集合) {
                for (PossibleWorld pw : 可能世界の集合) {
                    if (debug && false) {
                        System.out.println("テスト");
                        System.out.println("beliefMap：" + beliefMap);
                        System.out.println("視点agent：" + 視点agent);
                        System.out.println("pw：" + pw);
                    }
                    BigDecimal 他の人がroleである世界pwに視点agentが割り当てる確率 = beliefMap.get(視点agent).probMap.get(pw);
                    if (debug && false) {
                        System.out.println("他の人がroleである世界pwに視点agentが割り当てる確率" + 他の人がroleである世界pwに視点agentが割り当てる確率);
                    }
                    if (Objects.isNull(他の人がroleである世界pwに視点agentが割り当てる確率)) {
                        // TODO：なんかよくわかんないけど，なるぽエラーが出るので，とりあえずcontinueにしておく．
                        continue;
                    }
                    if (他の人がroleである世界pwに視点agentが割り当てる確率.compareTo(BigDecimal.ZERO) == 0) {
                        continue;
                    }
                    for (Agent 他の人 : 度合い配列.keySet()) {
                        if (他の人 == 視点agent) {
                            System.out.println("ここに来るまでに，度合い配列は自分以外のエージェントだけになっているはずなのに…。おかしいです！");
                            System.out.println("度合い配列：" + 度合い配列);
                            (new Throwable()).printStackTrace();
                            System.exit(0);
                            continue;
                        } // エラー
                        if (pw.roleMap.get(他の人) == role) {
                            BigDecimal もとの値 = 度合い配列.get(他の人);
                            度合い配列.put(他の人, もとの値.add(他の人がroleである世界pwに視点agentが割り当てる確率));
                        }
                    }
                }
            }
            
            // 度合い配列の確率の和を100にそろえる。
            
            BigDecimal sum = BigDecimal.ZERO;
			/*
			for (BigDecimal val : 度合い配列.values()) {
			    sum = sum.add(val);
			} */ // 書き換え -> 下のようにします。
            for (Agent 他の人 : 度合い配列.keySet()) {
                sum = sum.add(度合い配列.get(他の人));
            }
            if (sum.compareTo(BigDecimal.ZERO) == 0) {
                // sum == 0 のとき（正しく実装できてれば，あり得ない）
                if (debug) {
                    System.out.println("【おかしいですね…】度合い配列の確率の和が0になってる：" + 度合い配列);
                    (new Throwable()).printStackTrace();
                    System.exit(0);
                }
                // では，こういうときは，自分以外のみんなで等分することにしましょう。
                for (Agent 他の人 : 度合い配列.keySet()) {
                    // 度合い配列.replace(他の人, BigDecimal.valueOf(((double)100.0) / 度合い配列.keySet().size()));
                    度合い配列.replace(他の人,
                            BigDecimal.valueOf(100)
                                    .divide(BigDecimal.valueOf(度合い配列.keySet().size()), mc));
                }
                // TODO：いや，違う。人が減ったときにも対応しなければならない。
                //  -> 一応，できたかな？（100 / 度合い配列.keySet().size()) にしてあるので。）
            } else if (sum.compareTo(BigDecimal.valueOf(100)) == 0) {
                // sum == 100 のとき
                // なにもしなくてよい
            } else {
                // sum が 100 でない数になっているとき
                if (debug && false) {
                    System.out.print("度合い配列の和が " + sum + " になってるので，調整します：");
                }
                sum = sum.divide(BigDecimal.valueOf(100), mc);
                if (sum.compareTo(BigDecimal.ZERO) == 0) {
                    
                    // System.out.println("sumを100にするために100で割ったら0になった，つまり，100にとても近かった。ということで，そのままにします。");
                    // いやいやいやいや，100/100 = 1 ですよ。0じゃないですよ。0になるわけないのではないか。
                    System.out.println("sumを100にするために100で割ったら0になった。どゆことや。");
                    (new Throwable()).printStackTrace();
                    System.exit(0);
                    
                } else {
                    for (Agent 他の人 : 度合い配列.keySet()) {
                        BigDecimal もとの値 = 度合い配列.get(他の人);
                        度合い配列.put(他の人, もとの値.divide(sum, mc));
                    }
                    if (debug && false) {
                        BigDecimal 信念の和 = BigDecimal.ZERO;
                        for (Agent 他の人 : 度合い配列.keySet()) {
                            信念の和 = 信念の和.add(度合い配列.get(他の人));
                        }
                        System.out.println("度合い配列の和が " + 信念の和 + " になりました。");
                    }
                }
                
            }
            
            // 度合い配列の確率の和を100に揃え終わった。
            
            投票先の混合戦略.put(視点agent, new HashMap<Agent, BigDecimal>(度合い配列));
        }
        
        // List<Agent> aliveAgents = new ArrayList<>(myPlayer.currentGameInfo.getAliveAgentList());
        
        
        
        
        
        // 各人の投票混合戦略をもとに，主人公さんの利得を求めます。で，メソッドの返り値として返却します。
        
        List<Agent> aliveAgents = new ArrayList<>(myPlayer.currentGameInfo.getAliveAgentList());
        // = new ArrayList<Agent>(発言プロファイル.keySet());
        
        if (debug) {
            System.out.println("aliveAgents リスト：" + aliveAgents);
        }
        
        // TODO：5人のときにしか対応していません
        //  -> いや，今は対応できてる気がします．
        
        List<Map<Agent, Agent>> 投票先の集合リスト = 投票プロファイル2(aliveAgents);
        if (debug && false) {
            System.out.println("投票パターンのリスト："); // + 投票プロファイル2(aliveAgents));
            for (Map<Agent, Agent> map : 投票先の集合リスト) {
                System.out.print("{");
                for (Map.Entry<Agent, Agent> entry : map.entrySet()) {
                    System.out.print(" " + entry.getKey().getAgentIdx() + "->" + entry.getValue().getAgentIdx() + " ");
                }
                System.out.println("}");
            }
        }
        
        Map<Agent, BigDecimal> 追放される確率 = new HashMap<>();
        
        for (Map<Agent, Agent> 投票先の集合 : 投票先の集合リスト) {
            BigDecimal tmp利得 = null;
            // 誰が追放されるのか。
            // 確率はいくつか。
            BigDecimal 確率 = BigDecimal.ONE;
            
            
            Map<Agent, Integer> 得票数 = new HashMap<>();
            int 現在の最多得票数 = 0;
            Set<Agent> 最多得票エージェント = new HashSet<>();
            for (Map.Entry<Agent, Agent> entry : 投票先の集合.entrySet()) {
                // for (Agent 投票者 : 投票先の集合.keySet()) {
                Agent 投票先 = entry.getValue();
                int 新たな得票数 = 得票数.getOrDefault(投票先, 0) + 1;
                得票数.put(投票先, 新たな得票数);
                if (現在の最多得票数 < 新たな得票数) {
                    現在の最多得票数 = 新たな得票数;
                    最多得票エージェント.clear();
                    最多得票エージェント.add(投票先);
                } else if (現在の最多得票数 == 新たな得票数) {
                    最多得票エージェント.add(投票先);
                }
                確率 = 確率.multiply(投票先の混合戦略.get(entry.getKey()).get(投票先)
                        .divide(BigDecimal.valueOf(100), mc));
            }
            
            if (最多得票エージェント.isEmpty()) {
                最多得票エージェント = new HashSet<>(aliveAgents);
            }
            BigDecimal 各人に加える確率 = 確率.divide(
                    BigDecimal.valueOf(最多得票エージェント.size()), mc);
            for (Agent agt : 最多得票エージェント) {
                BigDecimal 現在の追放される確率 = 追放される確率.getOrDefault(agt, BigDecimal.ZERO);
                追放される確率.put(agt, 現在の追放される確率.add(各人に加える確率));
            }
        }
        
        // 追放される確率 に，各人の追放される確率が入っている。
        if (debug) {
            System.out.println("追放される確率：" + 追放される確率);
        }
        
        if (debug) {
            System.out.print("確率の和：");
            BigDecimal sum = BigDecimal.ZERO;
            for (Map.Entry<Agent, BigDecimal> entry : 追放される確率.entrySet()) {
                sum = sum.add(entry.getValue());
            }
            System.out.println(sum);
        }
        
        BigDecimal 期待利得 = BigDecimal.ZERO;
        
        for (Map.Entry<Agent, BigDecimal> entry : 追放される確率.entrySet()) {
            BigDecimal 追放確率 = entry.getValue();
            if (追放確率.compareTo(BigDecimal.ZERO) > 0) {
                // TODO：村人陣営・人狼陣営の勝利条件が満たされる場合を追加する必要がありそう．
                // 追放される人がいない日はないから，残り人数が3人で，かつ人狼でない人が追放されるとき，人狼の勝ち．
                if (aliveAgents.size() <= 3 && roleMap.get(entry.getKey()) != Role.WEREWOLF) {
                    // 人狼陣営の勝ち
                    switch (myPlayer.myRole.getTeam()) {
                        case VILLAGER:
                            期待利得 = 期待利得.add(追放確率.multiply(
                                    Payoff.MAKE // 人狼陣営が勝ったときの村人陣営の利得
                            ));
                            break;
                        case WEREWOLF:
                            期待利得 = 期待利得.add(追放確率.multiply(
                                    Payoff.KACHI // 人狼陣営が勝ったときの人狼陣営の利得
                            ));
                            break;
                    }
                } else {
                    // 期待利得 += 追放される人の確率 * 追放される人を追放したときの主人公の利得
                    期待利得 = 期待利得.add(
                            追放確率.multiply(
                                    Payoff.payoff_5(roleMap.get(entry.getKey()), myPlayer.myRole.getTeam()) // その人が追放されたときの主人公の利得
                            ));
                }
            }
        }
        
        return 期待利得;
    }
    
    /** 主人公の投票先を受け取り，他の皆が beliefMap をもとに投票を行ったときの主人公の期待利得を求めます */ // （全員の期待利得を求める必要はないですよね！？）
    BigDecimal 投票先の期待利得(Agent 自分の投票先) {
        
        return null;
    }
    
    List<Map<Agent, Agent>> 投票プロファイル2(List<Agent> エージェント集合) {
        List<Agent> listingAgents = new ArrayList<Agent>(エージェント集合);
        List<Agent> allAgents = new ArrayList<Agent>(エージェント集合);
        return generateMap(listingAgents, allAgents);
    }
    
    List<Map<Agent, Agent>> generateMap(List<Agent> listingAgents, List<Agent> allAgents) {
        List<Map<Agent, Agent>> ret = new ArrayList<>();
        if (listingAgents.size() == 1) {
            for (Agent agt : allAgents) {
                if (agt != listingAgents.get(0)) {
                    Map<Agent, Agent> inside = new HashMap<>();
                    inside.put(listingAgents.get(0), agt);
                    ret.add(inside);
                }
            }
        } else {
            List<Map<Agent, Agent>> restAgents = generateMap(rest(listingAgents), allAgents);
            for (Agent agt : allAgents) {
                if (agt != listingAgents.get(0)) {
                    for (Map<Agent, Agent> map : restAgents) {
                        Map<Agent, Agent> inside = new HashMap<>();
                        inside.put(listingAgents.get(0), agt);
                        inside.putAll(map);
                        ret.add(inside);
                    }
                }
            }
        }
        return ret;
    }
    
    List<Agent> rest(List<Agent> list) {
        List<Agent> newList = new ArrayList<>();
        for (int i = 1; i < list.size(); i++) {
            newList.add(list.get(i));
        }
        return newList;
    }
    
    Role getRole(Agent agent) {
        return roleMap.get(agent);
    }
    
    int getWorldIndex() {
        return worldIndex;
    }
    
    @Override
    public String toString() {
        return "世界(" + worldIndex + ")";
    }
}
