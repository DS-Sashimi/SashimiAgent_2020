package jp.ac.tsukuba.s.s2020602;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import java.math.BigDecimal;
import java.util.*;

public class SashimiBasePlayer_15 extends SashimiBasePlayer {
    
    /** 自分以外のエージェント全員のリスト */
    List<Agent> allOtherAgents;
    
    /** 主人公の信念テーブル（各人の各役職の度合いを記録する表。カウンタの集まり。） */
    Table15 myTable;
    
    /** 投票先候補 */
    Agent voteCandidate;
    
    /** 各役職のグローバル（？）カウンタ。（だれの視点も持たず，（信念の更新にかかわる）出来事をすべて記録しておく） */
    Map<Role, Map<Agent, Integer>> グローバルカウンタ群;
    
    /** 自分の（実際に本当に）占い済みのエージェントと判定のマップ（霊媒師のときにも これを用いる） */
    List<Judge> myResultList = new ArrayList<>();
    
    /** 嘘をつくことを許可するかどうか。trueであれば，嘘をつくことが選択肢に含まれる。falseであれば，嘘をつかない。 */
    boolean isうそつき = true;
    
    /** 自分がCOした役職 */
    Role myRoleCO;
    
    /** 自分が報告した占いまたは霊媒の結果の一覧（BodyguardをCOした場合は，報告した護衛先を ここに保存することにする。） */
    Map<Agent, Judge> 報告済み結果リスト = new HashMap<Agent, Judge>();
    
    /** その日に可能な発言の種類（Topic）の集合 */
    List<Topic> possibleTopics = new ArrayList<Topic>();
    
    /** その日に可能な発言（Content）の（Topicごとの）集合 */
    Map<Topic, Set<Content>> 可能な発言の集合 = new HashMap<>();
    
    // ========================================================================
    // ========================================================================
    // ========================================================================
    
    @Override
    public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
        super.initialize(gameInfo, gameSetting);
        
        // isCameout = false;
        報告済み結果リスト.clear();
        // myResultMap.clear();
        myResultList.clear();
        possibleTopics.clear();
        可能な発言の集合.clear();
        // guardedAgentMap.clear();
        
        allOtherAgents = new ArrayList<>(currentGameInfo.getAliveAgentList());
        allOtherAgents.remove(me);
        
        // whiteList.clear(); // 15人村では，ホワイトリスト＝襲撃者リストですかね。
        
        // talkChoicesについての処理 -> dayStart() において，1日ごとにやります。
        // talksMapについての処理（初期化）-> 15人村では，カウンタ群。initialize() 内で初期化します。
        // 可能世界についての処理 -> なし。期待利得を考えるときだけ，局所的に（？）考える。
        
        // 主人公のテーブルの初期化
        myTable = new Table15(me, myRole, allOtherAgents);
        
        // みんなのテーブルの初期化
        
        グローバルカウンタ群 = new HashMap<>();
        
        // 主人公も含めた15人全員のエージェントのリスト
        List<Agent> allAgentList = currentGameInfo.getAgentList();
        
        Map<Agent, Integer> 人狼カウンタ = new HashMap<Agent, Integer>();
        Utility.カウンタを初期化(人狼カウンタ, allAgentList);
        グローバルカウンタ群.put(Role.WEREWOLF, 人狼カウンタ);
        
        Map<Agent, Integer> 占い師カウンタ = new HashMap<Agent, Integer>();
        Utility.カウンタを初期化(占い師カウンタ, allAgentList);
        グローバルカウンタ群.put(Role.SEER, 占い師カウンタ);
        
        Map<Agent, Integer> 狩人カウンタ = new HashMap<Agent, Integer>();
        Utility.カウンタを初期化(狩人カウンタ, allAgentList);
        グローバルカウンタ群.put(Role.BODYGUARD, 狩人カウンタ);
        
        Map<Agent, Integer> 霊媒師カウンタ = new HashMap<Agent, Integer>();
        Utility.カウンタを初期化(霊媒師カウンタ, allAgentList);
        グローバルカウンタ群.put(Role.MEDIUM, 霊媒師カウンタ);
        
        Map<Agent, Integer> 狂人カウンタ = new HashMap<Agent, Integer>();
        Utility.カウンタを初期化(狂人カウンタ, allAgentList);
        グローバルカウンタ群.put(Role.POSSESSED, 狂人カウンタ);
        
