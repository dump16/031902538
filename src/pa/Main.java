package pa;

import com.github.stuxuhai.jpinyin.ChineseHelper;
import com.github.stuxuhai.jpinyin.PinyinException;
import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings({"unchecked"})
public class Main {

    public static void main(String[] args) throws IOException {

        //  敏感词词汇文件
        File words = new File(args[0]);
        FileInputStream wordsFip = new FileInputStream(words);
        InputStreamReader wordsReader = new InputStreamReader(wordsFip, "UTF-8");
        BufferedReader wordsBuff = new BufferedReader(wordsReader);

        //  读取数据,放入HashSet中
        String temp;
        HashSet<String> keywordSet = new HashSet<>();
        while ((temp = wordsBuff.readLine()) != null) {

            //  原始敏感词添加
            keywordSet.add(temp);

            //  拼音替代、拼音首字母中文敏感词替代添加
            if(temp.matches("[\u4e00-\u9fa5]+")){
                getPyWord(0, temp, keywordSet);
            }
        }
        //System.out.println(keywordSet);   //  输出敏感词HashSet(测试用)

        //  释放资源
        wordsReader.close();
        wordsFip.close();

        //  初始化敏感词库
        HashMap keywordMap = new HashMap(keywordSet.size());
        String key;
        Map currMap;
        Map<String, String> newWordMap;

        //  循环敏感词集合HashSet
        for (String s : keywordSet) {
            key = s;
            currMap = keywordMap;
            for (int i = 0; i < key.length(); i++) {
                char keyChar = key.charAt(i);

                //  判断该字是否存在于敏感词库中
                Object wordMap = currMap.get(keyChar);
                if (wordMap != null) {
                    currMap = (Map) wordMap;
                }else {
                    newWordMap = new HashMap<>();
                    newWordMap.put("isEnd", "0");
                    currMap.put(keyChar, newWordMap);
                    currMap = newWordMap;
                }

                //  标志结尾字
                if (i == key.length() - 1) {
                    currMap.put("isEnd", "1");
                }
            }
        }
        //System.out.println(keywordMap);   //  输出敏感词HashMap（测试用）

        //  待检测文件
        File org = new File(args[1]);
        FileInputStream orgFip = new FileInputStream(org);
        InputStreamReader orgReader = new InputStreamReader(orgFip, "UTF-8");
        BufferedReader orgBuff = new BufferedReader(orgReader);

        //  读取数据,逐行检测
        String txt;
        int line=0; //  从第一行开始
        int total = 0;
        ArrayList<String> ansArr =new ArrayList<>();
        while ((txt = orgBuff.readLine()) != null) {
            line++;
            total += CheckKeyword(line, txt, keywordMap, ansArr);
        }

        //  释放资源
        orgReader.close();
        orgFip.close();

        //  答案文件
        File ans = new File(args[2]);
        FileOutputStream ansFop = new FileOutputStream(ans);
        OutputStreamWriter ansWriter = new OutputStreamWriter(ansFop, "UTF-8");

        //  写入数据
        ansWriter.append("Total: " + total + "\r\n");
        for(int i = 0; i < ansArr.size() - 1; i++){
            ansWriter.append(ansArr.get(i) + "\r\n");
        }
        ansWriter.append(ansArr.get(ansArr.size() - 1));

        //  直接输出（测试用）
        System.out.println("Total: " + total + "\r\n");
        for(int i = 0; i < ansArr.size() - 1; i++){
            System.out.println(ansArr.get(i) + "\r\n");
        }
        System.out.println(ansArr.get(ansArr.size() - 1));

        //  释放资源
        ansWriter.close();
        ansFop.close();

    }

