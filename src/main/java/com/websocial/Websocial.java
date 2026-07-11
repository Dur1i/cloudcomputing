/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.websocial;

/**
 *
 * @author lhtdu
 */

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Websocial {

    public static void main(String[] args) {
        // Lệnh này giúp khởi động server Tomcat và ứng dụng Spring Boot
        SpringApplication.run(Websocial.class, args);
    }
}