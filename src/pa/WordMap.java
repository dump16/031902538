package pa;

import com.github.stuxuhai.jpinyin.PinyinException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@SuppressWarnings({"unchecked"})
public class WordMap {

    public Tool tool = new Tool();
    public String wordsPath;
    public HashSet<String> keywordSet = new HashSet<>();

    public WordMap(String path){
        wordsPath = path;
    }

    private void addToWordSet() throws IOException, PinyinException {
        //  敏感词词汇文件
        File words = new File(wordsPath);
        FileInputStream wordsFip = new FileInputStream(words);
        InputStreamReader wordsReader = new InputStreamReader(wordsFip, StandardCharsets.UTF_8);
        BufferedReader wordsBuff = new BufferedReader(wordsReader);

        //  读取数据,放入HashSet中
        String temp;
        //tool.initDictMap();
        while ((temp = wordsBuff.readLine()) != null) {

            //  原始敏感词添加
            keywordSet.add(temp);

            //  拼音替代、拼音首字母中文敏感词替代添加
            if(temp.matches("[\u4e00-\u9fa5]+")){
                tool.getPyWord(0, temp, keywordSet);
                //tool.getBsWord(0, temp, keywordSet);
            }
        }
        //System.out.println(keywordSet);   //  输出敏感词HashSet(测试用)

        //  释放资源
        wordsReader.close();
        wordsFip.close();
    }

    //  初始化敏感词库
    public HashMap initWordMap() throws IOException, PinyinException {

        addToWordSet();

        //  初始化敏感词库
        HashMap keywordMap = new HashMap(keywordSet.size());
        Map currMap;
        Map<String, String> newWordMap;

        //  循环敏感词集合HashSet
        for (String keyword : keywordSet) {
            currMap = keywordMap;
            for (int i = 0; i < keyword.length(); i++) {
                char keyChar = keyword.charAt(i);

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
                if (i == keyword.length() - 1) {
                    currMap.put("isEnd", "1");
                }
            }
        }
        //System.out.println(keywordMap);   //  输出敏感词HashMap（测试用）
        return keywordMap;
    }

}
