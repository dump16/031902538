package pa;

import com.github.stuxuhai.jpinyin.PinyinException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings({"unchecked"})
public class Main {

    public static void main(String[] args) throws IOException, PinyinException {

        String wordsPath = "C:\\tests\\words.txt" ;    //args[0];
        String orgPath = "C:\\tests\\org.txt" ;      //args[1];
        String ansPath = "C:\\tests\\ans.txt" ;    //args[2];

        Tool tool = new Tool();
        WordMap wordMap = new WordMap(wordsPath);
        HashMap keywordMap = wordMap.initWordMap();

        //  待检测文件
        File org = new File(orgPath);
        FileInputStream orgFip = new FileInputStream(org);
        InputStreamReader orgReader = new InputStreamReader(orgFip, StandardCharsets.UTF_8);
        BufferedReader orgBuff = new BufferedReader(orgReader);

        //  读取数据,逐行检测
        String txt;
        int line=0; //  从第一行开始
        int total = 0;
        ArrayList<String> ansArr =new ArrayList<>();
        while ((txt = orgBuff.readLine()) != null) {
            line++;
            total += tool.checkKeyword(line, txt, keywordMap, ansArr);
        }

        //  释放资源
        orgReader.close();
        orgFip.close();

        //  答案文件
        File ans = new File(ansPath);
        FileOutputStream ansFop = new FileOutputStream(ans);
        OutputStreamWriter ansWriter = new OutputStreamWriter(ansFop, StandardCharsets.UTF_8);

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
}