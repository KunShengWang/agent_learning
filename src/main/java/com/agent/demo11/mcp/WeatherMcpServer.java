package com.agent.demo11.mcp;

import com.agent.demo11.framework.JsonUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class WeatherMcpServer {

    private static final Map<String, WeatherInfo> WEATHER_DATA = new LinkedHashMap<>();

    static {
        WEATHER_DATA.put("北京", new WeatherInfo("晴", "28°C", "东北风 2 级", "适合外出，但中午注意防晒。"));
        WEATHER_DATA.put("上海", new WeatherInfo("多云", "26°C", "东南风 3 级", "体感舒适，适合通勤和散步。"));
        WEATHER_DATA.put("杭州", new WeatherInfo("小雨", "24°C", "西南风 2 级", "建议带伞，路面湿滑注意安全。"));
        WEATHER_DATA.put("深圳", new WeatherInfo("雷阵雨", "30°C", "南风 3 级", "注意短时强降雨，尽量避开户外长时间停留。"));
    }

    public static void main(String[] args) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String method = JsonUtil.stringField(line, "method");
                if ("notifications/initialized".equals(method)) {
                    continue;
                }

                String id = JsonUtil.rawField(line, "id");
                if (id == null) {
                    id = "null";
                }

                String response = switch (method) {
                    case "initialize" -> response(id, initializeResult());
                    case "tools/list" -> response(id, toolsListResult());
                    case "tools/call" -> response(id, callToolResult(line));
                    default -> error(id, -32601, "Unknown method: " + method);
                };

                writer.write(response);
                writer.newLine();
                writer.flush();
            }
        }
    }

    private static String initializeResult() {
        return "{"
                + "\"protocolVersion\":\"2024-11-05\","
                + "\"capabilities\":{\"tools\":{}},"
                + "\"serverInfo\":{\"name\":\"weather-server\",\"version\":\"1.0\"}"
                + "}";
    }

    private static String toolsListResult() {
        return "{"
                + "\"tools\":["
                + "{"
                + "\"name\":\"query_weather\","
                + "\"description\":\"查询指定城市的天气。\","
                + "\"inputSchema\":{"
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "\"city\":{\"type\":\"string\",\"description\":\"城市名称，例如杭州。\"}"
                + "},"
                + "\"required\":[\"city\"]"
                + "}"
                + "},"
                + "{"
                + "\"name\":\"list_supported_cities\","
                + "\"description\":\"列出当前天气 MCP Server 支持查询的城市。\","
                + "\"inputSchema\":{"
                + "\"type\":\"object\","
                + "\"properties\":{},"
                + "\"required\":[]"
                + "}"
                + "}"
                + "]"
                + "}";
    }

    private static String callToolResult(String requestJson) {
        String params = JsonUtil.objectField(requestJson, "params");
        String name = JsonUtil.stringField(params, "name");
        String arguments = JsonUtil.objectField(params, "arguments");

        String text = switch (name) {
            case "query_weather" -> queryWeather(JsonUtil.stringField(arguments, "city"));
            case "list_supported_cities" -> listSupportedCities();
            default -> "未知天气工具：" + name;
        };

        boolean isError = name == null || (!"query_weather".equals(name) && !"list_supported_cities".equals(name));
        return "{"
                + "\"content\":[{\"type\":\"text\",\"text\":\"" + JsonUtil.escape(text) + "\"}],"
                + "\"isError\":" + isError
                + "}";
    }

    private static String queryWeather(String city) {
        String normalizedCity = city == null ? "" : city.strip();
        WeatherInfo info = WEATHER_DATA.get(normalizedCity);
        if (info == null) {
            return "暂时没有 " + normalizedCity + " 的天气数据。当前支持的城市：" + supportedCitiesText() + "。";
        }

        return normalizedCity
                + "天气：" + info.weather()
                + "，气温：" + info.temperature()
                + "，风力：" + info.wind()
                + "。建议：" + info.suggestion();
    }

    private static String listSupportedCities() {
        return "当前支持查询的城市：" + supportedCitiesText();
    }

    private static String supportedCitiesText() {
        return String.join("、", WEATHER_DATA.keySet());
    }

    private static String response(String id, String resultJson) {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":" + resultJson + "}";
    }

    private static String error(String id, int code, String message) {
        return "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"id\":" + id + ","
                + "\"error\":{"
                + "\"code\":" + code + ","
                + "\"message\":\"" + JsonUtil.escape(message) + "\""
                + "}"
                + "}";
    }

    private record WeatherInfo(
            String weather,
            String temperature,
            String wind,
            String suggestion
    ) {
    }
}
