package jp.ac.tsukuba.s.s2020602;

import org.aiwolf.client.lib.Content;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import java.math.BigDecimal;
import java.util.*;

public class SashimiBasePlayer_5 extends SashimiBasePlayer {
    
    /** 誰から見ても白確定のエージェントたち */
    private final Set<Agent> whiteAgentSet = new HashSet<>();
    
    /** CO済みなら true */
    boolean isCameout;
    
    /** 占い結果報告数 */
    List<Judge> myJudgeList = new ArrayList<Judge>();
    
    /** 主人公が選択できる発言の集合 */
    Set<Content> talkChoices = new HashSet<>();
    
    /** 各エージェントがすでに行った発言を格納するマップ */
    Map<Agent, Set<Content>> talksMap = new HashMap<Agent, Set<Content>>();
    
    /** 可能世界（5人村の場合，60個） */
    PossibleWorld[] possibleWorlds;
    
    /** meの信念：可能世界と，それの確率 */
    Belief myBelief;
    
    // ========================================================================
    // ========================================================================
    // ========================================================================
    
    @Override
    public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
        super.initialize(gameInfo, gameSetting);
        
        isCameout = false;
        myJudgeList.clear();
        
        whiteAgentSet.clear();
        
        // 発言の選択肢を初期化。選べる発言を全て格納しておく。
        talkChoices.clear();
        // まずは，SKIPの発言
        talkChoices.add(Content.SKIP);
        // 占い結果の各場合をセット
        for (Agent targetAgent : aliveOthers) {
            // 「targetAgent さんが人狼でした」
            talkChoices.add(divinedContent(me, targetAgent, Species.WEREWOLF));
            // 「targetAgent さんは人狼ではありませんでした」
            talkChoices.add(divinedContent(me, targetAgent, Species.HUMAN));
        }
        
        // 行われた発言を記録しておくMapを初期化
        talksMap.clear();
        for (Agent agent : gameInfo.getAgentList()) {
            HashSet<Content> contentSet = new HashSet<Content>();
            talksMap.put(agent, contentSet);
        }
        
