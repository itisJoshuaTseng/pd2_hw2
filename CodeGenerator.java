import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

class MermaidReader {
    public static String readMermaidCode(String fileName) {
        String mermaidCode = "";
        try {
            mermaidCode = Files.readString(Paths.get(fileName));
            mermaidCode = mermaidCode.replaceAll(" *\\( *", "(");
            mermaidCode = mermaidCode.replaceAll(" *\\)", ") ");
            mermaidCode = mermaidCode.replaceAll(" *,", ", ");
            mermaidCode = mermaidCode.replaceAll(":", " : ");
            mermaidCode = mermaidCode.replaceAll("  *\\[ *", "[");
            mermaidCode = mermaidCode.replaceAll("\\+", "+ ");
            mermaidCode = mermaidCode.replaceAll("\\-", "- ");
            mermaidCode = mermaidCode.replaceAll(" +", " ");
            return mermaidCode;
        } catch (IOException e) {
            System.err.println("無法讀取文件 " + fileName);
            e.printStackTrace();
            return "";
        }
    }
}

class Parser {
    private String[] lines;
    private Map<String, String> result = new HashMap<String, String>();
    public Parser(String mermaidCode) {
        this.lines = mermaidCode.split("\n");
    }
    public Map<String, String> toJava() {
        for(String line : lines){
            line = line.trim();
            String[] parts = line.split("\\s+");
            if(line.isEmpty() || parts.length < 2) {
                continue; // Skip empty lines
            }
            String className = "";
            String returnType = "";
            if(line.startsWith ("class ") && line.length() > 6 && Character.isUpperCase(line.charAt(6))){
                //String[] parts = line.split("\\s+");
                className = parts[1];
                String modifiedline = "public class " + className + " {";
                String extistingContent = result.getOrDefault(className,"");
                result.put(className, extistingContent + modifiedline + "\n");
            }
            else if(line.contains("()") && line.contains(":")){
                //String[] parts = line.split("\\s+");
                String visibility = "";
                if("+".equals(parts[2])) visibility = "public";
                if("-".equals(parts[2])) visibility = "private";
                if(parts[3].contains("get") && parts[3].length() > 3 && Character.isUpperCase(parts[3].charAt(3))){
                    String theReturnObject = parts[3].substring(3);
                    String withoutGet = theReturnObject.substring(0, 1).toLowerCase() + theReturnObject.substring(1);
                    String modifiedline = "    " + visibility + " " + parts[4] + " " + parts[3] + " " + "{" + "\n" + "        " + "return " + withoutGet + ";" + "\n"+ "    }" + "\n";
                    String extistingContent = result.getOrDefault(className,"");
                    result.put(className, extistingContent + modifiedline);
                }else if(parts[3].contains("set") && parts[3].length() > 3 && Character.isUpperCase(parts[3].charAt(3))){
                    String withoutSet = parts[4];
                    if(parts.length < 6){
                        returnType = "void";
                    }else{
                        returnType = parts[5];
                    }
                    String modifiedline = "    " + visibility + " " + returnType + " " + parts[3] + " " + parts[4] + " {" + "\n" + "        " + "this." + withoutSet + " = " + withoutSet + ";" + "\n"+ "    }" + "\n";
                    String extistingContent = result.getOrDefault(className,"");
                    result.put(className, extistingContent + modifiedline);
                }else{
                    int start = 0;
                    if(parts[2] == "+"){start = line.indexOf('+') + 2;}
                    if(parts[2] == "-"){start = line.indexOf('-') + 2;}
                    int end = line.indexOf(')') + 1;//沒有取到空格
                    String methodName = line.substring(start, end);

                    char lastCharacter = line.charAt(line.length() - 1);
                    int ifReturnTypeExist = line.indexOf(')');
                    //有沒有returntype還是直接留白表示void
                    if(lastCharacter == ifReturnTypeExist){
                        String modifiedline = "    " + visibility + " void " + methodName + " {;}" + "\n";
                        String extistingContent = result.getOrDefault(className,"");
                        result.put(className, extistingContent + modifiedline);
                    }
                    returnType = line.substring(line.indexOf(')') + 2);
                    if(returnType.contains("boolean")){
                        String modifiedline = "    " + visibility + " " + returnType + " " + methodName + " {return false;}" + "\n";
                        String extistingContent = result.getOrDefault(className,"");
                        result.put(className, extistingContent + modifiedline);
                    }else if(returnType.contains("int")){
                        String modifiedline = "    " + visibility + " " + returnType + " " + methodName + " {return 0;}" + "\n";
                        String extistingContent = result.getOrDefault(className,"");
                        result.put(className, extistingContent + modifiedline);
                    }else if(returnType.contains("void")){
                        String modifiedline = "    " + visibility + " " + returnType + " " + methodName + " {;}" + "\n";
                        String extistingContent = result.getOrDefault(className,"");
                        result.put(className, extistingContent + modifiedline);
                    }else if(returnType.contains("String")){
                        String modifiedline = "    " + visibility + " " + returnType + " " + methodName + " {return \"\";}" + "\n";
                        String extistingContent = result.getOrDefault(className,"");
                        result.put(className, extistingContent + modifiedline);
                    }
                }  
            }
            else {
                if(line.isEmpty() || parts.length < 2) {
                    continue; // Skip empty lines
                }
                //String[] parts = line.split("\\s+");
                String visibility = "";
                if("+".equals(parts[2])) visibility = "public";
                if("-".equals(parts[2])) visibility = "private";
                String modifiedline = "    " + visibility + " " + parts[3] + " " + parts[4] + ";" + "\n";
                String extistingContent = result.getOrDefault(className,"");
                result.put(className, extistingContent + modifiedline + "\n");
            } 
            // "Person : -int age"
            // "Family : +isAlone(int[] nums) boolean[]"
            // "Family : +voidAlone() void"
            // "Family : +voidAlone()"
        }
        System.out.println(result);
        return result;
    }
}
public class CodeGenerator {
    public static void main(String[] args) {
        // 讀取文件
        if (args.length == 0) {
            System.err.println("請輸入檔案名稱");
            return;
        }
        String mermaidCode = MermaidReader.readMermaidCode(args[0]);
        // System.out.println("File name: " + fileName);
        Parser parser = new Parser(mermaidCode);

        // Call toJava to parse the code and get the result
        Map<String, String> javaCode = parser.toJava();

        // 加上每個class最後的}
        for (Map.Entry<String, String> entry : javaCode.entrySet()) {
            // Get the current value for the entry
            String currentValue = entry.getValue();
            // Append "}" + "\n" to the current value
            String newValue = currentValue + "}" + "\n";
            // Update the entry's value with the new value
            entry.setValue(newValue);
        }

        // 寫入文件
        for (Map.Entry<String, String> entry : javaCode.entrySet()) {
            String key = entry.getKey();
            String content = entry.getValue();
            String output = key + ".java"; // File name is the key with .java extension

            try {
                File file = new File(output);
                if (!file.exists()) {
                    file.createNewFile();
                }
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                    bw.write(content);
                }
                System.out.println("Java class has been generated: " + output);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}