    //  中文敏感词拼音替代、拼音首字母替代
    public static void getPyWord(int index, String str, HashSet keywordSet){
        String temp = str;
        String pinyin = null;
        if(index == str.length() - 1){
            keywordSet.add(str);
        }else{
            getPyWord(index+1, str, keywordSet);
        }

        try {
            temp = str.replace(str.charAt(index), PinyinHelper.getShortPinyin(str).charAt(index));
        } catch (PinyinException e) {
            e.printStackTrace();
        }
        if(index == str.length() - 1){
            keywordSet.add(temp);
        }else{
            getPyWord(index+1, temp, keywordSet);
        }

        try {
            pinyin = PinyinHelper.convertToPinyinString(str.charAt(index)+" ","", PinyinFormat.WITHOUT_TONE);
        } catch (PinyinException e) {
            e.printStackTrace();
        }
        temp = str.replaceAll(str.charAt(index) + "", Objects.requireNonNull(pinyin).substring(0,pinyin.length()-1));
        if(index == str.length() - 1){
            keywordSet.add(temp);
        }else{
            getPyWord(index+1, temp, keywordSet);
        }
    }

    public static int CheckKeyword(int line, String txt, HashMap keywordMap, ArrayList<String> ansArr){
        int matchNum = 0;           //  敏感词数量
        String wordTxt = "";        //  检测到的敏感词
        int beginIndex = 100000;    //  文本首部
        int endIndex = 0;           //  文本尾部
        char word;
        Map currMap = keywordMap;
        Map prevMap;
        for(int i = 0; i < txt.length(); i++){
            word = txt.charAt(i);
            if(word >= 'A' && word <= 'Z'){
                //  大小写字母替代
                word -= 'A'-'a';
            }else if(ChineseHelper.isTraditionalChinese(word)){
                //  繁体字替代
                word = ChineseHelper.convertToSimplifiedChinese(word);
            }
            //  判断该字是否存在于敏感词库中
            prevMap = currMap;
            currMap = (Map) currMap.get(word);
            if(currMap != null){     //  存在
                if(beginIndex > i){
                    beginIndex = i;
                }
                endIndex++;
                wordTxt += word;
                //  判断该字是否为结尾字
                if("1".equals(currMap.get("isEnd"))){
                    matchNum++;
                    if(i != txt.length() - 1 && (Map) currMap.get(txt.charAt(i+1))!=null){    //   最大规则
                        continue;
                    }
                    ansArr.add("Line" + line + ": <" + wordTxt + ">"+txt.substring(beginIndex, beginIndex + endIndex));
                    beginIndex = 100000;
                    endIndex = 0;
                    wordTxt = "";
                    currMap = keywordMap;
                }
            }else{                 //  不存在
                if(endIndex > 0 && (word+"").matches("[^(a-zA-Z\u4e00-\u9fa5)]")){
                    //  过滤伪装字
                    endIndex++;
                    currMap = prevMap;
                }else if(ChineseHelper.isChinese(word)){
                    //  谐音字替代 (繁体替代无法判断的字)
                    String pinyin = null;
                    try {
                        pinyin = PinyinHelper.convertToPinyinString(word+"","", PinyinFormat.WITHOUT_TONE);
                    } catch (PinyinException e) {
                        e.printStackTrace();
                    }
                    char pyWord;
                    currMap = prevMap;
                    for(int j = 0; j < Objects.requireNonNull(pinyin).length(); j++){
                        pyWord = pinyin.charAt(j);
                        //  判断该字是否存在于敏感词库中
                        currMap = (Map) currMap.get(pyWord);
                        if(currMap != null && j == pinyin.length() - 1 && "1".equals(currMap.get("isEnd"))){
                            endIndex++;
                            matchNum++;
                            wordTxt += word;
                            ansArr.add("Line" + line + ": <" + wordTxt + ">"+txt.substring(beginIndex, beginIndex + endIndex));
                            beginIndex = 100000;
                            endIndex = 0;
                            wordTxt = "";
                            currMap = keywordMap;
                        }else if(currMap != null && j == pinyin.length() - 1){
                            if(beginIndex > i){
                                beginIndex = i;
                            }
                            endIndex++;
                            wordTxt += word;
                        }else if(currMap == null){     //  不存在
                            beginIndex = 100000;
                            endIndex = 0;
                            wordTxt = "";
                            currMap = keywordMap;
                            break;
                        }
                    }
                }else{
                    beginIndex = 100000;
                    endIndex = 0;
                    wordTxt = "";
                    currMap = keywordMap;
                }
            }
        }
        return matchNum;
    }
}