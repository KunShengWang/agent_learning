package com.agent.demo3;

/**
 * 使用for循环计算1到50的和
 */
public class Sum1To50 {

    public static void main(String[] args) {
        int sum = 0;

        // 使用for循环累加1到50
        for (int i = 1; i <= 50; i++) {
            sum += i;
        }

        System.out.println("1到50的和为: " + sum);
    }
}