        // 可能世界を作成（初期化）。信念を持つ（信念は，自分の役職を反映して初期化される）。
        可能世界を作成();
    }
    
    @Override
    public void dayStart() {
        super.dayStart();
    }
    
    @Override
    public void update(GameInfo gameInfo) {
        super.update(gameInfo);
    }
    
    /** 再帰的に文を解析する */
    void parseSentence(Content content) {
        // TODO：聞くようにします：推測文と投票宣言
		/*
		if (estimateReasonMap.put(content)) {
		    return; // 推測文と解析できた
		} */
		/*
		if (voteReasonMap.put(content)) {
		    return; // 投票宣言と解析できた
		} */
        switch (content.getTopic()) {
            case COMINGOUT:
                comingoutMap.put(content.getTarget(), content.getRole());
                // BRChooserのために
                // 発言Mapにcontentを格納
                // talksMapにおいて，OOさんの発言集合に，contentを追加する…というメソッドが必要である。
                出された発言をtalksMapに追加(content);
                // BRChooserのためにの部分，ここまで。
                return;
            case DIVINED:
                // divinationList.add(new Judge(day, content.getSubject(), content.getTarget(), content.getResult()));
                // BRChooserのためにtalkProfileを更新
                出された発言をtalksMapに追加(content);
                // BRChooserのためにの部分，ここまで。
                return;
            case ESTIMATE:
                出された発言をtalksMapに追加(content);
                return;
            case VOTE:
                出された発言をtalksMapに追加(content);
                return;
            case IDENTIFIED:
                // identList.add(new Judge(day, content.getSubject(), content.getTarget(), content.getResult()));
                // 5人村では霊媒師はいませんので…とりあえずスルー。
                return;
            case OPERATOR:
                parseOperator(content);
                return;
            default:
                break;
        }
    }
    
    void addExecutedAgent(Agent executedAgent) {
        if (executedAgent != null) {
            aliveOthers.remove(executedAgent);
            if (!executedAgents.contains(executedAgent)) {
                executedAgents.add(executedAgent);
                // 5人村の場合だけですが // TODO：15人村の場合は別途考えよ。
                addToWhiteList(executedAgent);
            }
        }
    }
    
    void addKilledAgent(Agent killedAgent) {
        if (killedAgent != null) {
            aliveOthers.remove(killedAgent);
            if (!killedAgents.contains(killedAgent)) {
                killedAgents.add(killedAgent);
                // 襲撃されたエージェントは，白確定 // ここは15人村も同じです．
                addToWhiteList(killedAgent);
            }
        }
    }
    
    void addToWhiteList(Agent whiteAgent) {
        whiteAgentSet.add(whiteAgent);
        // 自分の信念を更新
        myBelief.役職でない固定(whiteAgent, Role.WEREWOLF);
        // 皆の信念を更新
        for (Map.Entry<PossibleWorld, BigDecimal> entry : myBelief.probMap.entrySet()) {
            // 当該世界の可能性が0より大きいときに限り
            //    if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) { }
            // ↑ とりあえず全部の世界でやってみる．
            
            // 各人の信念を更新
            entry.getKey().白確定者を追加(whiteAgent);
        }
    }
    
    // ========================================================================
    
    /** content を，subject の発言として talksMap に追加します。 */
    void 出された発言をtalksMapに追加(Content content) {
        if (!Objects.isNull(content)) {
            Agent subject = content.getSubject();
            // 発言者（talker）と主語（subject）が一致するときだけ talksMap に追加
            if (subject == talker) {
                talksMap.get(subject).add(content);
            }
        }
    }
    
    @Override
    public String talk() {
        
        log("talkを決める前のCO状況：" + comingoutMap);
        
        long start_time = System.currentTimeMillis();
        log("talk() スタート：" + (System.currentTimeMillis() - start_time));
        
        talkTurn++;
        
        // log(day + "日目 第" + talkTurn + "ターンまでの talksMap：" + talksMap);
        
        // これ以上 何も言えないとき
        if (isCameout && myJudgeList.size() >= day) {
            return dequeueTalk();
        }
        
        // まず，自分の発言の選択肢を準備
        // ここで用意するのではなくて，ここに来たときにはそのまま使えるようにしておく。
        // 最初にすべての選択肢を列挙しておいて，無効になったものがあったら
        // その都度削除するようにする。
        // 削除には Contentクラスの equalsメソッドが使用できると思われる。
        // たぶん，誰かが追放されたり襲撃されたりしたときに，
        // その人を黒だと言わないようにすれば，大丈夫。
        // そのために，「誰から見ても白確定の人」リストを作成して，
        // そのリストに入った人は，「全員がその人を非人狼だと見る」ということでどうでしょうか。
        // 非人狼と見る
        // ＝ 「占い黒発言をしない」，
        // ＆ 「信念の更新（というか…信念を更新するときに，当該リストを参照するようにしたほうがいいですね。）」
        
        // 各発言の期待利得をもとめる
        // 各可能世界において
        // 現在すでに出ている情報＋自分のContent（による投票先）による利得を求める
        
        // Map<Agent, Set<Content>> 発言プロファイル = new HashMap<>(talksMap);
        Map<Content, BigDecimal> 期待利得関数 = new HashMap<Content, BigDecimal>();
        
        log("for1 の前：" + (System.currentTimeMillis() - start_time));
        
        // log("【talksMapより発言プロファイル生成】主人公の発言候補を加える前：" + talksMap);
        
        // 調べる世界を，6つまでにします。6つに絞ったものが tmpProbMap です。
        Map<PossibleWorld, BigDecimal> tmpProbMap = new HashMap<>();
        tmpProbMap.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .limit(6)
                .forEach(e -> tmpProbMap.put(e.getKey(), e.getValue()));
        
        for (Content 発言候補 : talkChoices) {
            
            // 発言プロファイル <- talksMap;（参照をコピーしないようにしてコピーする）
            Map<Agent, Set<Content>> 発言プロファイル = new HashMap<>();
            for (Map.Entry<Agent, Set<Content>> entry : talksMap.entrySet()) {
                発言プロファイル.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }
            
            // log("【talksMapより発言プロファイル生成】主人公の発言候補を加える前：" + 発言プロファイル);
            
            // talkChoices の中の各 発言候補 について：
            
            // まずは発言プロファイルに加える
            発言プロファイル.get(me).add(発言候補);
            
            // 期待利得を求める。
            // 初期値は，ゼロ
            BigDecimal 期待利得 = BigDecimal.ZERO;
            
            long start_time_internal = System.currentTimeMillis();
            int 個数 = 0;
            
            for (PossibleWorld pw : possibleWorlds) {
                // もし，可能性を 0 と考えている世界であれば，スルー
                if (tmpProbMap.getOrDefault(pw, BigDecimal.ZERO).compareTo(BigDecimal.ZERO) == 0) { // myBelief.probMap.get(pw) == BigDecimal.ZERO
                    continue;
                }
                // そうでなければ，期待利得を加算
                BigDecimal 加算分 = pw.期待利得(発言プロファイル)
                        .multiply(tmpProbMap.get(pw).divide(BigDecimal.valueOf(100), mc));
                期待利得 = 期待利得.add(加算分);
                
                個数++;
                log(個数 + "個目の世界：" + (System.currentTimeMillis() - start_time_internal));
            }
            
            log("「" + 発言候補 + "」の期待利得：" + 期待利得);
            
            期待利得関数.put(発言候補, 期待利得);
            
            log("1個：" + (System.currentTimeMillis() - start_time));
        }
        
        log("for1 の後：" + (System.currentTimeMillis() - start_time));
        
        // 期待利得を比べて，最も大きいものを集合に格納する
        Set<Content> 最適な発言の集合 = new HashSet<>();
        BigDecimal 期待利得の最大値 = Payoff.MAKE.subtract(BigDecimal.ONE); // -3点になってるはず。
        
        // log("期待利得の最大値の初期値：" + 期待利得の最大値); // OK
        
        for (Content 発言 : 期待利得関数.keySet()) {
            int 比較 = 期待利得関数.get(発言).compareTo(期待利得の最大値);
            if (比較 == 0) {
                最適な発言の集合.add(発言);
            } else if (比較 > 0) {
                期待利得の最大値 = 期待利得関数.get(発言);
                最適な発言の集合.clear();
                最適な発言の集合.add(発言);
            }
        }
        
        log("for2 の後：" + (System.currentTimeMillis() - start_time));
        
        log("最適な発言の集合：" + 最適な発言の集合);
        
        // どれか一つを選んで，発言キューに格納する
        
        // ランダムセレクト
        // もしトピックがdivined で，未COの場合，CO文を先に格納してからdivinedを格納。
        
        Content 選択する発言 = Utility.randomSelect(new ArrayList<>(最適な発言の集合));
        log("選んだ発言は：" + 選択する発言);
        
        switch (選択する発言.getTopic()) {
            case DIVINED:
                // 未COであれば，まずCOする
                if (!isCameout) {
                    Content content = coContent(me, me, Role.SEER);
                    enqueueTalk(content);
                    isCameout = true;
                    // parseSentence の処理をここでやってしまう
                    comingoutMap.put(me, Role.SEER);
                }
                // 占い結果報告数が可能な範囲内であれば，結果を報告する
                int 既出のマイ占い結果報告数 = myJudgeList.size();
                log("DIVINED発言をしたい：day＝" + day + "，myJudgeListのサイズ＝" + 既出のマイ占い結果報告数 + "，発言を…");
                
                if (既出のマイ占い結果報告数 < day) {
                    log("します！");
                    
                    enqueueTalk(選択する発言);
                    myJudgeList.add(new Judge(既出のマイ占い結果報告数, me,
                            選択する発言.getTarget(), 選択する発言.getResult()));
                    // parseSentence の処理をここでやってしまう
                    // 出された発言をtalksMapに追加(選択する発言); // ここじゃなくて，dequeueTalk() でやることにする。
                } else {
                    log("しません！");
                }
                break;
            default:
                enqueueTalk(選択する発言);
                break;
        }
        
        log("return 直前：" + (System.currentTimeMillis() - start_time));
        
        return dequeueTalk();
    }
    
    /** 発言キューからcontentを1つ取り出して，返します。 */
    String dequeueTalk() {
        if (talkQueue.isEmpty()) {
            return Talk.SKIP;
        }
        Content content = talkQueue.poll();
        
        // 自分の発言を，talksMapに格納します。
        talker = me;
        出された発言をtalksMapに追加(content);
        // 注意：自分の発言 content において，subject＝meじゃないと，talksMapに格納されません。
        
        if (content.getSubject() == me) {
            return Content.stripSubject(content.getText());
        }
        return content.getText();
    }
    
    // ========================================================================
    
    @Override
    public Agent vote() {
        
        log("【VOTEメソッド】主人公の信念：" + myBelief);
        
        // 発言を聞いていきます
        
        // 白確定リストを反映します
        for (Agent 白確定の人 : whiteAgentSet) {
            myBelief.役職でない固定(白確定の人, Role.WEREWOLF);
        }
        
        // リスニング
        log(me + "さんがみんなの発言を聞きます。");
        
        Set<Belief> tmpBeliefSet = new HashSet<Belief>();
        
        for (Agent 発言者 : talksMap.keySet()) {
            log(me + "さんが" + 発言者 + "さんの発言を聞きます。");
            
            if (talksMap.get(発言者).isEmpty()) {
                log("Emptyだったので，" + me + "さんが continue; しました。");
                continue;
            }
            if (me == 発言者) {
                log("自分の発言だったので，" + me + "さんが continue; しました。");
                continue;
            }
            Belief tmpBelief = new Belief(possibleWorlds, me, myRole);
            if (tmpBelief.発言をすべて聞く(talksMap.get(発言者))) {
                tmpBeliefSet.add(tmpBelief);
            }
        }
        
        // Belief 最終的なmeの信念 = null;
        
        if (tmpBeliefSet.isEmpty()) {
            log(me + "さんは，何も聞きませんでした。");
            // 最終的なmeの信念 = myBelief;
        } else {
            // myBelief = new Belief(possibleWorlds, me, tmpBeliefSet);
            myBelief = new Belief(possibleWorlds, me, myRole, tmpBeliefSet);
        }
        
        log("最終的な" + me + "さんの信念：" + myBelief);
        
        // 狙う役職を設定
        Set<Role> 狙う役職の集合 = new HashSet<Role>();
        // 所属陣営ごとに，狙う役職を決定
        switch (myRole.getTeam()) {
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
        
        log("私（役職は" + myRole + "）の狙う役職：" + 狙う役職の集合);
        
        // 視点agent さんが OOさんに投票する確率は，
        // OOさんが 狙う役職であるような世界の確率の和であります。
        
        // 各人の狙う役職っぽい度合いを求める
        // 求：Agent -> BigDecimal（Agentが狙う役職のいずれかである確率）
        Map<Agent, BigDecimal> 度合い配列 = new HashMap<Agent, BigDecimal>();
        // この配列（のちに投票先の混合戦略にもなるかも）には，他の人だけを格納します。
        for (Agent 他の人 : aliveOthers) {
            度合い配列.put(他の人, BigDecimal.ZERO);
        }
        
        for (Role role : 狙う役職の集合) {
            for (PossibleWorld pw : possibleWorlds) {
                if (myBelief.probMap.get(pw).compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }
                for (Agent 他の人 : 度合い配列.keySet()) {
                    if (他の人 == me) {
                        System.out.println("ここに来るまでに，度合い配列は自分以外のエージェントだけになっているはずなのに…。おかしいです！");
                        System.out.println("度合い配列：" + 度合い配列);
                        (new Throwable()).printStackTrace();
                        System.exit(0);
                        continue;
                    } // エラー
                    if (pw.getRole(他の人) == role) {
                        BigDecimal もとの値 = 度合い配列.get(他の人);
                        度合い配列.put(他の人, もとの値.add(myBelief.probMap.get(pw)));
                    }
                }
            }
        }
        
        // 度合い配列の確率の和を100にそろえる。
        BigDecimal sum = BigDecimal.ZERO;
        
        for (Agent 他の人 : 度合い配列.keySet()) {
            sum = sum.add(度合い配列.get(他の人));
        }
        if (sum.compareTo(BigDecimal.ZERO) == 0) {
            // sum == 0 のとき（正しく実装できてれば，あり得ない）
            
            // if (debug) {
            
            // TODO：ここどうにかする
            //  たとえば，占い師を狙うけど，すでに排除された人たちの占い師度の和が0であったとき。
            //  -> そういうときは，生存者のなかで村人陣営度（？）が一番高い人ですかね。
            log("【おかしいですね…】度合い配列の確率の和が0になってる：" + 度合い配列);
            
            // }
            
            // では，こういうときは，自分以外のみんなで等分することにしましょう。
            for (Agent 他の人 : 度合い配列.keySet()) {
                //度合い配列.replace(他の人, BigDecimal.valueOf(100.0 / 度合い配列.keySet().size()));
                度合い配列.replace(他の人,
                        BigDecimal.valueOf(100)
                                .divide(BigDecimal.valueOf(度合い配列.keySet().size()), mc));
            }
            // TODO：いや，違う。人が減ったときにも対応しなければならない。
            //  -> 一応，できたかな？
            
            // TODO：等分ではなくて，村人陣営度を使うことが望ましい。
            
        } else if (sum.compareTo(BigDecimal.valueOf(100)) == 0) {
            // sum == 100 のとき
            // なにもしなくてよい
        } else {
            // sum が 100 でない数になっているとき
            log("度合い配列の和が " + sum + " になってるので，調整します：");
            
            sum = sum.divide(BigDecimal.valueOf(100), mc);
            if (sum.compareTo(BigDecimal.ZERO) == 0) {
                log("sumを100にするために100で割ったら0になった，つまり，100にとても近かった。ということで，そのままにします。");
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
                    log("度合い配列の和が " + 信念の和 + " になりました。");
                }
            }
        }
        
        // 投票先の混合戦略.put(視点agent, new HashMap<Agent, BigDecimal>(度合い配列));
        // ここまでで，度合い配列が，投票先の戦略になりました。
        log("【主人公の投票先決定】" + 度合い配列);
        
        // この確率分布から，サンプリング（？）する必要がある。
        
        return 投票先を混合戦略から決定(度合い配列);
    }
    
    // ========================================================================
    
    @Override
    public String whisper() {
        return null;
    }
    
    @Override
    public Agent attack() {
        return null;
    }
    
    @Override
    public Agent divine() {
        return null;
    }
    
    @Override
    public Agent guard() {
        return null;
    }
    
    @Override
    public void finish() {
    }
    
    @Override
    public String getName() {
        return "SashimiBasePlayer_5";
    }
    
    // ========================================================================
    
    /** 可能世界を配列 possibleWorlds に格納 */
    void 可能世界を作成() {
        possibleWorlds = new PossibleWorld[POSSIBLE_WORLD.length];
        List<Agent> agents = currentGameInfo.getAgentList();
        for (int i = 0; i < POSSIBLE_WORLD.length; i++) {
            PossibleWorld pw = new PossibleWorld(this, N, agents, POSSIBLE_WORLD[i], i);
            // PossibleWorld pw = new PossibleWorld(this, N, agents, POSSIBLE_WORLD[i], i, BigDecimal.ZERO); // 可能世界ごとに主人公の現在の利得を保持するのであれば，こちらを使用する。しかし，現時点でそうする必要性は感じられないため，従来の方法で続けます。
            possibleWorlds[i] = pw;
        }
		/* 5人以外のときのヒント：
		// Roleのリスト
		currentGameInfo.getExistingRoles();
		// 各Roleの人数は
		gameSetting.getRoleNum( role );
		// あるいは
		gameSetting.getRoleNumMap(); // において調べる
		*/
        // <- 5人以外のときのヒント
        
        // 主人公の信念を初期化
        myBelief = new Belief(possibleWorlds, me, myRole);
        
        // 各可能世界において，各人の役職で各人の信念を初期化
        for (PossibleWorld pw : possibleWorlds) {
            pw.set可能世界の集合(possibleWorlds);
            pw.各人の役職により各人の信念を初期化();
        }
    }
    
    // ========================================================================
    
    /** 投票先の混合戦略から，確率的に選びます。 */
    Agent 投票先を混合戦略から決定(Map<Agent, BigDecimal> 混合戦略) {
		/*
		BigDecimal sum = BigDecimal.ZERO;
		for (Agent 他の人 : 度合い配列.keySet()) {
		    sum = sum.add(度合い配列.get(他の人));
		} */ // 時間かかるから，sumじゃなくて100.0でいっか。
        BigDecimal ran = BigDecimal.valueOf(Math.random()).multiply(BigDecimal.valueOf(100));
        BigDecimal sum = BigDecimal.ZERO;
        for (Agent agent : aliveOthers) {
            sum = sum.add(混合戦略.get(agent));
            if (ran.compareTo(sum) < 0) { // ran < sum になったら，その人にする
                return agent;
            }
        }
        log("ここに来るのは好ましくないような気もしますが…");
        
        return aliveOthers.get(aliveOthers.size() - 1);
    }
    
    // ========================================================================
    
    /** 可能世界の配列（もともとのすがた（？）） */
    public static Role[][] POSSIBLE_WORLD = {
            {Role.WEREWOLF, Role.POSSESSED, Role.SEER, Role.VILLAGER, Role.VILLAGER},
            {Role.WEREWOLF, Role.POSSESSED, Role.VILLAGER, Role.SEER, Role.VILLAGER},
            {Role.WEREWOLF, Role.POSSESSED, Role.VILLAGER, Role.VILLAGER, Role.SEER},
            {Role.WEREWOLF, Role.SEER, Role.POSSESSED, Role.VILLAGER, Role.VILLAGER},
            {Role.WEREWOLF, Role.VILLAGER, Role.POSSESSED, Role.SEER, Role.VILLAGER},
            {Role.WEREWOLF, Role.VILLAGER, Role.POSSESSED, Role.VILLAGER, Role.SEER},
            {Role.WEREWOLF, Role.SEER, Role.VILLAGER, Role.POSSESSED, Role.VILLAGER},
            {Role.WEREWOLF, Role.VILLAGER, Role.SEER, Role.POSSESSED, Role.VILLAGER},
            {Role.WEREWOLF, Role.VILLAGER, Role.VILLAGER, Role.POSSESSED, Role.SEER},
            {Role.WEREWOLF, Role.SEER, Role.VILLAGER, Role.VILLAGER, Role.POSSESSED},
            {Role.WEREWOLF, Role.VILLAGER, Role.SEER, Role.VILLAGER, Role.POSSESSED},
            {Role.WEREWOLF, Role.VILLAGER, Role.VILLAGER, Role.SEER, Role.POSSESSED},
            {Role.POSSESSED, Role.WEREWOLF, Role.SEER, Role.VILLAGER, Role.VILLAGER},
            {Role.POSSESSED, Role.WEREWOLF, Role.VILLAGER, Role.SEER, Role.VILLAGER},
            {Role.POSSESSED, Role.WEREWOLF, Role.VILLAGER, Role.VILLAGER, Role.SEER},
            {Role.SEER, Role.WEREWOLF, Role.POSSESSED, Role.VILLAGER, Role.VILLAGER},
            {Role.VILLAGER, Role.WEREWOLF, Role.POSSESSED, Role.SEER, Role.VILLAGER},
            {Role.VILLAGER, Role.WEREWOLF, Role.POSSESSED, Role.VILLAGER, Role.SEER},
            {Role.SEER, Role.WEREWOLF, Role.VILLAGER, Role.POSSESSED, Role.VILLAGER},
            {Role.VILLAGER, Role.WEREWOLF, Role.SEER, Role.POSSESSED, Role.VILLAGER},
            {Role.VILLAGER, Role.WEREWOLF, Role.VILLAGER, Role.POSSESSED, Role.SEER},
            {Role.SEER, Role.WEREWOLF, Role.VILLAGER, Role.VILLAGER, Role.POSSESSED},
            {Role.VILLAGER, Role.WEREWOLF, Role.SEER, Role.VILLAGER, Role.POSSESSED},
            {Role.VILLAGER, Role.WEREWOLF, Role.VILLAGER, Role.SEER, Role.POSSESSED},
            {Role.POSSESSED, Role.SEER, Role.WEREWOLF, Role.VILLAGER, Role.VILLAGER},
            {Role.POSSESSED, Role.VILLAGER, Role.WEREWOLF, Role.SEER, Role.VILLAGER},
            {Role.POSSESSED, Role.VILLAGER, Role.WEREWOLF, Role.VILLAGER, Role.SEER},
            {Role.SEER, Role.POSSESSED, Role.WEREWOLF, Role.VILLAGER, Role.VILLAGER},
            {Role.VILLAGER, Role.POSSESSED, Role.WEREWOLF, Role.SEER, Role.VILLAGER},
            {Role.VILLAGER, Role.POSSESSED, Role.WEREWOLF, Role.VILLAGER, Role.SEER},
            {Role.SEER, Role.VILLAGER, Role.WEREWOLF, Role.POSSESSED, Role.VILLAGER},
            {Role.VILLAGER, Role.SEER, Role.WEREWOLF, Role.POSSESSED, Role.VILLAGER},
            {Role.VILLAGER, Role.VILLAGER, Role.WEREWOLF, Role.POSSESSED, Role.SEER},
            {Role.SEER, Role.VILLAGER, Role.WEREWOLF, Role.VILLAGER, Role.POSSESSED},
            {Role.VILLAGER, Role.SEER, Role.WEREWOLF, Role.VILLAGER, Role.POSSESSED},
            {Role.VILLAGER, Role.VILLAGER, Role.WEREWOLF, Role.SEER, Role.POSSESSED},
            {Role.POSSESSED, Role.SEER, Role.VILLAGER, Role.WEREWOLF, Role.VILLAGER},
            {Role.POSSESSED, Role.VILLAGER, Role.SEER, Role.WEREWOLF, Role.VILLAGER},
            {Role.POSSESSED, Role.VILLAGER, Role.VILLAGER, Role.WEREWOLF, Role.SEER},
            {Role.SEER, Role.POSSESSED, Role.VILLAGER, Role.WEREWOLF, Role.VILLAGER},
            {Role.VILLAGER, Role.POSSESSED, Role.SEER, Role.WEREWOLF, Role.VILLAGER},
            {Role.VILLAGER, Role.POSSESSED, Role.VILLAGER, Role.WEREWOLF, Role.SEER},
            {Role.SEER, Role.VILLAGER, Role.POSSESSED, Role.WEREWOLF, Role.VILLAGER},
            {Role.VILLAGER, Role.SEER, Role.POSSESSED, Role.WEREWOLF, Role.VILLAGER},
            {Role.VILLAGER, Role.VILLAGER, Role.POSSESSED, Role.WEREWOLF, Role.SEER},
            {Role.SEER, Role.VILLAGER, Role.VILLAGER, Role.WEREWOLF, Role.POSSESSED},
            {Role.VILLAGER, Role.SEER, Role.VILLAGER, Role.WEREWOLF, Role.POSSESSED},
            {Role.VILLAGER, Role.VILLAGER, Role.SEER, Role.WEREWOLF, Role.POSSESSED},
            {Role.POSSESSED, Role.SEER, Role.VILLAGER, Role.VILLAGER, Role.WEREWOLF},
            {Role.POSSESSED, Role.VILLAGER, Role.SEER, Role.VILLAGER, Role.WEREWOLF},
            {Role.POSSESSED, Role.VILLAGER, Role.VILLAGER, Role.SEER, Role.WEREWOLF},
            {Role.SEER, Role.POSSESSED, Role.VILLAGER, Role.VILLAGER, Role.WEREWOLF},
            {Role.VILLAGER, Role.POSSESSED, Role.SEER, Role.VILLAGER, Role.WEREWOLF},
            {Role.VILLAGER, Role.POSSESSED, Role.VILLAGER, Role.SEER, Role.WEREWOLF},
            {Role.SEER, Role.VILLAGER, Role.POSSESSED, Role.VILLAGER, Role.WEREWOLF},
            {Role.VILLAGER, Role.SEER, Role.POSSESSED, Role.VILLAGER, Role.WEREWOLF},
            {Role.VILLAGER, Role.VILLAGER, Role.POSSESSED, Role.SEER, Role.WEREWOLF},
            {Role.SEER, Role.VILLAGER, Role.VILLAGER, Role.POSSESSED, Role.WEREWOLF},
            {Role.VILLAGER, Role.SEER, Role.VILLAGER, Role.POSSESSED, Role.WEREWOLF},
            {Role.VILLAGER, Role.VILLAGER, Role.SEER, Role.POSSESSED, Role.WEREWOLF}};
    
}
