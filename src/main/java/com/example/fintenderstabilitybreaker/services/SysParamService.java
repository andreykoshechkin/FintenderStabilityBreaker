package com.example.fintenderstabilitybreaker.services;

public class SysParamService {

    public static boolean isEnabled(){
        return true;
    }

    //Псеводкод. На практике, происходит атомарное обновление через бд. Пример: set status where <> status;
    public static boolean changeIgnoreFns(boolean b){
        return true;
    }
}