        // 「自分がCOした役職」の初期化
        myRoleCO = null;
    }
    
    @Override
    public void dayStart() {
        super.dayStart();
        
        // 新しい日の投票先の候補を 空にする。
        voteCandidate = null;
    }
    
    /** その日に可能な発言のトピックを選んで possibleTopics にセットします。 */
    void 発言のトピックを選ぶ() {
        possibleTopics = new ArrayList<>();
        
        // まず，「何も言わない」という選択肢
        possibleTopics.add(Topic.SKIP);
        
        // TODO：今は，日付や役職にかかわらず，次のようにしている。
        //  現段階では，日付や役職にかかわらず，同じトピックたちをセットしています。
        possibleTopics.add(Topic.COMINGOUT);
        possibleTopics.add(Topic.DIVINED);
        possibleTopics.add(Topic.IDENTIFIED);
        possibleTopics.add(Topic.GUARDED);
        possibleTopics.add(Topic.ESTIMATE);
        possibleTopics.add(Topic.VOTE);
        
        if (day == 1) {
            // 0日目の夜は，人狼の襲撃がなく，誰も守らないので，GUARDED を発言しない。
            possibleTopics.remove(Topic.GUARDED);
            // 同じく，霊媒もないので，IDENTIFIED を発言しない。
            possibleTopics.remove(Topic.IDENTIFIED);
            // さらに，1日目の最初のターンでは，VOTE のみにする。
            if (talkTurn < 1) {
                possibleTopics.remove(Topic.ESTIMATE);
                possibleTopics.remove(Topic.COMINGOUT);
                possibleTopics.remove(Topic.DIVINED);
            }
        }
        
        // 嘘をつかないならば，実際と異なる役職についての発言は しない。
        if (!isうそつき) {
            if (myRole != Role.SEER) {
                possibleTopics.remove(Topic.DIVINED);
            }
            if (myRole != Role.MEDIUM) {
                possibleTopics.remove(Topic.IDENTIFIED);
            }
            if (myRole != Role.BODYGUARD) {
                possibleTopics.remove(Topic.GUARDED);
            }
        }
        
        if (!Objects.isNull(myRoleCO)) {
            // すでに何らかのCOをしている場合は，改めてCOすることはない
            possibleTopics.remove(Topic.COMINGOUT);
            if (myRoleCO != Role.SEER) {
                // Seer以外の役職をCOしている場合，DIVINED は なし。
                possibleTopics.remove(Topic.DIVINED);
            }
            if (myRoleCO != Role.MEDIUM) {
                possibleTopics.remove(Topic.IDENTIFIED);
            }
            if (myRoleCO != Role.BODYGUARD) {
                possibleTopics.remove(Topic.GUARDED);
            }
            
            // かつ，結果報告数が上限に達している場合は，divined／identified／guardedをやめる。
            switch (myRoleCO) {
                case SEER:
                    // divinedの上限：day（n日目には，累計n個の結果を報告できる）
                    if (報告済み結果リスト.size() >= day) {
                        possibleTopics.remove(Topic.DIVINED);
                    }
                    break;
                case MEDIUM:
                    // identifiedの上限：day-1（n日目には，累計n-1個の結果を報告できる）
                    if (報告済み結果リスト.size() >= day - 1) {
                        possibleTopics.remove(Topic.IDENTIFIED);
                    }
                    break;
                case BODYGUARD:
                    // guardedの上限：day-1（n日目には，累計n-1個の結果を報告できる）
                    if (報告済み結果リスト.size() >= day - 1) {
                        possibleTopics.remove(Topic.GUARDED);
                    }
                    break;
                default:
                    break;
            }
        }
    }
    
    /** possibleTopics にセットされている各トピックに関して，可能な発言の集合 をセットします。 */
    void 発言の集合をセット() {
        // 発言の集合をリセット
        可能な発言の集合 = new HashMap<>();
        
        // 各トピックについて
        for (Topic topic : possibleTopics) {
            Set<Content> 発言たち = new HashSet<>();
            switch (topic) {
                case ESTIMATE:
                    // 人狼の推測
                    for (Agent target : aliveOthers) {
                        発言たち.add(estimateContent(me, target, Role.WEREWOLF));
                    }
                    break;
                case COMINGOUT:
                    // 役職をCOする。嘘をつくかつかないか（isうそつき）で変わる。
                    if (isうそつき) {
                        // 嘘をつくなら，あらゆる役職
                        for (Role r : currentGameInfo.getExistingRoles()) {
                            発言たち.add(coContent(me, me, r));
                        }
                    } else {
                        // 嘘をつかないなら，自分のほんとうの役職だけ
                        発言たち.add(coContent(me, me, myRole));
                    }
                    break;
                case DIVINED:
                    // 占い結果。
                    if (!isうそつき) {
                        // うそをつかないならば，過去に得た結果のなかで，まだ報告してないものをセット。
                        for (Judge judge : myResultList) {
                            Agent target = judge.getTarget();
                            if (!報告済み結果リスト.containsKey(target)) {
                                発言たち.add(divinedContent(me, target, judge.getResult()));
                            }
                        }
                    } else {
                        // うそをつくこともあるならば，あらゆる可能な結果をセット
                        for (Agent target : aliveOthers) {
                            // target は，まだ報告してない人に限る
                            if (!報告済み結果リスト.containsKey(target)) {
                                if (!killedAgents.contains(target)) {
                                    // target が 襲撃されていない場合，人狼の可能性がある。
                                    // （襲撃された人は，人狼ではない）
                                    発言たち.add(divinedContent(me, target, Species.WEREWOLF));
                                }
                                発言たち.add(divinedContent(me, target, Species.HUMAN));
                            }
                        }
                        // NOTE：最新の被襲撃者も，候補に入れる。-> 入れました。
                        if (!currentGameInfo.getLastDeadAgentList().isEmpty()) {
                            // NOTE：1回の夜で殺されるエージェントは，（妖狐がいないから）たかだか1人と思われる。
                            Agent target = currentGameInfo.getLastDeadAgentList().get(0);
                            // target は，まだ報告してない人に限る
                            if (!報告済み結果リスト.containsKey(target)) {
                                発言たち.add(divinedContent(me, target, Species.HUMAN));
                            }
                        }
                    }
                    break;
                case IDENTIFIED:
                    if (!isうそつき) {
                        // うそをつかないならば，過去に得た結果のなかで，まだ報告してないものをセット。
                        for (Judge judge : myResultList) {
                            Agent target = judge.getTarget();
                            if (!報告済み結果リスト.containsKey(target)) {
                                発言たち.add(identContent(me, judge.getTarget(), judge.getResult()));
                            }
                        }
                    } else {
                        // うそをつくこともあるならば，あらゆる可能な結果をセット
                        for (Agent target : executedAgents) {
                            // target は，まだ報告してない人に限る
                            if (!報告済み結果リスト.containsKey(target)) {
                                発言たち.add(identContent(me, target, Species.WEREWOLF));
                                発言たち.add(identContent(me, target, Species.HUMAN));
                            }
                        }
                    }
                    break;
                case GUARDED:
                    if (!isうそつき) {
                        // うそをつかないならば，昨日の夜に守った人だけセット
                        if (Objects.isNull(currentGameInfo.getGuardedAgent())) {
                            発言たち.add(guardedContent(me, currentGameInfo.getGuardedAgent()));
                        }
                    } else {
                        // うそつきのばあいは，生きてるエージェントみんなについて言える。
                        for (Agent target : aliveOthers) {
                            発言たち.add(guardedContent(me, target));
                        }
                    }
                    break;
                case VOTE:
                    // NOTE：うそつきと非うそつきの区別が難しいので，とりあえず全員について言えることにする。
                    for (Agent target : aliveOthers) {
                        発言たち.add(voteContent(me, target));
                    }
                    break;
                case SKIP:
                    発言たち.add(Content.SKIP);
                    break;
                default:
                    break;
            }
            // 「発言たち」を 可能な発言の集合 に加える
            可能な発言の集合.put(topic, 発言たち);
        }
    }
    
    @Override
    public void update(GameInfo gameInfo) {
        super.update(gameInfo);
    }
    
    // ========================================================================
    
    void addExecutedAgent(Agent executedAgent) {
        if (executedAgent != null) {
            aliveOthers.remove(executedAgent);
            if (!executedAgents.contains(executedAgent)) {
                executedAgents.add(executedAgent);
            }
        }
    }
    
    void addKilledAgent(Agent killedAgent) {
        if (killedAgent != null) {
            aliveOthers.remove(killedAgent);
            if (!killedAgents.contains(killedAgent)) {
                killedAgents.add(killedAgent);
                全員から確定白の人(killedAgent);
            }
        }
    }
    
    void 全員から確定白の人(Agent agt) {
        // 自分のテーブルを更新
        myTable.確定白の人(agt);
        
        // カウンタ群を更新
        グローバルカウンタ群.get(Role.WEREWOLF).put(agt, -500);
    }
    
    // ========================================================================
    
    /** その発言が(視点エージェント)の知っている事実に矛盾しないならば，true を返す。視点エージェントが me 以外でもよい。 */
    boolean isValid(Agent agt, Role role, boolean isAdd,
                    Agent 視点エージェント, Role 視点エージェントの役職) {
        if (agt == 視点エージェント) {
            // 「(視点エージェント)さんはroleではない」という発言：うそなので，false
            if (role != 視点エージェントの役職 && isAdd) {
                // 「(視点エージェント)さんはroleです」という発言：うそなので，false
                return false;
            } else return role != 視点エージェントの役職 || isAdd;
        }
        // TODO：占い結果，霊媒結果，仲間の人狼などについての情報も適用すること。
        // TODO：事実と異なることを言う人は，まったく無視するのではなくて，
        //  占い師度（など）を デクリメント するようにする，みたいなことも考えられる。
        return true;
    }
    
    void グローバル役職カウンタのみ更新(Agent agt, Role role, boolean isAdd) {
        int 増分;
        if (isAdd) {
            増分 = 1;
        } else {
            増分 = -1;
        }
        switch (role) {
            case BODYGUARD:
            case MEDIUM:
            case SEER:
            case POSSESSED:
            case WEREWOLF:
                int 新しいカウント = グローバルカウンタ群.get(role).get(agt) + 増分;
                グローバルカウンタ群.get(role).put(agt, 新しいカウント);
                break;
            case VILLAGER:
                int 新しいカウント2 = グローバルカウンタ群.get(Role.WEREWOLF).get(agt) - 増分;
                グローバルカウンタ群.get(Role.WEREWOLF).put(agt, 新しいカウント2);
                break;
            case FOX:
            case FREEMASON:
            case ANY:
                break;
        }
    }
    
    void 主人公のテーブルとグローバル役職カウンタを更新(Agent agt, Role role, boolean isAdd) {
        // まずは自分のテーブルを更新（有効な（＝主人公の知識に矛盾しない）発言に限る）
        if (agt != me && isValid(agt, role, isAdd, me, myRole)) {
            //（↑ agt == me のときはテーブル更新できないので，「agt != me」の条件を加えている。）
            myTable.テーブルを更新(agt, role, isAdd);
        }
        
        // グローバル役職カウンタを更新
        グローバル役職カウンタのみ更新(agt, role, isAdd);
    }
    
    /** 再帰的に文を解析する。主に，「主人公のテーブルとグローバル役職カウンタを更新()」をする。 */
    void parseSentence(Content content) {
        switch (content.getTopic()) {
            case COMINGOUT:
                comingoutMap.put(content.getTarget(), content.getRole());
                主人公のテーブルとグローバル役職カウンタを更新(content.getTarget(), content.getRole(), true);
                return;
            case DIVINED:
            case IDENTIFIED:
                // 占い・霊媒の結果。「人狼ではなかった」のときは，対象者の人狼度を1下げる。
                主人公のテーブルとグローバル役職カウンタを更新(content.getTarget(), Role.WEREWOLF, content.getResult() == Species.WEREWOLF);
                return;
            case GUARDED:
                // 守られた（と言われている）人の人狼度を1下げる。
                主人公のテーブルとグローバル役職カウンタを更新(content.getTarget(), Role.WEREWOLF, false);
                return;
            case ESTIMATE:
                主人公のテーブルとグローバル役職カウンタを更新(content.getTarget(), content.getRole(), true);
                return;
            case VOTE:
            case AGREE:
                // undefined
                return;
            case OPERATOR:
                parseOperator(content);
                return;
            default:
                break;
        }
    }
    
    // ========================================================================
    
    @Override
    public String talk() {
        talkTurn++;
        
        発言のトピックを選ぶ(); // このタイミングで発言できるトピックを列挙します。。
        発言の集合をセット(); // 各トピックについて，可能な発言の集合をセットします。
        choiceUtterance(); // 可能な発言の集合から，1つを選びます。
        
        return dequeueTalk();
    }
    
    /** 発言の内容を1つに決めて，talkQueue にセット。可能な発言の集合 から，期待利得が最も大きいものを選択する。 */
    void choiceUtterance() {
        // 世界を決定する。とりあえず1つにする。
        Map<Agent, Role> 世界 = new HashMap<>();
        世界を決める(世界);
        log("世界：" + 世界); // 世界が決まりました。
        
        // で，その世界において…
        
        // この世界における各人の現時点（主人公の発言の前）でのテーブルを求める。
        // 投票するのは生存しているエージェントだけだから，
        // 生存しているエージェントのテーブルだけを考える。
        Map<Agent, Table15> tableMap = new HashMap<>();
        for (Agent 視点agt : aliveOthers) {
            List<Agent> 視点エージェント以外のすべてのエージェント = new ArrayList<>(currentGameInfo.getAgentList());
            視点エージェント以外のすべてのエージェント.remove(視点agt);
            // 視点agtさんの役職は，上で決めた世界の通りであるとして，テーブルを作成
            Table15 table = new Table15(視点agt, 世界.get(視点agt), 視点エージェント以外のすべてのエージェント);
            
            // agtさんのテーブルに，グローバルカウンタ群から適宜コピーしていく。
            
            // 自分の役職について，テーブルを整理する（自分が占い師のとき，他の人の占い師度を-500にするなど）
            自分の役職についてのテーブル整理(視点agt, table, 世界);
            // ↑ でも，これは，自分が人狼のとき以外は，要らないかもしれない
            
            // カウンタを転記します！
            table.グローバルカウンタから転記する(グローバルカウンタ群);
            
            tableMap.put(視点agt, table);
        }
        
        // これで，各人のテーブルが出来上がりました。
        
        // 各発言の期待利得を求めて，最大の発言を選びます。
        
        Content 最適な発言 = Content.SKIP;
        BigDecimal 最大の期待利得 = BigDecimal.valueOf(-2.1);
        
        for (Map.Entry<Topic, Set<Content>> entry : 可能な発言の集合.entrySet()) {
            Set<Content> 発言集合 = entry.getValue();
            for (Content content : 発言集合) {
                BigDecimal contentの期待利得 = 発言の期待利得(content, 世界, tableMap);
                log("発言の期待利得：" + contentの期待利得 + "：" + content);
                if (contentの期待利得.compareTo(最大の期待利得) > 0) {
                    最適な発言 = content;
                    最大の期待利得 = contentの期待利得;
                } else if (最適な発言.getTopic() == Topic.SKIP && contentの期待利得.compareTo(最大の期待利得) == 0) {
                    // 期待利得が同じであっても，SKIPではない発言のほうを選ぶ。
                    最適な発言 = content;
                    最大の期待利得 = contentの期待利得;
                }
            }
        }
        
        // 1日目の最初のターンは VOTE 発言をする。
        if (day == 1 && talkTurn < 1 && 最適な発言.getTopic() == Topic.SKIP) {
            // 1日目の最初のターンでは，何かしら言うことにする：
            // 適当なエージェントを選び，VOTE 発言をする。
            Agent randomAgent = Agent.getAgent((int) (Math.random() * 15) + 1);
            最適な発言 = voteContent(me, randomAgent);
        }
        
        // 未COの役職の能力についての発言（divined，identified，guarded）が選ばれたら，
        // まずはCOする。その次の発言は，次のターンにもう一度 選択する。
        if (Objects.isNull(myRoleCO)) {
            switch (最適な発言.getTopic()) {
                case DIVINED:
                    最適な発言 = coContent(me, me, Role.SEER);
                    myRoleCO = Role.SEER;
                    break;
                case IDENTIFIED:
                    最適な発言 = coContent(me, me, Role.MEDIUM);
                    myRoleCO = Role.MEDIUM;
                    break;
                case GUARDED:
                    最適な発言 = coContent(me, me, Role.BODYGUARD);
                    myRoleCO = Role.BODYGUARD;
                    break;
                default:
                    // 何もしない
                    break;
            }
        }
        
        talkQueue.add(最適な発言);
        
        log("【選ばれた発言】" + 最適な発言);
        
        // TODO：
        //  COしてないのに，divined，identified，guardedが出た場合，まずはCOする。
        //  このとき，次の発言（現時点で最適な発言となった発言）をキューに入れてしまうかどうかですが…
        //  次のターンに，改めて もう一度，それが最適かどうか確かめるべきと思われる。
        //  COすることで発言の範囲が絞られる（はずである）ので，いいか…。
        
        // 発言に際する処理（グローバル役職カウンタに対してのみ parseSentence(Content content) のようなことをする）
        switch (最適な発言.getTopic()) {
            case ESTIMATE:
                // グローバル役職カウンタを更新
                グローバル役職カウンタのみ更新(最適な発言.getTarget(), 最適な発言.getRole(), true);
                break;
            case COMINGOUT:
                myRoleCO = 最適な発言.getRole();
                comingoutMap.put(me, myRoleCO);
                // グローバル役職カウンタを更新
                グローバル役職カウンタのみ更新(me, 最適な発言.getRole(), true);
                break;
            case DIVINED:
                int divinationDay = 報告済み結果リスト.size();
                Agent divinationTarget = 最適な発言.getTarget();
                Species divinationResult = 最適な発言.getResult();
                報告済み結果リスト.put(divinationTarget, new Judge(divinationDay, me, divinationTarget, divinationResult));
                // グローバル役職カウンタを更新
                グローバル役職カウンタのみ更新(divinationTarget, Role.WEREWOLF, divinationResult == Species.WEREWOLF);
                break;
            case IDENTIFIED:
                int identificationDay = 報告済み結果リスト.size() + 1;
                Agent identificationTarget = 最適な発言.getTarget();
                Species identificationResult = 最適な発言.getResult();
                報告済み結果リスト.put(identificationTarget,
                        new Judge(identificationDay, me, identificationTarget, identificationResult));
                // グローバル役職カウンタを更新
                グローバル役職カウンタのみ更新(identificationTarget, Role.WEREWOLF, identificationResult == Species.WEREWOLF);
                break;
            case GUARDED:
                Agent guardTarget = 最適な発言.getTarget();
                グローバル役職カウンタのみ更新(guardTarget, Role.WEREWOLF, false);
                // （護衛が事実かどうかにかかわらず）報告した護衛先を「報告済み結果リスト」にセットしておく。
                int guardDay = 報告済み結果リスト.size() + 1;
                報告済み結果リスト.put(guardTarget, new Judge(guardDay, me, guardTarget, Species.HUMAN)); // HUMAN は，仮の値。使用されない（はず）。
                break;
            case VOTE:
                // TODO：難しいので保留
                break;
            default:
                // undefined
                break;
        }
        
    }
    
    /** 期待利得メソッドで使います。 */
    void テーブルに反映(Table15 table, Agent agt, Role role, boolean isAdd) {
        // validかどうかを確認したのち反映
        if (isValid(agt, role, isAdd, table.getAgent(), table.getMyRole())) {
            table.テーブルを更新(agt, role, isAdd, 1);
        }
    }
    
    BigDecimal 発言の期待利得(Content content, Map<Agent, Role> 可能世界, Map<Agent, Table15> tableMap) {
        // 下の「世界」「テーブル群」を書き換えても，もとの「可能世界」「tableMap」は変更されない。
        Map<Agent, Role> 世界 = new HashMap<>(可能世界);
        Map<Agent, Table15> テーブル群 = new HashMap<>(tableMap); // 自分のテーブルを含まない
        
        // 次に，発言を足したときの期待利得を求める
        
        // content を足したときのテーブルの更新
        // TODO：これは，parseSentence からコピーしてきた（そして矢印記法に変更した，
        //  そして更新対象のテーブルを変更した）ものです。整理したほうがいいかも。
        switch (content.getTopic()) {
            case COMINGOUT:
                // 主人公のテーブルとグローバル役職カウンタを更新(content.getTarget(), content.getRole(), true);
                for (Map.Entry<Agent, Table15> entry : テーブル群.entrySet()) {
                    テーブルに反映(entry.getValue(), content.getTarget(), content.getRole(), true);
                }
                break;
            case DIVINED:
            case IDENTIFIED:// 占い・霊媒の結果
                if (content.getResult() == Species.WEREWOLF) {
                    // 主人公のテーブルとグローバル役職カウンタを更新(content.getTarget(), Role.WEREWOLF, true);
                    for (Map.Entry<Agent, Table15> entry : テーブル群.entrySet()) {
                        テーブルに反映(entry.getValue(), content.getTarget(), Role.WEREWOLF, true);
                    }
                } else {
                    // 主人公のテーブルとグローバル役職カウンタを更新(content.getTarget(), Role.WEREWOLF, false);
                    for (Map.Entry<Agent, Table15> entry : テーブル群.entrySet()) {
                        テーブルに反映(entry.getValue(), content.getTarget(), Role.WEREWOLF, false);
                    }
                }
                break;
            case GUARDED:
                // 主人公のテーブルとグローバル役職カウンタを更新(content.getTarget(), Role.WEREWOLF, false);
                for (Map.Entry<Agent, Table15> entry : テーブル群.entrySet()) {
                    テーブルに反映(entry.getValue(), content.getTarget(), Role.WEREWOLF, false);
                }
                break;
            case ESTIMATE:
                // 主人公のテーブルとグローバル役職カウンタを更新(content.getTarget(), content.getRole(), true);
                for (Map.Entry<Agent, Table15> entry : テーブル群.entrySet()) {
                    テーブルに反映(entry.getValue(), content.getTarget(), content.getRole(), true);
                }
                break;
            case VOTE:
                // TODO：これ，一概には言えないですね。とりあえず聞かないことにして，ESTIMATE だけ聞く。
                break;
            case AGREE:
                // TODO：これもできたらよさそう。
                break;
            case OPERATOR:
                // 現時点のプログラムでは，ここに来ることは なさそう。
                break;
            default:
                break;
        }
        
        // で，テーブル群の更新が終わりました。
        
        // テーブル群から，投票先を求めます。
        
        // ここでは，投票先の純粋戦略を用います。
        
        Map<Agent, BigDecimal> 得票数 = new HashMap<>();
        for (Map.Entry<Agent, Table15> entry : テーブル群.entrySet()) {
            Set<Agent> 投票先 = entry.getValue().投票先の純粋戦略(currentGameInfo.getAliveAgentList());
            int size = 投票先.size();
            for (Agent target : 投票先) {
                BigDecimal tmp = 得票数.getOrDefault(target, BigDecimal.ZERO);
                得票数.put(target, tmp.add(BigDecimal.ONE.divide(BigDecimal.valueOf(size), mc)));
            }
        }
        
        // ここまで，投票先の純粋戦略を用いました。
        
        // 自分も投票に参加します。
        Set<Agent> 自分の投票先 = myTable.投票先の純粋戦略(currentGameInfo.getAliveAgentList());
        int size = 自分の投票先.size();
        for (Agent target : 自分の投票先) {
            BigDecimal tmp = 得票数.getOrDefault(target, BigDecimal.ZERO);
            得票数.put(target, tmp.add(BigDecimal.ONE.divide(BigDecimal.valueOf(size), mc)));
        }
        
        // 最多得票者は…！
        // Agent 最多得票者 = null;
        Set<Agent> 最多得票者ら = new HashSet<>();
        BigDecimal 最多得票者の得票数 = BigDecimal.ZERO;
        for (Map.Entry<Agent, BigDecimal> entry : 得票数.entrySet()) {
            if (entry.getValue().compareTo(最多得票者の得票数) > 0) {
                // 最多得票者 = entry.getKey();
                最多得票者ら.clear();
                最多得票者ら.add(entry.getKey());
                最多得票者の得票数 = entry.getValue();
            } else if (entry.getValue().compareTo(最多得票者の得票数) == 0) {
                最多得票者ら.add(entry.getKey());
            }
        }
        
        // 追放される人（1人または複数人）が決まりました。
        
        // これを変更していく。（これが返り値になります。）
        BigDecimal 期待利得 = BigDecimal.ZERO;
        
        for (Agent 追放される人 : 最多得票者ら) {
            期待利得 = 期待利得.add(
                    Payoff.payoff_15(世界.get(追放される人), myRole.getTeam()).divide(BigDecimal.valueOf(最多得票者ら.size()), mc));
        }
        
        return 期待利得;
    }
    
    /** 最終的な更新（テーブルと最も近いメソッド） */
    void 他の人のテーブルを更新する() {
    
    }
    
    /** 他の人のテーブルを更新するメソッド */
    void 自分の役職についてのテーブル整理(Agent 視点エージェント,
                          Table15 table, Map<Agent, Role> 可能世界) {
        Role agtRole = 可能世界.get(視点エージェント);
        
        switch (agtRole) {
            case WEREWOLF:
                // 人狼なので，仲間の人狼を知ってる -> 人狼リストは使わない，ってことでいいのかな。
                // とりあえず，仲間の人狼を人狼度 500 にしておく。
                int werewolfCounter = 0;
                for (Agent agt : table.getAllOtherAgents()) {
                    if (可能世界.get(agt) == Role.WEREWOLF) {
                        table.テーブルを更新(agt, Role.WEREWOLF, true, 500);
                        werewolfCounter++;
                        if (werewolfCounter >= 2) {
                            break; // 人狼は3人いて，自分以外に2人だけなので，2人を追加したら終わり。
                        }
                    }
                }
                break;
            case VILLAGER:
                // 何も知らないので，何もしません。
                break;
            case POSSESSED:
                // 他の人の狂人度を-500にする（必要…？？）-> ないですね，たぶん。
                // 他の役職も一緒か？？？
                break;
            case BODYGUARD:
                break;
            case MEDIUM:
                break;
            case SEER:
                break;
            case FREEMASON:
            case FOX:
            case ANY:
                break;
            default:
                break;
        }
    }
    
    /**
     * テーブルをもとにして，与えられたMapに一番可能性の高い世界を格納する。 占い師，人狼(+狂人)，狩人，霊媒師の順で，最も
     * その役職度が高い人を選ぶ。 人狼・狂人については，(人狼度＋狂人度) の高い順から数えて，3人目までが人狼，4人目が狂人，とする。
     */
    void 世界を決める(Map<Agent, Role> 可能世界) {
        // NOTE：15人村の役職構成であることを前提としています。違う人数や配役だと，結果がおかしくなると思われます。
        
        log("世界を決める前のCO状況：" + comingoutMap);
        
        // 他の人たち
        ArrayDeque<Agent> 残りのエージェントたち = new ArrayDeque<>(allOtherAgents);
        
        // まずは，自分の役職（と，人狼の場合は仲間の人狼も）をセット
        // 可能世界.put(me, myRole);
        for (Map.Entry<Agent, Role> entry : currentGameInfo.getRoleMap().entrySet()) {
            可能世界.put(entry.getKey(), entry.getValue());
            log("世界にセット：既知：" + entry.getKey() + "さん＝" + entry.getValue());
            残りのエージェントたち.remove(entry.getKey());
        }
        
        // 占い師
        if (myRole != Role.SEER) {
            Agent 占い師 = myTable.役職度最大(new ArrayDeque<Agent>(残りのエージェントたち), Role.SEER);
            log("世界にセット：予想：" + 占い師 + "さん-占い師");
            可能世界.put(占い師, Role.SEER);
            残りのエージェントたち.remove(占い師);
        }
        
        // 人狼
        if (myRole != Role.WEREWOLF) {
            // 人狼1
            Agent 人狼A = myTable.人狼陣営度最大(new ArrayDeque<Agent>(残りのエージェントたち));
            log("世界にセット：予想：" + 人狼A + "さん-人狼");
            可能世界.put(人狼A, Role.WEREWOLF);
            残りのエージェントたち.remove(人狼A);
            
            // 人狼2
            Agent 人狼B = myTable.人狼陣営度最大(new ArrayDeque<Agent>(残りのエージェントたち));
            log("世界にセット：予想：" + 人狼B + "さん-人狼");
            可能世界.put(人狼B, Role.WEREWOLF);
            残りのエージェントたち.remove(人狼B);
            
            // 人狼3
            Agent 人狼C = myTable.人狼陣営度最大(new ArrayDeque<Agent>(残りのエージェントたち));
            log("世界にセット：予想：" + 人狼C + "さん-人狼");
            可能世界.put(人狼C, Role.WEREWOLF);
            残りのエージェントたち.remove(人狼C);
        }
        
        // 狂人
        if (myRole != Role.POSSESSED) {
            Agent 狂人 = myTable.人狼陣営度最大(new ArrayDeque<Agent>(残りのエージェントたち));
            log("世界にセット：予想：" + 狂人 + "さん-狂人");
            可能世界.put(狂人, Role.POSSESSED);
            残りのエージェントたち.remove(狂人);
        }
        
        // 狩人
        if (myRole != Role.BODYGUARD) {
            Agent 狩人 = myTable.役職度最大(new ArrayDeque<Agent>(残りのエージェントたち), Role.BODYGUARD);
            log("世界にセット：予想：" + 狩人 + "さん-狩人");
            可能世界.put(狩人, Role.BODYGUARD);
            残りのエージェントたち.remove(狩人);
        }
        
        // 霊媒師
        if (myRole != Role.MEDIUM) {
            Agent 霊媒師 = myTable.役職度最大(new ArrayDeque<Agent>(残りのエージェントたち), Role.MEDIUM);
            log("世界にセット：予想：" + 霊媒師 + "さん-霊媒師");
            可能世界.put(霊媒師, Role.MEDIUM);
            残りのエージェントたち.remove(霊媒師);
        }
        
        // 残りは全員 村人
        for (Agent agt : 残りのエージェントたち) {
            // log("世界にセット：余り：" + agt + "さん-村人");
            可能世界.put(agt, Role.VILLAGER);
        }
        
        // TODO：世界を決めた後，ゲームが進行中であることに矛盾しないかどうか確かめる必要がある。
        //  （人狼が全員排除されてしまっていないか，人狼が人間より多くなっていないか，など）
    }
    
    // ========================================================================
    
    @Override
    public final Agent vote() {
        chooseVoteCandidate(); // 投票先（voteCandidate）を決める
        return voteCandidate;
    }
    
    /** 投票先候補を1人選び，voteCandidate にセットする。 */
    void chooseVoteCandidate() {
        // デフォルト：村人陣営のものを書いておく
        voteCandidate = myTable.人狼陣営度最大(new ArrayDeque<>(aliveOthers));
    }
    
    // ========================================================================
    
    @Override
    public Agent attack() {
        return null; // サブクラスで実装
    }
    
    @Override
    public String whisper() {
        return null; // サブクラスで実装
    }
    
    @Override
    public Agent divine() {
        return null; // サブクラスで実装
    }
    
    @Override
    public Agent guard() {
        return null; // サブクラスで実装
    }
    
    @Override
    public String getName() {
        return "SashimiBasePlayer_15";
    }
    
}
