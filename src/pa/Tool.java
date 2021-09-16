package pa;

import com.github.stuxuhai.jpinyin.ChineseHelper;
import com.github.stuxuhai.jpinyin.PinyinException;
import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;

import java.util.*;

@SuppressWarnings({"unchecked"})

public class Tool {

    public HashMap<String,String> dictMap = new HashMap<String, String>();
    //  中文敏感词拼音替代、拼音首字母替代
    public void getPyWord(int index, String str, HashSet keywordSet) throws PinyinException {
        String temp = str;
        String pinyin = null;

        if(index == str.length() - 1){
            keywordSet.add(str);
        }else{
            getPyWord(index+1, str, keywordSet);
        }

        temp = str.replace(str.charAt(index), PinyinHelper.getShortPinyin(str).charAt(index));
        if(index == str.length() - 1){
            keywordSet.add(temp);
        }else{
            getPyWord(index+1, temp, keywordSet);
        }

        pinyin = PinyinHelper.convertToPinyinString(str.charAt(index)+" ","", PinyinFormat.WITHOUT_TONE);
        temp = str.replaceAll(str.charAt(index) + "", Objects.requireNonNull(pinyin).substring(0,pinyin.length()-1));
        if(index == str.length() - 1){
            keywordSet.add(temp);
        }else{
            getPyWord(index+1, temp, keywordSet);
        }
    }

//    public void initDictMap() throws IOException {
//        InputStream path = this.getClass().getResourceAsStream("chaizi.txt");
//        BufferedReader reader = new BufferedReader(new InputStreamReader(path));
//        String str = "";
//        while ((str = reader.readLine()) != null) {
//            dictMap.put(String.valueOf(str.split("\"")),String.valueOf(str.split("\"")));
//        }
//        System.out.println(dictMap);
//
//        //return breakList;
//    }
//
//    public void getBsWord(int index, String str, HashSet keywordSet){
//        String temp = str;
//        String bushou = null;
//
//        if(index == str.length() - 1){
//            keywordSet.add(str);
//        }else{
//            getBsWord(index+1, str, keywordSet);
//        }
//
//        if(dictMap.containsKey(str)){
//            bushou = dictMap.get(str);
//            temp = str.replaceAll(str.charAt(index) + "", Objects.requireNonNull(bushou).substring(0,bushou.length()-1));
//            if(index == str.length() - 1){
//                keywordSet.add(temp);
//            }else{
//                getBsWord(index+1, temp, keywordSet);
//            }
//        }
//    }

    public static int checkKeyword(int line, String txt, HashMap keywordMap, ArrayList<String> ansArr){
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
                    if(i != txt.length() - 1 && (Map) currMap.get(txt.charAt(i+1))!=null){    //   最大规则
                        continue;
                    }
                    matchNum++;
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
