package pa.java;

import com.github.stuxuhai.jpinyin.ChineseHelper;
import com.github.stuxuhai.jpinyin.PinyinException;
import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings({"unchecked"})

public class Tool {

    public HashMap<String,String> dictMap = new HashMap<>();
    public HashSet set;

    //  中文敏感词拼音替代、拼音首字母替代
    public void getPyWord(int index, String str, HashSet keywordSet) throws PinyinException {
        if(index == str.length() - 1){
            keywordSet.add(str);
        }else{
            getPyWord(index+1, str, keywordSet);
        }

        String temp = str.replace(str.charAt(index), PinyinHelper.getShortPinyin(str).charAt(index));
        if(index == str.length() - 1){
            keywordSet.add(temp);
        }else{
            getPyWord(index+1, temp, keywordSet);
        }

        String pinyin = PinyinHelper.convertToPinyinString(str.charAt(index)+" ","", PinyinFormat.WITHOUT_TONE);
        temp = str.replaceAll(str.charAt(index) + "", Objects.requireNonNull(pinyin).substring(0,pinyin.length()-1));
        if(index == str.length() - 1){
            keywordSet.add(temp);
        }else{
            getPyWord(index+1, temp, keywordSet);
        }
    }

    public void initDictMap() throws IOException {
        InputStream path = Thread.currentThread().getContextClassLoader().getResourceAsStream("chaizi.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(path, StandardCharsets.UTF_8));
        String str;
        while ((str = reader.readLine()) != null) {
            String[] buff = str.split("\"");
            for(int i = 0; i < buff.length; i++){
                dictMap.put(buff[1],buff[3]);
            }
        }
        //System.out.println(dictMap);
    }

    public void getBsWord(int index, String str, HashSet keywordSet){

        if(index == str.length() - 1){
            keywordSet.add(str);
        }else{
            getBsWord(index+1, str, keywordSet);
        }

        if(dictMap.containsKey(str.charAt(index) + "")){
            String bushou = dictMap.get(str.charAt(index) + "");
            String temp = str.replaceAll(str.charAt(index) + "", Objects.requireNonNull(bushou).substring(0,bushou.length()));
            if(index == str.length() - 1){
                keywordSet.add(temp);
            }else{
                getBsWord(index+1, temp, keywordSet);
            }
        }
    }

    public static int checkKeyword(int line, String txt, HashMap keywordMap, ArrayList<String> ansArr)throws PinyinException{
        int matchNum = 0;           //  敏感词数量
        StringBuilder keyword = new StringBuilder();        //  匹配敏感词
        int beginIndex = 0;         //  文本首部位置
        int length = 0;             //  文本长度
        char word;
        Map currMap = keywordMap;
        Map prevMap;

        for(int i = 0; i < txt.length(); i++){
            word = txt.charAt(i);

            if(word >= 'A' && word <= 'Z'){
                //  大写字母转小写字母
                word -= 'A'-'a';
            }else if(ChineseHelper.isTraditionalChinese(word)){

                word = ChineseHelper.convertToSimplifiedChinese(word);
            }
            //  判断该字是否存在于敏感词库中
            prevMap = currMap;
            currMap = (Map) currMap.get(word);
            if(currMap != null){     //  存在
                if(length == 0){
                    beginIndex = i;
                }
                length++;
                keyword.append(word);
                //  判断该字是否为结尾字
                if("1".equals(currMap.get("isEnd"))){
                    if(i != txt.length() - 1 && currMap.get(txt.charAt(i+1)) != null){    //   最大规则
                        continue;
                    }
                    matchNum++;
                    ansArr.add("Line" + line + ": <" + keyword + ">"+txt.substring(beginIndex, beginIndex + length));
                    length = 0;
                    keyword = new StringBuilder();
                    currMap = keywordMap;
                }

            }else{                 //  不存在
                if(length > 0 && ((word + "").matches("[^(a-zA-Z\u4e00-\u9fa5)]") || word =='(' || word == ')')){
                    //  过滤伪装字
                    length++;
                    currMap = prevMap;
                }else if(ChineseHelper.isChinese(word)){
                    //  谐音字替代 (繁体替代无法判断的字)
                    String pinyin = PinyinHelper.convertToPinyinString(word+"","", PinyinFormat.WITHOUT_TONE);
                    char pyWord;
                    currMap = prevMap;
                    for(int j = 0; j < pinyin.length(); j++){
                        pyWord = pinyin.charAt(j);

                        //  判断该字是否存在于敏感词库中
                        currMap = (Map) currMap.get(pyWord);
                        if(currMap != null && j == pinyin.length() - 1){
                            if(length == 0){
                                beginIndex = i;
                            }
                            length++;
                            keyword.append(word);
                            //  这里的currMap还有问题
                            if("1".equals(currMap.get("isEnd"))){
                                matchNum++;
                                ansArr.add("Line" + line + ": <" + keyword + ">"+txt.substring(beginIndex, beginIndex + length));
                                length = 0;
                                keyword = new StringBuilder();
                                currMap = keywordMap;
                            }
                        }else if(currMap == null){     //  不存在
                            if(length != 0){
                                i--;
                            }
                            length = 0;
                            keyword = new StringBuilder();
                            currMap = keywordMap;
                            break;
                        }
                    }
                }else{
                    if(length != 0){
                        i--;
                    }
                    length = 0;
                    keyword = new StringBuilder();
                    currMap = keywordMap;
                }
            }
        }
        return matchNum;
    }

//    public String getWord(String str) throws PinyinException {
//        for(int i = 0; i < str.length(); i++){
//            if(ChineseHelper.isChinese(str.charAt(i))) {
//                String pinyin = PinyinHelper.convertToPinyinString(str.charAt(i) + " ", "", PinyinFormat.WITHOUT_TONE);
//                str = str.replaceAll(str.charAt(i) + "", Objects.requireNonNull(pinyin).substring(0, pinyin.length() - 1));
//            }
//        }
//        for(int i = 0; i < set.size(); i++){
//            String temp = set.toString();
//            if(ChineseHelper.isChinese(temp.charAt(i))) {
//                String pinyin = PinyinHelper.convertToPinyinString(str.charAt(i) + " ", "", PinyinFormat.WITHOUT_TONE);
//                temp = str.replaceAll(str.charAt(i) + "", Objects.requireNonNull(pinyin).substring(0, pinyin.length() - 1));
//            }
//        }
//        if()
//        return str;
//    }
}
