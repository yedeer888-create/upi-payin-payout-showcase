package com.tripartite.core.net.phonepe;



import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java Unicode编码解码工具类
 * 支持字符与Unicode转义序列之间的双向转换
 */
public class UnicodeConverter {

    /**
     * 将普通字符串编码为Unicode转义序列
     * @param text 原始字符串
     * @return 包含Unicode转义序列的字符串
     */
    public static String encodeToUnicode(String text) {
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c <= 127) {
                // ASCII字符直接保留
                encoded.append(c);
            } else {
                // 非ASCII字符转换为Unicode转义序列
                encoded.append(String.format("\\u%04x", (int) c));
            }
        }
        return encoded.toString();
    }

    /**
     * 将Unicode转义序列解码为普通字符串
     * @param unicodeString 包含Unicode转义序列的字符串
     * @return 解码后的普通字符串
     */
    public static String decodeFromUnicode(String unicodeString) {
        Pattern pattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        Matcher matcher = pattern.matcher(unicodeString);
        StringBuffer decoded = new StringBuffer();

        while (matcher.find()) {
            String hexCode = matcher.group(1);
            int codePoint = Integer.parseInt(hexCode, 16);
            matcher.appendReplacement(decoded, String.valueOf((char) codePoint));
        }
        matcher.appendTail(decoded);

        return decoded.toString();
    }

    /**
     * 将单个字符编码为Unicode转义序列
     * @param c 要编码的字符
     * @return Unicode转义序列字符串
     */
    public static String encodeChar(char c) {
        return String.format("\\u%04x", (int) c);
    }

    /**
     * 主方法 - 演示编码和解码功能
     */
    public static void main(String[] args) {
        System.out.println("=== Java Unicode编码解码演示 ===");

        // 演示"=="编码为"\u003d\u003d"
        String originalText = "hq4wOGdzX31IuPyyh7/7AYOLiipO42P8QtgmusudZHta7zUAMbV5uMV5f6kF1hmv78PNhqBXjiAyRjVgU9T7XT5D3BntAsng+S0RuWE9tlEUFpeJTbpxbnpo2MRKnapVEN5MuuVCvo7Y9kBxEE6yuk0CkYjBPLA58jljgI+k4kpLIZIY7WwWWvux/uZQOT1btVPEbS/FJdAckS47HXb6bx1e+QJswwJShPbHQqiC1uvaZmy7meVGwnoqJ1veFPrPOlhHQbI09kU/rJLguhONPEzdAkIx1aG2meBfGz63bzZ4TOL3KotVbaqOx/N9aHqazlS/Ex+Hd9u8hkuzoll7BVOVWW1WpJ5YNqSc48JHsBnRAJpAxbMkkjjV/X7RaNO9XLh4YN24PM97capottnGSVsUI70Exxp9+apGhBXnNu2rRefowBFu+HbL3srVsvMaAnw5DeX8+tKmOP61qsTo+4JfFpbde+fSqa7wpN1UzI7fhvNxcQcLw1YhILN5ezF1WV3t1FsXaI12G+ikg1zAeszcysGKQ95S1amx734YbsbbpTvjZiJaSBlfAjojeP7yQ32uZ1doirZV3OqMLm9iWcnJZ8bRuOCZ5DAQJxNcs9Kyk9GnFdTvJlBvQ5U5zFisUhZji1QWgV91GEGi7mR2Apzuovo1vuaTXSvOozaojEzDcbFA/vpY2dyq1t3Z5uItUNy3EmLPjuPqw3GHqtb8OJFRlChOrU88BN7nT7EY/zfIsiVqhVLWsYYkm2oGqEmhCngE0FAroaZfMBQO0tPoeNK2fLjuQo+9CjRGKrg0PQckQlAhMoRsNw==";
        String encodedText = encodeToUnicode(originalText);
        System.out.println("原始文本: " + originalText);
        System.out.println("编码结果: " + encodedText);

        // 演示Unicode转义序列解码
        String unicodeInput = "\\u003d\\u003d";
        String decodedText = decodeFromUnicode(unicodeInput);
        System.out.println("Unicode输入: " + unicodeInput);
        System.out.println("解码结果: " + decodedText);

        // 验证编码解码的正确性
        boolean verification = originalText.equals(decodedText);
        System.out.println("编码解码验证: " + verification);

        // 常用特殊字符编码参考
        System.out.println("\n=== 常用字符编码参考 ===");
        char[] specialChars = {'=', '>', '<', '&', '"', '\'', '\\'};
        for (char c : specialChars) {
            String encoded = encodeChar(c);
            System.out.println("字符 '" + c + "' -> " + encoded);
        }
    }
    }